package com.github.halfdeadgames.asciigdx.tween;

/**
 * A value over time. Before {@link #getStartSeconds()} it holds its first value, from
 * {@link #getEndSeconds()} on it holds its last value.
 */
public interface FloatTrack {

    /**
     * Returns the value at the given time. Any time can be queried in any order.
     *
     * @param timeSeconds the time to query, in seconds
     * @return the value at that time
     */
    float getValueAt(float timeSeconds);

    /**
     * Returns the time at which the track starts changing.
     *
     * @return the start time in seconds
     */
    float getStartSeconds();

    /**
     * Returns the time from which the track holds its last value.
     *
     * @return the end time in seconds, not before the start time
     */
    float getEndSeconds();
}
