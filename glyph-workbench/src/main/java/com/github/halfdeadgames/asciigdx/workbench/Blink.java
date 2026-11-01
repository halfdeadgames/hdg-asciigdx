package com.github.halfdeadgames.asciigdx.workbench;

/** Tells when the target letter in the sample words shows its highlight colour. */
final class Blink {

    static final float PERIOD_SECONDS = 2f;
    static final float ON_SECONDS = 0.4f;

    private Blink() {
    }

    static boolean isOn(float elapsedSeconds) {
        return elapsedSeconds % PERIOD_SECONDS < ON_SECONDS;
    }
}
