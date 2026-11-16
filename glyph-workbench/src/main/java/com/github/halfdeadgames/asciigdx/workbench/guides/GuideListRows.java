package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;

/** Where the guide rows sit inside the list panel, shared by drawing and clicking. */
final class GuideListRows {

    static final int NONE = -1;

    /** What a click on a guide row does. */
    enum Click { NONE, SELECT, OPEN_GROUPS }

    private GuideListRows() {
    }

    /**
     * A click on the row that is already selected opens its groups; a click on another row only
     * selects it.
     */
    static Click clickOutcome(int clickedIndex, int selectedIndex) {
        if (clickedIndex == NONE) {
            return Click.NONE;
        }
        return clickedIndex == selectedIndex ? Click.OPEN_GROUPS : Click.SELECT;
    }

    /** Index of the first row to draw: scrolls only as far as keeping the selection in view. */
    static int scrolledFirst(int firstVisible, int selected, int size, int visibleRows) {
        int fit = Math.max(1, visibleRows);
        int first = firstVisible;
        if (selected < first) {
            first = selected;
        } else if (selected >= first + fit) {
            first = selected - fit + 1;
        }
        return Math.max(0, Math.min(first, Math.max(0, size - fit)));
    }

    /**
     * Rows available to guides: the area inside the frame, less the legend rows when the legend is
     * shown.
     */
    static int visibleRows(CellRect area, int legendRows) {
        return area.height() - 2 - legendRows;
    }

    /**
     * Index of the guide whose row is at the cell, or {@link #NONE} when the cell is not on a guide
     * row.
     */
    static int indexAt(CellRect area, int visibleRows, int firstVisible, int size, int cellX,
            int cellY) {
        boolean insideFrameColumns = cellX > area.x() && cellX < area.x() + area.width() - 1;
        if (!insideFrameColumns) {
            return NONE;
        }
        int rowInWindow = cellY - area.y() - 1;
        if (rowInWindow < 0 || rowInWindow >= visibleRows) {
            return NONE;
        }
        int index = firstVisible + rowInWindow;
        return index < size ? index : NONE;
    }
}
