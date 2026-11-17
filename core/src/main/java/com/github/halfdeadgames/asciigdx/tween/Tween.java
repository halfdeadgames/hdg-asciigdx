package com.github.halfdeadgames.asciigdx.tween;

import static com.github.halfdeadgames.asciigdx.tween.TweenChecks.requireFinite;

import com.badlogic.gdx.math.Interpolation;
import java.util.Objects;

/**
 * A value that changes from {@code from} to {@code to} over a time span. The value is computed from
 * the time alone, so any time can be queried in any order.
 */
public final class Tween implements FloatTrack {

    private final float startSeconds;
    private final float durationSeconds;
    private final float from;
    private final float to;
    private final Interpolation easing;

    private Tween(float startSeconds, float durationSeconds, float from, float to,
            Interpolation easing) {
        requireFinite("startSeconds", startSeconds);
        requireFinite("durationSeconds", durationSeconds);
        if (durationSeconds < 0f) {
            throw new IllegalArgumentException("durationSeconds must be >= 0: " + durationSeconds);
        }
        requireFinite("from", from);
        requireFinite("to", to);
        Objects.requireNonNull(easing, "easing");
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;
        this.from = from;
        this.to = to;
        this.easing = easing;
    }

    /**
     * Creates a tween that changes at a constant rate from {@code from} to {@code to}.
     *
     * @param startSeconds time at which the change begins, in seconds; must be finite
     * @param durationSeconds length of the change in seconds; finite and at least 0. A duration of
     *     0 jumps to {@code to} at {@code startSeconds}.
     * @param from value before and at the start; must be finite
     * @param to value from the end on; must be finite
     * @return a new immutable tween
     * @throws IllegalArgumentException if a number is NaN or infinite, or the duration is negative
     */
    public static Tween linear(float startSeconds, float durationSeconds, float from, float to) {
        return new Tween(startSeconds, durationSeconds, from, to, Interpolation.linear);
    }

    /**
     * Creates a tween that changes from {@code from} to {@code to} following an easing curve.
     *
     * @param startSeconds time at which the change begins, in seconds; must be finite
     * @param durationSeconds length of the change in seconds; finite and at least 0. A duration of
     *     0 jumps to {@code to} at {@code startSeconds}.
     * @param from value before and at the start; must be finite
     * @param to value from the end on; must be finite
     * @param easing curve applied to the progress from 0 to 1; not null. Curves that overshoot
     *     make the value leave the range between {@code from} and {@code to} inside the span.
     * @return a new immutable tween
     * @throws NullPointerException if the easing is null
     * @throws IllegalArgumentException if a number is NaN or infinite or the duration is
     *     negative
     */
    public static Tween eased(float startSeconds, float durationSeconds, float from, float to,
            Interpolation easing) {
        return new Tween(startSeconds, durationSeconds, from, to, easing);
    }

    /**
     * Returns the value at the given time: {@code from} before the start, {@code to} from the end
     * on, and the eased value in between. Any time can be queried in any order.
     *
     * @param timeSeconds the time to query, in seconds
     * @return the value at that time
     */
    @Override
    public float getValueAt(float timeSeconds) {
        return from + (to - from) * getProgressAt(timeSeconds);
    }

    /**
     * Returns the eased progress at the given time: 0 before the start and 1 from the end on.
     * Inside the span an overshooting easing may return values below 0 or above 1.
     *
     * @param timeSeconds the time to query, in seconds
     * @return the eased progress, normally from 0 to 1
     */
    public float getProgressAt(float timeSeconds) {
        if (timeSeconds >= getEndSeconds()) {
            return 1f;
        }
        if (timeSeconds < startSeconds) {
            return 0f;
        }
        return easing.apply((timeSeconds - startSeconds) / durationSeconds);
    }

    /**
     * Returns whether the tween has begun at the given time.
     *
     * @param timeSeconds the time to query, in seconds
     * @return true if {@code timeSeconds} is at or after the start
     */
    public boolean hasStartedAt(float timeSeconds) {
        return timeSeconds >= startSeconds;
    }

    /**
     * Returns whether the given time is inside the span, start included and end excluded. A
     * tween with duration 0 is never active.
     *
     * @param timeSeconds the time to query, in seconds
     * @return true if {@code start <= timeSeconds < end}
     */
    public boolean isActiveAt(float timeSeconds) {
        return timeSeconds >= startSeconds && timeSeconds < getEndSeconds();
    }

    /**
     * Returns whether the tween has reached its end value at the given time.
     *
     * @param timeSeconds the time to query, in seconds
     * @return true if {@code timeSeconds} is at or after the end
     */
    public boolean isFinishedAt(float timeSeconds) {
        return timeSeconds >= getEndSeconds();
    }

    /**
     * Returns the time at which the change begins.
     *
     * @return the start time in seconds
     */
    @Override
    public float getStartSeconds() {
        return startSeconds;
    }

    /**
     * Returns the time at which the change ends, which is the start plus the duration.
     *
     * @return the end time in seconds
     */
    @Override
    public float getEndSeconds() {
        return startSeconds + durationSeconds;
    }

    /**
     * Returns the length of the change.
     *
     * @return the duration in seconds, at least 0
     */
    public float getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Returns the value held before and at the start.
     *
     * @return the start value
     */
    public float getFromValue() {
        return from;
    }

    /**
     * Returns the value held from the end on.
     *
     * @return the end value
     */
    public float getToValue() {
        return to;
    }
}
