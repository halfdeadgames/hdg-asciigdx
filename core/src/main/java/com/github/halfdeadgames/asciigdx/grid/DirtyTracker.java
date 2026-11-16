package com.github.halfdeadgames.asciigdx.grid;

import java.util.BitSet;
import java.util.Objects;

/**
 * Records which cells of a {@link TerminalGrid} changed since this tracker was
 * last marked clean. Obtain one with {@link TerminalGrid#createDirtyTracker()}.
 * Each consumer should use its own tracker, so marking one clean does not affect
 * the others.
 *
 * <p>A new tracker reports every cell as dirty. Cell coordinates are column and
 * row indices in cells, with (0, 0) at the top-left and y growing downward.
 *
 * <p>When the grid is resized with a call that changes anything (see
 * {@link TerminalGrid#resize(int, int, int, int)}), the tracker follows the new
 * size and reports every cell as dirty. When the grid scrolls (see
 * {@link TerminalGrid#scroll(int, int)}), every cell becomes dirty. The bounds
 * accepted by {@link #isDirty(int, int)} always follow the current grid size.
 *
 * <p>Call {@link #release()} when done. After release the tracker receives no
 * more marks and no longer follows resizes: its other methods still work, and
 * the bounds of {@link #isDirty(int, int)} stay at the grid size at the moment
 * of release.
 *
 * <p>Not thread-safe. Use from the render thread.
 */
public final class DirtyTracker {

    private final TerminalGrid grid;
    private int columns;
    private int rows;
    private final BitSet dirtyCells;
    private int dirtyCellCount;
    private boolean released;

    DirtyTracker(TerminalGrid grid) {
        this.grid = grid;
        columns = grid.getColumns();
        rows = grid.getRows();
        dirtyCells = new BitSet(columns * rows);
        markAllDirty();
    }

    /**
     * Returns whether a cell is dirty.
     *
     * @param x column index in cells, from 0 to the grid's column count minus 1
     * @param y row index in cells, from 0 to the grid's row count minus 1
     * @return {@code true} if the cell is dirty
     * @throws IndexOutOfBoundsException if {@code (x, y)} is outside the grid
     */
    public boolean isDirty(int x, int y) {
        TerminalGrid.requireCellInside(x, y, columns, rows);
        return dirtyCells.get(y * columns + x);
    }

    /**
     * Returns whether any cell is dirty.
     *
     * @return {@code true} if at least one cell is dirty
     */
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
     * top to bottom). Does not mark any cell clean.
     *
     * <p>The visitor must not change the grid or this tracker, including
     * {@link #markAllClean()} and {@link #markAllDirty()}. Results are unspecified if it
     * does. Call {@link #markAllClean()} after this method returns. Exceptions thrown
     * by the visitor propagate.
     *
     * <p>A lambda that captures local variables allocates on each call, so
     * per-frame callers should reuse a visitor.
     *
     * @param visitor receives the column and row index, in cells, of each dirty cell; must not be
     *         null
     * @throws NullPointerException if {@code visitor} is null
     */
    public void forEachDirtyCell(DirtyCellVisitor visitor) {
        Objects.requireNonNull(visitor, "visitor");
        for (int index = dirtyCells.nextSetBit(0);
             index >= 0; index = dirtyCells.nextSetBit(index + 1)) {
            visitor.visit(index % columns, index / columns);
        }
    }

    /** Marks every cell clean without changing cell content. */
    public void markAllClean() {
        dirtyCells.clear();
        dirtyCellCount = 0;
    }

    /** Marks every cell dirty, for example to force a full redraw. */
    public void markAllDirty() {
        dirtyCellCount = columns * rows;
        dirtyCells.set(0, dirtyCellCount);
    }

    /**
     * Unregisters this tracker from its grid. Safe to call more than once.
     * Afterwards the tracker receives no further marks and no longer follows
     * grid resizes.
     */
    public void release() {
        if (!released) {
            released = true;
            grid.unregister(this);
        }
    }

    /**
     * Returns whether {@link #release()} has been called.
     *
     * @return {@code true} once {@link #release()} has been called
     */
    public boolean isReleased() {
        return released;
    }

    void resetToSize(int newColumns, int newRows) {
        columns = newColumns;
        rows = newRows;
        dirtyCells.clear();
        markAllDirty();
    }

    void mark(int index) {
        if (!dirtyCells.get(index)) {
            dirtyCells.set(index);
            dirtyCellCount++;
        }
    }
}
