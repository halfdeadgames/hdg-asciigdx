package com.github.halfdeadgames.asciigdx.grid;

/**
 * Read-only view of grid cells. Each cell holds a glyph (a Unicode code point)
 * and foreground and background colors. Implementations may be mutable
 * underneath; this interface only exposes reading.
 *
 * <p>The origin (0, 0) is the top-left cell. {@code x} grows to the right and
 * {@code y} grows downward, in text-row order.
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
 */
public interface TerminalView {

    /** Returns the number of columns. */
    int getWidth();

    /** Returns the number of rows. */
    int getHeight();

    /**
     * Returns whether {@code (x, y)} is inside the view.
     *
     * @param x column
     * @param y row
     * @return {@code true} if the cell exists
     */
    boolean contains(int x, int y);

    /**
     * Returns the glyph of a cell.
     *
     * @param x column
     * @param y row
     * @return the Unicode code point stored in the cell
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the view
     */
    int getGlyph(int x, int y);

    /**
     * Returns the foreground color of a cell.
     *
     * @param x column
     * @param y row
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the view
     */
    int getForeground(int x, int y);

    /**
     * Returns the background color of a cell.
     *
     * @param x column
     * @param y row
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the view
     */
    int getBackground(int x, int y);

    /**
     * Creates a tracker that starts with every cell dirty and then records each
     * content change. The tracker stays registered, and keeps receiving marks,
     * until {@link DirtyTracker#release()} is called.
     *
     * @return a new tracker for this view
     */
    DirtyTracker createDirtyTracker();
}
