package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.tween.Tween;

/**
 * A spin and shrink over a time span; once it ends, the smooth glyph gives way to the crisp one.
 */
public final class FallSpin {

    private final Tween spin;
    private final float totalDegrees;
    private final float endScale;

    public FallSpin(Tween spin, float totalDegrees, float endScale) {
        if (spin == null) {
            throw new IllegalArgumentException("spin must not be null");
        }
        this.spin = spin;
        this.totalDegrees = totalDegrees;
        this.endScale = endScale;
    }

    public float rotationDegreesAt(float timeSeconds) {
        return totalDegrees * spin.progressAt(timeSeconds);
    }

    public float scaleAt(float timeSeconds) {
        return 1f + (endScale - 1f) * spin.progressAt(timeSeconds);
    }

    /** Whether the crisp glyph replaces the smooth one. */
    public boolean isCrispAt(float timeSeconds) {
        return timeSeconds >= spin.end();
    }
}
