package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.List;

/** Draws a {@link GroupedPickList}: the rows in view and, when it scrolls, the scroll bar. */
public final class GroupedPickListView {

    private GroupedPickListView() {
    }

    /**
     * @param textX first text column of the dialog
     * @param listTop cell row of the first list line
     * @param textWidth width in cells of the dialog text area; the bar takes its last column
     */
    public static void draw(Painter painter, GroupedPickList<?> list, int textX, int listTop, int textWidth) {
        List<SelectList.Row> visibleRows = list.visibleRows();
        SelectList.draw(painter,
            SelectList.areaOf(textX, listTop, Math.min(list.listWidth(), textWidth), visibleRows.size()),
            visibleRows, list.visibleSelectedIndex());
        if (list.scrolls()) {
            drawBar(painter, list, textX + textWidth - 1, listTop);
        }
    }

    // The bar sits in the last text column beside the list: arrows on the first and last cell, track between.
    private static void drawBar(Painter painter, GroupedPickList<?> list, int column, int listTop) {
        int cell = painter.cell();
        ScrollState window = list.window();
        int barCells = list.shownRows() * SelectList.PITCH - 1;
        int trackCells = barCells - 2;
        int x = column * cell;
        painter.text("▲", x, listTop * cell, UiPalette.FRAME);
        painter.text("▼", x, (listTop + barCells - 1) * cell, UiPalette.FRAME);
        int thumbStart = window.thumbStart(trackCells);
        int thumbEnd = thumbStart + window.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            boolean thumb = i >= thumbStart && i < thumbEnd;
            painter.text(thumb ? "█" : "░", x, (listTop + 1 + i) * cell, thumb ? UiPalette.FOCUS : UiPalette.FRAME);
        }
    }
}
