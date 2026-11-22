package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.ui.SelectBoxPlacement.Span;
import org.junit.jupiter.api.Test;

class SelectBoxPlacementTest {
    private static final int SURFACE = 20;

    @Test
    void opensDownwardWhenAllWantedRowsFitBelow() {
        assertEquals(new Span(6, 5), SelectBoxPlacement.placeVertically(5, 5, SURFACE));
    }

    @Test
    void opensDownwardWhenTheRowsFitExactlyToTheSurfaceEdge() {
        assertEquals(new Span(16, 4), SelectBoxPlacement.placeVertically(15, 4, SURFACE));
    }

    @Test
    void opensUpwardWhenAboveHasMoreRoomAndTheRowsFitThere() {
        assertEquals(new Span(10, 5), SelectBoxPlacement.placeVertically(15, 5, SURFACE));
    }

    @Test
    void shrinksToTheRoomAboveWhenThatSideIsLarger() {
        assertEquals(new Span(0, 17), SelectBoxPlacement.placeVertically(17, 30, SURFACE));
    }

    @Test
    void shrinksToTheRoomBelowWhenThatSideIsLarger() {
        assertEquals(new Span(4, 16), SelectBoxPlacement.placeVertically(3, 30, SURFACE));
    }

    @Test
    void prefersBelowWhenBothSidesHaveEqualRoom() {
        assertEquals(new Span(10, 9), SelectBoxPlacement.placeVertically(9, 30, 19));
    }

    @Test
    void keepsAtLeastOneRow() {
        assertEquals(1, SelectBoxPlacement.placeVertically(0, 3, 1).length());
    }

    @Test
    void aBoxAboveTheSurfaceStillGetsAListOnTheSurface() {
        assertEquals(new Span(0, 4), SelectBoxPlacement.placeVertically(-5, 4, SURFACE));
    }

    @Test
    void aBoxBelowTheSurfaceStillGetsAListOnTheSurface() {
        assertEquals(new Span(16, 4), SelectBoxPlacement.placeVertically(25, 4, SURFACE));
        assertEquals(new Span(16, 4),
                SelectBoxPlacement.placeVertically(SURFACE, 4, SURFACE));
    }

    @Test
    void aOneRowSurfaceGetsTheListOnItsOnlyRow() {
        assertEquals(new Span(0, 1), SelectBoxPlacement.placeVertically(0, 3, 1));
    }

    @Test
    void matchButtonUsesExactlyTheBoxWidth() {
        Span span = SelectBoxPlacement.placeHorizontally(4, 8, 30, true,
                SelectBoxListWidth.MATCH_BUTTON, SURFACE);

        assertEquals(new Span(4, 8), span);
    }

    @Test
    void fitEntriesIsAtLeastTheBoxWidth() {
        Span span = SelectBoxPlacement.placeHorizontally(4, 8, 5, false,
                SelectBoxListWidth.FIT_ENTRIES, SURFACE);

        assertEquals(new Span(4, 8), span);
    }

    @Test
    void fitEntriesGrowsToTheEntriesAndTheScrollBarColumn() {
        assertEquals(12, SelectBoxPlacement.placeHorizontally(2, 8, 12, false,
                SelectBoxListWidth.FIT_ENTRIES, SURFACE).length());
        assertEquals(13, SelectBoxPlacement.placeHorizontally(2, 8, 12, true,
                SelectBoxListWidth.FIT_ENTRIES, SURFACE).length());
    }

    @Test
    void fitEntriesShiftsLeftToStayOnTheSurface() {
        Span span = SelectBoxPlacement.placeHorizontally(15, 4, 12, false,
                SelectBoxListWidth.FIT_ENTRIES, SURFACE);

        assertEquals(new Span(8, 12), span);
    }

    @Test
    void fitEntriesNeverExceedsTheSurface() {
        Span span = SelectBoxPlacement.placeHorizontally(6, 4, 50, true,
                SelectBoxListWidth.FIT_ENTRIES, SURFACE);

        assertEquals(new Span(0, SURFACE), span);
    }
}
