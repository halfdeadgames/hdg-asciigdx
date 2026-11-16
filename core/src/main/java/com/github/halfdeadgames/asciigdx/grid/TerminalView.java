package com.github.halfdeadgames.asciigdx.grid;

/**
 * Read-only view of grid cells. Each cell holds a code point (a Unicode value
 * that picks the glyph the font draws) and foreground and background colors.
 * Implementations may be mutable underneath; this interface only exposes reading.
 *
 * <p>The origin (0, 0) is the top-left cell. {@code x} grows to the right and
 * {@code y} grows downward, in text-row order. Coordinates are cell indices, not pixels.
 *
 * <p>Colors are packed RGBA8888 ints ({@code 0xRRGGBBAA}), the format returned by
 * libGDX {@code Color.rgba8888()}.
 *
 * <p>Bounds rule: reads outside the view throw {@link IndexOutOfBoundsException}.
 * Use {@link #contains(int, int)} to check first.
 *
 * <p>Dirty tracking: each consumer (for example a renderer) creates its own
 * {@link DirtyTracker} with {@link #createDirtyTracker()}, which starts with all
 * cells dirty. Creating a tracker does not change cell content.
 *
 * <p>Only {@link TerminalGrid} implements this interface, because the grid creates the trackers.
 */
public sealed interface TerminalView permits TerminalGrid {

    /**
     * Returns the number of columns.
     *
     * @return the column count in cells, always positive
     */
    int getColumns();

    /**
     * Returns the number of rows.
     *
     * @return the row count in cells, always positive
     */
    int getRows();

    /**
     * Returns whether {@code (x, y)} is inside the view.
     *
     * @param x column index in cells; any int is accepted
     * @param y row index in cells; any int is accepted
     * @return {@code true} if {@code x} is in 0..getColumns()-1 and {@code y} is in 0..getRows()-1
     */
    boolean contains(int x, int y);

    /**
     * Returns the code point of a cell. The code point picks the glyph the font draws.
     *
     * @param x column index in cells, from 0 to getColumns()-1
     * @param y row index in cells, from 0 to getRows()-1
     * @return the Unicode code point stored in the cell
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the view
     */
    int getCodePoint(int x, int y);

    /**
     * Returns the foreground color of a cell.
     *
     * @param x column index in cells, from 0 to getColumns()-1
     * @param y row index in cells, from 0 to getRows()-1
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the view
     */
    int getForeground(int x, int y);

    /**
     * Returns the background color of a cell.
     *
     * @param x column index in cells, from 0 to getColumns()-1
     * @param y row index in cells, from 0 to getRows()-1
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the view
     */
    int getBackground(int x, int y);

    /**
     * Creates a tracker that starts with every cell dirty and then records each
     * content change. The tracker stays registered, and keeps receiving marks,
     * until {@link DirtyTracker#release()} is called.
     *
     * @return a new tracker for this view; the caller owns it and must release it
     */
    DirtyTracker createDirtyTracker();
}
