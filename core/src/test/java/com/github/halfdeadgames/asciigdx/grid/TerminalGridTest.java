package com.github.halfdeadgames.asciigdx.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerminalGridTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    @Test
    void newGridIsEmpty() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getForeground(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(2, 1));
    }

    @Test
    void setStoresAllCellValues() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.set(1, 1, '@', RED, BLUE);

        assertEquals('@', grid.getGlyph(1, 1));
        assertEquals(RED, grid.getForeground(1, 1));
        assertEquals(BLUE, grid.getBackground(1, 1));
    }

    @Test
    void setDoesNotTouchNeighbours() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.set(1, 0, '@', RED, BLUE);

        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(0, 0));
        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(2, 0));
        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(1, 1));
    }

    @Test
    void storesCodePointsOutsideBasicPlane() {
        TerminalGrid grid = new TerminalGrid(1, 1);
        int alembic = 0x1F702;

        grid.set(0, 0, alembic, RED, BLUE);

        assertEquals(alembic, grid.getGlyph(0, 0));
    }

    @Test
    void outOfBoundsWriteIsIgnored() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.set(-1, 0, '@', RED, BLUE);
        grid.set(3, 0, '@', RED, BLUE);
        grid.set(0, 2, '@', RED, BLUE);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(x, y));
            }
        }
    }

    @Test
    void outOfBoundsReadThrows() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertThrows(IndexOutOfBoundsException.class, () -> grid.getGlyph(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.getForeground(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.getBackground(0, 2));
    }

    @Test
    void containsMatchesGridBounds() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertTrue(grid.contains(0, 0));
        assertTrue(grid.contains(2, 1));
        assertFalse(grid.contains(3, 1));
        assertFalse(grid.contains(2, 2));
        assertFalse(grid.contains(-1, 0));
    }

    @Test
    void clearEmptiesEveryCell() {
        TerminalGrid grid = new TerminalGrid(2, 2);
        grid.set(0, 0, '@', RED, BLUE);
        grid.set(1, 1, '#', BLUE, RED);

        grid.clear();

        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(1, 1));
    }

    @Test
    void reportsWidthAndHeight() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertEquals(3, grid.getWidth());
        assertEquals(2, grid.getHeight());
    }

    @Test
    void rejectsNonPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalGrid(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new TerminalGrid(5, -1));
    }

    @Test
    void rejectsSizeThatOverflows() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalGrid(50000, 50000));
    }

    private static void assertCellEmpty(TerminalGrid grid, int x, int y) {
        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(x, y));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getForeground(x, y));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(x, y));
    }

    private static void assertGridEmpty(TerminalGrid grid) {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                assertCellEmpty(grid, x, y);
            }
        }
    }

    @Test
    void growingKeepsCellsAtTheirCoordinatesAndNewCellsAreEmpty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(2, 1, '@', RED, BLUE);

        grid.resize(5, 4);

        assertEquals(5, grid.getWidth());
        assertEquals(4, grid.getHeight());
        assertEquals('@', grid.getGlyph(2, 1));
        assertEquals(RED, grid.getForeground(2, 1));
        assertEquals(BLUE, grid.getBackground(2, 1));
        assertCellEmpty(grid, 3, 1);
        assertCellEmpty(grid, 2, 3);
        assertCellEmpty(grid, 4, 3);
    }

    @Test
    void shrinkingDiscardsCellsAndGrowingBackDoesNotRestoreThem() {
        TerminalGrid grid = new TerminalGrid(3, 3);
        grid.set(0, 0, 'a', RED, BLUE);
        grid.set(2, 2, 'b', RED, BLUE);

        grid.resize(2, 2);
        assertEquals('a', grid.getGlyph(0, 0));
        assertFalse(grid.contains(2, 2));

        grid.resize(3, 3);
        assertEquals('a', grid.getGlyph(0, 0));
        assertCellEmpty(grid, 2, 2);
    }

    @Test
    void shiftingDownWhileGrowingUpwardMovesContentDown() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 0, '@', RED, BLUE);

        grid.resize(3, 7, 0, 5);

        assertEquals('@', grid.getGlyph(1, 5));
        assertCellEmpty(grid, 1, 0);
        assertCellEmpty(grid, 1, 4);
    }

    @Test
    void negativeShiftCropsTopAndLeft() {
        TerminalGrid grid = new TerminalGrid(3, 3);
        grid.set(0, 0, 'a', RED, BLUE);
        grid.set(1, 1, 'b', RED, BLUE);
        grid.set(2, 2, 'c', RED, BLUE);

        grid.resize(2, 2, -1, -1);

        assertEquals('b', grid.getGlyph(0, 0));
        assertEquals('c', grid.getGlyph(1, 1));
        assertCellEmpty(grid, 1, 0);
        assertCellEmpty(grid, 0, 1);
    }

    @Test
    void centeredGrowthPutsOldContentInTheMiddle() {
        TerminalGrid grid = new TerminalGrid(2, 2);
        grid.set(0, 0, 'a', RED, BLUE);
        grid.set(1, 1, 'd', RED, BLUE);

        grid.resize(4, 4, 1, 1);

        assertEquals('a', grid.getGlyph(1, 1));
        assertEquals('d', grid.getGlyph(2, 2));
        assertCellEmpty(grid, 0, 0);
        assertCellEmpty(grid, 3, 3);
    }

    @Test
    void shiftThatMissesTheGridLeavesItEmpty() {
        TerminalGrid grid = new TerminalGrid(3, 3);
        grid.set(1, 1, '@', RED, BLUE);

        grid.resize(3, 3, 3, 0);
        assertGridEmpty(grid);

        grid.set(1, 1, '@', RED, BLUE);
        grid.resize(3, 3, 0, -3);
        assertGridEmpty(grid);
    }

    @Test
    void hugeShiftsDoNotOverflow() {
        TerminalGrid grid = new TerminalGrid(3, 3);
        grid.set(1, 1, '@', RED, BLUE);

        grid.resize(3, 3, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertGridEmpty(grid);

        grid.set(1, 1, '@', RED, BLUE);
        grid.resize(3, 3, Integer.MIN_VALUE, Integer.MIN_VALUE);
        assertGridEmpty(grid);

        grid.set(1, 1, '@', RED, BLUE);
        grid.resize(3, 3, Integer.MAX_VALUE, -Integer.MAX_VALUE);
        assertGridEmpty(grid);
    }

    @Test
    void sameSizeWithZeroShiftChangesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.resize(3, 2);

        assertFalse(tracker.hasDirtyCells());
        assertEquals('@', grid.getGlyph(1, 1));
    }

    @Test
    void sameSizeWithShiftMovesContentAndMarksAllDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(0, 0, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.resize(3, 2, 1, 1);

        assertEquals('@', grid.getGlyph(1, 1));
        assertCellEmpty(grid, 0, 0);
        assertEquals(6, tracker.getDirtyCellCount());
    }

    @Test
    void invalidResizeThrowsAndChangesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        assertThrows(IllegalArgumentException.class, () -> grid.resize(0, 2));
        assertThrows(IllegalArgumentException.class, () -> grid.resize(3, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> grid.resize(50000, 50000));

        assertEquals(3, grid.getWidth());
        assertEquals(2, grid.getHeight());
        assertEquals('@', grid.getGlyph(1, 1));
        assertFalse(tracker.hasDirtyCells());
        assertEquals(0, tracker.getDirtyCellCount());
    }
}
