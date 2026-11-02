package com.github.halfdeadgames.asciigdx.workbench;

/** Whole-row scroll position over a list of rows, plus the maths for drawing a scroll bar. */
final class ScrollState {

    private int totalRows;
    private int visibleRows = 1;
    private int offset;

    ScrollState(int totalRows) {
        this.totalRows = Math.max(0, totalRows);
    }

    int offset() {
        return offset;
    }

    int visibleRows() {
        return visibleRows;
    }

    int totalRows() {
        return totalRows;
    }

    int maxOffset() {
        return Math.max(0, totalRows - visibleRows);
    }

    boolean needed() {
        return totalRows > visibleRows;
    }

    /** Changes the number of rows; the offset is pulled back into range. */
    void setTotalRows(int rows) {
        totalRows = Math.max(0, rows);
        offset = clamp(offset);
    }

    void setVisibleRows(int rows) {
        visibleRows = Math.max(1, rows);
        offset = clamp(offset);
    }

    void scrollBy(int rows) {
        offset = clamp(offset + rows);
    }

    void pageUp() {
        scrollBy(-visibleRows);
    }

    void pageDown() {
        scrollBy(visibleRows);
    }

    void toTop() {
        offset = 0;
    }

    void toBottom() {
        offset = maxOffset();
    }

    /** Scrolls the least amount that makes the row visible. */
    void keepVisible(int row) {
        if (row < offset) {
            offset = clamp(row);
        } else if (row >= offset + visibleRows) {
            offset = clamp(row - visibleRows + 1);
        }
    }

    boolean isVisible(int row) {
        return row >= offset && row < offset + visibleRows;
    }

    /** Thumb length in track cells: proportional to visible over total rows, at least 1. */
    int thumbSize(int trackCells) {
        if (trackCells <= 0) {
            return 0;
        }
        if (!needed()) {
            return trackCells;
        }
        int size = Math.round((float) trackCells * visibleRows / totalRows);
        return Math.max(1, Math.min(trackCells, size));
    }

    /** First thumb cell in the track; the thumb touches the track end exactly at the last offset. */
    int thumbStart(int trackCells) {
        int travel = trackCells - thumbSize(trackCells);
        int max = maxOffset();
        if (travel <= 0 || max == 0) {
            return 0;
        }
        return Math.round((float) offset * travel / max);
    }

    /** Pages towards a clicked track cell; clicks on the thumb do nothing. */
    void clickTrack(int trackCell, int trackCells) {
        int start = thumbStart(trackCells);
        if (trackCell < start) {
            pageUp();
        } else if (trackCell >= start + thumbSize(trackCells)) {
            pageDown();
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(maxOffset(), value));
    }
}
