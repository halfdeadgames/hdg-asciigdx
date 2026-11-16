package com.github.halfdeadgames.asciigdx.tween;

/** Receives a float value for one item of a group, such as one letter of a title. */
@FunctionalInterface
public interface IndexedFloatSetter {

    /**
     * Writes the value for one item of the group.
     *
     * @param index the item position, from 0 up to the count given when binding
     * @param value the value to write for that item
     */
    void set(int index, float value);
}
