package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.tommyettinger.colorful.rgb.ColorTools;
import org.junit.jupiter.api.Test;

class FlatCellWriterTest {

    private static final float EPSILON = 1e-6f;
    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final int GRID_FOREGROUND = 0x112233FF;
    private static final int GRID_BACKGROUND = 0x445566FF;
    private static final int TARGET = 0xAABBCC00;

    /** Gives each code point its own u so a test can tell which glyph was written. */
    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 12;
        }

        @Override
        public TextureRegion getGlyphRegion(int codePoint) {
            return new TextureRegion() {
                @Override
                public float getU() {
                    return codePoint;
                }

                @Override
                public float getV() {
                    return 0f;
                }

                @Override
                public float getU2() {
                    return codePoint + 1f;
                }

                @Override
                public float getV2() {
                    return 1f;
                }
            };
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return true;
        }

        @Override
        public void dispose() {
        }
    }

    private final TerminalGrid grid = new TerminalGrid(COLUMNS, ROWS);
    private final CellVertexCache cache = new CellVertexCache(COLUMNS, ROWS, 8f, 12f);
    private final FlatCellWriter writer = new FlatCellWriter(grid, cache, new FakeSource());
    private final CellHandle handle = new CellHandle(2, 1);

    private float vertexValue(int column, int row, int offset) {
        int start = (row * COLUMNS + column) * CellVertexLayout.FLOATS_PER_QUAD;
        return cache.getVertices()[start + offset];
    }

    private float backgroundOf(int column, int row) {
        return vertexValue(column, row, CellVertexLayout.BACKGROUND_COLOR_OFFSET);
    }

    private float foregroundOf(int column, int row) {
        return vertexValue(column, row, CellVertexLayout.FOREGROUND_COLOR_OFFSET);
    }

    private float uOf(int column, int row) {
        return vertexValue(column, row, CellVertexLayout.UV_OFFSET);
    }

    private void putGridCell(int codePoint) {
        grid.set(2, 1, codePoint, GRID_FOREGROUND, GRID_BACKGROUND);
    }

    @Test
    void cellWithoutHandleWritesTheGridValues() {
        putGridCell('a');

        writer.write(2, 1, false, null);

        assertEquals(ColorTools.fromRGBA8888(GRID_BACKGROUND), backgroundOf(2, 1), EPSILON);
        assertEquals(ColorTools.fromRGBA8888(GRID_FOREGROUND), foregroundOf(2, 1), EPSILON);
        assertEquals('a', uOf(2, 1), EPSILON);
    }

    @Test
    void overridesWriteTheResolvedColorsAndGlyph() {
        putGridCell('a');
        handle.setForegroundMix(TARGET, 1f);
        handle.setBackgroundMix(TARGET, 1f);
        handle.setCodePoint('z');

        writer.write(2, 1, false, handle);

        assertEquals(ColorTools.fromRGBA8888(0xAABBCCFF), backgroundOf(2, 1), EPSILON);
        assertEquals(ColorTools.fromRGBA8888(0xAABBCCFF), foregroundOf(2, 1), EPSILON);
        assertEquals('z', uOf(2, 1), EPSILON);
    }

    @Test
    void foregroundAlphaWritesTheFadedForegroundWithoutLifting() {
        putGridCell('a');
        handle.setForegroundAlpha(0.5f);

        writer.write(2, 1, false, handle);

        assertEquals(ColorTools.fromRGBA8888(0x11223380), foregroundOf(2, 1), EPSILON);
        assertEquals(ColorTools.fromRGBA8888(GRID_BACKGROUND), backgroundOf(2, 1), EPSILON);
    }

    @Test
    void backgroundAlphaFadesTheBackgroundLeftByAHiddenGlyph() {
        putGridCell('a');
        handle.setBackgroundAlpha(0.5f);

        writer.write(2, 1, true, handle);

        assertEquals(ColorTools.fromRGBA8888(0x44556680), backgroundOf(2, 1), EPSILON);
        assertEquals(1, cache.getNotOpaqueCellCount());
    }

    @Test
    void cellAlphaIsWrittenForTheGlyphAndForAHiddenGlyphsBackground() {
        putGridCell('a');
        handle.setCellAlpha(0.5f);

        writer.write(2, 1, false, handle);
        assertEquals(0.5f, vertexValue(2, 1, CellVertexLayout.CELL_ALPHA_OFFSET), EPSILON);

        writer.write(2, 1, true, handle);
        assertEquals(0.5f, vertexValue(2, 1, CellVertexLayout.CELL_ALPHA_OFFSET), EPSILON);
        assertEquals(1, cache.getNotOpaqueCellCount());
    }

    @Test
    void cellAlphaIsOneWithoutAHandleOrWithADetachedOne() {
        putGridCell('a');
        handle.setCellAlpha(0f);
        handle.detach();

        writer.write(2, 1, false, handle);
        assertEquals(1f, vertexValue(2, 1, CellVertexLayout.CELL_ALPHA_OFFSET), EPSILON);

        writer.write(2, 1, false, null);
        assertEquals(1f, vertexValue(2, 1, CellVertexLayout.CELL_ALPHA_OFFSET), EPSILON);
        assertEquals(1, cache.getVisibleCellCount());
    }

    @Test
    void overrideGlyphMakesABlankGridCellVisible() {
        grid.set(2, 1, ' ', GRID_FOREGROUND, TerminalGrid.TRANSPARENT_RGBA);
        handle.setCodePoint('z');

        writer.write(2, 1, false, handle);

        assertEquals(1, cache.getVisibleCellCount());
        assertEquals('z', uOf(2, 1), EPSILON);
    }

    @Test
    void overrideSpaceHidesTheGridGlyph() {
        grid.set(2, 1, 'a', GRID_FOREGROUND, TerminalGrid.TRANSPARENT_RGBA);
        handle.setCodePoint(' ');

        writer.write(2, 1, false, handle);

        assertEquals(0, cache.getVisibleCellCount());
    }

    @Test
    void hiddenGlyphKeepsTheOverriddenBackgroundOnly() {
        putGridCell('a');
        handle.setBackgroundMix(TARGET, 1f);
        handle.setCodePoint('z');

        writer.write(2, 1, true, handle);

        assertEquals(ColorTools.fromRGBA8888(0xAABBCCFF), backgroundOf(2, 1), EPSILON);
        assertEquals(0f, foregroundOf(2, 1), EPSILON);
        assertTrue(cache.getVisibleCellCount() == 1);
    }

    @Test
    void detachedHandleWritesTheGridValues() {
        putGridCell('a');
        handle.setCodePoint('z');
        handle.setBackgroundMix(TARGET, 1f);
        handle.detach();

        writer.write(2, 1, false, handle);

        assertEquals(ColorTools.fromRGBA8888(GRID_BACKGROUND), backgroundOf(2, 1), EPSILON);
        assertEquals('a', uOf(2, 1), EPSILON);
    }
}
