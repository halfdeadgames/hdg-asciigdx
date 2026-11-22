/**
 * Animated effects over the cells of a shape on a layer. Each frame, call
 * {@link com.github.halfdeadgames.asciigdx.layer.LayerStack#beginCellEffects} once, then
 * {@link com.github.halfdeadgames.asciigdx.zone.EffectZone#apply} for every zone; the caller owns
 * the zones and the time. Zones write to frame handles, which combine with the lasting handles the
 * game holds. On a map-window layer, shapes and cell positions are in map coordinates, so zones and
 * per-cell variation stay with the world as the camera moves. Time is in seconds, passed in by the
 * game.
 *
 * <p>{@link com.github.halfdeadgames.asciigdx.zone.Ripples} supplies expanding ring waves for
 * zones: start one on a splash, then each frame, if it is active, size an effect zone to its
 * active box. Callers write their own {@code CellEffect} that calls {@code getStrengthAt} for
 * each cell.
 *
 * <p>Ripples example:
 * <pre>{@code
 * final int maxRings = 4;
 * final float speedCellsPerSecond = 3f;
 * final float lifeSeconds = 1f;
 * final float halfWidthCells = 0.8f;
 * Ripples ripples = Ripples.of(maxRings, speedCellsPerSecond, lifeSeconds, halfWidthCells);
 * RectangleShape area = RectangleShape.forSquareCells(0, 0, 1, 1, ShapeFill.FILLED);
 * EffectZone zone = new EffectZone(area, myRippleEffect); // your CellEffect
 * ripples.start(splashColumn, splashRow, elapsedSeconds);
 * // each frame, on the render thread:
 * if (ripples.isActiveAt(elapsedSeconds)) {
 *     int left = ripples.getActiveLeftColumn(elapsedSeconds);
 *     int top = ripples.getActiveTopRow(elapsedSeconds);
 *     area.setPosition(left, top);
 *     area.setSize(ripples.getActiveRightColumn(elapsedSeconds) - left + 1,
 *             ripples.getActiveBottomRow(elapsedSeconds) - top + 1);
 *     zone.apply(layer, elapsedSeconds);
 * }
 * }</pre>
 *
 * <p>Zone example:
 * <pre>{@code
 * EffectZone zone = new EffectZone(shape, CellEffects.wave(0.25f, 1.5f, Phase.alongColumns(8f)));
 * // each frame, on the render thread:
 * stack.beginCellEffects(deltaSeconds);
 * zone.apply(layer, elapsedSeconds);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.zone;
