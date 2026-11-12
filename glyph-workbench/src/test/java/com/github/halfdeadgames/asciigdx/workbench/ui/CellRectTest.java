package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellRectTest {

    private final CellRect rect = new CellRect(2, 3, 4, 5);

    @Test
    void containsCellInside() {
        assertTrue(rect.contains(4, 5));
    }

    @Test
    void leftAndTopEdgesAreInclusive() {
        assertTrue(rect.contains(2, 5));
        assertTrue(rect.contains(4, 3));
        assertFalse(rect.contains(1, 5));
        assertFalse(rect.contains(4, 2));
    }

    @Test
    void rightAndBottomEdgesAreExclusive() {
        assertTrue(rect.contains(5, 5));
        assertTrue(rect.contains(4, 7));
        assertFalse(rect.contains(6, 5));
        assertFalse(rect.contains(4, 8));
    }
}
