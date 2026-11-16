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

    public Tween(float startSeconds, float durationSeconds, float from, float to) {
        this(startSeconds, durationSeconds, from, to, Interpolation.linear);
    }

    public Tween(float startSeconds, float durationSeconds, float from, float to,
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

    @Override
    public float valueAt(float timeSeconds) {
        return from + (to - from) * progressAt(timeSeconds);
    }

    /**
     * Eased progress, 0 before the start and 1 from the end on. Inside the span it may overshoot.
     */
    public float progressAt(float timeSeconds) {
        if (timeSeconds >= end()) {
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
        return timeSeconds >= startSeconds && timeSeconds < end();
    }

    public boolean isFinishedAt(float timeSeconds) {
        return timeSeconds >= end();
    }

    @Override
    public float start() {
        return startSeconds;
    }

    @Override
    public float end() {
        return startSeconds + durationSeconds;
    }

    public float duration() {
        return durationSeconds;
    }

    public float from() {
        return from;
    }

    public float to() {
        return to;
    }
}
