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
        assertEquals(TerminalGrid.TRANSPARENT, grid.getForeground(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT, grid.getBackground(2, 1));
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
        assertEquals(TerminalGrid.TRANSPARENT, grid.getBackground(1, 1));
    }

    @Test
    void newGridStartsFullyDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertTrue(grid.hasDirtyCells());
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertTrue(grid.isDirty(x, y));
            }
        }
    }

    @Test
    void clearDirtyResetsAllFlags() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.clearDirty();

        assertFalse(grid.hasDirtyCells());
        assertFalse(grid.isDirty(0, 0));
        assertFalse(grid.isDirty(2, 1));
    }

    @Test
    void changedWriteMarksOnlyThatCell() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();

        grid.set(1, 1, '@', RED, BLUE);

        assertTrue(grid.isDirty(1, 1));
        assertEquals(4, grid.nextDirtyIndex(0));
        assertEquals(-1, grid.nextDirtyIndex(5));
    }

    @Test
    void identicalWriteDoesNotMarkDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        grid.clearDirty();

        grid.set(1, 1, '@', RED, BLUE);

        assertFalse(grid.hasDirtyCells());
    }

    @Test
    void changingOnlyForegroundMarksDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        grid.clearDirty();

        grid.set(1, 1, '@', BLUE, BLUE);

        assertTrue(grid.isDirty(1, 1));
    }

    @Test
    void changingOnlyBackgroundMarksDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        grid.clearDirty();

        grid.set(1, 1, '@', RED, RED);

        assertTrue(grid.isDirty(1, 1));
    }

    @Test
    void changingOnlyGlyphMarksDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        grid.clearDirty();

        grid.set(1, 1, '#', RED, BLUE);

        assertTrue(grid.isDirty(1, 1));
    }

    @Test
    void writingEmptyValuesToEmptyCellDoesNotMarkDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();

        grid.set(1, 1, TerminalGrid.EMPTY_GLYPH, TerminalGrid.TRANSPARENT, TerminalGrid.TRANSPARENT);

        assertFalse(grid.hasDirtyCells());
    }

    @Test
    void clearOnCleanEmptyGridMarksNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();

        grid.clear();

        assertFalse(grid.hasDirtyCells());
    }

    @Test
    void clearKeepsExistingDirtyFlags() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();
        grid.markAllDirty();

        grid.clear();

        assertTrue(grid.isDirty(0, 0));
        assertTrue(grid.isDirty(2, 1));
    }

    @Test
    void reportsWidthAndHeight() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertEquals(3, grid.getWidth());
        assertEquals(2, grid.getHeight());
    }

    @Test
    void outOfBoundsWriteDoesNotMarkDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();

        grid.set(-1, 0, '@', RED, BLUE);
        grid.set(3, 0, '@', RED, BLUE);
        grid.set(0, 2, '@', RED, BLUE);

        assertFalse(grid.hasDirtyCells());
    }

    @Test
    void clearMarksOnlyNonEmptyCells() {
        TerminalGrid grid = new TerminalGrid(2, 2);
        grid.set(1, 0, '@', RED, BLUE);
        grid.clearDirty();

        grid.clear();

        assertTrue(grid.isDirty(1, 0));
        assertFalse(grid.isDirty(0, 0));
        assertFalse(grid.isDirty(0, 1));
        assertFalse(grid.isDirty(1, 1));
    }

    @Test
    void markAllDirtyMarksEveryCell() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();

        grid.markAllDirty();

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertTrue(grid.isDirty(x, y));
            }
        }
    }

    @Test
    void nextDirtyIndexWalksDirtyCellsInOrder() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.clearDirty();
        grid.set(2, 1, '@', RED, BLUE);
        grid.set(1, 0, '#', RED, BLUE);

        int first = grid.nextDirtyIndex(0);
        int second = grid.nextDirtyIndex(first + 1);
        int third = grid.nextDirtyIndex(second + 1);

        assertEquals(1, first);
        assertEquals(5, second);
        assertEquals(-1, third);
    }

    @Test
    void isDirtyOutOfBoundsThrows() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertThrows(IndexOutOfBoundsException.class, () -> grid.isDirty(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.isDirty(0, -1));
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
}
