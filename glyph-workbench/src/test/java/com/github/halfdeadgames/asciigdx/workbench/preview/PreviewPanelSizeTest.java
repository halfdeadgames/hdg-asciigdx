package com.github.halfdeadgames.asciigdx.workbench.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PreviewPanelSizeTest {

    private static final int[] OTHER_CELL_SIZES = {8, 16, 32};

    @Test
    void heightGrowsWithTheGlyphHeight() {
        assertEquals(8, PreviewPanelSize.height(8, 8, 24));
        assertTrue(PreviewPanelSize.height(32, 8, 24) > PreviewPanelSize.height(16, 8, 24));
        // tab row, gap, label row, and the 8x block must fit inside the borders
        for (int height : new int[] {8, 16, 24, 32, 40}) {
            int content = 24 + 12 + 24 + 8 * height;
            assertTrue((PreviewPanelSize.height(height, 8, 24) - 2) * 24 >= content);
        }
    }

    @Test
    void heightFitsItsContentAtEveryCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            for (int height : new int[] {8, 16, 32}) {
                int content = cell + cell / 2 + cell + 8 * height;
                int rows = PreviewPanelSize.height(height, 8, cell);
                assertTrue((rows - 2) * cell >= content);
                assertTrue((rows - 3) * cell < content);
            }
        }
    }

    @Test
    void blockBudgetAndInnerWidthScaleWithTheCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            assertEquals(21 * cell, PreviewPanelSize.innerWidth(24, cell));
            int rows = PreviewPanelSize.height(8, 8, cell);
            assertTrue(PreviewPanelSize.blockBudget(rows, cell) >= 8 * 8);
            assertTrue(PreviewPanelSize.blockBudget(rows - 1, cell) < 8 * 8);
        }
    }
}
