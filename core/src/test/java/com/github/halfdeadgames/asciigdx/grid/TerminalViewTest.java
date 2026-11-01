package com.github.halfdeadgames.asciigdx.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerminalViewTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    @Test
    void viewReturnsSameValuesAsDirectCalls() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        grid.set(2, 1, 'A', RED, BLUE);
        TerminalView view = grid;

        assertEquals(grid.getWidth(), view.getWidth());
        assertEquals(grid.getHeight(), view.getHeight());
        for (int y = -1; y <= 2; y++) {
            for (int x = -1; x <= 3; x++) {
                assertEquals(grid.contains(x, y), view.contains(x, y));
            }
        }
        assertEquals('A', view.getGlyph(2, 1));
        assertEquals(RED, view.getForeground(2, 1));
        assertEquals(BLUE, view.getBackground(2, 1));
        assertEquals(grid.getGlyph(0, 0), view.getGlyph(0, 0));
    }

    @Test
    void outOfBoundsReadThrowsThroughView() {
        TerminalView view = new TerminalGrid(3, 2);

        assertThrows(IndexOutOfBoundsException.class, () -> view.getGlyph(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> view.getForeground(0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> view.getBackground(-1, 0));
    }

    @Test
    void trackerCreatedThroughViewStartsDirtyAndRecordsSet() {
        TerminalGrid grid = new TerminalGrid(3, 2);
        TerminalView view = grid;

        DirtyTracker tracker = view.createDirtyTracker();
        assertEquals(6, tracker.getDirtyCellCount());

        tracker.clear();
        assertFalse(tracker.hasDirtyCells());

        grid.set(1, 1, 'B', RED, BLUE);
        assertTrue(tracker.isDirty(1, 1));
        assertEquals(1, tracker.getDirtyCellCount());
    }
}
