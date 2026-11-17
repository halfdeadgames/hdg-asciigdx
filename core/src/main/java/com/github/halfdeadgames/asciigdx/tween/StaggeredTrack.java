package com.github.halfdeadgames.asciigdx.tween;

import java.util.Objects;

/**
 * The same track repeated for several items, each item starting {@code stepSeconds} later than the
 * previous one.
 */
public final class StaggeredTrack {

    private final FloatTrack track;
    private final float stepSeconds;

    /**
     * Creates a staggered track.
     *
     * @param track the track every item plays; not null
     * @param stepSeconds delay between consecutive items in seconds; finite and at least 0
     * @throws NullPointerException if the track is null
     * @throws IllegalArgumentException if the step is NaN, infinite, or negative
     */
    public StaggeredTrack(FloatTrack track, float stepSeconds) {
        Objects.requireNonNull(track, "track");
        TweenChecks.requireFinite("stepSeconds", stepSeconds);
        if (stepSeconds < 0f) {
            throw new IllegalArgumentException("stepSeconds must be >= 0: " + stepSeconds);
        }
        this.track = track;
        this.stepSeconds = stepSeconds;
    }

    /**
     * Returns the value of one item at the given time. Item 0 plays the track as is, and item
     * {@code n} plays it {@code n * stepSeconds} later.
     *
     * @param index the item position, 0 or greater
     * @param timeSeconds the time to query, in seconds
     * @return the item's value at that time
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public float getValueAt(int index, float timeSeconds) {
        return track.getValueAt(timeSeconds - delayOf(index));
    }

    /**
     * Returns the time at which one item starts changing.
     *
     * @param index the item position, 0 or greater
     * @return the item's start time in seconds
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public float getStartSecondsOf(int index) {
        return track.getStartSeconds() + delayOf(index);
    }

    /**
     * Returns the time from which one item holds its last value.
     *
     * @param index the item position, 0 or greater
     * @return the item's end time in seconds
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public float getEndSecondsOf(int index) {
        return track.getEndSeconds() + delayOf(index);
    }

    /**
     * Returns whether one item has begun changing at the given time.
     *
     * @param index the item position, 0 or greater
     * @param timeSeconds the time to query, in seconds
     * @return true if {@code timeSeconds} is at or after the item's start
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public boolean hasStartedAt(int index, float timeSeconds) {
        return timeSeconds >= getStartSecondsOf(index);
    }

    private float delayOf(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0: " + index);
        }
        return index * stepSeconds;
    }
}
