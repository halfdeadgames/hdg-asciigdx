package com.github.halfdeadgames.asciigdx.draw;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeCellVisitor;
import java.util.List;
import java.util.Objects;

/**
 * Static helpers that draw text, filled rectangles, boxes in several line styles, eighth-block
 * bars, and cell shapes into a {@link TerminalGrid}.
 *
 * <p>Coordinates are cells, given as column and row. Columns grow to the right and rows grow
 * downward (y-down), so row 0 is the top row. All drawing is clipped to the grid: cells outside it
 * are skipped, so text, rectangles, boxes, bars, and shapes may start or end off-grid. Colors are
 * packed RGBA8888 ints. None of the methods allocate.
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
 * GridDrawing.drawBox(grid, 24, 1, 10, 5, BoxStyle.ROUNDED, 0xFFFFFFFF, 0x000000FF);
 * GridDrawing.drawHorizontalBar(grid, 3, 4, 10, 0.55f, 0x00FF00FF, 0x000000FF);
 * GridDrawing.drawVerticalBar(grid, 38, 1, 8, 0.3f, 0xFF0000FF, 0x000000FF);
 * }</pre>
 */
public final class GridDrawing {

    private static final ShapeWriter shapeWriter = new ShapeWriter();

    private static final int EIGHTHS_PER_CELL = 8;
    private static final int FULL_BLOCK_GLYPH = 0x2588;
    private static final int EMPTY_BAR_GLYPH = ' ';
    /** Index is the number of filled eighths, 0 to 7; index 0 is never read. */
    private static final List<Integer> LEFT_EIGHTH_GLYPHS =
            List.of(EMPTY_BAR_GLYPH, 0x258F, 0x258E, 0x258D, 0x258C, 0x258B, 0x258A, 0x2589);
    /** Index is the number of filled eighths, 0 to 7; index 0 is never read. */
    private static final List<Integer> LOWER_EIGHTH_GLYPHS =
            List.of(EMPTY_BAR_GLYPH, 0x2581, 0x2582, 0x2583, 0x2584, 0x2585, 0x2586, 0x2587);

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
     * <p>For a gradient, a kept background, or a typewriter effect, use
     * {@code TextDrawing.drawText} with a {@code TextPaint}, which places text the same way.
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
        shapeWriter.configure(grid, codePoint, foreground, background);
        try {
            shape.forEachCell(shapeWriter);
        } finally {
            shapeWriter.release();
        }
    }

    private static boolean overlapsGrid(TerminalGrid grid, CellShape shape) {
        return shape.getRight() >= 0 && shape.getBottom() >= 0
                && shape.getLeft() < grid.getColumns() && shape.getTop() < grid.getRows();
    }

    /**
     * Draws a single-line border with box-drawing glyphs. The inside is left untouched. Parts
     * outside the grid are skipped. Same as calling
     * {@link #drawBox(TerminalGrid, int, int, int, int, BoxStyle, int, int)} with
     * {@link BoxStyle#SINGLE}.
     *
     * <p>The font needs the box-drawing glyphs; otherwise the glyph source's fallback glyph is
     * drawn.
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
        drawBox(grid, left, top, width, height, BoxStyle.SINGLE, foreground, background);
    }

    /**
     * Draws a border with the glyphs of the given style. The inside is left untouched. Parts
     * outside the grid are skipped.
     *
     * <p>The font needs the box-drawing glyphs of the style; otherwise the glyph source's
     * fallback glyph is drawn.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param left column of the left edge, in cells; any value is accepted, negative or past the
     *         grid
     * @param top row of the top edge, in cells; any value is accepted, negative or past the grid
     * @param width number of columns; zero or greater, under 2 draws nothing
     * @param height number of rows; zero or greater, under 2 draws nothing
     * @param style line style of the border; must not be {@code null}
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} or {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public static void drawBox(TerminalGrid grid, int left, int top, int width, int height,
            BoxStyle style, int foreground, int background) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(style, "style");
        requireNonNegativeSize(width, height);
        if (width < 2 || height < 2) {
            return;
        }
        long right = (long) left + width - 1;
        long bottom = (long) top + height - 1;
        int horizontal = style.getHorizontalGlyph();
        int vertical = style.getVerticalGlyph();
        drawHorizontalEdge(grid, left + 1L, right, top, horizontal, foreground, background);
        drawHorizontalEdge(grid, left + 1L, right, bottom, horizontal, foreground, background);
        drawVerticalEdge(grid, left, top + 1L, bottom, vertical, foreground, background);
        drawVerticalEdge(grid, right, top + 1L, bottom, vertical, foreground, background);
        setIfInside(grid, left, top, style.getTopLeftGlyph(), foreground, background);
        setIfInside(grid, right, top, style.getTopRightGlyph(), foreground, background);
        setIfInside(grid, left, bottom, style.getBottomLeftGlyph(), foreground, background);
        setIfInside(grid, right, bottom, style.getBottomRightGlyph(), foreground, background);
    }

    /**
     * Draws a bar that fills from left to right in eighths of a cell. Every one of the
     * {@code lengthCells} cells is written, so a bar drawn over an older, longer fill erases it.
     * Filled cells use full blocks (U+2588), the cell at the fill boundary uses a left eighth
     * block (U+258F to U+2589), and the remaining cells hold a space with the given colors.
     *
     * <p>The filled amount is rounded to the nearest eighth of the whole bar. Cells outside the
     * grid are skipped.
     *
     * <p>The font needs the glyphs U+2589 to U+258F and U+2588; otherwise the glyph source's
     * fallback glyph is drawn.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param left column of the leftmost cell, in cells; any value is accepted, negative or past
     *         the grid
     * @param row row of the bar, in cells, counted from the top; any value is accepted, rows
     *         outside the grid write nothing
     * @param lengthCells number of cells in the bar; zero or greater, zero draws nothing
     * @param fraction filled share of the bar; deliberately clamped to 0 to 1 rather than
     *         rejected (unlike effect amounts), so values outside the range draw an empty or full
     *         bar; NaN throws
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} is {@code null}
     * @throws IllegalArgumentException if {@code lengthCells} is negative or {@code fraction} is
     *         NaN
     */
    public static void drawHorizontalBar(TerminalGrid grid, int left, int row, int lengthCells,
            float fraction, int foreground, int background) {
        Objects.requireNonNull(grid, "grid");
        long totalEighths = totalEighths(lengthCells, fraction);
        if (row < 0 || row >= grid.getRows()) {
            return;
        }
        int firstIndex = clampToGrid(-(long) left, lengthCells);
        int endIndex = clampToGrid(grid.getColumns() - (long) left, lengthCells);
        for (int index = firstIndex; index < endIndex; index++) {
            long eighthsInCell = totalEighths - (long) index * EIGHTHS_PER_CELL;
            int codePoint = barGlyph(eighthsInCell, LEFT_EIGHTH_GLYPHS);
            grid.set(left + index, row, codePoint, foreground, background);
        }
    }

    /**
     * Draws a vertical bar anchored at its top row. Covers rows {@code top} to
     * {@code top + lengthCells - 1}; the fill starts at the bottom cell (row
     * {@code top + lengthCells - 1}) and grows toward {@code top}. Rows grow downward (y-down),
     * so the bottom cell has the highest row number. Every cell of the bar is written, so a bar
     * drawn over an older, taller fill erases it. Filled cells use full blocks (U+2588), the cell
     * at the fill boundary uses a lower eighth block (U+2581 to U+2587), and the remaining cells
     * hold a space with the given colors.
     *
     * <p>The filled amount is rounded to the nearest eighth of the whole bar. Cells outside the
     * grid are skipped.
     *
     * <p>The font needs the glyphs U+2581 to U+2588; otherwise the glyph source's fallback glyph
     * is drawn.
     *
     * <p>To place a bar whose bottom sits on a given row, pass
     * {@code top = bottomRow - lengthCells + 1}:
     * <pre>{@code
     * GridDrawing.drawVerticalBar(grid, 5, bottomRow - 8 + 1, 8, 0.5f, 0x00FF00FF, 0x000000FF);
     * }</pre>
     *
     * @param grid grid to write to; must not be {@code null}
     * @param column column of the bar, in cells; any value is accepted, columns outside the grid
     *         write nothing
     * @param top row of the topmost cell, in cells; any value is accepted, negative or past the
     *         grid
     * @param lengthCells number of cells in the bar; zero or greater, zero draws nothing
     * @param fraction filled share of the bar; deliberately clamped to 0 to 1 rather than
     *         rejected (unlike effect amounts), so values outside the range draw an empty or full
     *         bar; NaN throws
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} is {@code null}
     * @throws IllegalArgumentException if {@code lengthCells} is negative or {@code fraction} is
     *         NaN
     */
    public static void drawVerticalBar(TerminalGrid grid, int column, int top, int lengthCells,
            float fraction, int foreground, int background) {
        Objects.requireNonNull(grid, "grid");
        long totalEighths = totalEighths(lengthCells, fraction);
        if (column < 0 || column >= grid.getColumns()) {
            return;
        }
        int firstIndex = clampToGrid(-(long) top, lengthCells);
        int endIndex = clampToGrid(grid.getRows() - (long) top, lengthCells);
        for (int index = firstIndex; index < endIndex; index++) {
            int cellsFromBottom = lengthCells - 1 - index;
            long eighthsInCell = totalEighths - (long) cellsFromBottom * EIGHTHS_PER_CELL;
            int codePoint = barGlyph(eighthsInCell, LOWER_EIGHTH_GLYPHS);
            grid.set(column, top + index, codePoint, foreground, background);
        }
    }

    private static long totalEighths(int lengthCells, float fraction) {
        if (lengthCells < 0) {
            throw new IllegalArgumentException(
                    "lengthCells must not be negative, was " + lengthCells);
        }
        if (Float.isNaN(fraction)) {
            throw new IllegalArgumentException("fraction must not be NaN");
        }
        float clamped = fraction > 0f ? Math.min(fraction, 1f) : 0f;
        float eighthsProduct = clamped * lengthCells * EIGHTHS_PER_CELL;
        // Rounding as double yields a long, so huge bars never saturate at Integer.MAX_VALUE.
        return Math.round((double) eighthsProduct);
    }

    private static int barGlyph(long eighthsInCell, List<Integer> partialGlyphs) {
        if (eighthsInCell <= 0) {
            return EMPTY_BAR_GLYPH;
        }
        if (eighthsInCell >= EIGHTHS_PER_CELL) {
            return FULL_BLOCK_GLYPH;
        }
        return partialGlyphs.get((int) eighthsInCell);
    }

    private static void drawHorizontalEdge(TerminalGrid grid, long firstColumn, long endColumn,
            long row, int glyph, int foreground, int background) {
        if (row < 0 || row >= grid.getRows()) {
            return;
        }
        int start = clampToGrid(firstColumn, grid.getColumns());
        int end = clampToGrid(endColumn, grid.getColumns());
        for (int column = start; column < end; column++) {
            grid.set(column, (int) row, glyph, foreground, background);
        }
    }

    private static void drawVerticalEdge(TerminalGrid grid, long column, long firstRow,
            long endRow, int glyph, int foreground, int background) {
        if (column < 0 || column >= grid.getColumns()) {
            return;
        }
        int start = clampToGrid(firstRow, grid.getRows());
        int end = clampToGrid(endRow, grid.getRows());
        for (int row = start; row < end; row++) {
            grid.set((int) column, row, glyph, foreground, background);
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
