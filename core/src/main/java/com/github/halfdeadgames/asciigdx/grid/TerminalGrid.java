package com.github.halfdeadgames.asciigdx.grid;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

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
 * Writing identical content does not mark it. All cells start dirty. A renderer
 * draws the dirty cells with {@link #forEachDirtyCell(DirtyCellVisitor)}, then
 * calls {@link #clearDirty()}. If {@code clearDirty()} is never called, every
 * changed cell keeps being reported.
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
    private final BitSet dirtyCells;

    /**
     * Creates a grid with all cells empty and dirty.
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
        dirtyCells = new BitSet(cellCount);
        dirtyCells.set(0, cellCount);
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
        dirtyCells.set(index);
    }

    /**
     * Returns whether a cell is dirty.
     *
     * @param x column
     * @param y row
     * @return {@code true} if the cell is dirty
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the grid
     */
    public boolean isDirty(int x, int y) {
        return dirtyCells.get(checkedIndexOf(x, y));
    }

    /** Returns {@code true} if any cell is dirty. */
    public boolean hasDirtyCells() {
        return !dirtyCells.isEmpty();
    }

    /**
     * Calls the visitor once per dirty cell in row-major order (left to right,
     * top to bottom). Does not clear the dirty flags.
     *
     * <p>The visitor must not call any method that changes the grid, including
     * {@link #clearDirty()} and {@link #markAllDirty()}. Results are unspecified
     * if it does. Call {@link #clearDirty()} after this method returns.
     * Exceptions thrown by the visitor propagate.
     *
     * <p>A lambda that captures local variables allocates on each call, so
     * per-frame callers should reuse a visitor.
     *
     * @param visitor receives the coordinates of each dirty cell
     * @throws NullPointerException if {@code visitor} is null
     */
    public void forEachDirtyCell(DirtyCellVisitor visitor) {
        Objects.requireNonNull(visitor, "visitor");
        for (int index = dirtyCells.nextSetBit(0); index >= 0; index = dirtyCells.nextSetBit(index + 1)) {
            visitor.visit(index % width, index / width);
        }
    }

    /** Marks every cell clean without changing cell content. */
    public void clearDirty() {
        dirtyCells.clear();
    }

    /** Marks every cell dirty, for example to force a full redraw. */
    public void markAllDirty() {
        dirtyCells.set(0, glyphs.length);
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
     * {@link #clearDirty()}, this changes cell content.
     */
    public void clear() {
        for (int index = 0; index < glyphs.length; index++) {
            if (glyphs[index] != EMPTY_GLYPH
                || foregrounds[index] != TRANSPARENT_RGBA
                || backgrounds[index] != TRANSPARENT_RGBA) {
                dirtyCells.set(index);
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
