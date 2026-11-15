package com.github.halfdeadgames.asciigdx.render;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Arrays;

/**
 * A layer's grid seen as a window onto a larger map, addressed in map coordinates. Get it from
 * {@link Layer#getMapWindow()} after {@link Layer#enableMapWindow()}.
 *
 * <p>The window remembers which grid cells have not been filled since they were created or
 * scrolled into view, so the game only draws those:
 *
 * <pre>{@code
 * layer.enableMapWindow();
 * MapWindow window = layer.getMapWindow();
 * // each frame, after the camera moved:
 * window.forEachCellToFill(filler);   // filler calls window.set(mapX, mapY, ...)
 * window.markAllFilled();
 * }</pre>
 *
 * <p>Keep {@code filler} in a field, not a lambda created inside the frame loop, which would
 * allocate every frame. For the grid size to use, see {@link Layer#enableMapWindow(int, int)}.
 *
 * <p>A window is detached when the layer disables map window mode, enables it again (which creates
 * a new window), or is removed. Every method of a detached window throws
 * {@link IllegalStateException}.
 */
public final class MapWindow {

    private final Layer layer;
    private final TerminalGrid grid;
    private final MapWindowOrigin origin;

    private boolean[] needsFill;
    private int flagColumns;
    private int flagRows;
    private boolean detached;

    MapWindow(Layer layer, TerminalGrid grid, MapWindowOrigin origin) {
        this.layer = layer;
        this.grid = grid;
        this.origin = origin;
        resetFlagsToGridSize();
    }

    /**
     * Returns the map column shown at grid column 0.
     *
     * @throws IllegalStateException if this window is detached
     */
    public int getOriginColumn() {
        requireAttached();
        return origin.getOriginColumn();
    }

    /**
     * Returns the map row shown at grid row 0.
     *
     * @throws IllegalStateException if this window is detached
     */
    public int getOriginRow() {
        requireAttached();
        return origin.getOriginRow();
    }

    /**
     * Returns whether a map cell is inside the window's grid.
     *
     * @throws IllegalStateException if this window is detached
     */
    public boolean contains(int mapColumn, int mapRow) {
        requireAttached();
        return grid.contains(toGridColumn(mapColumn), toGridRow(mapRow));
    }

    /**
     * Writes a cell at map coordinates. A cell outside the window is skipped silently.
     *
     * @throws IllegalStateException if this window is detached
     */
    public void set(int mapColumn, int mapRow, int codePoint, int foregroundRgba, int backgroundRgba) {
        if (!contains(mapColumn, mapRow)) {
            return;
        }
        grid.set(toGridColumn(mapColumn), toGridRow(mapRow), codePoint, foregroundRgba, backgroundRgba);
    }

    /**
     * Returns the handle of the cell at map coordinates, as {@link Layer#cell} does for the grid
     * position.
     *
     * <p>A lasting handle is dropped when its cell leaves the window. Lasting handles are meant for
     * display-owned animation such as tweens, flashes, and bumps. Game state that must survive
     * scrolling belongs in the grid, set through the window's filler; per-frame effects belong in
     * {@link #frameCell frame handles}.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the window
     * @throws IllegalStateException if this window is detached
     */
    public CellHandle cell(int mapColumn, int mapRow) {
        requireAttached();
        return layer.cell(toGridColumn(mapColumn), toGridRow(mapRow));
    }

    /**
     * Returns the frame handle of the cell at map coordinates, as {@link Layer#frameCell} does for
     * the grid position.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the window
     * @throws IllegalStateException if this window is detached
     */
    public CellHandle frameCell(int mapColumn, int mapRow) {
        requireAttached();
        return layer.frameCell(toGridColumn(mapColumn), toGridRow(mapRow));
    }

    /** Converts a map column to a grid column, without a bounds check. */
    public int toGridColumn(int mapColumn) {
        requireAttached();
        return mapColumn - origin.getOriginColumn();
    }

    /** Converts a map row to a grid row, without a bounds check. */
    public int toGridRow(int mapRow) {
        requireAttached();
        return mapRow - origin.getOriginRow();
    }

    /** Converts a grid column to a map column, without a bounds check. */
    public int toMapColumn(int gridColumn) {
        requireAttached();
        return gridColumn + origin.getOriginColumn();
    }

    /** Converts a grid row to a map row, without a bounds check. */
    public int toMapRow(int gridRow) {
        requireAttached();
        return gridRow + origin.getOriginRow();
    }

    /**
     * Visits every cell that needs filling, in row-major order, with map coordinates. Cells need
     * filling when the window was enabled, when they scrolled into view, or when the grid was
     * resized. This only queries: call {@link #markAllFilled()} afterward.
     *
     * <p>The visitor may call {@link #set} and {@link #cell}, but must not cause a scroll. Writes do
     * not clear the flags, because a map cell may have nothing to draw.
     *
     * @throws IllegalStateException if this window is detached
     */
    public void forEachCellToFill(MapCellVisitor visitor) {
        requireAttached();
        if (visitor == null) {
            throw new IllegalArgumentException("Visitor must not be null");
        }
        resetFlagsIfGridResized();
        for (int row = 0; row < flagRows; row++) {
            visitFlaggedCellsInRow(row, visitor);
        }
    }

    /**
     * Clears the needs-fill flag of every cell.
     *
     * @throws IllegalStateException if this window is detached
     */
    public void markAllFilled() {
        requireAttached();
        resetFlagsIfGridResized();
        Arrays.fill(needsFill, false);
    }

    /** Flags every cell as needing a fill, for example after a font change made the content stale. */
    void markAllNeedFill() {
        resetFlagsIfGridResized();
        Arrays.fill(needsFill, true);
    }

    MapWindowOrigin origin() {
        return origin;
    }

    void detach() {
        detached = true;
    }

    /** Moves the flags with the content that just scrolled; cells that scrolled in are flagged. */
    void shiftFillFlags(int columns, int rows) {
        if (!flagsMatchGrid()) {
            resetFlagsToGridSize();
            return;
        }
        if (columns <= -flagColumns || columns >= flagColumns || rows <= -flagRows || rows >= flagRows) {
            Arrays.fill(needsFill, true);
            return;
        }
        // Walk away from the shift direction so no source flag is overwritten before it is read.
        int rowStep = rows > 0 ? -1 : 1;
        int columnStep = columns > 0 ? -1 : 1;
        int row = rows > 0 ? flagRows - 1 : 0;
        for (int count = 0; count < flagRows; count++, row += rowStep) {
            shiftFlagsInRow(row, columns, rows, columnStep);
        }
    }

    private void shiftFlagsInRow(int row, int columns, int rows, int columnStep) {
        int sourceRow = row - rows;
        boolean sourceRowExists = sourceRow >= 0 && sourceRow < flagRows;
        int column = columns > 0 ? flagColumns - 1 : 0;
        for (int count = 0; count < flagColumns; count++, column += columnStep) {
            int sourceColumn = column - columns;
            boolean sourceExists = sourceRowExists && sourceColumn >= 0 && sourceColumn < flagColumns;
            needsFill[row * flagColumns + column] =
                    !sourceExists || needsFill[sourceRow * flagColumns + sourceColumn];
        }
    }

    private void visitFlaggedCellsInRow(int row, MapCellVisitor visitor) {
        int mapRow = origin.getOriginRow() + row;
        for (int column = 0; column < flagColumns; column++) {
            if (needsFill[row * flagColumns + column]) {
                visitor.visit(origin.getOriginColumn() + column, mapRow);
            }
        }
    }

    private boolean flagsMatchGrid() {
        return grid.getWidth() == flagColumns && grid.getHeight() == flagRows;
    }

    private void resetFlagsIfGridResized() {
        if (!flagsMatchGrid()) {
            resetFlagsToGridSize();
        }
    }

    private void resetFlagsToGridSize() {
        flagColumns = grid.getWidth();
        flagRows = grid.getHeight();
        needsFill = new boolean[flagColumns * flagRows];
        Arrays.fill(needsFill, true);
    }

    private void requireAttached() {
        if (detached) {
            throw new IllegalStateException("Map window is detached");
        }
    }
}
