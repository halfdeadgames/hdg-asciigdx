package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

/** The UI-side eighth rule must give the same glyphs as the bars of the core module. */
class EighthBlocksTest {

    private static final float[] FRACTIONS =
            {0f, 0.01f, 0.03f, 0.1f, 0.33f, 0.55f, 0.5f, 0.999f, 1f, -1f, 2f};
    private static final int[] LENGTHS = {1, 2, 5, 10};

    @Test
    void horizontalGlyphsMatchTheCoreBar() {
        for (int length : LENGTHS) {
            for (float fraction : FRACTIONS) {
                TerminalGrid grid = new TerminalGrid(length, 1);
                GridDrawing.drawHorizontalBar(grid, 0, 0, length, fraction, 0xFFFFFFFF, 0);
                long total = EighthBlocks.totalEighths(length, fraction);
                for (int index = 0; index < length; index++) {
                    assertEquals(grid.getCodePoint(index, 0),
                            EighthBlocks.glyphForCell(BarOrientation.HORIZONTAL, total, index),
                            "length " + length + ", fraction " + fraction + ", cell " + index);
                }
            }
        }
    }

    @Test
    void verticalGlyphsMatchTheCoreBar() {
        for (int length : LENGTHS) {
            for (float fraction : FRACTIONS) {
                TerminalGrid grid = new TerminalGrid(1, length);
                GridDrawing.drawVerticalBar(grid, 0, 0, length, fraction, 0xFFFFFFFF, 0);
                long total = EighthBlocks.totalEighths(length, fraction);
                for (int index = 0; index < length; index++) {
                    int row = length - 1 - index;
                    assertEquals(grid.getCodePoint(0, row),
                            EighthBlocks.glyphForCell(BarOrientation.VERTICAL, total, index),
                            "length " + length + ", fraction " + fraction + ", cell " + index);
                }
            }
        }
    }

    @Test
    void partialCellsUseTheEighthBlocks() {
        BarOrientation horizontal = BarOrientation.HORIZONTAL;
        BarOrientation vertical = BarOrientation.VERTICAL;

        assertEquals(0x258F, EighthBlocks.glyphForCell(horizontal, 1, 0));
        assertEquals(0x2589, EighthBlocks.glyphForCell(horizontal, 7, 0));
        assertEquals(0x2581, EighthBlocks.glyphForCell(vertical, 1, 0));
        assertEquals(0x2587, EighthBlocks.glyphForCell(vertical, 7, 0));
        assertEquals(EighthBlocks.FULL_GLYPH, EighthBlocks.glyphForCell(vertical, 8, 0));
        assertEquals(EighthBlocks.EMPTY_GLYPH, EighthBlocks.glyphForCell(vertical, 8, 1));
    }
}
