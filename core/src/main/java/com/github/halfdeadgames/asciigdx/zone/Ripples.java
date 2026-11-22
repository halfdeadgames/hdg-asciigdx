package com.github.halfdeadgames.asciigdx.zone;

/**
 * Expanding ring waves that start at a cell and spread outward, such as the ripples around a
 * splash. Each ring is a smooth bump around its radius whose strength fades out over the ring's
 * life. A fixed number of rings can be alive at once; starting another when all slots are in use
 * replaces the oldest, meaning the ring whose {@link #start} was called first (slot order), not the
 * one with the smallest start time. A ring whose start time is later than the query time is not
 * alive yet.
 *
 * <p>Grid coordinates are columns and rows in cells, with y pointing down (row 0 is the top). All
 * times are seconds from the game's clock. Storage is allocated once in {@link #of}, so no method
 * allocates per call. Queries do not change state. The active box is inclusive (its width is
 * right column minus left column plus 1); a cell outside every ring's box has strength 0. Box
 * edges are not clamped to any grid and may be negative or past the grid. {@link #clear} takes no
 * time. Call all methods from one thread, normally the render thread.
 *
 * <p>Example:
 * <pre>{@code
 * Ripples ripples = Ripples.of(4, 3f, 1f, 0.8f);
 * ripples.start(splashColumn, splashRow, elapsedSeconds);
 * // each frame:
 * if (ripples.isActiveAt(elapsedSeconds)) {
 *     // size the zone to the box, then read ripples.getStrengthAt(column, row, elapsedSeconds)
 * }
 * }</pre>
 */
public final class Ripples {

    /** The largest reach, in cells, that {@link #of} accepts. */
    public static final float MAX_REACH_CELLS = 1_000_000f;

    private final float speedCellsPerSecond;
    private final float lifeSeconds;
    private final float halfWidthCells;
    private final float reachCells;
    private final int reachWholeCells;
    private final int[] columns;
    private final int[] rows;
    private final float[] startSeconds;
    private int ringCount;
    private int nextSlot;

    private Ripples(
            int maxRings, float speedCellsPerSecond, float lifeSeconds, float halfWidthCells) {
        this.speedCellsPerSecond = speedCellsPerSecond;
        this.lifeSeconds = lifeSeconds;
        this.halfWidthCells = halfWidthCells;
        this.reachCells = speedCellsPerSecond * lifeSeconds + halfWidthCells;
        this.reachWholeCells = (int) Math.ceil(reachCells);
        this.columns = new int[maxRings];
        this.rows = new int[maxRings];
        this.startSeconds = new float[maxRings];
    }

    /**
     * Creates an empty set of ripples from the maximum ring count, speed, life, and half width,
     * in that order.
     *
     * @param maxRings how many rings can be alive at once; at least 1
     * @param speedCellsPerSecond how fast a ring radius grows, in cells per second; finite and
     *     greater than 0
     * @param lifeSeconds how long a ring lives, in seconds; finite and greater than 0
     * @param halfWidthCells half the width of the ring bump, in cells; finite and greater than 0
     * @return a new instance with no rings
     * @throws IllegalArgumentException if {@code maxRings} is below 1, a float is not finite and
     *     greater than 0, or the reach (speed times life plus half width) is above
     *     {@link #MAX_REACH_CELLS}
     */
    public static Ripples of(
            int maxRings, float speedCellsPerSecond, float lifeSeconds, float halfWidthCells) {
        if (maxRings < 1) {
            throw new IllegalArgumentException("maxRings must be at least 1: " + maxRings);
        }
        requirePositiveFinite("speedCellsPerSecond", speedCellsPerSecond);
        requirePositiveFinite("lifeSeconds", lifeSeconds);
        requirePositiveFinite("halfWidthCells", halfWidthCells);
        var reach = speedCellsPerSecond * lifeSeconds + halfWidthCells;
        if (!(reach <= MAX_REACH_CELLS)) {
            throw new IllegalArgumentException(
                    "speed times life plus half width must be at most " + MAX_REACH_CELLS
                            + ": " + reach);
        }
        return new Ripples(maxRings, speedCellsPerSecond, lifeSeconds, halfWidthCells);
    }

    /**
     * Starts a ring centered on the middle of a cell. When every slot is in use, the oldest ring
     * is replaced: the one whose start was called first, not the one with the smallest start time.
     * A ring whose start time is later than the query time is not alive yet.
     *
     * @param column the center column, in cells
     * @param row the center row, in cells, y-down
     * @param timeSeconds the start time, in seconds; finite
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     */
    public void start(int column, int row, float timeSeconds) {
        requireFinite(timeSeconds);
        columns[nextSlot] = column;
        rows[nextSlot] = row;
        startSeconds[nextSlot] = timeSeconds;
        nextSlot = (nextSlot + 1) % columns.length;
        ringCount = Math.min(ringCount + 1, columns.length);
    }

    /** Removes every ring. */
    public void clear() {
        ringCount = 0;
        nextSlot = 0;
    }

    /**
     * Returns whether any ring is alive at the time. A ring is alive from its start time up to,
     * but not including, its start time plus the life.
     *
     * @param timeSeconds the time to test, in seconds; finite
     * @return true if at least one ring is alive
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     */
    public boolean isActiveAt(float timeSeconds) {
        requireFinite(timeSeconds);
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the strongest ring strength at the center of a cell. Strengths of overlapping rings
     * are not added; the strongest one wins.
     *
     * @param column the column to sample, in cells
     * @param row the row to sample, in cells, y-down
     * @param timeSeconds the time to sample, in seconds; finite
     * @return the strength from 0 to 1 (the center of a ring at its start time); 0 where no ring
     *     has strength, also on a ring's outer edge
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     */
    public float getStrengthAt(int column, int row, float timeSeconds) {
        requireFinite(timeSeconds);
        var strongest = 0f;
        for (var slot = 0; slot < ringCount; slot++) {
            strongest = Math.max(strongest, ringStrengthAt(slot, column, row, timeSeconds));
        }
        return strongest;
    }

    /**
     * Returns the farthest distance from its start cell at which a ring can still have a strength
     * above 0: speed times life plus half width.
     *
     * @return the reach, in cells
     */
    public float getReachCells() {
        return reachCells;
    }

    /**
     * Returns the leftmost column, inclusive, that a living ring can reach: its center column
     * minus the reach rounded up to whole cells. Call {@link #isActiveAt} first.
     *
     * @param timeSeconds the time, in seconds; finite
     * @return the leftmost column, in cells
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     * @throws IllegalStateException if no ring is alive at the time
     */
    public int getActiveLeftColumn(float timeSeconds) {
        requireFinite(timeSeconds);
        var left = Integer.MAX_VALUE;
        var found = false;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                left = Math.min(left, columns[slot] - reachWholeCells);
                found = true;
            }
        }
        requireFound(found);
        return left;
    }

    /**
     * Returns the topmost row, inclusive, that a living ring can reach: its center row minus the
     * reach rounded up to whole cells. Call {@link #isActiveAt} first.
     *
     * @param timeSeconds the time, in seconds; finite
     * @return the topmost row, in cells, y-down
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     * @throws IllegalStateException if no ring is alive at the time
     */
    public int getActiveTopRow(float timeSeconds) {
        requireFinite(timeSeconds);
        var top = Integer.MAX_VALUE;
        var found = false;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                top = Math.min(top, rows[slot] - reachWholeCells);
                found = true;
            }
        }
        requireFound(found);
        return top;
    }

    /**
     * Returns the rightmost column, inclusive, that a living ring can reach: its center column
     * plus the reach rounded up to whole cells. Call {@link #isActiveAt} first.
     *
     * @param timeSeconds the time, in seconds; finite
     * @return the rightmost column, in cells
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     * @throws IllegalStateException if no ring is alive at the time
     */
    public int getActiveRightColumn(float timeSeconds) {
        requireFinite(timeSeconds);
        var right = Integer.MIN_VALUE;
        var found = false;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                right = Math.max(right, columns[slot] + reachWholeCells);
                found = true;
            }
        }
        requireFound(found);
        return right;
    }

    /**
     * Returns the bottommost row, inclusive, that a living ring can reach: its center row plus the
     * reach rounded up to whole cells. Call {@link #isActiveAt} first.
     *
     * @param timeSeconds the time, in seconds; finite
     * @return the bottommost row, in cells, y-down
     * @throws IllegalArgumentException if {@code timeSeconds} is not finite
     * @throws IllegalStateException if no ring is alive at the time
     */
    public int getActiveBottomRow(float timeSeconds) {
        requireFinite(timeSeconds);
        var bottom = Integer.MIN_VALUE;
        var found = false;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                bottom = Math.max(bottom, rows[slot] + reachWholeCells);
                found = true;
            }
        }
        requireFound(found);
        return bottom;
    }

    private boolean isAlive(int slot, float timeSeconds) {
        var age = timeSeconds - startSeconds[slot];
        return age >= 0f && age < lifeSeconds;
    }

    private float ringStrengthAt(int slot, int column, int row, float timeSeconds) {
        if (!isAlive(slot, timeSeconds)) {
            return 0f;
        }
        var age = timeSeconds - startSeconds[slot];
        var distance = (float) Math.hypot(column - columns[slot], row - rows[slot]);
        var distanceFromRing = Math.abs(distance - age * speedCellsPerSecond);
        if (distanceFromRing >= halfWidthCells) {
            return 0f;
        }
        var bump = 0.5f + 0.5f * (float) Math.cos(Math.PI * distanceFromRing / halfWidthCells);
        return bump * (1f - age / lifeSeconds);
    }

    private static void requireFound(boolean found) {
        if (!found) {
            throw new IllegalStateException("No ring is alive; check isActiveAt first");
        }
    }

    private static void requireFinite(float timeSeconds) {
        if (!Float.isFinite(timeSeconds)) {
            throw new IllegalArgumentException("timeSeconds must be finite: " + timeSeconds);
        }
    }

    private static void requirePositiveFinite(String name, float value) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
        }
    }
}
