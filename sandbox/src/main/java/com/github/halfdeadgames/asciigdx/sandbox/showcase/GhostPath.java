package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;

/** Where a ghost is, how much it bobs, and how opaque it is at a loop time. Positions are in cells. */
public final class GhostPath {

    private static final float DRIFT_START_SECONDS = 2.0f;
    private static final float DRIFT_SECONDS = 1.4f;
    private static final float WAIL_START_SECONDS = 3.4f;
    private static final float WAIL_SECONDS = 1.0f;

    private static final float SETTLED_ALPHA = 0.8f;
    private static final float WOBBLE_ROWS = 0.15f;
    private static final float WOBBLE_HERTZ = 0.8f;
    private static final float FLICKER_HERTZ = 6f;
    private static final float FLICKER_FLOOR = 0.55f;
    private static final float FLICKER_RANGE = 1f - FLICKER_FLOOR;
    private static final float TWO_PI = (float) (2.0 * Math.PI);

    private final Beat drift = new Beat(DRIFT_START_SECONDS, DRIFT_SECONDS, Interpolation.sineOut);
    private final Beat wail = new Beat(WAIL_START_SECONDS, WAIL_SECONDS);
    private final float startColumn;
    private final float endColumn;

    public GhostPath(float startColumn, float endColumn) {
        this.startColumn = startColumn;
        this.endColumn = endColumn;
    }

    public float driftStartSeconds() {
        return drift.start();
    }

    public boolean isVisibleAt(float timeSeconds) {
        return timeSeconds >= drift.start();
    }

    public float columnAt(float timeSeconds) {
        return startColumn + (endColumn - startColumn) * drift.progress(timeSeconds);
    }

    public float wobbleRowsAt(float timeSeconds) {
        return WOBBLE_ROWS * (float) Math.sin(TWO_PI * WOBBLE_HERTZ * timeSeconds);
    }

    public float alphaAt(float timeSeconds) {
        if (!isVisibleAt(timeSeconds)) {
            return 0f;
        }
        float baseAlpha = SETTLED_ALPHA * drift.progress(timeSeconds);
        if (!wail.isActive(timeSeconds)) {
            return baseAlpha;
        }
        return baseAlpha * flickerAt(timeSeconds);
    }

    private static float flickerAt(float timeSeconds) {
        return FLICKER_FLOOR + FLICKER_RANGE * Math.abs((float) Math.sin(TWO_PI * FLICKER_HERTZ * timeSeconds));
    }
}
