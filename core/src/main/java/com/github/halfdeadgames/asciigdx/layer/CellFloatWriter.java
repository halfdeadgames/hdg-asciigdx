package com.github.halfdeadgames.asciigdx.layer;

/**
 * Writes one float value to one property of a {@link CellHandle}, for example its offset, rotation,
 * or foreground mix amount. {@link MapCellTarget#writeIfShown} calls it with the cell's handle.
 *
 * <p>Pass a static method reference or a non-capturing lambda, so no object is allocated per
 * call.
 */
@FunctionalInterface
public interface CellFloatWriter {

    /**
     * Writes {@code value} to a property of {@code cell}.
     *
     * @param cell the handle of the cell to write, never null; valid only during this call, so do
     *     not store it
     * @param value the value to write; its unit and valid range are defined by the property the
     *     implementation writes
     */
    void write(CellHandle cell, float value);
}
