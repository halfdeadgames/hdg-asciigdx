package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CandidateMergeTest {

    @Test
    void keepsOnlyTheFirstOfIdenticalCells() {
        int[][] cells = {{1, 2}, {3, 4}, {1, 2}, {5, 6}, {3, 4}};
        assertArrayEquals(new int[] {0, 1, 3}, CandidateMerge.firstOfEach(cells));
    }

    @Test
    void keepsAllCandidatesWhenEveryCellDiffers() {
        assertArrayEquals(new int[] {0, 1}, CandidateMerge.firstOfEach(new int[][] {{1}, {2}}));
    }

    @Test
    void keepsOnlyTheFirstWhenAllCellsMatch() {
        assertArrayEquals(new int[] {0}, CandidateMerge.firstOfEach(new int[][] {{7}, {7}, {7}}));
    }

    @Test
    void handlesNoCandidates() {
        assertEquals(0, CandidateMerge.firstOfEach(new int[0][]).length);
    }
}
