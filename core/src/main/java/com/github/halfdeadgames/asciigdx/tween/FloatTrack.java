package com.github.halfdeadgames.asciigdx.tween;

/**
 * A value over time. Before {@link #start()} it holds its first value, from {@link #end()} on it
 * holds its last value.
 */
public interface FloatTrack {

    float valueAt(float timeSeconds);

    float start();

    float end();
}
