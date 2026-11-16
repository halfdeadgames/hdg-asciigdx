/**
 * Animated effects over the cells of a shape on a layer. Each frame, call
 * {@link com.github.halfdeadgames.asciigdx.render.LayerStack#beginCellEffects} once, then
 * {@link com.github.halfdeadgames.asciigdx.zone.EffectZone#apply} for every zone; the caller owns the
 * zones and the time. Zones write to frame handles, which combine with the lasting handles the game
 * holds. On a map-window layer, shapes and cell positions are in map coordinates, so zones and per-cell
 * variation stay with the world as the camera moves.
 */
package com.github.halfdeadgames.asciigdx.zone;
