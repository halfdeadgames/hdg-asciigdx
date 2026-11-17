package com.github.halfdeadgames.asciigdx.layer;

import java.util.Objects;

/**
 * A target for one map cell that a tween or timeline setter can keep writing while the map window
 * scrolls, without throwing. Create it with {@link MapWindow#createCellTarget}.
 *
 * <p>A lasting handle from {@link MapWindow#obtainCellHandle} is detached when its cell scrolls out
 * of the window, so a setter that holds one throws later. This target holds only the map
 * coordinates and looks the lasting handle up again on every write, skipping the write while the
 * cell is outside the window:
 *
 * <pre>{@code
 * MapCellTarget target = window.createCellTarget(mapColumn, mapRow);
 * timeline.bind(fade, amount -> target.writeIfShown(Flashes::setWhiteMix, amount));
 * }</pre>
 *
 * <p>Create the target and the bound setter once per cell, not per frame, and pass a static method
 * reference or non-capturing lambda as the writer, so nothing allocates per frame. After its window
 * is detached, {@link #writeIfShown} throws {@link IllegalStateException}.
 *
 * <p>A cell that scrolls back in gets a new lasting handle with default values. A normal timeline
 * skips writes while a value holds steady, so the new handle gets the value only at the next
 * change. If a value can hold steady while its cell scrolls back in, build the timeline with
 * {@code Timeline.writingEveryApply()}, which writes on every apply.
 */
public final class MapCellTarget {

    private final MapWindow window;
    private final int mapColumn;
    private final int mapRow;

    MapCellTarget(MapWindow window, int mapColumn, int mapRow) {
        this.window = window;
        this.mapColumn = mapColumn;
        this.mapRow = mapRow;
    }

    /**
     * Returns the map column of the target cell, as given to {@link MapWindow#createCellTarget}.
     *
     * @return the map column, which may be outside the window
     */
    public int getMapColumn() {
        return mapColumn;
    }

    /**
     * Returns the map row of the target cell, as given to {@link MapWindow#createCellTarget}.
     *
     * @return the map row, which may be outside the window
     */
    public int getMapRow() {
        return mapRow;
    }

    /**
     * Writes {@code value} through {@code writer} to the lasting handle of the target cell, if the
     * cell is inside the window now. The handle is looked up on this call and not kept. A cell
     * outside the window is skipped silently.
     *
     * @param writer receives the cell's lasting handle and {@code value}; must not be null, and
     *     should be a static method reference or non-capturing lambda
     * @param value the value passed on to {@code writer}
     * @throws NullPointerException if {@code writer} is null
     * @throws IllegalStateException if the window is detached
     */
    public void writeIfShown(CellFloatWriter writer, float value) {
        Objects.requireNonNull(writer, "writer");
        if (!window.contains(mapColumn, mapRow)) {
            return;
        }
        writer.write(window.obtainCellHandle(mapColumn, mapRow), value);
    }
}
