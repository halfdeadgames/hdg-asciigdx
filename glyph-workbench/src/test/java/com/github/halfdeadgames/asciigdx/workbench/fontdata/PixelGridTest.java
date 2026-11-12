package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PixelGridTest {

    private final PixelGrid grid = PixelGrid.ofRows(List.of("#.", ".#", "##"));

    @Test
    void isLitWithinReportsInsidePixels() {
        assertTrue(grid.isLitWithin(0, 0));
        assertFalse(grid.isLitWithin(1, 0));
        assertTrue(grid.isLitWithin(1, 2));
    }

    @Test
    void isLitWithinIsFalseOutsideOnEachSide() {
        assertFalse(grid.isLitWithin(-1, 0));
        assertFalse(grid.isLitWithin(2, 0));
        assertFalse(grid.isLitWithin(0, -1));
        assertFalse(grid.isLitWithin(0, 3));
    }
}
