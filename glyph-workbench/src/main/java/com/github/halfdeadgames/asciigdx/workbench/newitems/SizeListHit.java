package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;

/**
 * What lies under the pointer in the size dialog.
 *
 * @param inside whether the pointer is within the dialog frame
 * @param onBar whether the pointer is on the scroll bar column
 * @param cellRow the cell row under the pointer
 * @param row the list row under the pointer including the scroll offset, or -1 when none
 */
record SizeListHit(boolean inside, boolean onBar, int cellRow, int row) {

    static SizeListHit of(Dialog.Layout layout, int pointerX, int pointerY, int cell, int listTop, int shownRows,
                          int offset, boolean scrolls) {
        int column = Math.floorDiv(pointerX, cell);
        int cellRow = Math.floorDiv(pointerY, cell);
        boolean inside = column >= layout.x() && column < layout.x() + layout.width()
            && cellRow >= layout.y() && cellRow < layout.y() + layout.height();
        boolean onBar = scrolls && column == layout.textX() + layout.textWidth() - 1;
        int viewRow = onBar || !inside ? -1 : SelectList.rowAt(cellRow, listTop, shownRows);
        int row = viewRow < 0 ? -1 : viewRow + offset;
        return new SizeListHit(inside, onBar, cellRow, row);
    }
}
