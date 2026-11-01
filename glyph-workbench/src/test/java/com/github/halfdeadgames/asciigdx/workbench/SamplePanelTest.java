package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SamplePanelTest {

    @Test
    void frameSizesAreWholeTextCells() {
        assertEquals(0, SamplePanel.outerWidth() % Painter.TEXT_SIZE);
        for (int words = 0; words <= 4; words++) {
            assertEquals(0, SamplePanel.outerHeight(words) % Painter.TEXT_SIZE);
        }
    }

    @Test
    void frameGrowsWithWordsAndHoldsTheCloud() {
        assertTrue(SamplePanel.outerHeight(4) > SamplePanel.outerHeight(0));
        assertTrue(SamplePanel.outerWidth() >= 9 * 32 + 2 * ColumnPanel.BORDER);
    }
}
