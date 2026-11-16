package com.github.halfdeadgames.asciigdx.sandbox.pond;

import java.util.Objects;

/** Turns a player's step into pushes and rest-point changes on the lilies around it. */
public final class LilyPusher {

    private static final float BOW_WAVE_SPEED = 3.0f;
    private static final float ARC_SIDE_SPEED = 3.0f;
    private static final float ARC_DELAY_SECONDS = 0.12f;
    private static final float HELD_SIDE = 0.45f;
    private static final float HELD_AHEAD = 0.2f;
    private static final int NEIGHBOR_RADIUS = 3;
    private static final float NEIGHBOR_SPEED = 2.0f;
    private static final float NEAR_ZERO_LENGTH = 1e-4f;
    private static final LilySlot[] SLOTS = LilySlot.values();

    private static final int SLOTS_PER_AXIS = 2;

    private final PondMap map;
    private final MovingLilies moving;

    /**
     * Creates a pusher for the lilies of the map; every lily it touches is added to {@code moving}.
     */
    public LilyPusher(PondMap map, MovingLilies moving) {
        this.map = Objects.requireNonNull(map, "map");
        this.moving = Objects.requireNonNull(moving, "moving");
    }

    private void markMoving(Lily lily, int cellColumn, int cellRow, LilySlot slot) {
        moving.add(lily, cellColumn * SLOTS_PER_AXIS + slot.columnOffset(),
                cellRow * SLOTS_PER_AXIS + slot.rowOffset());
    }

    /**
     * Call when the player starts a one-cell step, before the player has arrived.
     *
     * @param fromColumn column of the cell being left
     * @param fromRow row of the cell being left
     * @param deltaColumn -1, 0, or 1 columns moved
     * @param deltaRow -1, 0, or 1 rows moved; exactly one of the two deltas is not 0
     * @throws IllegalArgumentException if the step is not one cell orthogonally
     */
    public void onStepStart(int fromColumn, int fromRow, int deltaColumn, int deltaRow) {
        if (Math.abs(deltaColumn) + Math.abs(deltaRow) != 1) {
            throw new IllegalArgumentException(
                    "step must be one cell orthogonally but was (" + deltaColumn + ", " + deltaRow
                            + ")");
        }
        var destinationColumn = fromColumn + deltaColumn;
        var destinationRow = fromRow + deltaRow;
        releaseLeftCell(fromColumn, fromRow);
        disturbDestination(destinationColumn, destinationRow, deltaColumn, deltaRow);
        pushNeighbors(destinationColumn, destinationRow, fromColumn, fromRow, deltaColumn,
                deltaRow);
    }

    private void releaseLeftCell(int column, int row) {
        if (!map.contains(column, row)) {
            return;
        }
        var cell = map.cell(column, row);
        for (var slot : SLOTS) {
            if (cell.hasLily(slot)) {
                var lily = cell.lilyAt(slot);
                lily.setTarget(0f, 0f);
                markMoving(lily, column, row, slot);
            }
        }
    }

    private void disturbDestination(int column, int row, int deltaColumn, int deltaRow) {
        if (!map.contains(column, row)) {
            return;
        }
        var cell = map.cell(column, row);
        var movesAlongRow = deltaRow == 0;
        for (var slot : SLOTS) {
            if (!cell.hasLily(slot)) {
                continue;
            }
            var sideX = movesAlongRow ? 0f : (slot.columnOffset() == 0 ? -1f : 1f);
            var sideY = movesAlongRow ? (slot.rowOffset() == 0 ? -1f : 1f) : 0f;
            var lily = cell.lilyAt(slot);
            lily.push(deltaColumn * BOW_WAVE_SPEED, deltaRow * BOW_WAVE_SPEED);
            lily.pushLater(sideX * ARC_SIDE_SPEED, sideY * ARC_SIDE_SPEED, ARC_DELAY_SECONDS);
            lily.setTarget(sideX * HELD_SIDE + deltaColumn * HELD_AHEAD,
                    sideY * HELD_SIDE + deltaRow * HELD_AHEAD);
            markMoving(lily, column, row, slot);
        }
    }

    private void pushNeighbors(int destinationColumn, int destinationRow, int fromColumn,
            int fromRow,
            int deltaColumn, int deltaRow) {
        for (var offsetRow = -NEIGHBOR_RADIUS; offsetRow <= NEIGHBOR_RADIUS; offsetRow++) {
            for (var offsetColumn = -NEIGHBOR_RADIUS;
                    offsetColumn <= NEIGHBOR_RADIUS; offsetColumn++) {
                var column = destinationColumn + offsetColumn;
                var row = destinationRow + offsetRow;
                var isDestination = offsetColumn == 0 && offsetRow == 0;
                var isFromCell = column == fromColumn && row == fromRow;
                if (isDestination || isFromCell || !map.contains(column, row)) {
                    continue;
                }
                pushCell(map.cell(column, row), column, row, offsetColumn, offsetRow, deltaColumn,
                        deltaRow);
            }
        }
    }

    private void pushCell(PondCell cell, int column, int row, int offsetColumn, int offsetRow,
            int deltaColumn, int deltaRow) {
        var cellDistance = (float) Math.sqrt(offsetColumn * offsetColumn + offsetRow * offsetRow);
        var falloff = 1f - (cellDistance - 1f) / (NEIGHBOR_RADIUS - 1f);
        if (falloff <= 0f) {
            return;
        }
        falloff = Math.min(falloff, 1f);
        for (var slot : SLOTS) {
            if (cell.hasLily(slot)) {
                var lily = cell.lilyAt(slot);
                pushNeighborLily(lily, slot, offsetColumn, offsetRow, deltaColumn, deltaRow,
                        falloff);
                markMoving(lily, column, row, slot);
            }
        }
    }

    private void pushNeighborLily(Lily lily, LilySlot slot, int offsetColumn, int offsetRow,
            int deltaColumn, int deltaRow, float falloff) {
        // Slot centers sit at 0.25 or 0.75 of a cell; the destination center is at 0.5.
        var awayX = offsetColumn + 0.5f * slot.columnOffset() - 0.25f;
        var awayY = offsetRow + 0.5f * slot.rowOffset() - 0.25f;
        var awayLength = (float) Math.sqrt(awayX * awayX + awayY * awayY);
        awayX /= awayLength;
        awayY /= awayLength;
        var directionX = awayX + deltaColumn;
        var directionY = awayY + deltaRow;
        var directionLength = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (directionLength < NEAR_ZERO_LENGTH) {
            directionX = awayX;
            directionY = awayY;
        } else {
            directionX /= directionLength;
            directionY /= directionLength;
        }
        var speed = NEIGHBOR_SPEED * falloff;
        lily.push(directionX * speed, directionY * speed);
    }
}
