package com.github.halfdeadgames.asciigdx.zone;

import com.github.halfdeadgames.asciigdx.render.CellHandle;

/**
 * An animated effect applied to each cell inside an {@link EffectZone}.
 *
 * <p>Zones may overlap, and {@link EffectZone} does not arbitrate between them, so effects follow a
 * convention. Motion composes: use {@link CellHandle#offsetBy}, {@link CellHandle#rotateBy}, and
 * {@link CellHandle#scaleBy} so every zone's contribution stacks. Colors and the temporary glyph are
 * last-wins: set them directly, and the zone applied last decides.
 *
 * <p>An effect runs for every cell of its zone on every frame, so it should not allocate. Do not keep the
 * {@code cell} or {@code where} arguments after the call returns.
 */
@FunctionalInterface
public interface CellEffect {

    /**
     * Applies the effect to one cell.
     *
     * @param cell the cell's frame handle, holding whatever earlier zones set since the last
     *     {@link com.github.halfdeadgames.asciigdx.render.LayerStack#beginCellEffects}; it has no pivot,
     *     so the effect must not set one
     * @param where the cell's position in the grid and in the zone's shape
     * @param timeSeconds the time passed to {@link EffectZone#apply}
     */
    void apply(CellHandle cell, ZoneCell where, float timeSeconds);
}
