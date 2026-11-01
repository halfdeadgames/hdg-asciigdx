package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CursorNavigatorTest {

    // 4 columns x 3 rows, true marks a blank cell:
    //  . . . .
    //  . X X .
    //  X . . X
    private static final boolean[] BLANK = {
        false, false, false, false,
        false, true, true, false,
        true, false, false, true
    };

    @Test
    void movesToAdjacentCell() {
        assertEquals(1, CursorNavigator.move(BLANK, 4, 3, 0, 1, 0));
        assertEquals(4, CursorNavigator.move(BLANK, 4, 3, 0, 0, 1));
    }

    @Test
    void wrapsAtEdges() {
        assertEquals(3, CursorNavigator.move(BLANK, 4, 3, 0, -1, 0));
        assertEquals(0, CursorNavigator.move(BLANK, 4, 3, 3, 1, 0));
        assertEquals(4, CursorNavigator.move(BLANK, 4, 3, 0, 0, -1));
        assertEquals(1, CursorNavigator.move(BLANK, 4, 3, 9, 0, 1));
    }

    @Test
    void wrapsPastBlankCells() {
        assertEquals(10, CursorNavigator.move(BLANK, 4, 3, 9, -1, 0));
        assertEquals(9, CursorNavigator.move(BLANK, 4, 3, 10, 1, 0));
    }

    @Test
    void jumpsOverBlankCellsHorizontally() {
        assertEquals(7, CursorNavigator.move(BLANK, 4, 3, 4, 1, 0));
        assertEquals(4, CursorNavigator.move(BLANK, 4, 3, 7, -1, 0));
    }

    @Test
    void jumpsOverBlankCellsVertically() {
        assertEquals(9, CursorNavigator.move(BLANK, 4, 3, 1, 0, 1));
        assertEquals(1, CursorNavigator.move(BLANK, 4, 3, 9, 0, -1));
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
        assertEquals(0, CursorNavigator.firstNonBlank(BLANK));
        assertEquals(2, CursorNavigator.firstNonBlank(new boolean[] {true, true, false}));
        assertEquals(-1, CursorNavigator.firstNonBlank(new boolean[] {true, true}));
    }
}
