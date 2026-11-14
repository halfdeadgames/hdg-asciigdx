package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** A spin and shrink over a beat; once the beat ends, the smooth glyph gives way to the crisp one. */
public final class FallSpin {

    private final Beat beat;
    private final float totalDegrees;
    private final float endScale;

    public FallSpin(Beat beat, float totalDegrees, float endScale) {
        if (beat == null) {
            throw new IllegalArgumentException("beat must not be null");
        }
        this.beat = beat;
        this.totalDegrees = totalDegrees;
        this.endScale = endScale;
    }

    public float rotationDegreesAt(float timeSeconds) {
        return totalDegrees * beat.progress(timeSeconds);
    }

    public float scaleAt(float timeSeconds) {
        return 1f + (endScale - 1f) * beat.progress(timeSeconds);
    }

    /** Whether the crisp glyph replaces the smooth one. */
    public boolean isCrispAt(float timeSeconds) {
        return timeSeconds >= beat.end();
    }
}
