package com.github.halfdeadgames.asciigdx.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DirtyTrackerTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    private static List<String> visitedCells(DirtyTracker tracker) {
        List<String> visited = new ArrayList<>();
        tracker.forEachDirtyCell((x, y) -> visited.add(x + "," + y));
        return visited;
    }

    @Test
    void newTrackerStartsFullyDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();

        assertTrue(tracker.hasDirtyCells());
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertTrue(tracker.isDirty(x, y));
            }
        }
    }

    @Test
    void newTrackerOnCleanGridStillStartsDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.createDirtyTracker().clear();

        assertTrue(grid.createDirtyTracker().hasDirtyCells());
    }

    @Test
    void clearResetsAllFlags() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        tracker.clear();

        assertFalse(tracker.hasDirtyCells());
        assertFalse(tracker.isDirty(0, 0));
        assertFalse(tracker.isDirty(2, 1));
    }

    @Test
    void markAllMarksEveryCell() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();
        tracker.clear();

        tracker.markAll();

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                assertTrue(tracker.isDirty(x, y));
            }
        }
    }

    @Test
    void changedWriteMarksOnlyThatCell() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, '@', RED, BLUE);

        assertTrue(tracker.isDirty(1, 1));
        assertEquals(List.of("1,1"), visitedCells(tracker));
    }

    @Test
    void identicalWriteMarksNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, '@', RED, BLUE);

        assertFalse(tracker.hasDirtyCells());
    }

    @Test
    void changingOnlyForegroundMarksDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, '@', BLUE, BLUE);

        assertTrue(tracker.isDirty(1, 1));
    }

    @Test
    void changingOnlyBackgroundMarksDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, '@', RED, RED);

        assertTrue(tracker.isDirty(1, 1));
    }

    @Test
    void changingOnlyGlyphMarksDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(1, 1, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, '#', RED, BLUE);

        assertTrue(tracker.isDirty(1, 1));
    }

    @Test
    void writingEmptyValuesToEmptyCellMarksNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, TerminalGrid.EMPTY_GLYPH, TerminalGrid.TRANSPARENT_RGBA, TerminalGrid.TRANSPARENT_RGBA);

        assertFalse(tracker.hasDirtyCells());
    }

    @Test
    void outOfBoundsSetMarksNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(-1, 0, '@', RED, BLUE);
        grid.set(3, 0, '@', RED, BLUE);
        grid.set(0, 2, '@', RED, BLUE);

        assertFalse(tracker.hasDirtyCells());
    }

    @Test
    void gridClearOnCleanEmptyGridMarksNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.clear();

        assertFalse(tracker.hasDirtyCells());
    }

    @Test
    void gridClearMarksOnlyChangedCells() {
        TerminalGrid grid = new TerminalGrid(2, 2);
        grid.set(1, 0, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.clear();

        assertTrue(tracker.isDirty(1, 0));
        assertFalse(tracker.isDirty(0, 0));
        assertFalse(tracker.isDirty(0, 1));
        assertFalse(tracker.isDirty(1, 1));
    }

    @Test
    void gridClearKeepsExistingDirtyFlags() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();

        grid.clear();

        assertTrue(tracker.isDirty(0, 0));
        assertTrue(tracker.isDirty(2, 1));
    }

    @Test
    void trackersAreIndependent() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker first = grid.createDirtyTracker();
        DirtyTracker second = grid.createDirtyTracker();

        first.clear();

        assertFalse(first.hasDirtyCells());
        assertTrue(second.hasDirtyCells());

        grid.set(2, 1, '@', RED, BLUE);

        assertEquals(List.of("2,1"), visitedCells(first));
        assertEquals(6, visitedCells(second).size());
    }

    @Test
    void forEachDirtyCellVisitsEveryCellOfNewTrackerInRowMajorOrder() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        assertEquals(List.of("0,0", "1,0", "2,0", "0,1", "1,1", "2,1"), visitedCells(tracker));
    }

    @Test
    void forEachDirtyCellVisitsOnlyChangedCellsWithCorrectCoordinates() {
        TerminalGrid grid = new TerminalGrid(4, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();
        grid.set(3, 0, '@', RED, BLUE);
        grid.set(1, 1, '#', RED, BLUE);

        assertEquals(List.of("3,0", "1,1"), visitedCells(tracker));
    }

    @Test
    void forEachDirtyCellVisitsNothingAfterClear() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();
        tracker.clear();

        assertTrue(visitedCells(tracker).isEmpty());
    }

    @Test
    void forEachDirtyCellDoesNotClearFlags() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        tracker.forEachDirtyCell((x, y) -> { });

        assertTrue(tracker.isDirty(0, 0));
        assertTrue(tracker.isDirty(2, 1));
    }

    @Test
    void forEachDirtyCellAfterGridClearVisitsOnlyCellsThatHadContent() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(2, 0, '@', RED, BLUE);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.clear();

        assertEquals(List.of("2,0"), visitedCells(tracker));
    }

    @Test
    void forEachDirtyCellRejectsNullVisitor() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        assertThrows(NullPointerException.class, () -> tracker.forEachDirtyCell(null));
    }

    @Test
    void isDirtyOutOfBoundsThrows() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        assertThrows(IndexOutOfBoundsException.class, () -> tracker.isDirty(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> tracker.isDirty(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> tracker.isDirty(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> tracker.isDirty(0, 2));
    }

    @Test
    void dirtyCellCountOfNewTrackerIsAllCells() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        assertEquals(6, tracker.getDirtyCellCount());
    }

    @Test
    void dirtyCellCountIsZeroAfterClear() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        tracker.clear();

        assertEquals(0, tracker.getDirtyCellCount());
    }

    @Test
    void dirtyCellCountMatchesNumberOfDistinctChangedCells() {
        TerminalGrid grid = new TerminalGrid(4, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(3, 0, '@', RED, BLUE);
        grid.set(1, 1, '#', RED, BLUE);

        assertEquals(2, tracker.getDirtyCellCount());
    }

    @Test
    void dirtyCellCountCountsRepeatedlyChangedCellOnce() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.set(1, 1, '@', RED, BLUE);
        grid.set(1, 1, '#', BLUE, RED);

        assertEquals(1, tracker.getDirtyCellCount());
    }

    @Test
    void releaseStopsMarking() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker released = grid.createDirtyTracker();
        DirtyTracker active = grid.createDirtyTracker();
        released.clear();
        active.clear();

        released.release();
        grid.set(1, 1, '@', RED, BLUE);

        assertTrue(released.isReleased());
        assertFalse(released.hasDirtyCells());
        assertTrue(active.isDirty(1, 1));
    }

    @Test
    void releaseIsIdempotent() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        DirtyTracker other = grid.createDirtyTracker();
        other.clear();

        tracker.release();
        tracker.release();
        grid.set(0, 0, '@', RED, BLUE);

        assertTrue(tracker.isReleased());
        assertTrue(other.isDirty(0, 0));
    }

    @Test
    void resizeMarksEveryNewCellDirty() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        tracker.clear();

        grid.resize(4, 5);

        assertEquals(20, tracker.getDirtyCellCount());
        assertTrue(tracker.isDirty(3, 4));
        assertEquals(20, visitedCells(tracker).size());
    }

    @Test
    void shrinkingResizeReducesDirtyCountAndBounds() {
        TerminalGrid grid = new TerminalGrid(4, 4);
        DirtyTracker tracker = grid.createDirtyTracker();

        grid.resize(2, 3);

        assertEquals(6, tracker.getDirtyCellCount());
        assertThrows(IndexOutOfBoundsException.class, () -> tracker.isDirty(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> tracker.isDirty(0, 3));
    }

    @Test
    void resizeResetsSeveralTrackers() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker first = grid.createDirtyTracker();
        DirtyTracker second = grid.createDirtyTracker();
        first.clear();
        second.clear();

        grid.resize(2, 2, 1, 0);

        assertEquals(4, first.getDirtyCellCount());
        assertEquals(4, second.getDirtyCellCount());
    }

    @Test
    void resizeDoesNotAffectReleasedTracker() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker released = grid.createDirtyTracker();
        released.clear();
        released.release();

        grid.resize(5, 5);

        assertEquals(0, released.getDirtyCellCount());
        assertFalse(released.hasDirtyCells());
    }

    @Test
    void setAfterResizeMarksTheRightCell() {
        TerminalGrid grid = new TerminalGrid(2, 2);
        DirtyTracker tracker = grid.createDirtyTracker();
        grid.resize(5, 3);
        tracker.clear();

        grid.set(4, 2, '@', RED, BLUE);

        assertEquals(1, tracker.getDirtyCellCount());
        assertEquals(List.of("4,2"), visitedCells(tracker));
    }

    @Test
    void forEachDirtyCellUsesNewWidthAfterResize() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        DirtyTracker tracker = grid.createDirtyTracker();

        grid.resize(2, 3);

        assertEquals(List.of("0,0", "1,0", "0,1", "1,1", "0,2", "1,2"), visitedCells(tracker));
    }

    @Test
    void releasedTrackerStillAnswersQueries() {
        DirtyTracker tracker = new TerminalGrid(3, 2).createDirtyTracker();

        tracker.release();

        assertTrue(tracker.isDirty(0, 0));
        tracker.clear();
        assertFalse(tracker.hasDirtyCells());
    }
}
