/**
 * Animated effects over the cells of a shape on a layer. Each frame, call
 * {@link com.github.halfdeadgames.asciigdx.layer.LayerStack#beginCellEffects} once, then
 * {@link com.github.halfdeadgames.asciigdx.zone.EffectZone#apply} for every zone; the caller owns
 * the zones and the time. Zones write to frame handles, which combine with the lasting handles the
 * game holds. On a map-window layer, shapes and cell positions are in map coordinates, so zones and
 * per-cell variation stay with the world as the camera moves. Time is in seconds, passed in by the
 * game.
 *
 * <p>Example:
 * <pre>{@code
 * EffectZone zone = new EffectZone(shape, CellEffects.wave(0.25f, 1.5f, Phase.alongColumns(8f)));
 * // each frame, on the render thread:
 * stack.beginCellEffects(deltaSeconds);
 * zone.apply(layer, elapsedSeconds);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.zone;
