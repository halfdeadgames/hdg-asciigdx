package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** How broken the plank under the player is at a loop time. */
public enum PlankBreak {
    INTACT,
    CRACKED,
    BROKEN;

    private static final float CRACK_START_SECONDS = 5.4f;
    private static final float BREAK_START_SECONDS = 5.6f;

    public static PlankBreak at(float timeSeconds) {
        if (timeSeconds >= BREAK_START_SECONDS) {
            return BROKEN;
        }
        if (timeSeconds >= CRACK_START_SECONDS) {
            return CRACKED;
        }
        return INTACT;
    }
}
