package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/**
 * A slow diagonal wave pattern of water glyphs that advances one step every {@link #STEP_SECONDS}.
 */
public final class WaterPattern {

    public static final float STEP_SECONDS = 0.4f;

    private static final int BAND_PERIOD = 8;
    private static final int CREST_BANDS = 2;
    private static final int RIPPLE_BANDS = 5;
    private static final int CREST = '≈';
    private static final int RIPPLE = '~';
    private static final int CALM = ' ';

    private WaterPattern() {
    }

    /** Returns the wave step shown at the given time; it changes once per {@link #STEP_SECONDS}. */
    public static int stepAt(float seconds) {
        return (int) Math.floor(seconds / STEP_SECONDS);
    }

    /** Returns '~', '≈', or ' ' for a cell at the given time. */
    public static int codePointAt(int column, int row, float seconds) {
        int band = Math.floorMod(column + row - stepAt(seconds), BAND_PERIOD);
        if (band < CREST_BANDS) {
            return CREST;
        }
        if (band < RIPPLE_BANDS) {
            return RIPPLE;
        }
        return CALM;
    }
}
