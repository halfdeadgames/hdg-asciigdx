package com.github.halfdeadgames.asciigdx.tween;

/**
 * The same track repeated for several items, each item starting {@code stepSeconds} later than the
 * previous one.
 */
public final class StaggeredTrack {

    private final FloatTrack track;
    private final float stepSeconds;

    public StaggeredTrack(FloatTrack track, float stepSeconds) {
        if (track == null) {
            throw new IllegalArgumentException("track must not be null");
        }
        TweenChecks.requireFinite("stepSeconds", stepSeconds);
        if (stepSeconds < 0f) {
            throw new IllegalArgumentException("stepSeconds must be >= 0: " + stepSeconds);
        }
        this.track = track;
        this.stepSeconds = stepSeconds;
    }

    public float valueAt(int index, float timeSeconds) {
        return track.valueAt(timeSeconds - delayOf(index));
    }

    public float startOf(int index) {
        return track.start() + delayOf(index);
    }

    public float endOf(int index) {
        return track.end() + delayOf(index);
    }

    public boolean hasStartedAt(int index, float timeSeconds) {
        return timeSeconds >= startOf(index);
    }

    private float delayOf(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0: " + index);
        }
        return index * stepSeconds;
    }
}
