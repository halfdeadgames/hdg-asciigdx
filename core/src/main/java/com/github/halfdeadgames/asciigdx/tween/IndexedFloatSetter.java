package com.github.halfdeadgames.asciigdx.tween;

/** Receives a float value for one item of a group, such as one letter of a title. */
@FunctionalInterface
public interface IndexedFloatSetter {

    /** Writes {@code value} for the item at {@code index}. */
    void set(int index, float value);
}
