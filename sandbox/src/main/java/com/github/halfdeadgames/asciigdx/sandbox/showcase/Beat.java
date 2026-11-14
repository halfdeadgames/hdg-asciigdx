package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;

public final class Beat {

    private final float startSeconds;
    private final float durationSeconds;
    private final Interpolation easing;

    public Beat(float startSeconds, float durationSeconds) {
        this(startSeconds, durationSeconds, Interpolation.linear);
    }

    public Beat(float startSeconds, float durationSeconds, Interpolation easing) {
        if (!(durationSeconds >= 0f)) {
            throw new IllegalArgumentException("durationSeconds must be >= 0: " + durationSeconds);
        }
        if (easing == null) {
            throw new IllegalArgumentException("easing must not be null");
        }
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;
        this.easing = easing;
    }

    public float progress(float timeSeconds) {
        if (timeSeconds < startSeconds) {
            return 0f;
        }
        if (timeSeconds >= end()) {
            return 1f;
        }
        return easing.apply((timeSeconds - startSeconds) / durationSeconds);
    }

    public float start() {
        return startSeconds;
    }

    public float end() {
        return startSeconds + durationSeconds;
    }

    public boolean isActive(float timeSeconds) {
        return timeSeconds >= startSeconds && timeSeconds < end();
    }
}
