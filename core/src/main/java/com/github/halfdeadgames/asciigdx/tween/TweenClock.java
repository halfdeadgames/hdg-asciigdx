package com.github.halfdeadgames.asciigdx.tween;

/**
 * One clock advanced by the frame delta drives every track. The delta cap keeps a long stall
 * (window drag, breakpoint) from skipping animations. Tracks are pure functions of time, so
 * {@link #setTime(float)} seeks to any moment.
 *
 * <p>Time is a {@code float}, which gets coarser as it grows. Use {@link #looping} for anything
 * that repeats (idle animations, ambient effects, or cutscene loops): its time stays below the
 * loop length, so precision never degrades. Use {@link #unbounded} for one-off animations, and
 * reset it with {@link #setTime(float)} once each animation ends. Left running, it steps in about
 * 0.5 ms increments after 2 hours and about 8 ms after 24 hours, which shows as jerky motion.
 */
public final class TweenClock {

    private static final float NOT_LOOPING = 0f;

    private final float loopSeconds;
    private final float maxDeltaSeconds;
    private float time;

    private TweenClock(float loopSeconds, float maxDeltaSeconds) {
        TweenChecks.requireFinite("maxDeltaSeconds", maxDeltaSeconds);
        if (!(maxDeltaSeconds > 0f)) {
            throw new IllegalArgumentException("maxDeltaSeconds must be > 0: " + maxDeltaSeconds);
        }
        this.loopSeconds = loopSeconds;
        this.maxDeltaSeconds = maxDeltaSeconds;
    }

    /**
     * Creates a clock whose time wraps back to zero every {@code loopSeconds}.
     *
     * @param loopSeconds     loop length in seconds; finite and greater than zero
     * @param maxDeltaSeconds largest delta one update applies; finite and greater than zero
     * @return a new looping clock at time zero
     */
    public static TweenClock looping(float loopSeconds, float maxDeltaSeconds) {
        TweenChecks.requireFinite("loopSeconds", loopSeconds);
        if (!(loopSeconds > 0f)) {
            throw new IllegalArgumentException("loopSeconds must be > 0: " + loopSeconds);
        }
        return new TweenClock(loopSeconds, maxDeltaSeconds);
    }

    /**
     * Creates a clock that only accumulates time and never wraps. Float precision drops as time
     * grows (about 0.5 ms steps after 2 hours, about 8 ms after 24 hours), so prefer
     * {@link #looping} for repeating animations, or reset this clock between one-off animations.
     *
     * @param maxDeltaSeconds largest delta one update applies; finite and greater than zero
     * @return a new unbounded clock at time zero
     */
    public static TweenClock unbounded(float maxDeltaSeconds) {
        return new TweenClock(NOT_LOOPING, maxDeltaSeconds);
    }

    /**
     * Advances the clock by the delta, capped at the maximum delta. Deltas that are zero,
     * negative, or NaN are ignored.
     *
     * @param deltaSeconds frame delta in seconds
     */
    public void update(float deltaSeconds) {
        if (!(deltaSeconds > 0f)) {
            return;
        }
        time = normalize(time + Math.min(deltaSeconds, maxDeltaSeconds));
    }

    /**
     * Seeks to the given time. A looping clock wraps it into the loop; an unbounded clock
     * requires a non-negative value.
     *
     * @param seconds finite time in seconds
     * @throws IllegalArgumentException if not finite, or negative on an unbounded clock
     */
    public void setTime(float seconds) {
        TweenChecks.requireFinite("seconds", seconds);
        if (!isLooping() && seconds < 0f) {
            throw new IllegalArgumentException(
                    "seconds must be >= 0 on an unbounded clock: " + seconds);
        }
        time = normalize(seconds);
    }

    /**
     * Returns the current time in seconds.
     *
     * @return time in seconds; within [0, loopSeconds) when looping
     */
    public float time() {
        return time;
    }

    /**
     * Tells whether this clock wraps.
     *
     * @return true for a looping clock
     */
    public boolean isLooping() {
        return loopSeconds > NOT_LOOPING;
    }

    /**
     * Returns the loop length.
     *
     * @return loop length in seconds
     * @throws IllegalStateException if the clock is unbounded
     */
    public float loopSeconds() {
        if (!isLooping()) {
            throw new IllegalStateException("Clock is not looping");
        }
        return loopSeconds;
    }

    private float normalize(float seconds) {
        return isLooping() ? wrap(seconds) : seconds;
    }

    private float wrap(float seconds) {
        float wrapped = seconds % loopSeconds;
        if (wrapped < 0f) {
            wrapped += loopSeconds;
        }
        // Float rounding can land exactly on the loop end for tiny negative inputs.
        return wrapped >= loopSeconds ? 0f : wrapped;
    }
}
