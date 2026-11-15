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
 * libGDX {@code Color.rgba8888()}. A 6-digit hex literal such as {@code 0xFF4040} is read as
 * {@code 0x00FF4040} and draws the wrong color; wrap it with
 * {@link com.github.halfdeadgames.asciigdx.color.CellColor#rgb(int)}.
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

    /**
     * Writes only the glyph of a cell. The colors are kept. Does nothing if
     * {@code (x, y)} is outside the grid. The cell becomes dirty only if the
     * stored glyph changes.
     *
     * <p>The code point is stored as-is without validation, like in
     * {@link #set(int, int, int, int, int)}.
     *
     * @param x column
     * @param y row
     * @param codePoint glyph as a Unicode code point
     */
    public void setGlyph(int x, int y, int codePoint) {
        writeChannel(glyphs, x, y, codePoint);
    }

    /**
     * Writes only the foreground color of a cell. The glyph and background are
     * kept. Does nothing if {@code (x, y)} is outside the grid. The cell becomes
     * dirty only if the stored color changes.
     *
     * @param x column
     * @param y row
     * @param foregroundRgba foreground color as RGBA8888
     */
    public void setForeground(int x, int y, int foregroundRgba) {
        writeChannel(foregrounds, x, y, foregroundRgba);
    }

    /**
     * Writes only the background color of a cell. The glyph and foreground are
     * kept. Does nothing if {@code (x, y)} is outside the grid. The cell becomes
     * dirty only if the stored color changes.
     *
     * @param x column
     * @param y row
     * @param backgroundRgba background color as RGBA8888
     */
    public void setBackground(int x, int y, int backgroundRgba) {
        writeChannel(backgrounds, x, y, backgroundRgba);
    }

    private void writeChannel(int[] channel, int x, int y, int value) {
        if (!contains(x, y)) {
            return;
        }
        int index = indexOf(x, y);
        if (channel[index] == value) {
            return;
        }
        channel[index] = value;
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

    /**
     * Moves the content inside the grid without changing its size: every cell {@code (x, y)} moves to
     * {@code (x + shiftX, y + shiftY)}. Cells that land outside are discarded and do not come back. Cells that
     * scroll in are empty ({@link #EMPTY_GLYPH}, {@link #TRANSPARENT_RGBA}), as after {@link #clear()}.
     *
     * <p>This moves content inside the grid, not the layer on screen, and callers must shift their own stored
     * coordinates by the same amount. It does not allocate, so it is safe for per-step camera scrolling. Use
     * {@link #resize(int, int, int, int)} for the allocating variant that also changes the size.
     *
     * <p>A zero shift does nothing. Any other shift marks every cell of every registered {@link DirtyTracker}
     * dirty. A shift of at least the width or height empties the grid.
     *
     * @param shiftX columns to move the content to the right (negative moves left)
     * @param shiftY rows to move the content down (negative moves up)
     */
    public void scroll(int shiftX, int shiftY) {
        if (shiftX == 0 && shiftY == 0) {
            return;
        }
        if (Math.abs((long) shiftX) >= width || Math.abs((long) shiftY) >= height) {
            fillCells(0, glyphs.length);
        } else {
            copyShiftedRows(shiftX, shiftY);
            clearExposedColumns(shiftX);
            clearExposedRows(shiftY);
        }
        markAllTrackers();
    }

    private void copyShiftedRows(int shiftX, int shiftY) {
        int copyLength = width - Math.abs(shiftX);
        int sourceColumn = Math.max(0, -shiftX);
        int targetColumn = Math.max(0, shiftX);
        int firstRow = Math.max(0, -shiftY);
        int endRow = height - Math.max(0, shiftY);
        // Copy against the direction of travel so source rows are read before they are overwritten.
        if (shiftY > 0) {
            for (int row = endRow - 1; row >= firstRow; row--) {
                copyRow(row, sourceColumn, row + shiftY, targetColumn, copyLength);
            }
            return;
        }
        for (int row = firstRow; row < endRow; row++) {
            copyRow(row, sourceColumn, row + shiftY, targetColumn, copyLength);
        }
    }

    private void copyRow(int sourceRow, int sourceColumn, int targetRow, int targetColumn, int length) {
        int source = sourceRow * width + sourceColumn;
        int target = targetRow * width + targetColumn;
        System.arraycopy(glyphs, source, glyphs, target, length);
        System.arraycopy(foregrounds, source, foregrounds, target, length);
        System.arraycopy(backgrounds, source, backgrounds, target, length);
    }

    private void clearExposedColumns(int shiftX) {
        if (shiftX == 0) {
            return;
        }
        int firstColumn = shiftX > 0 ? 0 : width + shiftX;
        int length = Math.abs(shiftX);
        for (int row = 0; row < height; row++) {
            int start = row * width + firstColumn;
            fillCells(start, start + length);
        }
    }

    private void clearExposedRows(int shiftY) {
        if (shiftY == 0) {
            return;
        }
        int firstRow = shiftY > 0 ? 0 : height + shiftY;
        fillCells(firstRow * width, (firstRow + Math.abs(shiftY)) * width);
    }

    private void fillCells(int fromIndex, int toIndex) {
        Arrays.fill(glyphs, fromIndex, toIndex, EMPTY_GLYPH);
        Arrays.fill(foregrounds, fromIndex, toIndex, TRANSPARENT_RGBA);
        Arrays.fill(backgrounds, fromIndex, toIndex, TRANSPARENT_RGBA);
    }

    private void markAllTrackers() {
        for (int i = 0, count = trackers.size(); i < count; i++) {
            trackers.get(i).markAll();
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
        requireCellInside(x, y, width, height);
        return indexOf(x, y);
    }

    static void requireCellInside(int x, int y, int width, int height) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                "Cell (" + x + ", " + y + ") is outside a " + width + "x" + height + " grid");
        }
    }
}
