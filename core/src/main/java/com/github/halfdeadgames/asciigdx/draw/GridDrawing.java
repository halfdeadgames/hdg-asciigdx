package com.github.halfdeadgames.asciigdx.draw;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;

/**
 * Static helpers that draw text, filled rectangles, and boxes into a {@link TerminalGrid}.
 *
 * <p>All drawing is clipped to the grid: cells outside it are skipped, so shapes may start or end off-grid.
 * Colors are packed RGBA8888 ints. None of the methods allocate.
 */
public final class GridDrawing {

    private static final int HORIZONTAL_GLYPH = '─';
    private static final int VERTICAL_GLYPH = '│';
    private static final int TOP_LEFT_GLYPH = '┌';
    private static final int TOP_RIGHT_GLYPH = '┐';
    private static final int BOTTOM_LEFT_GLYPH = '└';
    private static final int BOTTOM_RIGHT_GLYPH = '┘';

    private GridDrawing() {
    }

    /**
     * Writes text on a single row, left to right, one cell per code point. Code points above U+FFFF take one cell.
     * The text does not wrap, and {@code '\n'} gets no special handling: it is written like any other character.
     * Cells outside the grid are skipped.
     *
     * @param grid grid to write to
     * @param column column of the first character
     * @param row row of the text
     * @param text text to write
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     */
    public static void text(TerminalGrid grid, int column, int row, CharSequence text, int foreground,
                            int background) {
        if (row < 0 || row >= grid.getHeight() || column >= grid.getWidth()) {
            return;
        }
        int cellColumn = column;
        int index = 0;
        int length = text.length();
        while (index < length && cellColumn < grid.getWidth()) {
            int codePoint = Character.codePointAt(text, index);
            grid.set(cellColumn, row, codePoint, foreground, background);
            index += Character.charCount(codePoint);
            cellColumn++;
        }
    }

    /**
     * Sets every cell of a rectangle to the same glyph and colors. Parts outside the grid are skipped.
     *
     * @param grid grid to write to
     * @param left column of the left edge
     * @param top row of the top edge
     * @param width number of columns; zero draws nothing
     * @param height number of rows; zero draws nothing
     * @param codePoint glyph as a Unicode code point
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public static void fill(TerminalGrid grid, int left, int top, int width, int height, int codePoint,
                            int foreground, int background) {
        requireNonNegativeSize(width, height);
        int firstColumn = clampToGrid(left, grid.getWidth());
        int endColumn = clampToGrid((long) left + width, grid.getWidth());
        int firstRow = clampToGrid(top, grid.getHeight());
        int endRow = clampToGrid((long) top + height, grid.getHeight());
        for (int row = firstRow; row < endRow; row++) {
            for (int column = firstColumn; column < endColumn; column++) {
                grid.set(column, row, codePoint, foreground, background);
            }
        }
    }

    /**
     * Draws a single-line border with box-drawing glyphs. The inside is left untouched. Parts outside the grid are
     * skipped.
     *
     * @param grid grid to write to
     * @param left column of the left edge
     * @param top row of the top edge
     * @param width number of columns; under 2 draws nothing
     * @param height number of rows; under 2 draws nothing
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public static void box(TerminalGrid grid, int left, int top, int width, int height, int foreground,
                           int background) {
        requireNonNegativeSize(width, height);
        if (width < 2 || height < 2) {
            return;
        }
        long right = (long) left + width - 1;
        long bottom = (long) top + height - 1;
        drawHorizontalEdge(grid, left + 1L, right, top, foreground, background);
        drawHorizontalEdge(grid, left + 1L, right, bottom, foreground, background);
        drawVerticalEdge(grid, left, top + 1L, bottom, foreground, background);
        drawVerticalEdge(grid, right, top + 1L, bottom, foreground, background);
        setIfInside(grid, left, top, TOP_LEFT_GLYPH, foreground, background);
        setIfInside(grid, right, top, TOP_RIGHT_GLYPH, foreground, background);
        setIfInside(grid, left, bottom, BOTTOM_LEFT_GLYPH, foreground, background);
        setIfInside(grid, right, bottom, BOTTOM_RIGHT_GLYPH, foreground, background);
    }

    private static void drawHorizontalEdge(TerminalGrid grid, long firstColumn, long endColumn, long row,
                                           int foreground, int background) {
        if (row < 0 || row >= grid.getHeight()) {
            return;
        }
        int start = clampToGrid(firstColumn, grid.getWidth());
        int end = clampToGrid(endColumn, grid.getWidth());
        for (int column = start; column < end; column++) {
            grid.set(column, (int) row, HORIZONTAL_GLYPH, foreground, background);
        }
    }

    private static void drawVerticalEdge(TerminalGrid grid, long column, long firstRow, long endRow,
                                         int foreground, int background) {
        if (column < 0 || column >= grid.getWidth()) {
            return;
        }
        int start = clampToGrid(firstRow, grid.getHeight());
        int end = clampToGrid(endRow, grid.getHeight());
        for (int row = start; row < end; row++) {
            grid.set((int) column, row, VERTICAL_GLYPH, foreground, background);
        }
    }

    private static void setIfInside(TerminalGrid grid, long column, long row, int codePoint, int foreground,
                                    int background) {
        if (column < 0 || column >= grid.getWidth() || row < 0 || row >= grid.getHeight()) {
            return;
        }
        grid.set((int) column, (int) row, codePoint, foreground, background);
    }

    private static int clampToGrid(long position, int limit) {
        return (int) Math.min(Math.max(position, 0L), limit);
    }

    private static void requireNonNegativeSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size must not be negative, was " + width + "x" + height);
        }
    }
}
