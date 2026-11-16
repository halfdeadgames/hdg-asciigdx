package com.github.halfdeadgames.asciigdx.draw;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeCellVisitor;
import java.util.Objects;

/**
 * Static helpers that draw text, filled rectangles, boxes, and cell shapes into a
 * {@link TerminalGrid}.
 *
 * <p>Coordinates are cells, given as column and row. Columns grow to the right and rows grow
 * downward (y-down), so row 0 is the top row. All drawing is clipped to the grid: cells outside it
 * are skipped, so text, rectangles, boxes, and shapes may start or end off-grid. Colors are packed
 * RGBA8888 ints. None of the methods allocate.
 *
 * <p>The methods are not thread-safe, because shape drawing reuses one shared visitor. Call them
 * from the render thread, like {@link TerminalGrid}.
 *
 * <p>Example:
 * <pre>{@code
 * TerminalGrid grid = new TerminalGrid(40, 12);
 * GridDrawing.fillRectangle(grid, 0, 0, 40, 12, ' ', 0xFFFFFFFF, 0x000000FF);
 * GridDrawing.drawBox(grid, 1, 1, 20, 5, 0xFFFFFFFF, 0x000000FF);
 * GridDrawing.drawText(grid, 3, 2, "Hello", 0xFFFF00FF, 0x000000FF);
 * }</pre>
 */
public final class GridDrawing {

    private static final ShapeWriter SHAPE_WRITER = new ShapeWriter();

    private static final int HORIZONTAL_GLYPH = '─';
    private static final int VERTICAL_GLYPH = '│';
    private static final int TOP_LEFT_GLYPH = '┌';
    private static final int TOP_RIGHT_GLYPH = '┐';
    private static final int BOTTOM_LEFT_GLYPH = '└';
    private static final int BOTTOM_RIGHT_GLYPH = '┘';

    private GridDrawing() {
    }

    /**
     * Writes text on a single row, left to right, one cell per code point. Code points above U+FFFF
     * take one cell. A lone (unpaired) surrogate char is written as its own glyph. The text does
     * not wrap, and {@code '\n'} gets no special handling: it is written like any other character.
     *
     * <p>{@code column} may be negative or past the right edge. Characters that land outside the
     * grid are skipped but still consume a cell, so the visible characters keep their position. A
     * row outside the grid writes nothing.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param column column of the first character, in cells; any value is accepted, negative or
     *         past the grid
     * @param row row of the text, in cells; any value is accepted, rows outside the grid write
     *         nothing
     * @param text text to write; must not be {@code null}; may be empty
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} or {@code text} is {@code null}
     */
    public static void drawText(TerminalGrid grid, int column, int row, CharSequence text,
            int foreground,
            int background) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        if (row < 0 || row >= grid.getRows() || column >= grid.getColumns()) {
            return;
        }
        int cellColumn = column;
        int index = 0;
        int length = text.length();
        while (index < length && cellColumn < grid.getColumns()) {
            int codePoint = Character.codePointAt(text, index);
            grid.set(cellColumn, row, codePoint, foreground, background);
            index += Character.charCount(codePoint);
            cellColumn++;
        }
    }

    /**
     * Sets every cell of a rectangle to the same glyph and colors. Parts outside the grid are
     * skipped.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param left column of the left edge, in cells; any value is accepted, negative or past the
     *         grid
     * @param top row of the top edge, in cells; any value is accepted, negative or past the grid
     * @param width number of columns; zero or greater, zero draws nothing
     * @param height number of rows; zero or greater, zero draws nothing
     * @param codePoint glyph as a Unicode code point
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public static void fillRectangle(TerminalGrid grid, int left, int top, int width, int height,
            int codePoint,
            int foreground, int background) {
        Objects.requireNonNull(grid, "grid");
        requireNonNegativeSize(width, height);
        int firstColumn = clampToGrid(left, grid.getColumns());
        int endColumn = clampToGrid((long) left + width, grid.getColumns());
        int firstRow = clampToGrid(top, grid.getRows());
        int endRow = clampToGrid((long) top + height, grid.getRows());
        for (int row = firstRow; row < endRow; row++) {
            for (int column = firstColumn; column < endColumn; column++) {
                grid.set(column, row, codePoint, foreground, background);
            }
        }
    }

    /**
     * Sets every cell of a shape to the same glyph and colors. Cells outside the grid are skipped.
     *
     * <p>This method is not reentrant. Do not call it from inside a shape's own
     * {@code forEachCell} callback, because all calls share one internal writer and a nested call
     * would corrupt the outer one.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param shape cells to write, in the grid's column and row coordinates; must not be
     *         {@code null}
     * @param codePoint glyph as a Unicode code point
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} or {@code shape} is {@code null}
     */
    public static void fillShape(TerminalGrid grid, CellShape shape, int codePoint, int foreground,
            int background) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(shape, "shape");
        if (!overlapsGrid(grid, shape)) {
            return;
        }
        SHAPE_WRITER.configure(grid, codePoint, foreground, background);
        try {
            shape.forEachCell(SHAPE_WRITER);
        } finally {
            SHAPE_WRITER.release();
        }
    }

    private static boolean overlapsGrid(TerminalGrid grid, CellShape shape) {
        return shape.getRight() >= 0 && shape.getBottom() >= 0
                && shape.getLeft() < grid.getColumns() && shape.getTop() < grid.getRows();
    }

    /**
     * Draws a single-line border with box-drawing glyphs. The inside is left untouched. Parts
     * outside the grid are skipped.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param left column of the left edge, in cells; any value is accepted, negative or past the
     *         grid
     * @param top row of the top edge, in cells; any value is accepted, negative or past the grid
     * @param width number of columns; zero or greater, under 2 draws nothing
     * @param height number of rows; zero or greater, under 2 draws nothing
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public static void drawBox(TerminalGrid grid, int left, int top, int width, int height,
            int foreground,
            int background) {
        Objects.requireNonNull(grid, "grid");
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

    private static void drawHorizontalEdge(TerminalGrid grid, long firstColumn, long endColumn,
            long row,
            int foreground, int background) {
        if (row < 0 || row >= grid.getRows()) {
            return;
        }
        int start = clampToGrid(firstColumn, grid.getColumns());
        int end = clampToGrid(endColumn, grid.getColumns());
        for (int column = start; column < end; column++) {
            grid.set(column, (int) row, HORIZONTAL_GLYPH, foreground, background);
        }
    }

    private static void drawVerticalEdge(TerminalGrid grid, long column, long firstRow, long endRow,
            int foreground, int background) {
        if (column < 0 || column >= grid.getColumns()) {
            return;
        }
        int start = clampToGrid(firstRow, grid.getRows());
        int end = clampToGrid(endRow, grid.getRows());
        for (int row = start; row < end; row++) {
            grid.set((int) column, row, VERTICAL_GLYPH, foreground, background);
        }
    }

    private static void setIfInside(TerminalGrid grid, long column, long row, int codePoint,
            int foreground,
            int background) {
        if (column < 0 || column >= grid.getColumns() || row < 0 || row >= grid.getRows()) {
            return;
        }
        grid.set((int) column, (int) row, codePoint, foreground, background);
    }

    private static int clampToGrid(long position, int limit) {
        return (int) Math.min(Math.max(position, 0L), limit);
    }

    private static void requireNonNegativeSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Size must not be negative, was " + width + "x" + height);
        }
    }

    /**
     * Writes one glyph and color pair to each visited cell; the grid ignores writes outside itself.
     */
    private static final class ShapeWriter implements ShapeCellVisitor {

        private TerminalGrid grid;
        private int codePoint;
        private int foreground;
        private int background;

        void configure(TerminalGrid targetGrid, int targetCodePoint, int targetForeground,
                int targetBackground) {
            grid = targetGrid;
            codePoint = targetCodePoint;
            foreground = targetForeground;
            background = targetBackground;
        }

        void release() {
            grid = null;
        }

        @Override
        public void visit(int column, int row, float distance, float distanceFraction) {
            grid.set(column, row, codePoint, foreground, background);
        }
    }
}
