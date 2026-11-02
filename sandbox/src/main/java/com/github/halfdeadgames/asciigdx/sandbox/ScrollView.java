package com.github.halfdeadgames.asciigdx.sandbox;

/** Whole-cell sizing and scrolling math for a window showing part of a larger grid. */
final class ScrollView {
    static final int MIN_VISIBLE_COLUMNS = 40;
    static final int MIN_VISIBLE_ROWS = 30;

    private ScrollView() {
    }

    /** Largest whole-number scale that still shows the minimum visible cells, at least 1. */
    static int chooseScale(int windowWidth, int windowHeight, int cellSize) {
        int scaleForColumns = windowWidth / (cellSize * MIN_VISIBLE_COLUMNS);
        int scaleForRows = windowHeight / (cellSize * MIN_VISIBLE_ROWS);
        return Math.max(1, Math.min(scaleForColumns, scaleForRows));
    }

    /** Whole cells that fit in the window along one axis, at least 1. */
    static int visibleCells(int windowPixels, int cellSize, int scale) {
        return Math.max(1, windowPixels / (cellSize * scale));
    }

    /** Whole cells covered by an overlay of the given screen-pixel size, rounded up. */
    static int coveredCells(int overlayPixels, int cellSize, int scale) {
        int cellPixels = cellSize * scale;
        return (overlayPixels + cellPixels - 1) / cellPixels;
    }

    /** Keeps a scroll position inside 0 to the number of cells that do not fit. */
    static int clampScroll(int scroll, int contentCells, int visibleCells) {
        return Math.max(0, Math.min(scroll, contentCells - visibleCells));
    }

    /** Whole steps contained in a pixel distance, rounding toward zero. */
    static int wholeSteps(int pixels, int stepPixels) {
        return pixels / stepPixels;
    }
}
