package com.github.halfdeadgames.asciigdx.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Grid of cells that can be resized with {@link #resize(int, int)}. Each cell holds a glyph (a Unicode code point) and
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
public final class TerminalGrid implements TerminalView {

    /** Glyph of a cleared cell. Nothing is drawn for it. It is the NUL code point, so NUL cannot be drawn. */
    public static final int EMPTY_GLYPH = 0;

    /** Fully transparent color (RGBA8888 with alpha 0). Color of a cleared cell. */
    public static final int TRANSPARENT_RGBA = 0x00000000;

    private int width;
    private int height;
    private int[] glyphs;
    private int[] foregrounds;
    private int[] backgrounds;
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

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
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

    @Override
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

    @Override
    public int getGlyph(int x, int y) {
        return glyphs[checkedIndexOf(x, y)];
    }

    @Override
    public int getForeground(int x, int y) {
        return foregrounds[checkedIndexOf(x, y)];
    }

    @Override
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

    /**
     * Resizes the grid, keeping each cell at its coordinates. Same as
     * {@code resize(columns, rows, 0, 0)}.
     *
     * @param columns new number of columns, must be positive
     * @param rows new number of rows, must be positive
     * @throws IllegalArgumentException if a size is not positive or
     *         {@code columns * rows} does not fit in an int
     * @see #resize(int, int, int, int)
     */
    public void resize(int columns, int rows) {
        resize(columns, rows, 0, 0);
    }

    /**
     * Resizes the grid and moves the content: the old cell (0, 0) lands at
     * ({@code shiftX}, {@code shiftY}), and every old cell {@code (x, y)} moves to
     * {@code (x + shiftX, y + shiftY)}. Cells that land outside the new size are
     * discarded, so negative shifts crop. Cells not covered by old content are
     * empty ({@link #EMPTY_GLYPH}, {@link #TRANSPARENT_RGBA}). Shifts may be any
     * int value; content may miss the new grid entirely.
     *
     * <p>Gotcha: a shift moves content inside the grid, not the layer on screen.
     * Layers draw from their top-left corner, so after growing upward by N rows
     * with {@code resize(w, h + N, 0, N)} the old content appears N rows lower
     * unless the caller also moves the layer offset up by N rows
     * ({@code Layer.setCellOffset} or {@code Layer.setOffset}).
     *
     * <p>Coordinates the caller has stored (positions, cursors, selections) must
     * be shifted by the same amount. Discarded cells do not come back when the
     * grid grows again.
     *
     * <p>When recentering across repeated resizes, compute each shift from the content's current position rather than
     * halving the size change, because integer division makes the halved form drift.
     *
     * <p>Resizing allocates new storage, so call it from resize handling, not
     * from per-frame code.
     *
     * <p>Resizing to the current size with a zero shift does nothing. Any other
     * call resets every registered {@link DirtyTracker} to the new size with all
     * cells dirty, including a same-size call with a non-zero shift.
     *
     * @param columns new number of columns, must be positive
     * @param rows new number of rows, must be positive
     * @param shiftX columns to move old content to the right (negative moves left)
     * @param shiftY rows to move old content down (negative moves up)
     * @throws IllegalArgumentException if a size is not positive or
     *         {@code columns * rows} does not fit in an int; the grid is unchanged
     */
    public void resize(int columns, int rows, int shiftX, int shiftY) {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException(
                "Grid size must be positive, was " + columns + "x" + rows);
        }
        int newCellCount = cellCountOf(columns, rows);
        if (columns == width && rows == height && shiftX == 0 && shiftY == 0) {
            return;
        }
        int[] newGlyphs = new int[newCellCount];
        int[] newForegrounds = new int[newCellCount];
        int[] newBackgrounds = new int[newCellCount];

        long firstColumn = Math.max(0L, -(long) shiftX);
        long endColumn = Math.min((long) width, (long) columns - shiftX);
        long firstRow = Math.max(0L, -(long) shiftY);
        long endRow = Math.min((long) height, (long) rows - shiftY);
        if (firstColumn < endColumn && firstRow < endRow) {
            int copyLength = (int) (endColumn - firstColumn);
            for (long row = firstRow; row < endRow; row++) {
                int source = (int) row * width + (int) firstColumn;
                int target = (int) (row + shiftY) * columns + (int) (firstColumn + shiftX);
                System.arraycopy(glyphs, source, newGlyphs, target, copyLength);
                System.arraycopy(foregrounds, source, newForegrounds, target, copyLength);
                System.arraycopy(backgrounds, source, newBackgrounds, target, copyLength);
            }
        }

        width = columns;
        height = rows;
        glyphs = newGlyphs;
        foregrounds = newForegrounds;
        backgrounds = newBackgrounds;
        for (int i = 0, count = trackers.size(); i < count; i++) {
            trackers.get(i).resetToSize(columns, rows);
        }
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
