package com.github.halfdeadgames.asciigdx.workbench;

/** Turns a held key into moves: one on press, then steady repeats after a delay. */
final class KeyRepeat {

    static final float DELAY_SECONDS = 0.5f;
    static final float REPEATS_PER_SECOND = 12f;

    private static final float INTERVAL_SECONDS = 1f / REPEATS_PER_SECOND;

    private boolean wasDown;
    private boolean ignoredUntilRelease;
    private float heldSeconds;
    private float nextRepeatAt;

    /** Makes a key that is down now produce no moves until it has been released. */
    void hold(boolean down) {
        wasDown = down;
        ignoredUntilRelease = down;
    }

    /** Call once per frame; returns how many moves to make this frame. */
    int update(boolean down, float deltaSeconds) {
        if (!down) {
            wasDown = false;
            ignoredUntilRelease = false;
            return 0;
        }
        if (ignoredUntilRelease) {
            return 0;
        }
        if (!wasDown) {
            wasDown = true;
            heldSeconds = 0f;
            nextRepeatAt = DELAY_SECONDS;
            return 1;
        }
        heldSeconds += deltaSeconds;
        int moves = 0;
        while (heldSeconds >= nextRepeatAt) {
            moves++;
            nextRepeatAt += INTERVAL_SECONDS;
        }
        return moves;
    }
}
