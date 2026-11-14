package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

/** Overrides must look identical whether the cell is drawn flat or lifted. */
class FlatAndLiftedCellParityTest {

    private static final int COLUMN = 2;
    private static final int ROW = 1;
    private static final int TARGET = 0xAABBCC00;

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
        public TextureRegion glyph(int codePoint) {
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

    private final FakeSource source = new FakeSource();
    private final TerminalGrid grid = new TerminalGrid(4, 3);
    private final CellVertexCache cache = new CellVertexCache(4, 3, 8f, 12f);
    private final float[] liftedVertices = new float[CellVertexLayout.FLOATS_PER_QUAD];
    private final CellHandle handle = new CellHandle(COLUMN, ROW);

    private void assertFlatEqualsLifted(int gridCodePoint, int gridForeground) {
        grid.set(COLUMN, ROW, gridCodePoint, gridForeground, 0x00000000);
        new FlatCellWriter(grid, cache, source).write(COLUMN, ROW, false, handle);
        SpriteMesh.writeLiftedCell(liftedVertices, 0, handle, grid, source, true);

        int flatStart = (ROW * 4 + COLUMN) * CellVertexLayout.FLOATS_PER_QUAD;
        float[] flat = cache.getVertices();
        assertEquals(flat[flatStart + CellVertexLayout.FOREGROUND_COLOR_OFFSET],
                liftedVertices[CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
        assertEquals(flat[flatStart + CellVertexLayout.UV_OFFSET],
                liftedVertices[CellVertexLayout.UV_OFFSET], 0f);
    }

    @Test
    void foregroundMixMatches() {
        handle.setForegroundMix(TARGET, 0.4f);

        assertFlatEqualsLifted('a', 0x112233FF);
    }

    @Test
    void codePointOverrideMatches() {
        handle.setCodePoint('z');

        assertFlatEqualsLifted('a', 0x112233FF);
    }

    @Test
    void combinedOverridesMatchOnAGridCellWithPartialAlpha() {
        handle.setForegroundMix(TARGET, 0.75f);
        handle.setCodePoint('q');

        assertFlatEqualsLifted('a', 0x11223380);
    }

    @Test
    void overrideOnABlankGridCellMatches() {
        handle.setCodePoint('z');
        handle.setForegroundMix(TARGET, 1f);

        assertFlatEqualsLifted(' ', 0x112233FF);
    }
}
