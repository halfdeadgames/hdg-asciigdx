package com.github.halfdeadgames.asciigdx.tween;

/** Receives a boolean property value from a {@link Timeline}. */
@FunctionalInterface
public interface BooleanSetter {

    /**
     * Writes the new value to the target property.
     *
     * @param value the value to write
     */
    void set(boolean value);
}
