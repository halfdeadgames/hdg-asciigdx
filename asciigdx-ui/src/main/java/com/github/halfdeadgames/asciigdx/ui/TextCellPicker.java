package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.math.GridPoint2;

/**
 * Converts a libGDX input position into a TEXT column and a row of the UI surface. Used by
 * {@link UiInput}.
 *
 * <p>Warning: the {@code x} output is a text column, not a UI column. A text column is half a UI
 * cell wide, so there are {@link UiSurface#TEXT_COLUMNS_PER_CELL} (2) per UI cell. The {@code y}
 * output is a UI row. {@link UiInput} derives the UI cell with
 * {@code Math.floorDiv(textColumn, UiSurface.TEXT_COLUMNS_PER_CELL)}. A picker that returns UI
 * columns compiles, but the pointer then lands at half the real column.
 *
 * <p>A lambda passed inline to {@link UiInput#forRoot} compiles with this type or with the old
 * name, because the interface has a single method; only code that names the type must use
 * {@code TextCellPicker}.
 *
 * <p>Positions outside the surface must floor to negative values on the left of and above it, not
 * round toward zero, or a position just outside would land on column 0. Use
 * {@code Math.floorDiv} for integer math, or {@code Math.floor} for floats.
 *
 * <p>How to build one:
 * <ul>
 *   <li>With simple math, for a surface shown at integer scale from the window's top-left corner,
 *       where {@code cellWidth} is the width of a UI cell (the square font):
 * <pre>{@code
 * TextCellPicker picker = (screenX, screenY, textColumnRowOut) -> {
 *     int textColumnWidth = cellWidth * scale / UiSurface.TEXT_COLUMNS_PER_CELL;
 *     textColumnRowOut.x = Math.floorDiv(screenX, textColumnWidth);
 *     textColumnRowOut.y = Math.floorDiv(screenY, cellHeight * scale);
 * };
 * }</pre>
 *   <li>With a viewport and a {@code LayerStack}: unproject first, then call
 *       {@code LayerStack.toCell(layer, 0, viewport.getWorldHeight(), worldX, worldY, out)} with
 *       the surface's TEXT layer (for example the one added for
 *       {@link UiSurface#getTextGrid()}) and the same stack position that is passed to
 *       {@code LayerStack.draw}, so the column comes out in text columns. With a square layer,
 *       multiply the column by {@link UiSurface#TEXT_COLUMNS_PER_CELL}, which gives the left half
 *       of the cell.
 * </ul>
 */
@FunctionalInterface
public interface TextCellPicker {

    /**
     * Writes the text column and the row under a position.
     *
     * @param screenX libGDX input x, in screen pixels from the window's left edge
     * @param screenY libGDX input y, in screen pixels, growing downward from the window's top edge
     * @param textColumnRowOut receives the TEXT column in {@code x} (two per UI cell, not a UI
     *         column) and the UI row in {@code y}, row 0 at the top; the values may lie outside
     *         the surface, even be negative; never {@code null}
     */
    void toTextCell(int screenX, int screenY, GridPoint2 textColumnRowOut);
}
