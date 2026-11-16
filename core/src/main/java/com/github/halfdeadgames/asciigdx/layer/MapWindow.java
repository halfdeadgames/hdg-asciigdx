package com.github.halfdeadgames.asciigdx.layer;

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
     * Returns the map column shown at grid column 0. It follows the layer's camera and changes when
     * the layer scrolls the grid.
     *
     * @return the map column of the grid's left edge
     * @throws IllegalStateException if this window is detached
     */
    public int getOriginColumn() {
        requireAttached();
        return origin.getOriginColumn();
    }

    /**
     * Returns the map row shown at grid row 0, counting down like grid rows. It follows the
     * layer's camera and changes when the layer scrolls the grid.
     *
     * @return the map row of the grid's top edge
     * @throws IllegalStateException if this window is detached
     */
    public int getOriginRow() {
        requireAttached();
        return origin.getOriginRow();
    }

    /**
     * Returns whether a map cell is inside the window's grid.
     *
     * @param mapColumn the map column; any value is allowed
     * @param mapRow the map row; any value is allowed
     * @return true if the cell is shown by a grid cell right now
     * @throws IllegalStateException if this window is detached
     */
    public boolean contains(int mapColumn, int mapRow) {
        requireAttached();
        return grid.contains(toGridColumn(mapColumn), toGridRow(mapRow));
    }

    /**
     * Writes a cell at map coordinates, as {@link TerminalGrid#set} does for grid coordinates. A
     * cell outside the window is skipped silently.
     *
     * @param mapColumn the map column; any value is allowed
     * @param mapRow the map row; any value is allowed
     * @param codePoint the Unicode code point of the glyph to draw
     * @param foregroundRgba the glyph color as packed RGBA8888 ({@code 0xRRGGBBAA})
     * @param backgroundRgba the cell background as packed RGBA8888 ({@code 0xRRGGBBAA})
     * @throws IllegalStateException if this window is detached
     */
    public void set(int mapColumn, int mapRow, int codePoint, int foregroundRgba,
            int backgroundRgba) {
        requireAttached();
        if (!contains(mapColumn, mapRow)) {
            return;
        }
        grid.set(toGridColumn(mapColumn), toGridRow(mapRow), codePoint, foregroundRgba,
                backgroundRgba);
    }

    /**
     * Returns the handle of the cell at map coordinates, as {@link Layer#obtainCellHandle} does for
     * the grid position.
     *
     * <p>A lasting handle is dropped when its cell leaves the window. Lasting handles are meant for
     * display-owned animation such as tweens, flashes, and bumps. Game state that must survive
     * scrolling belongs in the grid, set through the window's filler; per-frame effects belong in
     * {@link #obtainFrameCellHandle frame handles}. For a tween that must keep writing a cell after
     * it scrolls out and back in, use {@link #createCellTarget}.
     *
     * @param mapColumn the map column of a cell inside the window
     * @param mapRow the map row of a cell inside the window
     * @return the lasting handle of the cell; never null
     * @throws IndexOutOfBoundsException if the cell is outside the window
     * @throws IllegalStateException if this window is detached
     */
    public CellHandle obtainCellHandle(int mapColumn, int mapRow) {
        requireAttached();
        return layer.obtainCellHandle(toGridColumn(mapColumn), toGridRow(mapRow));
    }

    /**
     * Creates a target for the cell at map coordinates, for tween or timeline setters that must
     * keep working while the cell scrolls out of and back into the window. See
     * {@link MapCellTarget}.
     *
     * @param mapColumn the map column; any value is allowed, and the cell may be outside the window
     *     now
     * @param mapRow the map row; any value is allowed, and the cell may be outside the window now
     * @return a new target, owned by the caller
     * @throws IllegalStateException if this window is detached
     */
    public MapCellTarget createCellTarget(int mapColumn, int mapRow) {
        requireAttached();
        return new MapCellTarget(this, mapColumn, mapRow);
    }

    /**
     * Returns the frame handle of the cell at map coordinates, as
     * {@link Layer#obtainFrameCellHandle} does for the grid position. To drive it with a timeline,
     * use {@link com.github.halfdeadgames.asciigdx.tween.Timeline#writingEveryApply()} with a
     * setter that calls this method on every write; a held handle can be released or detached
     * between frames.
     *
     * @param mapColumn the map column of a cell inside the window
     * @param mapRow the map row of a cell inside the window
     * @return the frame handle of the cell; never null
     * @throws IndexOutOfBoundsException if the cell is outside the window
     * @throws IllegalStateException if this window is detached
     */
    public CellHandle obtainFrameCellHandle(int mapColumn, int mapRow) {
        requireAttached();
        return layer.obtainFrameCellHandle(toGridColumn(mapColumn), toGridRow(mapRow));
    }

    /**
     * Converts a map column to a grid column by subtracting the origin column, without a bounds
     * check. Use {@link #contains} to test whether the result is inside the grid.
     *
     * @param mapColumn the map column; any value is allowed
     * @return the grid column, which may be negative or past the grid
     * @throws IllegalStateException if this window is detached
     */
    public int toGridColumn(int mapColumn) {
        requireAttached();
        return mapColumn - origin.getOriginColumn();
    }

    /**
     * Converts a map row to a grid row by subtracting the origin row, without a bounds check. Use
     * {@link #contains} to test whether the result is inside the grid.
     *
     * @param mapRow the map row; any value is allowed
     * @return the grid row, which may be negative or past the grid
     * @throws IllegalStateException if this window is detached
     */
    public int toGridRow(int mapRow) {
        requireAttached();
        return mapRow - origin.getOriginRow();
    }

    /**
     * Converts a grid column to a map column by adding the origin column, without a bounds check.
     *
     * @param gridColumn the grid column; any value is allowed
     * @return the map column
     * @throws IllegalStateException if this window is detached
     */
    public int toMapColumn(int gridColumn) {
        requireAttached();
        return gridColumn + origin.getOriginColumn();
    }

    /**
     * Converts a grid row to a map row by adding the origin row, without a bounds check.
     *
     * @param gridRow the grid row; any value is allowed
     * @return the map row
     * @throws IllegalStateException if this window is detached
     */
    public int toMapRow(int gridRow) {
        requireAttached();
        return gridRow + origin.getOriginRow();
    }

    /**
     * Visits every cell that needs filling, in row-major order, with map coordinates. Cells need
     * filling when the window was enabled, when they scrolled into view, or when the grid was
     * resized. This only queries: call {@link #markAllFilled()} afterward.
     *
     * <p>The visitor may call {@link #set} and {@link #obtainCellHandle}, but must not cause a
     * scroll. Writes do not clear the flags, because a map cell may have nothing to draw.
     *
     * @param visitor receives each map cell to fill; not null
     * @throws IllegalArgumentException if the visitor is null
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
     * Clears the needs-fill flag of every cell. Call it after {@link #forEachCellToFill} finished
     * filling, so those cells are not visited again until they scroll into view.
     *
     * @throws IllegalStateException if this window is detached
     */
    public void markAllFilled() {
        requireAttached();
        resetFlagsIfGridResized();
        Arrays.fill(needsFill, false);
    }

    /**
     * Flags every cell as needing a fill, for example after a font change made the content stale.
     */
    void markAllNeedFill() {
        resetFlagsIfGridResized();
        Arrays.fill(needsFill, true);
    }

    void followCamera(float cameraX, float cameraY, float cellWidth, float cellHeight) {
        origin.follow(cameraX, cameraY, cellWidth, cellHeight);
    }

    /** Whether the latest {@link #followCamera} call moved the origin. */
    boolean hasShifted() {
        return origin.hasShifted();
    }

    /** Columns the origin moved in the latest {@link #followCamera} call. */
    int getShiftColumns() {
        return origin.getShiftColumns();
    }

    /** Rows the origin moved in the latest {@link #followCamera} call. */
    int getShiftRows() {
        return origin.getShiftRows();
    }

    float getOriginPixelsX(float cellWidth) {
        return origin.getOriginColumn() * cellWidth;
    }

    float getOriginPixelsY(float cellHeight) {
        return origin.getOriginRow() * cellHeight;
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
        if (columns <= -flagColumns || columns >= flagColumns || rows <= -flagRows
                || rows >= flagRows) {
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
            boolean sourceExists =
                    sourceRowExists && sourceColumn >= 0 && sourceColumn < flagColumns;
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
        return grid.getColumns() == flagColumns && grid.getRows() == flagRows;
    }

    private void resetFlagsIfGridResized() {
        if (!flagsMatchGrid()) {
            resetFlagsToGridSize();
        }
    }

    private void resetFlagsToGridSize() {
        flagColumns = grid.getColumns();
        flagRows = grid.getRows();
        needsFill = new boolean[flagColumns * flagRows];
        Arrays.fill(needsFill, true);
    }

    private void requireAttached() {
        if (detached) {
            throw new IllegalStateException("Map window is detached");
        }
    }
}
