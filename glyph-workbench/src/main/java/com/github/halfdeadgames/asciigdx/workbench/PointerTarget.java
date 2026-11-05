package com.github.halfdeadgames.asciigdx.workbench;

/** A panel's share of the pointer: hears presses, may claim one, and may capture the pointer while held. */
interface PointerTarget {

    /** Called on every press, whether or not another target claims it. */
    default void notifyPress(int x, int y) {
    }

    /** Returns true when this target takes the press, so later targets are not asked. */
    default boolean claimPress(int x, int y) {
        return false;
    }

    /** Whether this target is mid-gesture and should receive drags and the release. */
    default boolean isCaptured() {
        return false;
    }

    default void drag(int x, int y) {
    }

    default void release() {
    }
}
