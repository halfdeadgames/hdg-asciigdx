package com.github.halfdeadgames.asciigdx.tween;

/** Receives a color property value from a {@link Timeline}. */
@FunctionalInterface
public interface ColorSetter {

    /** Writes the new color as RGBA8888. */
    void set(int rgba);
}
