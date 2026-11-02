package com.github.halfdeadgames.asciigdx.grid;

import java.util.BitSet;
import java.util.Objects;

/**
 * Records which cells of a {@link TerminalGrid} changed since this tracker was
 * last cleared. Obtain one with {@link TerminalGrid#createDirtyTracker()}.
 * Each consumer should use its own tracker, so clearing one does not affect
 * the others.
 *
 * <p>A new tracker reports every cell as dirty. Call {@link #release()} when
 * done. After release the tracker stops receiving marks; its other methods
 * still work but the state no longer follows the grid.
 *
 * <p>Not thread-safe. Use from the render thread.
 */
public final class DirtyTracker {

    private final TerminalGrid grid;
    private int width;
    private int height;
    private final BitSet dirtyCells;
    private int dirtyCellCount;
    private boolean released;

    DirtyTracker(TerminalGrid grid) {
        this.grid = grid;
        width = grid.getWidth();
        height = grid.getHeight();
        dirtyCells = new BitSet(width * height);
        markAll();
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
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                "Cell (" + x + ", " + y + ") is outside a " + width + "x" + height + " grid");
        }
        return dirtyCells.get(y * width + x);
    }

    /** Returns {@code true} if any cell is dirty. */
    public boolean hasDirtyCells() {
        return !dirtyCells.isEmpty();
    }

    /**
     * Returns the number of dirty cells.
     *
     * @return the number of dirty cells, from 0 to the grid's cell count
     */
    public int getDirtyCellCount() {
        return dirtyCellCount;
    }

    /**
     * Calls the visitor once per dirty cell in row-major order (left to right,
     * top to bottom). Does not clear the dirty flags.
     *
     * <p>The visitor must not change the grid or this tracker, including
     * {@link #clear()} and {@link #markAll()}. Results are unspecified if it
     * does. Call {@link #clear()} after this method returns. Exceptions thrown
     * by the visitor propagate.
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
    public void clear() {
        dirtyCells.clear();
        dirtyCellCount = 0;
    }

    /** Marks every cell dirty, for example to force a full redraw. */
    public void markAll() {
        dirtyCellCount = width * height;
        dirtyCells.set(0, dirtyCellCount);
    }

    /**
     * Unregisters this tracker from its grid. Safe to call more than once.
     * Afterwards the tracker receives no further marks.
     */
    public void release() {
        if (!released) {
            released = true;
            grid.unregister(this);
        }
    }

    /** Returns {@code true} once {@link #release()} has been called. */
    public boolean isReleased() {
        return released;
    }

    void resetToSize(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        dirtyCells.clear();
        markAll();
    }

    void mark(int index) {
        if (!dirtyCells.get(index)) {
            dirtyCells.set(index);
            dirtyCellCount++;
        }
    }
}
