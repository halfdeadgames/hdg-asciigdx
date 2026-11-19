package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.math.GridPoint2;

/**
 * Converts a libGDX input position into a cell of the UI surface. Used by {@link UiInput}.
 *
 * <p>Positions outside the surface must floor to negative values on the left of and above it, not
 * round toward zero, or a position just outside would land on cell 0. Use {@code Math.floorDiv}
 * for integer math, or {@code Math.floor} for floats.
 *
 * <p>Example with simple math, for a surface shown at integer scale from the window's top-left
 * corner:
 * <pre>{@code
 * CellPicker picker = (screenX, screenY, cellOut) -> {
 *     cellOut.x = Math.floorDiv(screenX, cellWidth * scale);
 *     cellOut.y = Math.floorDiv(screenY, cellHeight * scale);
 * };
 * }</pre>
 * With a viewport and a {@code LayerStack}, unproject first and then call
 * {@code LayerStack.toCell(layer, 0, viewport.getWorldHeight(), worldX, worldY, cellOut)} with
 * the same stack position that is passed to {@code LayerStack.draw}.
 */
@FunctionalInterface
public interface CellPicker {

    /**
     * Writes the cell under a position.
     *
     * @param screenX libGDX input x, in screen pixels from the window's left edge
     * @param screenY libGDX input y, in screen pixels, growing downward from the window's top edge
     * @param cellOut receives the column in {@code x} and the row in {@code y}, row 0 at the top;
     *         the values may lie outside the surface, even be negative; never {@code null}
     */
    void toCell(int screenX, int screenY, GridPoint2 cellOut);
}
