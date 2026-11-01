package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VerdictMenuTest {

    @Test
    void perfectCompletesInOneStep() {
        VerdictMenu menu = new VerdictMenu();
        menu.open(false);
        assertEquals(2, menu.rowCount());
        assertTrue(menu.choose(0));
        assertEquals("perfect", menu.verdict());
        assertEquals("", menu.reason());
        assertFalse(menu.isOpen());
    }

    @Test
    void needsWorkAsksForAReason() {
        VerdictMenu menu = new VerdictMenu();
        menu.open(false);
        assertFalse(menu.choose(1));
        assertEquals(VerdictMenu.Stage.REASON, menu.stage());
        assertEquals(8, menu.rowCount());
        assertTrue(menu.choose(1));
        assertEquals("needs-work", menu.verdict());
        assertEquals("off-center", menu.reason());
    }

    @Test
    void noneSkipsStraightToReasons() {
        VerdictMenu menu = new VerdictMenu();
        menu.open(true);
        assertEquals(VerdictMenu.Stage.REASON, menu.stage());
        assertTrue(menu.choose(7));
        assertEquals("needs-hand-drawn", menu.verdict());
        assertEquals("", menu.reason());
    }

    @Test
    void focusWrapsAndIgnoresBadRows() {
        VerdictMenu menu = new VerdictMenu();
        menu.open(false);
        menu.moveFocus(-1);
        assertEquals(1, menu.focus());
        menu.moveFocus(1);
        assertEquals(0, menu.focus());
        menu.setFocus(5);
        assertEquals(0, menu.focus());
        assertFalse(menu.choose(9));
        assertTrue(menu.isOpen());
    }

    @Test
    void rowAtMapsPixelsToRows() {
        assertEquals(0, VerdictMenu.rowAt(110, 200, 100, 200, 300, 24, 3));
        assertEquals(2, VerdictMenu.rowAt(110, 200 + 48, 100, 200, 300, 24, 3));
        assertEquals(-1, VerdictMenu.rowAt(110, 200 + 72, 100, 200, 300, 24, 3));
        assertEquals(-1, VerdictMenu.rowAt(99, 210, 100, 200, 300, 24, 3));
        assertEquals(-1, VerdictMenu.rowAt(400, 210, 100, 200, 300, 24, 3));
        assertEquals(-1, VerdictMenu.rowAt(110, 150, 100, 200, 300, 24, 3));
    }
}
