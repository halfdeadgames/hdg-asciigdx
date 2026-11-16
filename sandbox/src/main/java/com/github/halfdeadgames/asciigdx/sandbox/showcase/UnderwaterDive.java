package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.tween.Tween;

/**
 * The timing of the player sinking below the water and coming back up: see-through water and the
 * surfacing.
 */
public final class UnderwaterDive {

    private static final float SEE_THROUGH_RAMP_SECONDS = 0.1f;
    private static final float MAX_WATER_TRANSPARENCY = 0.5f;
    private static final float SEE_THROUGH_RADIUS_CELLS = 1.5f;

    /** How see-through the water is at the deepest point, as a share of the full see-through. */
    private static final float DEEPEST_SEE_THROUGH = 0.35f;

    private final Tween sink;
    private final Tween surface;

    /**
     * @param sink eases the player down to the deepest point
     * @param surface eases back up; must not start before the sink ends
     */
    public UnderwaterDive(Tween sink, Tween surface) {
        if (sink == null || surface == null) {
            throw new IllegalArgumentException("sink and surface must not be null");
        }
        if (surface.start() < sink.end()) {
            throw new IllegalArgumentException("surface must not start before sink ends");
        }
        this.sink = sink;
        this.surface = surface;
    }

    public boolean isUnderwaterAt(float timeSeconds) {
        return timeSeconds >= sink.start() && timeSeconds < surface.end();
    }

    /** 0 until the surfacing starts, easing to 1 at its end. */
    public float surfaceProgressAt(float timeSeconds) {
        return surface.progressAt(timeSeconds);
    }

    public float sinkStartSeconds() {
        return sink.start();
    }

    public float surfaceEndSeconds() {
        return surface.end();
    }

    /**
     * 0 outside the dive; inside, the edge ramps times a depth curve that dips from 1 to the
     * minimum and back.
     */
    public float seeThroughAt(float timeSeconds) {
        if (!isUnderwaterAt(timeSeconds)) {
            return 0f;
        }
        return rampEnvelopeAt(timeSeconds) * depthCurveAt(timeSeconds);
    }

    private float rampEnvelopeAt(float timeSeconds) {
        float rampIn = (timeSeconds - sink.start()) / SEE_THROUGH_RAMP_SECONDS;
        float rampOut = (surface.end() - timeSeconds) / SEE_THROUGH_RAMP_SECONDS;
        return Math.min(1f, Math.min(rampIn, rampOut));
    }

    private float depthCurveAt(float timeSeconds) {
        float depth = sink.progressAt(timeSeconds) - surface.progressAt(timeSeconds);
        return 1f - (1f - DEEPEST_SEE_THROUGH) * depth;
    }

    /**
     * Opacity of a water cell this many cell widths from the player: 1 far away, lowest at the
     * center at the deepest point.
     */
    public float waterAlphaAt(float distanceCells, float timeSeconds) {
        if (!(distanceCells >= 0f) || Float.isInfinite(distanceCells)) {
            throw new IllegalArgumentException(
                    "distanceCells must be finite and >= 0: " + distanceCells);
        }
        return 1f - MAX_WATER_TRANSPARENCY * seeThroughAt(timeSeconds) * falloffAt(distanceCells);
    }

    /** A cosine bump: 1 at the center, 0 from the radius out. */
    private static float falloffAt(float distanceCells) {
        if (distanceCells >= SEE_THROUGH_RADIUS_CELLS) {
            return 0f;
        }
        return 0.5f * (1f + (float) Math.cos(Math.PI * distanceCells / SEE_THROUGH_RADIUS_CELLS));
    }

    public static float seeThroughRadiusCells() {
        return SEE_THROUGH_RADIUS_CELLS;
    }
}
