package com.github.halfdeadgames.asciigdx.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fixed-size grid of cells. Each cell holds a glyph (a Unicode code point) and
 * foreground and background colors. The grid stores display data only.
 *
 * <p>The origin (0, 0) is the top-left cell. {@code x} grows to the right and
 * {@code y} grows downward, in text-row order.
 *
 * <p>Colors are packed RGBA8888 ints ({@code 0xRRGGBBAA}), the format returned by
 * libGDX {@code Color.rgba8888()}.
 *
 * <p>Bounds rule: writes outside the grid are silently ignored, which allows
 * clipped drawing. Reads outside the grid throw {@link IndexOutOfBoundsException}.
 * Use {@link #contains(int, int)} to check first.
 *
 * <p>Dirty tracking: a cell becomes dirty when its content actually changes.
 * Writing identical content does not mark it. Each consumer (for example a
 * renderer) creates its own {@link DirtyTracker} with {@link #createDirtyTracker()},
 * which starts with all cells dirty. The consumer visits the dirty cells, clears
 * its tracker, and releases it when done. A forgotten tracker stays registered
 * and keeps receiving marks.
 *
 * <p>Not thread-safe. Use from the render thread.
 */
public final class TerminalGrid {

    /** Glyph of a cleared cell. Nothing is drawn for it. It is the NUL code point, so NUL cannot be drawn. */
    public static final int EMPTY_GLYPH = 0;

    /** Fully transparent color (RGBA8888 with alpha 0). Color of a cleared cell. */
    public static final int TRANSPARENT_RGBA = 0x00000000;

    private final int width;
    private final int height;
    private final int[] glyphs;
    private final int[] foregrounds;
    private final int[] backgrounds;
    private final List<DirtyTracker> trackers = new ArrayList<>(2);

    /**
     * Creates a grid with all cells empty.
     *
     * @param width number of columns, must be positive
     * @param height number of rows, must be positive
     * @throws IllegalArgumentException if a size is not positive or
     *         {@code width * height} does not fit in an int
     */
    public TerminalGrid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "Grid size must be positive, was " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        int cellCount = cellCountOf(width, height);
        glyphs = new int[cellCount];
        foregrounds = new int[cellCount];
        backgrounds = new int[cellCount];
    }

    /** Returns the number of columns. */
    public int getWidth() {
        return width;
    }

    /** Returns the number of rows. */
    public int getHeight() {
        return height;
    }

    /**
     * Returns whether {@code (x, y)} is inside the grid.
     *
     * @param x column
     * @param y row
     * @return {@code true} if the cell exists
     */
    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Writes a glyph and its colors to a cell. Does nothing if {@code (x, y)} is
     * outside the grid. The cell becomes dirty only if any stored value changes.
     *
     * <p>The code point is stored as-is without validation. Invalid values are
     * left to the font layer.
     *
     * @param x column
     * @param y row
     * @param codePoint glyph as a Unicode code point
     * @param foregroundRgba foreground color as RGBA8888
     * @param backgroundRgba background color as RGBA8888
     */
    public void set(int x, int y, int codePoint, int foregroundRgba, int backgroundRgba) {
        if (!contains(x, y)) {
            return;
        }
        int index = indexOf(x, y);
        boolean changed = glyphs[index] != codePoint
            || foregrounds[index] != foregroundRgba
            || backgrounds[index] != backgroundRgba;
        if (!changed) {
            return;
        }
        glyphs[index] = codePoint;
        foregrounds[index] = foregroundRgba;
        backgrounds[index] = backgroundRgba;
        markTrackers(index);
    }

    /**
     * Creates a tracker that starts with every cell dirty and then records each
     * content change. The tracker stays registered, and keeps receiving marks,
     * until {@link DirtyTracker#release()} is called.
     *
     * @return a new tracker for this grid
     */
    public DirtyTracker createDirtyTracker() {
        var tracker = new DirtyTracker(this);
        trackers.add(tracker);
        return tracker;
    }

    void unregister(DirtyTracker tracker) {
        trackers.remove(tracker);
    }

    private void markTrackers(int index) {
        for (int i = 0, count = trackers.size(); i < count; i++) {
            trackers.get(i).mark(index);
        }
    }

    /**
     * Returns the glyph of a cell.
     *
     * @param x column
     * @param y row
     * @return the Unicode code point stored in the cell
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the grid
     */
    public int getGlyph(int x, int y) {
        return glyphs[checkedIndexOf(x, y)];
    }

    /**
     * Returns the foreground color of a cell.
     *
     * @param x column
     * @param y row
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the grid
     */
    public int getForeground(int x, int y) {
        return foregrounds[checkedIndexOf(x, y)];
    }

    /**
     * Returns the background color of a cell.
     *
     * @param x column
     * @param y row
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the grid
     */
    public int getBackground(int x, int y) {
        return backgrounds[checkedIndexOf(x, y)];
    }

    /**
     * Resets every cell to {@link #EMPTY_GLYPH} with {@link #TRANSPARENT_RGBA}
     * colors. Only cells whose content changed become dirty. Unlike
     * {@link DirtyTracker#clear()}, this changes cell content.
     */
    public void clear() {
        for (int index = 0; index < glyphs.length; index++) {
            if (glyphs[index] != EMPTY_GLYPH
                || foregrounds[index] != TRANSPARENT_RGBA
                || backgrounds[index] != TRANSPARENT_RGBA) {
                markTrackers(index);
            }
        }
        Arrays.fill(glyphs, EMPTY_GLYPH);
        Arrays.fill(foregrounds, TRANSPARENT_RGBA);
        Arrays.fill(backgrounds, TRANSPARENT_RGBA);
    }

    private static int cellCountOf(int width, int height) {
        try {
            return Math.multiplyExact(width, height);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException(
                "Grid size " + width + "x" + height + " is too large", overflow);
        }
    }

    private int indexOf(int x, int y) {
        return y * width + x;
    }

    private int checkedIndexOf(int x, int y) {
        if (!contains(x, y)) {
            throw new IndexOutOfBoundsException(
                "Cell (" + x + ", " + y + ") is outside a " + width + "x" + height + " grid");
        }
        return indexOf(x, y);
    }
}
