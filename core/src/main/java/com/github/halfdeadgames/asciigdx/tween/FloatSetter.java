package com.github.halfdeadgames.asciigdx.tween;

/** Receives a float property value from a {@link Timeline}. */
@FunctionalInterface
public interface FloatSetter {

    /**
     * Writes the new value to the target property.
     *
     * @param value the value to write
     */
    void set(float value);
}
