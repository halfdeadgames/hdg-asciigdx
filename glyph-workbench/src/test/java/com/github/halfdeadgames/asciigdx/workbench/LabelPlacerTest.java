package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LabelPlacerTest {

    @Test
    void labelsThatDoNotCollideStayWhereTheyWere() {
        int[] placed = LabelPlacer.place(new int[] {10, 50, 100}, new int[] {24, 24, 24}, 0, 200, 0);
        assertArrayEquals(new int[] {10, 50, 100}, placed);
    }

    @Test
    void touchingLabelsAreNotMoved() {
        int[] placed = LabelPlacer.place(new int[] {10, 34}, new int[] {24, 24}, 0, 200, 0);
        assertArrayEquals(new int[] {10, 34}, placed);
    }

    @Test
    void twoCollidingLabelsSeparateEvenlyAndKeepOrder() {
        int[] placed = LabelPlacer.place(new int[] {100, 110}, new int[] {24, 24}, 0, 400, 0);
        assertArrayEquals(new int[] {93, 117}, placed);
    }

    @Test
    void inputOrderIsKeptInTheResultWhenWantedPositionsAreUnsorted() {
        int[] placed = LabelPlacer.place(new int[] {110, 100}, new int[] {24, 24}, 0, 400, 0);
        assertArrayEquals(new int[] {117, 93}, placed);
    }

    @Test
    void aChainOfThreeMovesAsOneBlockAroundTheirMean() {
        int[] placed = LabelPlacer.place(new int[] {100, 110, 120}, new int[] {24, 24, 24}, 0, 400, 0);
        assertArrayEquals(new int[] {86, 110, 134}, placed);
    }

    @Test
    void gapIsKeptBetweenNeighbours() {
        int[] placed = LabelPlacer.place(new int[] {100, 100}, new int[] {24, 24}, 0, 400, 6);
        assertTrue(placed[1] - placed[0] >= 24 + 6);
    }

    @Test
    void labelsAreClampedToTheBounds() {
        int[] low = LabelPlacer.place(new int[] {-30}, new int[] {24}, 0, 200, 0);
        int[] high = LabelPlacer.place(new int[] {300}, new int[] {24}, 0, 200, 0);
        assertArrayEquals(new int[] {0}, low);
        assertArrayEquals(new int[] {176}, high);
    }

    @Test
    void aCollidingPairAtTheEdgeStaysInsideTheBounds() {
        int[] placed = LabelPlacer.place(new int[] {0, 5}, new int[] {24, 24}, 0, 200, 0);
        assertArrayEquals(new int[] {0, 24}, placed);
    }

    @Test
    void moreLabelsThanFitStackFromTheStartInOrder() {
        int[] placed = LabelPlacer.place(new int[] {30, 20, 10, 0}, new int[] {24, 24, 24, 24}, 0, 60, 0);
        assertArrayEquals(new int[] {72, 48, 24, 0}, placed);
    }

    @Test
    void noLabelsGivesNoPositions() {
        assertArrayEquals(new int[0], LabelPlacer.place(new int[0], new int[0], 0, 100, 0));
    }
}
