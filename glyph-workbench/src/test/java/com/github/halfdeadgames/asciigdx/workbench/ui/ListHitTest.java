package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListHitTest {

    private static final int CELL = 10;
    private static final int LIST_TOP = 10;
    private static final int SHOWN_ROWS = 3;
    // Frame covers columns 10..29 and cell rows 5..18; text columns 13..26, so the bar column is 26.
    private static final Dialog.Layout LAYOUT = new Dialog.Layout(10, 5, 20, 14, 13, 8, 17, List.of());
    private static final int BAR_COLUMN = 26;

    private static int centerOf(int cells) {
        return cells * CELL + CELL / 2;
    }

    private static ListHit hit(int pointerX, int pointerY, int offset, boolean scrolls) {
        return ListHit.of(LAYOUT, pointerX, pointerY, CELL, LIST_TOP, SHOWN_ROWS, offset, scrolls);
    }

    private static ListHit hitAtCell(int column, int cellRow, int offset, boolean scrolls) {
        return hit(centerOf(column), centerOf(cellRow), offset, scrolls);
    }

    @Test
    void leftOfFrameIsOutside() {
        assertFalse(hitAtCell(9, 10, 0, false).inside());
        assertTrue(hitAtCell(10, 10, 0, false).inside());
    }

    @Test
    void rightOfFrameIsOutside() {
        assertTrue(hitAtCell(29, 10, 0, false).inside());
        assertFalse(hitAtCell(30, 10, 0, false).inside());
    }

    @Test
    void aboveFrameIsOutside() {
        assertFalse(hitAtCell(15, 4, 0, false).inside());
        assertTrue(hitAtCell(15, 5, 0, false).inside());
    }

    @Test
    void belowFrameIsOutside() {
        assertTrue(hitAtCell(15, 18, 0, false).inside());
        assertFalse(hitAtCell(15, 19, 0, false).inside());
    }

    @Test
    void outsideHasNoRow() {
        assertEquals(-1, hitAtCell(9, 10, 0, false).row());
    }

    @Test
    void barColumnIsBarWhenListScrolls() {
        ListHit hit = hitAtCell(BAR_COLUMN, 11, 0, true);
        assertTrue(hit.inside());
        assertTrue(hit.onBar());
        assertEquals(11, hit.cellRow());
        assertEquals(-1, hit.row());
    }

    @Test
    void barColumnIsARowWhenListDoesNotScroll() {
        ListHit hit = hitAtCell(BAR_COLUMN, 12, 0, false);
        assertFalse(hit.onBar());
        assertEquals(1, hit.row());
    }

    @Test
    void otherColumnsAreNotTheBarWhenListScrolls() {
        assertFalse(hitAtCell(BAR_COLUMN - 1, 12, 0, true).onBar());
        assertFalse(hitAtCell(BAR_COLUMN + 1, 12, 0, true).onBar());
    }

    @Test
    void bothCellsOfARowMapToIt() {
        assertEquals(0, hitAtCell(15, 10, 0, false).row());
        assertEquals(0, hitAtCell(15, 11, 0, false).row());
        assertEquals(1, hitAtCell(15, 12, 0, false).row());
        assertEquals(1, hitAtCell(15, 13, 0, false).row());
        assertEquals(2, hitAtCell(15, 14, 0, false).row());
        assertEquals(2, hitAtCell(15, 15, 0, false).row());
    }

    @Test
    void offsetShiftsTheRow() {
        assertEquals(4, hitAtCell(15, 10, 4, false).row());
        assertEquals(6, hitAtCell(15, 15, 4, false).row());
    }

    @Test
    void cellRowsOutsideTheListButInsideTheFrameHaveNoRow() {
        ListHit above = hitAtCell(15, LIST_TOP - 1, 4, false);
        ListHit below = hitAtCell(15, LIST_TOP + SHOWN_ROWS * 2, 4, false);
        assertTrue(above.inside());
        assertTrue(below.inside());
        assertEquals(-1, above.row());
        assertEquals(-1, below.row());
    }

    @Test
    void negativePointerFloorsTowardsNegativeInfinity() {
        ListHit hit = hit(-1, -1, 0, false);
        assertFalse(hit.inside());
        assertEquals(-1, hit.cellRow());
        assertEquals(-2, hit(0, -CELL - 1, 0, false).cellRow());
        assertEquals(-1, hit(0, -CELL, 0, false).cellRow());
    }

    @Test
    void pointerEdgesBelongToTheirOwnCell() {
        assertEquals(10, hit(0, 100, 0, false).cellRow());
        assertEquals(9, hit(0, 99, 0, false).cellRow());
    }
}
