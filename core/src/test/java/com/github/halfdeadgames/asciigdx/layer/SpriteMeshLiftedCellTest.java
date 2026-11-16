package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.tommyettinger.colorful.rgb.ColorTools;
import org.junit.jupiter.api.Test;

class SpriteMeshLiftedCellTest {

    private static final int STRIDE = CellVertexLayout.FLOATS_PER_VERTEX;
    private static final int QUAD = CellVertexLayout.FLOATS_PER_QUAD;
    private static final int FOREGROUND = 0xFF8040C0;

    /** Regions whose coordinates encode the code point, so tests can tell glyphs apart. */
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
                    return codePoint / 256f;
                }

                @Override
                public float getV() {
                    return 0.25f;
                }

                @Override
                public float getU2() {
                    return codePoint / 256f + 0.125f;
                }

                @Override
                public float getV2() {
                    return 0.5f;
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

    private final FakeSource source = new FakeSource();
    private final TerminalGrid grid = new TerminalGrid(6, 4);
    private final float[] vertices = new float[QUAD];

    private void write(CellHandle cell) {
        SpriteMesh.writeLiftedCell(vertices, 0, cell, grid, source, true);
    }

    private float[] ownQuadOf(int column, int row) {
        CellVertexCache cache = new CellVertexCache(grid.getColumns(), grid.getRows(), 8f, 12f);
        TextureRegion region = source.getGlyphRegion(grid.getCodePoint(column, row));
        cache.writeCell(column, row, 0, grid.getForeground(column, row),
                region.getU(), region.getV(), region.getU2(), region.getV2());
        float[] own = new float[QUAD];
        System.arraycopy(cache.getVertices(), (row * grid.getColumns() + column) * QUAD, own, 0, QUAD);
        return own;
    }

    private void assertPositionsAndUvsMatch(float[] expected) {
        for (int vertex = 0; vertex < CellVertexLayout.VERTICES_PER_QUAD; vertex++) {
            int base = vertex * STRIDE;
            assertEquals(expected[base + CellVertexLayout.POSITION_OFFSET],
                    vertices[base + CellVertexLayout.POSITION_OFFSET], 0f);
            assertEquals(expected[base + CellVertexLayout.POSITION_OFFSET + 1],
                    vertices[base + CellVertexLayout.POSITION_OFFSET + 1], 0f);
            assertEquals(expected[base + CellVertexLayout.UV_OFFSET], vertices[base + CellVertexLayout.UV_OFFSET], 0f);
            assertEquals(expected[base + CellVertexLayout.UV_OFFSET + 1],
                    vertices[base + CellVertexLayout.UV_OFFSET + 1], 0f);
        }
    }

    @Test
    void identityTransformMatchesTheCellsOwnQuad() {
        grid.set(2, 1, '@', FOREGROUND, 0x00000000);

        write(new CellHandle(2, 1));

        assertPositionsAndUvsMatch(ownQuadOf(2, 1));
        assertEquals(ColorTools.fromRGBA8888(FOREGROUND), vertices[CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
    }

    @Test
    void offsetMovesTheQuadDownAndRight() {
        grid.set(2, 1, '@', FOREGROUND, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setOffset(3f, 5f);

        write(cell);

        float[] own = ownQuadOf(2, 1);
        assertEquals(own[0] + 3f, vertices[0], 0f);
        assertEquals(own[1] - 5f, vertices[1], 0f);
    }

    @Test
    void followsTheGridsCurrentGlyphAndForeground() {
        CellHandle cell = new CellHandle(2, 1);
        grid.set(2, 1, '@', FOREGROUND, 0x00000000);
        write(cell);
        float firstU = vertices[CellVertexLayout.UV_OFFSET];

        grid.set(2, 1, 'g', 0x11223344, 0x00000000);
        write(cell);

        assertTrue(vertices[CellVertexLayout.UV_OFFSET] != firstU);
        assertPositionsAndUvsMatch(ownQuadOf(2, 1));
        assertEquals(ColorTools.fromRGBA8888(0x11223344), vertices[CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
    }

    @Test
    void handleAlphaIsMultipliedIntoTheGlyphAlpha() {
        grid.set(2, 1, '@', 0xFF8040C0, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setAlphaMultiplier(0.5f);

        write(cell);

        int expected = CellColor.withAlphaScaled(0xFF8040C0, 0.5f);
        assertEquals(0xFF8040 << 8 | 0x60, expected);
        assertEquals(ColorTools.fromRGBA8888(expected), vertices[CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
    }

    private static final int TARGET = 0xAABBCC00;

    private float writtenForeground() {
        return vertices[CellVertexLayout.FOREGROUND_COLOR_OFFSET];
    }

    private static Array<CellHandle> arrayOf(CellHandle cell) {
        Array<CellHandle> cells = new Array<>();
        cells.add(cell);
        return cells;
    }

    @Test
    void foregroundMixIsWrittenForALiftedCell() {
        grid.set(2, 1, '@', 0x112233FF, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setOffset(1f, 0f);
        cell.setForegroundMix(TARGET, 1f);

        write(cell);

        assertEquals(ColorTools.fromRGBA8888(0xAABBCCFF), writtenForeground(), 1e-6f);
    }

    @Test
    void codePointOverrideDrawsTheOverrideGlyph() {
        grid.set(2, 1, '@', FOREGROUND, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setOffset(1f, 0f);
        cell.setCodePoint('z');

        write(cell);

        assertEquals('z' / 256f, vertices[CellVertexLayout.UV_OFFSET], 0f);
    }

    @Test
    void overrideMakingABlankCellVisibleIsCountedAndWritten() {
        grid.set(2, 1, ' ', FOREGROUND, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setOffset(1f, 0f);
        cell.setCodePoint('z');

        assertEquals(1, SpriteMesh.countDrawnLifted(arrayOf(cell), grid));
        write(cell);
        assertEquals('z' / 256f, vertices[CellVertexLayout.UV_OFFSET], 0f);
    }

    @Test
    void overrideSpaceHidesALiftedGlyph() {
        grid.set(2, 1, '@', FOREGROUND, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setOffset(1f, 0f);
        cell.setCodePoint(' ');

        assertEquals(0, SpriteMesh.countDrawnLifted(arrayOf(cell), grid));
    }

    @Test
    void detachedHandleOverridesAreIgnored() {
        grid.set(2, 1, '@', 0x112233FF, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setOffset(1f, 0f);
        cell.setCodePoint('z');
        cell.setForegroundMix(TARGET, 1f);
        cell.detach();

        write(cell);

        assertEquals('@' / 256f, vertices[CellVertexLayout.UV_OFFSET], 0f);
        assertEquals(ColorTools.fromRGBA8888(0x112233FF), writtenForeground(), 0f);
    }

    @Test
    void foregroundMixAndAlphaMultiplierBothApply() {
        grid.set(2, 1, '@', 0x112233FF, 0x00000000);
        CellHandle cell = new CellHandle(2, 1);
        cell.setForegroundMix(TARGET, 1f);
        cell.setAlphaMultiplier(0.5f);

        write(cell);

        int expected = CellColor.withAlphaScaled(0xAABBCCFF, 0.5f);
        assertEquals(ColorTools.fromRGBA8888(expected), writtenForeground(), 1e-6f);
    }

    @Test
    void blankCellsDrawNothingAndRestingHandlesAreNotCounted() {
        CellHandle blank = new CellHandle(0, 0);
        CellHandle resting = new CellHandle(1, 0);
        CellHandle drawn = new CellHandle(2, 0);
        blank.setOffset(1f, 1f);
        drawn.setOffset(1f, 1f);
        grid.set(0, 0, ' ', FOREGROUND, 0x00000000);
        grid.set(1, 0, '@', FOREGROUND, 0x00000000);
        grid.set(2, 0, '@', FOREGROUND, 0x00000000);
        Array<CellHandle> cells = new Array<>();
        cells.add(blank);
        cells.add(resting);
        cells.add(drawn);

        assertEquals(1, SpriteMesh.countDrawnLifted(cells, grid));
        assertTrue(SpriteMesh.hasLifted(cells));
    }
}
