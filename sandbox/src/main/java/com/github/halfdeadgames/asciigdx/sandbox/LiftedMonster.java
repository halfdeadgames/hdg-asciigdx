package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.CellTransform;
import com.github.halfdeadgames.asciigdx.render.Layer;

/**
 * A glyph in the map grid that periodically lifts out of its cell: it either bumps a third of a
 * cell toward a direction and back, or wiggles through a quarter turn and back, then settles into
 * the grid again. The cell's background never moves.
 */
final class LiftedMonster {
    private static final float PERIOD_SECONDS = 1.5f;
    private static final float MOTION_SECONDS = 0.25f;
    private static final float BUMP_CELL_FRACTION = 1f / 3f;
    private static final float WIGGLE_DEGREES = 90f;

    private final int column;
    private final int row;
    private final int glyph;
    private final int color;
    private final int directionX;
    private final int directionY;
    private final boolean wiggles;
    private final float staggerSeconds;
    private CellTransform handle;
    private boolean lifted;
    private boolean suspended;

    LiftedMonster(int column, int row, int glyph, int color, int directionX, int directionY,
                  boolean wiggles, float staggerSeconds) {
        this.column = column;
        this.row = row;
        this.glyph = glyph;
        this.color = color;
        this.directionX = directionX;
        this.directionY = directionY;
        this.wiggles = wiggles;
        this.staggerSeconds = staggerSeconds;
    }

    int getColumn() {
        return column;
    }

    int getRow() {
        return row;
    }

    /** Writes the monster's glyph over the given background, if its cell exists in the grid. */
    void placeIn(TerminalGrid grid, int background) {
        if (column >= grid.getWidth() || row >= grid.getHeight()) {
            return;
        }
        grid.set(column, row, glyph, color, background);
    }

    /** Skips the motion in progress, if any, until the next cycle starts. */
    void suspendUntilNextCycle() {
        suspended = true;
        lifted = false;
    }

    /**
     * Moves the monster for the given time on the shared clock. Drops its handle when the grid no
     * longer has the cell, and asks the layer for a fresh one when the cell is back.
     */
    void update(Layer layer, int gridColumns, int gridRows, float clockSeconds, float cellWidth, float cellHeight) {
        if (column >= gridColumns || row >= gridRows) {
            handle = null;
            lifted = false;
            return;
        }
        float cyclePosition = (clockSeconds + staggerSeconds) % PERIOD_SECONDS;
        if (cyclePosition >= MOTION_SECONDS) {
            suspended = false;
            settle();
            return;
        }
        if (suspended) {
            return;
        }
        if (handle == null || handle.isDetached()) {
            handle = layer.cell(column, row);
        }
        applyMotion(triangle(cyclePosition / MOTION_SECONDS), cellWidth, cellHeight);
        lifted = true;
    }

    private void applyMotion(float amount, float cellWidth, float cellHeight) {
        if (wiggles) {
            handle.setRotation(WIGGLE_DEGREES * amount);
            return;
        }
        handle.setOffset(directionX * amount * cellWidth * BUMP_CELL_FRACTION,
            directionY * amount * cellHeight * BUMP_CELL_FRACTION);
    }

    private void settle() {
        if (!lifted) {
            return;
        }
        lifted = false;
        if (handle != null && !handle.isDetached()) {
            handle.reset();
        }
    }

    /** Rises from 0 to 1 over the first half of the unit interval, then falls back to 0. */
    private static float triangle(float t) {
        return t < 0.5f ? 2f * t : 2f * (1f - t);
    }
}
