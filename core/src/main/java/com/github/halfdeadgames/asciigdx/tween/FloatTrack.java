package com.github.halfdeadgames.asciigdx.tween;

/**
 * A value over time. Before {@link #getStartSeconds()} it holds its first value, from
 * {@link #getEndSeconds()} on it holds its last value.
 */
public interface FloatTrack {

    float getValueAt(float timeSeconds);

    float getStartSeconds();

    float getEndSeconds();
}
