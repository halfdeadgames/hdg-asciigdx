package com.github.halfdeadgames.asciigdx.workbench.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides.ListedKind;
import org.junit.jupiter.api.Test;

class GuideBandsTest {

    @Test
    void barBandCoversItsRowsFromTheGridTop() {
        assertEquals(100 + 2 * 8, GuideBands.bandStart(100, 2, 8));
        assertEquals(3 * 8, GuideBands.bandLength(3, 8));
    }

    @Test
    void columnBarBandCoversItsColumnsFromTheGridLeft() {
        assertEquals(40 + 5 * 6, GuideBands.bandStart(40, 5, 6));
        assertEquals(2 * 6, GuideBands.bandLength(2, 6));
    }

    @Test
    void shownBandMatchesTheSelectedGuideOnlyWhenEverythingAgrees() {
        PaintGuides.Listed selected = new PaintGuides.Listed(ListedKind.BAR, "x-height", 3, 4, "lower");
        assertTrue(GuideBands.isSelected(selected, ListedKind.BAR, "x-height", 3, 4));
        assertFalse(GuideBands.isSelected(selected, ListedKind.COLUMN_BAR, "x-height", 3, 4));
        assertFalse(GuideBands.isSelected(selected, ListedKind.BAR, "other", 3, 4));
        assertFalse(GuideBands.isSelected(selected, ListedKind.BAR, "x-height", 2, 4));
        assertFalse(GuideBands.isSelected(selected, ListedKind.BAR, "x-height", 3, 3));
    }

    @Test
    void nothingSelectedMatchesNoBand() {
        assertFalse(GuideBands.isSelected(null, ListedKind.BAR, "x-height", 3, 4));
    }
}
