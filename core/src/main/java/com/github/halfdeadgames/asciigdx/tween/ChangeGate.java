package com.github.halfdeadgames.asciigdx.tween;

import java.util.Arrays;

/**
 * Remembers the last value written per slot so bindings can skip writes that would change nothing.
 *
 * <p>Values are stored as int bits to avoid boxing: use {@link #bitsOf(float)} for floats and
 * {@link #bitsOf(boolean)} for booleans; packed colors are already ints. Float bits follow
 * {@link Float#compare}: all NaNs match each other, and 0.0 differs from -0.0.
 */
final class ChangeGate {

    private final int[] lastBits;
    private final boolean[] hasWritten;

    ChangeGate(int slotCount) {
        this.lastBits = new int[slotCount];
        this.hasWritten = new boolean[slotCount];
    }

    static int bitsOf(float value) {
        return Float.floatToIntBits(value);
    }

    static int bitsOf(boolean value) {
        return value ? 1 : 0;
    }

    /** Returns true when the slot already holds these bits, so writing them again is pointless. */
    boolean isUnchanged(int slot, int bits) {
        return hasWritten[slot] && lastBits[slot] == bits;
    }

    boolean isUnchanged(int bits) {
        return isUnchanged(0, bits);
    }

    /** Records that these bits were just written to the slot. */
    void remember(int slot, int bits) {
        lastBits[slot] = bits;
        hasWritten[slot] = true;
    }

    void remember(int bits) {
        remember(0, bits);
    }

    /** Makes the next {@link #isUnchanged} answer false for every slot. */
    void forgetAll() {
        Arrays.fill(hasWritten, false);
    }
}
