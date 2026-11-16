package com.github.halfdeadgames.asciigdx.tween;

import static com.github.halfdeadgames.asciigdx.tween.TweenChecks.requireFinite;

import com.badlogic.gdx.math.Interpolation;

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
        if (easing == null) {
            throw new IllegalArgumentException("easing must not be null");
        }
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;
        this.from = from;
        this.to = to;
        this.easing = easing;
    }

    public static Tween linear(float startSeconds, float durationSeconds, float from, float to) {
        return new Tween(startSeconds, durationSeconds, from, to, Interpolation.linear);
    }

    public static Tween eased(float startSeconds, float durationSeconds, float from, float to,
            Interpolation easing) {
        return new Tween(startSeconds, durationSeconds, from, to, easing);
    }

    @Override
    public float getValueAt(float timeSeconds) {
        return from + (to - from) * getProgressAt(timeSeconds);
    }

    /**
     * Eased progress, 0 before the start and 1 from the end on. Inside the span it may overshoot.
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

    public boolean hasStartedAt(float timeSeconds) {
        return timeSeconds >= startSeconds;
    }

    public boolean isActiveAt(float timeSeconds) {
        return timeSeconds >= startSeconds && timeSeconds < getEndSeconds();
    }

    public boolean isFinishedAt(float timeSeconds) {
        return timeSeconds >= getEndSeconds();
    }

    @Override
    public float getStartSeconds() {
        return startSeconds;
    }

    @Override
    public float getEndSeconds() {
        return startSeconds + durationSeconds;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public float getFromValue() {
        return from;
    }

    public float getToValue() {
        return to;
    }
}
