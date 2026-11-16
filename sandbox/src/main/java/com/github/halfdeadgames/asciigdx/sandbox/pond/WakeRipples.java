package com.github.halfdeadgames.asciigdx.sandbox.pond;

/**
 * Wake rings left by player steps. Each ring starts at the center of a map cell, spreads at a fixed
 * speed, and is a smooth bump around its radius whose height fades out over the ring's life.
 * Storage is fixed, so starting a ring never allocates; when all slots are in use the oldest ring
 * is replaced. Every query is a pure function of the stored rings and the time.
 */
public final class WakeRipples {

    /** Number of rings that can be alive at once. */
    public static final int MAX_RINGS = 4;
    /** How fast a ring radius grows. */
    public static final float SPEED_CELLS_PER_SECOND = 3f;
    /** How long a ring lives. */
    public static final float LIFE_SECONDS = 1f;
    /** Half the width of the ring bump. */
    public static final float HALF_WIDTH_CELLS = 0.8f;
    /** The farthest a ring can lift a cell from its start cell. */
    public static final float REACH_CELLS =
            SPEED_CELLS_PER_SECOND * LIFE_SECONDS + HALF_WIDTH_CELLS;

    private static final int REACH_WHOLE_CELLS = (int) Math.ceil(REACH_CELLS);

    private final int[] columns = new int[MAX_RINGS];
    private final int[] rows = new int[MAX_RINGS];
    private final float[] startSeconds = new float[MAX_RINGS];
    private int ringCount;
    private int nextSlot;

    /**
     * Starts a ring centered on the given map cell, replacing the oldest ring when all slots are in
     * use.
     */
    public void start(int column, int row, float timeSeconds) {
        requireFinite(timeSeconds);
        columns[nextSlot] = column;
        rows[nextSlot] = row;
        startSeconds[nextSlot] = timeSeconds;
        nextSlot = (nextSlot + 1) % MAX_RINGS;
        ringCount = Math.min(ringCount + 1, MAX_RINGS);
    }

    /** Removes every ring. */
    public void clear() {
        ringCount = 0;
        nextSlot = 0;
    }

    /** Whether any ring is alive at the time. */
    public boolean isActiveAt(float timeSeconds) {
        requireFinite(timeSeconds);
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                return true;
            }
        }
        return false;
    }

    /** The strongest ring lift at the center of the cell, 0 to 1. */
    public float liftAt(int column, int row, float timeSeconds) {
        requireFinite(timeSeconds);
        var strongest = 0f;
        for (var slot = 0; slot < ringCount; slot++) {
            strongest = Math.max(strongest, ringLiftAt(slot, column, row, timeSeconds));
        }
        return strongest;
    }

    /**
     * Leftmost column, inclusive, reached by a live ring. Only meaningful when {@link #isActiveAt}
     * is true.
     */
    public int activeLeft(float timeSeconds) {
        requireFinite(timeSeconds);
        var left = Integer.MAX_VALUE;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                left = Math.min(left, columns[slot] - REACH_WHOLE_CELLS);
            }
        }
        return left;
    }

    /**
     * Topmost row, inclusive, reached by a live ring. Only meaningful when {@link #isActiveAt} is
     * true.
     */
    public int activeTop(float timeSeconds) {
        requireFinite(timeSeconds);
        var top = Integer.MAX_VALUE;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                top = Math.min(top, rows[slot] - REACH_WHOLE_CELLS);
            }
        }
        return top;
    }

    /**
     * Rightmost column, inclusive, reached by a live ring. Only meaningful when {@link #isActiveAt}
     * is true.
     */
    public int activeRight(float timeSeconds) {
        requireFinite(timeSeconds);
        var right = Integer.MIN_VALUE;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                right = Math.max(right, columns[slot] + REACH_WHOLE_CELLS);
            }
        }
        return right;
    }

    /**
     * Bottommost row, inclusive, reached by a live ring. Only meaningful when {@link #isActiveAt}
     * is true.
     */
    public int activeBottom(float timeSeconds) {
        requireFinite(timeSeconds);
        var bottom = Integer.MIN_VALUE;
        for (var slot = 0; slot < ringCount; slot++) {
            if (isAlive(slot, timeSeconds)) {
                bottom = Math.max(bottom, rows[slot] + REACH_WHOLE_CELLS);
            }
        }
        return bottom;
    }

    private boolean isAlive(int slot, float timeSeconds) {
        var age = timeSeconds - startSeconds[slot];
        return age >= 0f && age < LIFE_SECONDS;
    }

    private float ringLiftAt(int slot, int column, int row, float timeSeconds) {
        if (!isAlive(slot, timeSeconds)) {
            return 0f;
        }
        var age = timeSeconds - startSeconds[slot];
        var distance = (float) Math.hypot(column - columns[slot], row - rows[slot]);
        var distanceFromRing = Math.abs(distance - age * SPEED_CELLS_PER_SECOND);
        if (distanceFromRing >= HALF_WIDTH_CELLS) {
            return 0f;
        }
        var bump = 0.5f + 0.5f * (float) Math.cos(Math.PI * distanceFromRing / HALF_WIDTH_CELLS);
        return bump * (1f - age / LIFE_SECONDS);
    }

    private static void requireFinite(float timeSeconds) {
        if (!Float.isFinite(timeSeconds)) {
            throw new IllegalArgumentException("timeSeconds must be finite: " + timeSeconds);
        }
    }
}
