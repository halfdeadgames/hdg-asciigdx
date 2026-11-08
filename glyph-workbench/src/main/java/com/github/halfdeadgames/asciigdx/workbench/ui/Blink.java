package com.github.halfdeadgames.asciigdx.workbench.ui;

/** A blinking marker's clock: lit for one half cycle, dark for the next, starting lit. */
public final class Blink {

    public static final float HALF_CYCLE_SECONDS = 0.25f;

    private float seconds;

    /** Whether a marker is lit at the given time since the blink started. */
    public static boolean isOnAt(float seconds) {
        float cycle = 2 * HALF_CYCLE_SECONDS;
        float phase = seconds % cycle;
        if (phase < 0) {
            phase += cycle;
        }
        return phase < HALF_CYCLE_SECONDS;
    }

    public void advance(float deltaSeconds) {
        seconds += deltaSeconds;
    }

    /** Makes the marker lit now and starts a fresh cycle. */
    public void restart() {
        seconds = 0f;
    }

    public boolean isOn() {
        return isOnAt(seconds);
    }
}
