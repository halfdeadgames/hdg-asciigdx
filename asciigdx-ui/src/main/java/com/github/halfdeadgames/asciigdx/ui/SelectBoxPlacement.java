package com.github.halfdeadgames.asciigdx.ui;

/** Decides where the open list of a {@link SelectBox} goes on the surface, in UI cells. */
final class SelectBoxPlacement {

    /** A run of cells on one axis. */
    record Span(int start, int length) {
    }

    private SelectBoxPlacement() {
    }

    /**
     * Opens downward right under the box when all wanted rows fit there. Otherwise opens on the
     * side, below or above, with more room, and shrinks to fit it. At least one row is kept.
     */
    static Span placeVertically(int boxRow, int wantedRows, int surfaceRows) {
        return keepOnSurface(chooseSide(boxRow, wantedRows, surfaceRows), surfaceRows);
    }

    // A box that is itself off the surface would otherwise put the span off it too.
    private static Span keepOnSurface(Span span, int surfaceRows) {
        int length = Math.min(span.length(), surfaceRows);
        int start = Math.max(0, Math.min(span.start(), surfaceRows - length));
        return new Span(start, length);
    }

    private static Span chooseSide(int boxRow, int wantedRows, int surfaceRows) {
        int roomBelow = Math.max(0, surfaceRows - (boxRow + 1));
        if (roomBelow >= wantedRows) {
            return new Span(boxRow + 1, wantedRows);
        }
        int roomAbove = Math.max(0, Math.min(boxRow, surfaceRows));
        if (roomAbove > roomBelow) {
            int rows = Math.min(wantedRows, roomAbove);
            return new Span(boxRow - rows, rows);
        }
        return new Span(boxRow + 1, Math.max(1, Math.min(wantedRows, roomBelow)));
    }

    /**
     * Gives the list the box width, or at least the box width and the width the entries need, and
     * moves it left to stay on the surface. It never exceeds the surface.
     */
    static Span placeHorizontally(int boxColumn, int boxWidth, int entryWidth,
            boolean scrollBarShown, SelectBoxListWidth mode, int surfaceColumns) {
        int wantedWidth = boxWidth;
        if (mode == SelectBoxListWidth.FIT_ENTRIES) {
            wantedWidth = Math.max(boxWidth, entryWidth + (scrollBarShown ? 1 : 0));
        }
        int width = Math.min(wantedWidth, surfaceColumns);
        int start = Math.max(0, Math.min(boxColumn, surfaceColumns - width));
        return new Span(start, width);
    }
}
