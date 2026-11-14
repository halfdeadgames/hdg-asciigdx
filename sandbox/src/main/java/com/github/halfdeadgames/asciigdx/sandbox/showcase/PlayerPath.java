package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;

/**
 * Where the player is at a loop time: whole-cell backing slides, then a hop in place (scale arc only) over the
 * broken plank. Positions are in cells.
 */
public final class PlayerPath {

    private static final float FIRST_STEP_START_SECONDS = 4.8f;
    private static final float STEP_INTERVAL_SECONDS = 0.8f / 3f;
    private static final float SLIDE_SECONDS = 0.1f;

    private static final float HOP_START_SECONDS = 5.6f;
    private static final float HOP_SECONDS = 0.4f;
    private static final float HOP_PEAK_SCALE_GAIN = 0.4f;

    private final Beat[] slides;
    private final Beat hopArc = new Beat(HOP_START_SECONDS, HOP_SECONDS);
    private final int startColumn;
    private final int startRow;

    public PlayerPath(int startColumn, int startRow, int stepCount) {
        if (stepCount < 0) {
            throw new IllegalArgumentException("stepCount must be >= 0: " + stepCount);
        }
        this.startColumn = startColumn;
        this.startRow = startRow;
        this.slides = new Beat[stepCount];
        for (int step = 0; step < stepCount; step++) {
            float slideStart = FIRST_STEP_START_SECONDS + step * STEP_INTERVAL_SECONDS;
            slides[step] = new Beat(slideStart, SLIDE_SECONDS, Interpolation.sineOut);
        }
    }

    public float columnAt(float timeSeconds) {
        float column = startColumn;
        for (int step = 0; step < slides.length; step++) {
            column += slides[step].progress(timeSeconds);
        }
        return column;
    }

    public float rowAt(float timeSeconds) {
        return startRow;
    }

    public float scaleAt(float timeSeconds) {
        return 1f + HOP_PEAK_SCALE_GAIN * (float) Math.sin(Math.PI * hopArc.progress(timeSeconds));
    }
}
