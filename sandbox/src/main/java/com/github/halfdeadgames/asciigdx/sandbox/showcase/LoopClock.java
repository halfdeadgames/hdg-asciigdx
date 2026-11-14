package com.github.halfdeadgames.asciigdx.sandbox.showcase;

public final class LoopClock {

    private final float loopSeconds;
    private final float maxDeltaSeconds;
    private float time;

    public LoopClock(float loopSeconds, float maxDeltaSeconds) {
        if (!(loopSeconds > 0f)) {
            throw new IllegalArgumentException("loopSeconds must be > 0: " + loopSeconds);
        }
        if (!(maxDeltaSeconds > 0f)) {
            throw new IllegalArgumentException("maxDeltaSeconds must be > 0: " + maxDeltaSeconds);
        }
        this.loopSeconds = loopSeconds;
        this.maxDeltaSeconds = maxDeltaSeconds;
    }

    public void advance(float deltaSeconds) {
        if (!(deltaSeconds > 0f)) {
            return;
        }
        time = wrap(time + Math.min(deltaSeconds, maxDeltaSeconds));
    }

    public float time() {
        return time;
    }

    public void setTime(float seconds) {
        if (!Float.isFinite(seconds)) {
            throw new IllegalArgumentException("seconds must be finite: " + seconds);
        }
        time = wrap(seconds);
    }

    public float loopSeconds() {
        return loopSeconds;
    }

    private float wrap(float seconds) {
        float wrapped = seconds % loopSeconds;
        if (wrapped < 0f) {
            wrapped += loopSeconds;
        }
        // Float rounding can land exactly on the loop end for tiny negative inputs.
        return wrapped >= loopSeconds ? 0f : wrapped;
    }
}
