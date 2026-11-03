package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PreviewTabsTest {

    private static final ReviewLayout.Rect RECT = new ReviewLayout.Rect(55, 2, 24, 7);
    private static final ReviewLayout.Rect NARROW = new ReviewLayout.Rect(55, 2, 16, 7);

    @Test
    void labelsSitSideBySideWithAGap() {
        assertEquals(57, PreviewTabs.labelColumn(RECT, PreviewTabs.Tab.SCALES));
        assertEquals(65, PreviewTabs.labelColumn(RECT, PreviewTabs.Tab.TILE));
        assertEquals(71, PreviewTabs.labelColumn(RECT, PreviewTabs.Tab.TEXT));
        assertEquals("SCALES", PreviewTabs.label(RECT, PreviewTabs.Tab.SCALES));
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

    @Test
    void narrowPanelUsesShortFormsThatFitInsideTheBorder() {
        assertEquals("SCL", PreviewTabs.label(NARROW, PreviewTabs.Tab.SCALES));
        assertEquals("TILE", PreviewTabs.label(NARROW, PreviewTabs.Tab.TILE));
        assertEquals("TXT", PreviewTabs.label(NARROW, PreviewTabs.Tab.TEXT));
        assertEquals(57, PreviewTabs.labelColumn(NARROW, PreviewTabs.Tab.SCALES));
        assertEquals(61, PreviewTabs.labelColumn(NARROW, PreviewTabs.Tab.TILE));
        assertEquals(66, PreviewTabs.labelColumn(NARROW, PreviewTabs.Tab.TEXT));
        int lastCell = PreviewTabs.labelColumn(NARROW, PreviewTabs.Tab.TEXT)
            + PreviewTabs.label(NARROW, PreviewTabs.Tab.TEXT).length() - 1;
        assertTrue(lastCell < NARROW.x() + NARROW.width() - 1);
    }

    @Test
    void narrowPanelClicksHitTheRightTab() {
        int row = PreviewTabs.labelRow(NARROW);
        assertEquals(PreviewTabs.Tab.SCALES, PreviewTabs.tabAt(NARROW, 57, row));
        assertEquals(PreviewTabs.Tab.SCALES, PreviewTabs.tabAt(NARROW, 59, row));
        assertNull(PreviewTabs.tabAt(NARROW, 60, row));
        assertEquals(PreviewTabs.Tab.TILE, PreviewTabs.tabAt(NARROW, 61, row));
        assertEquals(PreviewTabs.Tab.TILE, PreviewTabs.tabAt(NARROW, 64, row));
        assertNull(PreviewTabs.tabAt(NARROW, 65, row));
        assertEquals(PreviewTabs.Tab.TEXT, PreviewTabs.tabAt(NARROW, 66, row));
        assertEquals(PreviewTabs.Tab.TEXT, PreviewTabs.tabAt(NARROW, 68, row));
        assertNull(PreviewTabs.tabAt(NARROW, 69, row));
    }

    @Test
    void midWidthKeepsFullNamesWithATighterGap() {
        ReviewLayout.Rect mid = new ReviewLayout.Rect(0, 0, 19, 7);
        assertEquals("SCALES", PreviewTabs.label(mid, PreviewTabs.Tab.SCALES));
        assertEquals(9, PreviewTabs.labelColumn(mid, PreviewTabs.Tab.TILE));
        assertEquals(14, PreviewTabs.labelColumn(mid, PreviewTabs.Tab.TEXT));
    }
}
