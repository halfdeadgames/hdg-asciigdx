package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;

/** Where the guide rows sit inside the list panel, shared by drawing and clicking. */
final class GuideListRows {

    static final int NONE = -1;

    private GuideListRows() {
    }

    /** Rows available to guides: the area inside the frame, less the legend rows when the legend is shown. */
    static int visibleRows(CellRect area, int legendRows) {
        return area.height() - 2 - legendRows;
    }

    /** Index of the guide whose row is at the cell, or {@link #NONE} when the cell is not on a guide row. */
    static int indexAt(CellRect area, int visibleRows, int firstVisible, int size, int cellX, int cellY) {
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
