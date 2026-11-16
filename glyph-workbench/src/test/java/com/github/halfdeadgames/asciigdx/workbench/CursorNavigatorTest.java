package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CursorNavigatorTest {

    // 4 columns x 3 rows, X marks a blank cell.
    private static final String BLANK =
            "...."
            + ".XX."
            + "X..X";

    private static boolean[] blank(String pattern) {
        boolean[] cells = new boolean[pattern.length()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = pattern.charAt(i) == 'X';
        }
        return cells;
    }

    @Test
    void movesToAdjacentCell() {
        assertEquals(1, CursorNavigator.move(blank(BLANK), 4, 3, 0, 1, 0));
        assertEquals(4, CursorNavigator.move(blank(BLANK), 4, 3, 0, 0, 1));
    }

    @Test
    void wrapsAtEdges() {
        assertEquals(3, CursorNavigator.move(blank(BLANK), 4, 3, 0, -1, 0));
        assertEquals(0, CursorNavigator.move(blank(BLANK), 4, 3, 3, 1, 0));
        assertEquals(4, CursorNavigator.move(blank(BLANK), 4, 3, 0, 0, -1));
        assertEquals(1, CursorNavigator.move(blank(BLANK), 4, 3, 9, 0, 1));
    }

    @Test
    void wrapsPastBlankCells() {
        assertEquals(10, CursorNavigator.move(blank(BLANK), 4, 3, 9, -1, 0));
        assertEquals(9, CursorNavigator.move(blank(BLANK), 4, 3, 10, 1, 0));
    }

    @Test
    void jumpsOverBlankCellsHorizontally() {
        assertEquals(7, CursorNavigator.move(blank(BLANK), 4, 3, 4, 1, 0));
        assertEquals(4, CursorNavigator.move(blank(BLANK), 4, 3, 7, -1, 0));
    }

    @Test
    void jumpsOverBlankCellsVertically() {
        assertEquals(9, CursorNavigator.move(blank(BLANK), 4, 3, 1, 0, 1));
        assertEquals(1, CursorNavigator.move(blank(BLANK), 4, 3, 9, 0, -1));
    }

    @Test
    void staysPutWhenTheWholeLineIsBlank() {
        boolean[] lonely = {
                false, true, true,
                true, true, true,
                true, true, true
        };
        assertEquals(0, CursorNavigator.move(lonely, 3, 3, 0, 1, 0));
        assertEquals(0, CursorNavigator.move(lonely, 3, 3, 0, -1, 0));
        assertEquals(0, CursorNavigator.move(lonely, 3, 3, 0, 0, 1));
        assertEquals(0, CursorNavigator.move(lonely, 3, 3, 0, 0, -1));
    }

    @Test
    void findsFirstNonBlank() {
        assertEquals(0, CursorNavigator.firstNonBlank(blank(BLANK)));
        assertEquals(2, CursorNavigator.firstNonBlank(new boolean[]{true, true, false}));
        assertEquals(-1, CursorNavigator.firstNonBlank(new boolean[]{true, true}));
    }
}
