package com.github.halfdeadgames.asciigdx.workbench.ui;

/** Whole-row scroll position over a list of rows, plus the maths for drawing a scroll bar. */
public final class ScrollState {

    private int totalRows;
    private int visibleRows = 1;
    private int offset;

    public ScrollState(int totalRows) {
        this.totalRows = Math.max(0, totalRows);
    }

    public int offset() {
        return offset;
    }

    public int totalRows() {
        return totalRows;
    }

    public int visibleRows() {
        return visibleRows;
    }

    int maxOffset() {
        return Math.max(0, totalRows - visibleRows);
    }

    public boolean needed() {
        return totalRows > visibleRows;
    }

    /** Changes the number of rows; the offset is pulled back into range. */
    public void setTotalRows(int rows) {
        totalRows = Math.max(0, rows);
        offset = clamp(offset);
    }

    public void setVisibleRows(int rows) {
        visibleRows = Math.max(1, rows);
        offset = clamp(offset);
    }

    public void scrollBy(int rows) {
        offset = clamp(offset + rows);
    }

    public void scrollTo(int newOffset) {
        offset = clamp(newOffset);
    }

    public void pageUp() {
        scrollBy(-visibleRows);
    }

    public void pageDown() {
        scrollBy(visibleRows);
    }

    public void toTop() {
        offset = 0;
    }

    public void toBottom() {
        offset = maxOffset();
    }

    /** Scrolls the least amount that makes the row visible. */
    public void keepVisible(int row) {
        if (row < offset) {
            offset = clamp(row);
        } else if (row >= offset + visibleRows) {
            offset = clamp(row - visibleRows + 1);
        }
    }

    public boolean isVisible(int row) {
        return row >= offset && row < offset + visibleRows;
    }

    /** Thumb length in track cells: proportional to visible over total rows, at least 1. */
    public int thumbSize(int trackCells) {
        return thumbSize(trackCells, totalRows, visibleRows);
    }

    /** First thumb cell in the track; the thumb touches the track end exactly at the last offset. */
    public int thumbStart(int trackCells) {
        return thumbStart(trackCells, totalRows, visibleRows, offset);
    }

    /** Thumb length for raw numbers: total is at least 0 and visible at least 1. */
    public static int thumbSize(int trackCells, int totalRows, int visibleRows) {
        if (trackCells <= 0) {
            return 0;
        }
        int total = Math.max(0, totalRows);
        int visible = Math.max(1, visibleRows);
        if (total <= visible) {
            return trackCells;
        }
        int size = Math.round((float) trackCells * visible / total);
        return Math.max(1, Math.min(trackCells, size));
    }

    /** First thumb cell for raw numbers; the offset is clamped into range. */
    public static int thumbStart(int trackCells, int totalRows, int visibleRows, int offset) {
        int travel = trackCells - thumbSize(trackCells, totalRows, visibleRows);
        int max = Math.max(0, Math.max(0, totalRows) - Math.max(1, visibleRows));
        if (travel <= 0 || max == 0) {
            return 0;
        }
        int clampedOffset = Math.max(0, Math.min(max, offset));
        return Math.round((float) clampedOffset * travel / max);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(maxOffset(), value));
    }
}
