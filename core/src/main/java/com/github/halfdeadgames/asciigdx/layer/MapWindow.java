package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * <p>To slide a glyph between map cells, write the glyph at its destination yourself, blank the
 * old cell, and call {@link #startSlide}. Each frame, {@link #applySlides} then offsets the
 * destination cell so the glyph appears to travel:
 *
 * <pre>{@code
 * // when the glyph moves from (4, 2) to (5, 2):
 * window.startSlide(4, 2, 5, 2, nowSeconds, 0.15f, Interpolation.smooth);
 * // each frame:
 * stack.beginCellEffects(deltaSeconds);
 * stack.setCamera(cameraX, cameraY);
 * window.forEachCellToFill(filler);
 * window.markAllFilled();
 * window.applySlides(nowSeconds);
 * }</pre>
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

    /** Slots below activeSlideCount are running; the rest are spare and reused. */
    private final List<CellSlide> slides = new ArrayList<>();
    private int activeSlideCount;

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
     * Starts a glyph sliding from one map cell into another. The game writes the glyph at the
     * destination cell and blanks the old cell itself; if the old cell is not blanked, the glyph
     * is drawn twice. The window only animates an offset on the destination cell: it starts
     * pointing back toward the old cell and eases to zero, so the glyph appears to travel. Before
     * {@code startSeconds}, the glyph is held at the full start offset. The cells do not need to
     * be next to each other. Offsets are applied by {@link #applySlides}, and they use frame
     * handles, so the game must call {@link LayerStack#beginCellEffects} every frame.
     *
     * <pre>{@code
     * // the game moved the monster from (fromColumn, fromRow) to (toColumn, toRow):
     * window.startSlide(fromColumn, fromRow, toColumn, toRow, nowSeconds, 0.15f,
     *         Interpolation.smooth);
     * }</pre>
     *
     * <p>If a running slide already ends at ({@code fromColumn}, {@code fromRow}), the new slide
     * starts from where that glyph is drawn at {@code startSeconds}, and the old slide is
     * removed. A running slide that ends at ({@code toColumn}, {@code toRow}) is replaced. At
     * most one slide runs per destination cell. A duration of 0 adds no slide but still removes
     * the slides described above.
     *
     * <p>If an argument is invalid, no running slide is changed. Call it when the game moves the
     * glyph, on the same thread as the frame loop.
     *
     * @param fromColumn the map column the glyph leaves; any value is allowed
     * @param fromRow the map row the glyph leaves, counting down; any value is allowed
     * @param toColumn the map column the glyph arrives in; any value is allowed, and the cell may
     *     be outside the window
     * @param toRow the map row the glyph arrives in, counting down; any value is allowed
     * @param startSeconds the time the slide starts, on the same clock as {@link #applySlides};
     *     must be finite, and may be in the past or the future
     * @param durationSeconds how long the slide lasts, in seconds; finite, 0 or more
     * @param easing the curve from start to arrival; not null
     * @throws IllegalArgumentException if easing is null, durationSeconds is negative or not
     *     finite, or startSeconds is not finite
     * @throws IllegalStateException if this window is detached
     */
    public void startSlide(int fromColumn, int fromRow, int toColumn, int toRow,
            float startSeconds, float durationSeconds, Interpolation easing) {
        requireAttached();
        CellSlide candidate = obtainSpareSlide();
        int chainedIndex = findSlideTargeting(fromColumn, fromRow);
        float startOffsetColumns = fromColumn - toColumn;
        float startOffsetRows = fromRow - toRow;
        if (chainedIndex >= 0) {
            CellSlide chained = slides.get(chainedIndex);
            startOffsetColumns += chained.getOffsetColumnsAt(startSeconds);
            startOffsetRows += chained.getOffsetRowsAt(startSeconds);
        }
        candidate.start(toColumn, toRow, startOffsetColumns, startOffsetRows, startSeconds,
                durationSeconds, easing);
        removeSlideAt(chainedIndex);
        removeSlideAt(findSlideTargeting(toColumn, toRow));
        if (durationSeconds > 0f) {
            commitSpareSlide(candidate);
        }
    }

    /**
     * Removes finished slides and offsets the destination cell of every other slide, so its glyph
     * appears to travel. Slides whose destination is outside the window are skipped but keep
     * running on the clock. A slide whose start time is still ahead holds its full start offset.
     *
     * <p>Call it once per frame, in this order: {@link LayerStack#beginCellEffects}, then the
     * camera move, then the fill ({@link #forEachCellToFill} and {@link #markAllFilled}), then
     * this method. Frame handles belong to grid positions, so a scroll after this call would leave
     * the offset on the wrong cell. The offset in pixels is added to the cell's frame handle with
     * {@link CellHandle#offsetBy}, so it combines with other frame effects. Calling this twice in
     * one frame doubles the offset, and without {@link LayerStack#beginCellEffects} the offsets
     * pile up from frame to frame.
     *
     * @param timeSeconds the current time in seconds, on the same clock as the start times given
     *     to {@link #startSlide}; must be finite
     * @throws IllegalArgumentException if timeSeconds is NaN or infinite
     * @throws IllegalStateException if this window is detached
     */
    public void applySlides(float timeSeconds) {
        requireAttached();
        requireFiniteTime(timeSeconds);
        float cellWidth = layer.getGlyphSource().getCellWidth();
        float cellHeight = layer.getGlyphSource().getCellHeight();
        // Walk backward so removing the current slide swaps in one already visited.
        for (int index = activeSlideCount - 1; index >= 0; index--) {
            applySlideAt(index, timeSeconds, cellWidth, cellHeight);
        }
    }

    /**
     * Returns the horizontal offset, in virtual pixels, of the glyph sliding into a map cell at a
     * given time, or 0 if no slide ends there. The offset is measured from the destination cell,
     * so a glyph moving right starts negative. It is reported for cells outside the window too.
     * This only queries: finished slides are not removed, and no handle is written.
     *
     * <p>To make a camera follow a sliding glyph, call it before moving the camera;
     * {@link #applySlides} adds the same offset later in the frame, so the camera and the glyph
     * stay in step. The result below is in map pixels, with {@code cellWidth} taken from
     * {@code layer.getGlyphSource().getCellWidth()}:
     *
     * <pre>{@code
     * float playerX = (playerColumn + 0.5f) * cellWidth
     *         + window.getSlideOffsetX(playerColumn, playerRow, nowSeconds);
     * }</pre>
     *
     * @param mapColumn the map column of the slide's destination; any value is allowed, and the
     *     cell may be outside the window
     * @param mapRow the map row of the slide's destination; any value is allowed
     * @param timeSeconds the time in seconds, on the same clock as the start times given to
     *     {@link #startSlide}; must be finite
     * @return the offset in virtual pixels, using the layer's current cell width; positive is
     *     right, and 0 once the slide has finished or if there is none
     * @throws IllegalArgumentException if timeSeconds is NaN or infinite
     * @throws IllegalStateException if this window is detached
     */
    public float getSlideOffsetX(int mapColumn, int mapRow, float timeSeconds) {
        requireAttached();
        requireFiniteTime(timeSeconds);
        int index = findSlideTargeting(mapColumn, mapRow);
        if (index < 0) {
            return 0f;
        }
        return slides.get(index).getOffsetColumnsAt(timeSeconds)
                * layer.getGlyphSource().getCellWidth();
    }

    /**
     * Returns the vertical offset, in virtual pixels growing downward, of the glyph sliding into a
     * map cell at a given time, or 0 if no slide ends there. The offset is measured from the
     * destination cell, so a glyph moving down starts negative. Like {@link #getSlideOffsetX},
     * this only queries and reports cells outside the window; see that method for the
     * camera-follow example.
     *
     * @param mapColumn the map column of the slide's destination; any value is allowed, and the
     *     cell may be outside the window
     * @param mapRow the map row of the slide's destination; any value is allowed
     * @param timeSeconds the time in seconds, on the same clock as the start times given to
     *     {@link #startSlide}; must be finite
     * @return the offset in virtual pixels, using the layer's current cell height; positive is
     *     down, and 0 once the slide has finished or if there is none
     * @throws IllegalArgumentException if timeSeconds is NaN or infinite
     * @throws IllegalStateException if this window is detached
     */
    public float getSlideOffsetY(int mapColumn, int mapRow, float timeSeconds) {
        requireAttached();
        requireFiniteTime(timeSeconds);
        int index = findSlideTargeting(mapColumn, mapRow);
        if (index < 0) {
            return 0f;
        }
        return slides.get(index).getOffsetRowsAt(timeSeconds)
                * layer.getGlyphSource().getCellHeight();
    }

    /**
     * Removes every running slide. Call it for teleports and scene resets, where the glyph should
     * appear at its destination at once. Cells keep the glyphs the game wrote. Offsets that
     * {@link #applySlides} already added this frame stay until the next
     * {@link LayerStack#beginCellEffects}, so call this before {@link #applySlides} to take
     * effect in the same frame.
     *
     * @throws IllegalStateException if this window is detached
     */
    public void clearSlides() {
        requireAttached();
        activeSlideCount = 0;
    }

    private void applySlideAt(int index, float timeSeconds, float cellWidth, float cellHeight) {
        CellSlide slide = slides.get(index);
        if (slide.isFinishedAt(timeSeconds)) {
            removeSlideAt(index);
            return;
        }
        int column = slide.getTargetColumn();
        int row = slide.getTargetRow();
        if (!contains(column, row)) {
            return;
        }
        obtainFrameCellHandle(column, row).offsetBy(
                slide.getOffsetColumnsAt(timeSeconds) * cellWidth,
                slide.getOffsetRowsAt(timeSeconds) * cellHeight);
    }

    /** Returns the first spare slot, growing the pool only when none is free. */
    private CellSlide obtainSpareSlide() {
        if (activeSlideCount == slides.size()) {
            slides.add(new CellSlide());
        }
        return slides.get(activeSlideCount);
    }

    /** Makes the spare slot returned by {@link #obtainSpareSlide} active. */
    private void commitSpareSlide(CellSlide candidate) {
        // Removals since obtainSpareSlide may have shrunk the active range, so the candidate can
        // sit past the first spare slot.
        int candidateIndex = slides.indexOf(candidate);
        int firstSpare = activeSlideCount;
        slides.set(candidateIndex, slides.get(firstSpare));
        slides.set(firstSpare, candidate);
        activeSlideCount++;
    }

    private int findSlideTargeting(int column, int row) {
        for (int index = 0; index < activeSlideCount; index++) {
            CellSlide slide = slides.get(index);
            if (slide.getTargetColumn() == column && slide.getTargetRow() == row) {
                return index;
            }
        }
        return -1;
    }

    private void removeSlideAt(int index) {
        if (index < 0) {
            return;
        }
        int lastIndex = activeSlideCount - 1;
        CellSlide removed = slides.get(index);
        slides.set(index, slides.get(lastIndex));
        slides.set(lastIndex, removed);
        activeSlideCount--;
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
        activeSlideCount = 0;
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

    private static void requireFiniteTime(float timeSeconds) {
        if (Float.isNaN(timeSeconds) || Float.isInfinite(timeSeconds)) {
            throw new IllegalArgumentException(
                    "timeSeconds must be finite, was " + timeSeconds);
        }
    }

    private void requireAttached() {
        if (detached) {
            throw new IllegalStateException("Map window is detached");
        }
    }
}
