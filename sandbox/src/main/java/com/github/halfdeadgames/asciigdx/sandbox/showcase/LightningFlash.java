package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/**
 * How strongly a lightning flash inverts the scene at a loop time: a fixed strength inside each
 * flicker, 0 outside.
 */
public final class LightningFlash {

    /** One flicker, on from startSeconds (inclusive) to endSeconds (exclusive). */
    public record Flicker(float startSeconds, float endSeconds) {

        public Flicker {
            if (!(endSeconds >= startSeconds)) {
                throw new IllegalArgumentException(
                        "endSeconds must be >= startSeconds: " + endSeconds);
            }
        }

        boolean isOnAt(float timeSeconds) {
            return timeSeconds >= startSeconds && timeSeconds < endSeconds;
        }
    }

    private final float strength;
    private final Flicker[] flickers;

    public LightningFlash(float strength, Flicker... flickers) {
        this.strength = strength;
        this.flickers = flickers.clone();
    }

    public float amountAt(float timeSeconds) {
        for (int index = 0; index < flickers.length; index++) {
            if (flickers[index].isOnAt(timeSeconds)) {
                return strength;
            }
        }
        return 0f;
    }
}
