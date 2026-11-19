package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import java.util.Objects;

/**
 * A drawing surface for widgets that mixes two fonts: a square font with cells N x N pixels and a
 * tall font with cells N x 2N pixels, both with the same column width.
 *
 * <p>Units are UI cells. One UI column is one square cell wide, and one UI row is one square cell
 * high. A tall glyph covers 1 column and 2 rows. Coordinates grow to the right and downward
 * (y-down), with the origin at the top-left corner.
 *
 * <p>The surface owns two grids. The square grid is columns x rows and holds every background and
 * all square glyphs. The tall grid is columns x ceil(rows / 2) and holds tall glyphs only; its
 * backgrounds are always {@link TerminalGrid#TRANSPARENT_RGBA}. {@link #addTo} puts the square
 * grid in a layer first and the tall grid above it, so tall glyphs draw over square glyphs.
 *
 * <p>A tall glyph at UI row r is stored in tall grid row {@code floorDiv(r, 2)}, so tall text on an
 * odd row snaps up to the even row above it. Writing a square glyph into a cell erases the tall
 * glyph that covers the cell, which clears both of that tall glyph's rows. Writing a tall glyph
 * erases the square glyphs in both of its rows.
 *
 * <p>Not thread-safe. Use it from the render thread. No method allocates.
 *
 * <p>Example:
 * <pre>{@code
 * UiSurface surface = UiSurface.ofSize(40, 12);
 * surface.addTo(layers, tallGlyphs, squareGlyphs);
 * surface.writeGlyph(UiFont.TALL, 2, 0, 'A', 0xFFFFFFFF);
 * }</pre>
 */
public final class UiSurface {

    private final int columns;
    private final int rows;
    private final TerminalGrid squareGrid;
    private final TerminalGrid tallGrid;

    private UiSurface(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.squareGrid = new TerminalGrid(columns, rows);
        this.tallGrid = new TerminalGrid(columns, (rows + 1) / 2);
    }

    /**
     * Creates a surface with empty cells.
     *
     * @param columns width in UI cells; 1 or more
     * @param rows height in UI cells; 1 or more
     * @return a new surface
     * @throws IllegalArgumentException if {@code columns} or {@code rows} is less than 1
     */
    public static UiSurface ofSize(int columns, int rows) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException(
                    "Size must be at least 1x1, was " + columns + "x" + rows);
        }
        return new UiSurface(columns, rows);
    }

    /**
     * Returns the width.
     *
     * @return the width in UI cells, 1 or more
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the height.
     *
     * @return the height in UI cells, 1 or more
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the grid that holds all backgrounds and the square glyphs. It is columns x rows. The
     * surface owns it; write to it through the surface so tall glyphs stay consistent.
     *
     * @return the square grid, never {@code null}
     */
    public TerminalGrid getSquareGrid() {
        return squareGrid;
    }

    /**
     * Returns the grid that holds the tall glyphs. It is columns x ceil(rows / 2), and every
     * background is {@link TerminalGrid#TRANSPARENT_RGBA}. The surface owns it.
     *
     * @return the tall grid, never {@code null}
     */
    public TerminalGrid getTallGrid() {
        return tallGrid;
    }

    /**
     * Adds both grids to a layer stack: the square grid first, then the tall grid on top of it.
     * Nothing is added when validation fails. Call it on the GL thread. The glyph sources are not
     * owned.
     *
     * @param layers stack to add two layers to; must not be {@code null}
     * @param tallGlyphs tall font; must not be {@code null}; cells must be as wide as the square
     *         cells and twice as high
     * @param squareGlyphs square font; must not be {@code null}; cell width must equal cell height
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the cell sizes do not form a square and tall pair
     */
    public void addTo(LayerStack layers, GlyphSource tallGlyphs, GlyphSource squareGlyphs) {
        Objects.requireNonNull(layers, "layers");
        requireFontPair(tallGlyphs, squareGlyphs);
        layers.add(squareGrid, squareGlyphs);
        layers.add(tallGrid, tallGlyphs);
    }

    static void requireFontPair(GlyphSource tallGlyphs, GlyphSource squareGlyphs) {
        Objects.requireNonNull(tallGlyphs, "tallGlyphs");
        Objects.requireNonNull(squareGlyphs, "squareGlyphs");
        int squareWidth = squareGlyphs.getCellWidth();
        int squareHeight = squareGlyphs.getCellHeight();
        int tallWidth = tallGlyphs.getCellWidth();
        int tallHeight = tallGlyphs.getCellHeight();
        if (squareWidth != squareHeight) {
            throw new IllegalArgumentException(
                    "Square font cells must be square, were " + squareWidth + "x" + squareHeight);
        }
        if (tallWidth != squareWidth || tallHeight != 2 * squareHeight) {
            throw new IllegalArgumentException("Tall font cells must be " + squareWidth + "x"
                    + 2 * squareHeight + ", were " + tallWidth + "x" + tallHeight);
        }
    }

    /**
     * Writes one glyph with its color. Cells outside the surface are skipped silently. A square
     * glyph keeps the cell's background and erases the tall glyph covering the cell, which removes
     * both rows of that tall glyph. A tall glyph is stored in tall grid row
     * {@code floorDiv(row, 2)}, so on an odd row it snaps up to the even row above, and it erases
     * the square glyphs in its two rows.
     *
     * @param font font to use; must not be {@code null}
     * @param column column in UI cells
     * @param row row in UI cells
     * @param codePoint Unicode code point of the glyph
     * @param foregroundRgba glyph color as RGBA8888
     * @throws NullPointerException if {@code font} is {@code null}
     */
    public void writeGlyph(UiFont font, int column, int row, int codePoint, int foregroundRgba) {
        Objects.requireNonNull(font, "font");
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            return;
        }
        int tallRow = Math.floorDiv(row, 2);
        if (font == UiFont.SQUARE) {
            squareGrid.setCodePoint(column, row, codePoint);
            squareGrid.setForeground(column, row, foregroundRgba);
            tallGrid.setCodePoint(column, tallRow, TerminalGrid.EMPTY_CODE_POINT);
            return;
        }
        tallGrid.setCodePoint(column, tallRow, codePoint);
        tallGrid.setForeground(column, tallRow, foregroundRgba);
        squareGrid.setCodePoint(column, 2 * tallRow, TerminalGrid.EMPTY_CODE_POINT);
        squareGrid.setCodePoint(column, 2 * tallRow + 1, TerminalGrid.EMPTY_CODE_POINT);
    }

    /**
     * Sets the background color of every square grid cell in a rectangle. Glyphs and the tall grid
     * are not touched. The rectangle is clipped to the surface.
     *
     * @param column left edge in UI cells; may be negative
     * @param row top edge in UI cells; may be negative
     * @param width width in UI cells; zero or greater
     * @param height height in UI cells; zero or greater
     * @param backgroundRgba background color as RGBA8888
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public void fillBackground(int column, int row, int width, int height, int backgroundRgba) {
        requireNonNegativeSize(width, height);
        int endColumn = (int) Math.min((long) column + width, columns);
        int endRow = (int) Math.min((long) row + height, rows);
        for (int y = Math.max(row, 0); y < endRow; y++) {
            for (int x = Math.max(column, 0); x < endColumn; x++) {
                squareGrid.setBackground(x, y, backgroundRgba);
            }
        }
    }

    /**
     * Removes the glyphs in a rectangle. Square grid cells in the rectangle are emptied, and so is
     * every tall grid cell that overlaps the rectangle's rows, even when it also covers rows
     * outside the rectangle. Backgrounds are kept. The rectangle is clipped to the surface.
     *
     * @param column left edge in UI cells; may be negative
     * @param row top edge in UI cells; may be negative
     * @param width width in UI cells; zero or greater
     * @param height height in UI cells; zero or greater
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public void eraseGlyphs(int column, int row, int width, int height) {
        requireNonNegativeSize(width, height);
        if (width == 0 || height == 0) {
            return;
        }
        int startColumn = Math.max(column, 0);
        int endColumn = (int) Math.min((long) column + width, columns);
        int endRow = (int) Math.min((long) row + height, rows);
        for (int y = Math.max(row, 0); y < endRow; y++) {
            for (int x = startColumn; x < endColumn; x++) {
                squareGrid.setCodePoint(x, y, TerminalGrid.EMPTY_CODE_POINT);
            }
        }
        int firstTallRow = Math.max(Math.floorDiv(row, 2), 0);
        int lastTallRow = Math.min(Math.floorDiv(endRow - 1, 2), tallGrid.getRows() - 1);
        for (int y = firstTallRow; y <= lastTallRow; y++) {
            for (int x = startColumn; x < endColumn; x++) {
                tallGrid.setCodePoint(x, y, TerminalGrid.EMPTY_CODE_POINT);
            }
        }
    }

    private static void requireNonNegativeSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Size must not be negative, was " + width + "x" + height);
        }
    }
}
