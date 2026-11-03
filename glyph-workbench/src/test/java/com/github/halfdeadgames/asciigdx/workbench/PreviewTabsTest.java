package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PreviewTabsTest {

    private static final ReviewLayout.Rect RECT = new ReviewLayout.Rect(55, 2, 24, 7);

    @Test
    void labelsSitSideBySideWithAGap() {
        assertEquals(57, PreviewTabs.labelColumn(55, PreviewTabs.Tab.SCALES));
        assertEquals(65, PreviewTabs.labelColumn(55, PreviewTabs.Tab.TILE));
        assertEquals(71, PreviewTabs.labelColumn(55, PreviewTabs.Tab.TEXT));
    }

    @Test
    void cellsOnALabelPickThatTab() {
        int row = PreviewTabs.labelRow(RECT);
        assertEquals(PreviewTabs.Tab.SCALES, PreviewTabs.tabAt(RECT, 57, row));
        assertEquals(PreviewTabs.Tab.SCALES, PreviewTabs.tabAt(RECT, 62, row));
        assertEquals(PreviewTabs.Tab.TILE, PreviewTabs.tabAt(RECT, 65, row));
        assertEquals(PreviewTabs.Tab.TEXT, PreviewTabs.tabAt(RECT, 74, row));
    }

    @Test
    void gapsAndOutsideCellsPickNothing() {
        int row = PreviewTabs.labelRow(RECT);
        assertNull(PreviewTabs.tabAt(RECT, 56, row));
        assertNull(PreviewTabs.tabAt(RECT, 63, row));
        assertNull(PreviewTabs.tabAt(RECT, 75, row));
        assertNull(PreviewTabs.tabAt(RECT, 57, row - 1));
        assertNull(PreviewTabs.tabAt(RECT, 57, row + 1));
    }
}
