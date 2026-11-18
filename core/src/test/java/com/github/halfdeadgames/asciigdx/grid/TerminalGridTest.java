package com.github.halfdeadgames.asciigdx.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

class TerminalGridTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    @Test
    void newGridIsEmpty() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getForeground(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(2, 1));
    }

    @Test
    void setStoresAllCellValues() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.set(1, 1, '@', RED, BLUE);

        assertEquals('@', grid.getCodePoint(1, 1));
        assertEquals(RED, grid.getForeground(1, 1));
        assertEquals(BLUE, grid.getBackground(1, 1));
    }

    @Test
    void setDoesNotTouchNeighbors() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.set(1, 0, '@', RED, BLUE);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(1, 1));
    }

    @Test
    void storesCodePointsOutsideBasicPlane() {
        TerminalGrid grid = new TerminalGrid(1, 1);
        int alembic = 0x1F702;

        grid.set(0, 0, alembic, RED, BLUE);

        assertEquals(alembic, grid.getCodePoint(0, 0));
    }

    @Test
    void outOfBoundsWriteIsIgnored() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        grid.set(-1, 0, '@', RED, BLUE);
        grid.set(3, 0, '@', RED, BLUE);
        grid.set(0, 2, '@', RED, BLUE);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(x, y));
            }
        }
    }

    @Test
    void outOfBoundsReadThrows() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertThrows(IndexOutOfBoundsException.class, () -> grid.getCodePoint(3, 0));
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

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(1, 1));
    }

    @Test
    void reportsColumnsAndRows() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertEquals(3, grid.getColumns());
        assertEquals(2, grid.getRows());
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

    @Test
    void colorOverloadSetStoresRgba8888() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        Color foreground = new Color(0.2f, 0.4f, 0.6f, 0.8f);
        Color background = new Color(0.9f, 0.1f, 0.3f, 1f);

        grid.set(1, 1, 'Q', foreground, background);

        assertEquals('Q', grid.getCodePoint(1, 1));
        assertEquals(Color.rgba8888(foreground), grid.getForeground(1, 1));
        assertEquals(Color.rgba8888(background), grid.getBackground(1, 1));
    }

    @Test
    void colorOverloadsStoreLiteralRgba8888NotToIntBits() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        Color color = new Color(1f, 0f, 0f, 0.5f);

        grid.set(0, 0, 'A', color, color);
        grid.setForeground(1, 0, color);
        grid.setBackground(2, 0, color);

        assertEquals(0xFF00007F, grid.getForeground(0, 0));
        assertEquals(0xFF00007F, grid.getBackground(0, 0));
        assertEquals(0xFF00007F, grid.getForeground(1, 0));
        assertEquals(0xFF00007F, grid.getBackground(2, 0));
        assertNotEquals(color.toIntBits(), grid.getForeground(0, 0));
    }

    @Test
    void colorOverloadsSetSingleChannels() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        Color color = new Color(0.5f, 0.25f, 0.75f, 1f);

        grid.setForeground(0, 0, color);
        grid.setBackground(2, 1, color);

        assertEquals(Color.rgba8888(color), grid.getForeground(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(0, 0));
        assertEquals(Color.rgba8888(color), grid.getBackground(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getForeground(2, 1));
    }

    @Test
    void colorOverloadsWithEqualColorDoNotMarkDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        Color color = new Color(0.5f, 0.25f, 0.75f, 1f);
        grid.set(1, 0, 'A', color, color);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();

        grid.set(1, 0, 'A', new Color(color), new Color(color));
        grid.setForeground(1, 0, new Color(color));
        grid.setBackground(1, 0, new Color(color));

        assertFalse(tracker.isDirty(1, 0));
        tracker.release();
    }

    @Test
    void colorOverloadsIgnoreWritesOutsideGrid() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        Color color = Color.RED;

        grid.set(3, 0, 'A', color, color);
        grid.setForeground(-1, 0, color);
        grid.setBackground(0, 2, color);

        assertGridEmpty(grid);
    }

    @Test
    void colorOverloadsRejectNullEvenOutsideGrid() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertThrows(NullPointerException.class, () -> grid.set(0, 0, 'A', null, Color.RED));
        assertThrows(NullPointerException.class, () -> grid.set(0, 0, 'A', Color.RED, null));
        assertThrows(NullPointerException.class, () -> grid.set(9, 9, 'A', null, Color.RED));
        assertThrows(NullPointerException.class, () -> grid.setForeground(0, 0, (Color) null));
        assertThrows(NullPointerException.class, () -> grid.setForeground(9, 9, (Color) null));
        assertThrows(NullPointerException.class, () -> grid.setBackground(0, 0, (Color) null));
        assertThrows(NullPointerException.class, () -> grid.setBackground(9, 9, (Color) null));
    }

    @Test
    void colorOverloadsCopyValueAtCallTime() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        Color color = new Color(0.2f, 0.4f, 0.6f, 1f);
        int expected = Color.rgba8888(color);

        grid.set(0, 0, 'A', color, color);
        grid.setForeground(1, 0, color);
        grid.setBackground(2, 0, color);
        color.set(1f, 1f, 1f, 1f);

        assertEquals(expected, grid.getForeground(0, 0));
        assertEquals(expected, grid.getBackground(0, 0));
        assertEquals(expected, grid.getForeground(1, 0));
        assertEquals(expected, grid.getBackground(2, 0));
    }

    private static void assertCellEmpty(TerminalGrid grid, int x, int y) {
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(x, y));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getForeground(x, y));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(x, y));
    }

    private static void assertGridEmpty(TerminalGrid grid) {
        for (int y = 0; y < grid.getRows(); y++) {
            for (int x = 0; x < grid.getColumns(); x++) {
                assertCellEmpty(grid, x, y);
            }
        }
    }

    @Test
    void growingKeepsCellsAtTheirCoordinatesAndNewCellsAreEmpty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(2, 1, '@', RED, BLUE);

        grid.resize(5, 4);

        assertEquals(5, grid.getColumns());
        assertEquals(4, grid.getRows());
        assertEquals('@', grid.getCodePoint(2, 1));
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
        assertEquals('a', grid.getCodePoint(0, 0));
        assertFalse(grid.contains(2, 2));

        grid.resize(3, 3);
        assertEquals('a', grid.getCodePoint(0, 0));
        assertCellEmpty(grid, 2, 2);
    }

    @Test
    void shiftingDownWhileGrowingUpwardMovesContentDown() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 0, '@', RED, BLUE);

        grid.resize(3, 7, 0, 5);

        assertEquals('@', grid.getCodePoint(1, 5));
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

        assertEquals('b', grid.getCodePoint(0, 0));
        assertEquals('c', grid.getCodePoint(1, 1));
        assertCellEmpty(grid, 1, 0);
        assertCellEmpty(grid, 0, 1);
    }

    @Test
    void centeredGrowthPutsOldContentInTheMiddle() {
        TerminalGrid grid = new TerminalGrid(2, 2);
        grid.set(0, 0, 'a', RED, BLUE);
        grid.set(1, 1, 'd', RED, BLUE);

        grid.resize(4, 4, 1, 1);

        assertEquals('a', grid.getCodePoint(1, 1));
        assertEquals('d', grid.getCodePoint(2, 2));
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
        tracker.markAllClean();

        grid.resize(3, 2);

        assertFalse(tracker.hasDirtyCells());
        assertEquals('@', grid.getCodePoint(1, 1));
    }

    @Test
    void sameSizeWithShiftMovesContentAndMarksAllDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(0, 0, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();

        grid.resize(3, 2, 1, 1);

        assertEquals('@', grid.getCodePoint(1, 1));
        assertCellEmpty(grid, 0, 0);
        assertEquals(6, tracker.getDirtyCellCount());
    }

    @Test
    void invalidResizeThrowsAndChangesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();

        assertThrows(IllegalArgumentException.class, () -> grid.resize(0, 2));
        assertThrows(IllegalArgumentException.class, () -> grid.resize(3, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> grid.resize(50000, 50000));

        assertEquals(3, grid.getColumns());
        assertEquals(2, grid.getRows());
        assertEquals('@', grid.getCodePoint(1, 1));
        assertFalse(tracker.hasDirtyCells());
        assertEquals(0, tracker.getDirtyCellCount());
    }

    @Test
    void setCodePointChangesOnlyTheCodePoint() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);

        grid.setCodePoint(1, 1, '#');

        assertEquals('#', grid.getCodePoint(1, 1));
        assertEquals(RED, grid.getForeground(1, 1));
        assertEquals(BLUE, grid.getBackground(1, 1));
    }

    @Test
    void setForegroundChangesOnlyTheForeground() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);

        grid.setForeground(1, 1, BLUE);

        assertEquals('@', grid.getCodePoint(1, 1));
        assertEquals(BLUE, grid.getForeground(1, 1));
        assertEquals(BLUE, grid.getBackground(1, 1));
    }

    @Test
    void setBackgroundChangesOnlyTheBackground() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);

        grid.setBackground(1, 1, RED);

        assertEquals('@', grid.getCodePoint(1, 1));
        assertEquals(RED, grid.getForeground(1, 1));
        assertEquals(RED, grid.getBackground(1, 1));
    }

    @Test
    void singleFieldSettersMarkTheCellDirtyOnRealChange() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();

        grid.setCodePoint(1, 1, '#');
        assertEquals(1, tracker.getDirtyCellCount());

        tracker.markAllClean();
        grid.setForeground(1, 1, BLUE);
        assertEquals(1, tracker.getDirtyCellCount());

        tracker.markAllClean();
        grid.setBackground(1, 1, RED);
        assertEquals(1, tracker.getDirtyCellCount());
    }

    @Test
    void singleFieldSettersDoNotMarkDirtyForIdenticalValue() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();

        grid.setCodePoint(1, 1, '@');
        grid.setForeground(1, 1, RED);
        grid.setBackground(1, 1, BLUE);

        assertFalse(tracker.hasDirtyCells());
    }

    @Test
    void singleFieldSettersIgnoreOutOfBoundsWrites() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.markAllClean();

        grid.setCodePoint(-1, 0, '#');
        grid.setCodePoint(3, 0, '#');
        grid.setForeground(0, -1, BLUE);
        grid.setForeground(0, 2, BLUE);
        grid.setBackground(-1, -1, RED);
        grid.setBackground(3, 2, RED);

        assertFalse(tracker.hasDirtyCells());
        assertEquals('@', grid.getCodePoint(1, 1));
        assertEquals(RED, grid.getForeground(1, 1));
        assertEquals(BLUE, grid.getBackground(1, 1));
    }
}
