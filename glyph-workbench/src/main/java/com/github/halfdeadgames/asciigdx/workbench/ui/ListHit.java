package com.github.halfdeadgames.asciigdx.workbench.ui;

/**
 * What lies under the pointer in a dialog holding a scrolling list.
 *
 * @param inside whether the pointer is within the dialog frame
 * @param onBar whether the pointer is on the scroll bar column
 * @param cellRow the cell row under the pointer
 * @param row the list row under the pointer including the scroll offset, or -1 when none
 */
public record ListHit(boolean inside, boolean onBar, int cellRow, int row) {

    public static ListHit of(Dialog.Layout layout, int pixelX, int pixelY, int cellPixels, int listTop, int shownRows,
                             int offset, boolean scrolls) {
        int column = Math.floorDiv(pixelX, cellPixels);
        int cellRow = Math.floorDiv(pixelY, cellPixels);
        boolean inside = layout.containsCell(column, cellRow);
        boolean onBar = scrolls && column == layout.textX() + layout.textWidth() - 1;
        int viewRow = onBar || !inside ? -1 : SelectList.rowAt(cellRow, listTop, shownRows);
        int row = viewRow < 0 ? -1 : viewRow + offset;
        return new ListHit(inside, onBar, cellRow, row);
    }
}
