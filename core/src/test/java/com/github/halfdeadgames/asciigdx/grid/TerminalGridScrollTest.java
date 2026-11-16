package com.github.halfdeadgames.asciigdx.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerminalGridScrollTest {

    private static final int WIDTH = 4;
    private static final int HEIGHT = 3;

    private static TerminalGrid filledGrid(int columns, int rows) {
        var grid = new TerminalGrid(columns, rows);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int id = y * columns + x + 1;
                grid.set(x, y, 100 + id, 0x1000 + id, 0x2000 + id);
            }
        }
        return grid;
    }

    private static void assertSameContent(TerminalGrid expected, TerminalGrid actual) {
        assertEquals(expected.getColumns(), actual.getColumns());
        assertEquals(expected.getRows(), actual.getRows());
        for (int y = 0; y < expected.getRows(); y++) {
            for (int x = 0; x < expected.getColumns(); x++) {
                assertEquals(expected.getCodePoint(x, y), actual.getCodePoint(x, y),
                        "code point " + x + "," + y);
                assertEquals(expected.getForeground(x, y), actual.getForeground(x, y),
                        "fg " + x + "," + y);
                assertEquals(expected.getBackground(x, y), actual.getBackground(x, y),
                        "bg " + x + "," + y);
            }
        }
    }

    private static void assertMovedOrEmpty(TerminalGrid original, TerminalGrid scrolled, int shiftX,
            int shiftY) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int sourceX = x - shiftX;
                int sourceY = y - shiftY;
                if (original.contains(sourceX, sourceY)) {
                    assertEquals(original.getCodePoint(sourceX, sourceY),
                            scrolled.getCodePoint(x, y));
                    assertEquals(original.getForeground(sourceX, sourceY),
                            scrolled.getForeground(x, y));
                    assertEquals(original.getBackground(sourceX, sourceY),
                            scrolled.getBackground(x, y));
                } else {
                    assertEquals(TerminalGrid.EMPTY_CODE_POINT, scrolled.getCodePoint(x, y));
                    assertEquals(TerminalGrid.TRANSPARENT_RGBA, scrolled.getForeground(x, y));
                    assertEquals(TerminalGrid.TRANSPARENT_RGBA, scrolled.getBackground(x, y));
                }
            }
        }
    }

    private static void assertScrollMovesContent(int shiftX, int shiftY) {
        var original = filledGrid(WIDTH, HEIGHT);
        var scrolled = filledGrid(WIDTH, HEIGHT);
        scrolled.scroll(shiftX, shiftY);
        assertMovedOrEmpty(original, scrolled, shiftX, shiftY);
    }

    @Test
    void scrollRightByOne() {
        assertScrollMovesContent(1, 0);
    }

    @Test
    void scrollLeftByOne() {
        assertScrollMovesContent(-1, 0);
    }

    @Test
    void scrollDownByOne() {
        assertScrollMovesContent(0, 1);
    }

    @Test
    void scrollUpByOne() {
        assertScrollMovesContent(0, -1);
    }

    @Test
    void scrollDiagonal() {
        assertScrollMovesContent(2, -1);
    }

    @Test
    void scrollRightKeepsExactCells() {
        var grid = filledGrid(WIDTH, HEIGHT);
        grid.scroll(1, 0);
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 1));
        assertEquals(105, grid.getCodePoint(1, 1));
        assertEquals(0x1005, grid.getForeground(1, 1));
        assertEquals(0x2005, grid.getBackground(1, 1));
        assertEquals(107, grid.getCodePoint(3, 1));
    }

    @Test
    void zeroShiftLeavesTrackersClean() {
        var grid = filledGrid(WIDTH, HEIGHT);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();
        grid.scroll(0, 0);
        assertFalse(tracker.hasDirtyCells());
    }

    @Test
    void nonzeroShiftMarksAllCellsDirty() {
        var grid = filledGrid(WIDTH, HEIGHT);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();
        grid.scroll(0, 1);
        assertEquals(WIDTH * HEIGHT, tracker.getDirtyCellCount());
    }

    @Test
    void shiftAtLeastColumnCountEmptiesGrid() {
        assertScrollMovesContent(WIDTH, 0);
        assertScrollMovesContent(-WIDTH, 0);
        assertScrollMovesContent(0, HEIGHT);
        assertScrollMovesContent(0, -HEIGHT);
    }

    @Test
    void extremeShiftsEmptyGridWithoutException() {
        for (int shift : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertScrollMovesContent(shift, 0);
            assertScrollMovesContent(0, shift);
            assertScrollMovesContent(shift, shift);
        }
    }

    @Test
    void extremeShiftStillMarksTrackersDirty() {
        var grid = filledGrid(WIDTH, HEIGHT);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();
        grid.scroll(Integer.MIN_VALUE, 0);
        assertTrue(tracker.hasDirtyCells());
    }

    @Test
    void singleCellGridEmptiesOnAnyShift() {
        var grid = filledGrid(1, 1);
        grid.scroll(1, 0);
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getForeground(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(0, 0));
    }

    @Test
    void singleCellGridKeepsContentOnZeroShift() {
        var grid = filledGrid(1, 1);
        grid.scroll(0, 0);
        assertEquals(101, grid.getCodePoint(0, 0));
    }

    @Test
    void matchesResizeToSameSize() {
        int[][] shifts =
                {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, -1}, {-3, 2}, {1, 1}, {-1, -2}, {3, 2},
                        {5, 5}};
        for (int[] shift : shifts) {
            var expected = filledGrid(WIDTH, HEIGHT);
            expected.resize(WIDTH, HEIGHT, shift[0], shift[1]);
            var actual = filledGrid(WIDTH, HEIGHT);
            actual.scroll(shift[0], shift[1]);
            assertSameContent(expected, actual);
        }
    }
}
