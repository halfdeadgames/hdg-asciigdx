package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** The timing of the player sinking below the water and coming back up: see-through water and the surfacing. */
public final class UnderwaterDive {

    private static final float SEE_THROUGH_RAMP_SECONDS = 0.1f;
    private static final float MAX_WATER_TRANSPARENCY = 0.5f;
    private static final float SEE_THROUGH_RADIUS_CELLS = 1.5f;

    /** How see-through the water is at the deepest point, as a share of the full see-through. */
    private static final float DEEPEST_SEE_THROUGH = 0.35f;

    private final Beat sinkBeat;
    private final Beat surfaceBeat;

    /**
     * @param sinkBeat eases the player down to the deepest point
     * @param surfaceBeat eases back up; must not start before the sink ends
     */
    public UnderwaterDive(Beat sinkBeat, Beat surfaceBeat) {
        if (sinkBeat == null || surfaceBeat == null) {
            throw new IllegalArgumentException("beats must not be null");
        }
        if (surfaceBeat.start() < sinkBeat.end()) {
            throw new IllegalArgumentException("surfaceBeat must not start before sinkBeat ends");
        }
        this.sinkBeat = sinkBeat;
        this.surfaceBeat = surfaceBeat;
    }

    public boolean isUnderwaterAt(float timeSeconds) {
        return timeSeconds >= sinkBeat.start() && timeSeconds < surfaceBeat.end();
    }

    /** 0 until the surfacing starts, easing to 1 at its end. */
    public float surfaceProgressAt(float timeSeconds) {
        return surfaceBeat.progress(timeSeconds);
    }

    /** 0 outside the dive; inside, the edge ramps times a depth curve that dips from 1 to the minimum and back. */
    public float seeThroughAt(float timeSeconds) {
        if (!isUnderwaterAt(timeSeconds)) {
            return 0f;
        }
        return rampEnvelopeAt(timeSeconds) * depthCurveAt(timeSeconds);
    }

    private float rampEnvelopeAt(float timeSeconds) {
        float rampIn = (timeSeconds - sinkBeat.start()) / SEE_THROUGH_RAMP_SECONDS;
        float rampOut = (surfaceBeat.end() - timeSeconds) / SEE_THROUGH_RAMP_SECONDS;
        return Math.min(1f, Math.min(rampIn, rampOut));
    }

    private float depthCurveAt(float timeSeconds) {
        float depth = sinkBeat.progress(timeSeconds) - surfaceBeat.progress(timeSeconds);
        return 1f - (1f - DEEPEST_SEE_THROUGH) * depth;
    }

    /** Opacity of a water cell this many cell widths from the player: 1 far away, lowest at the center at the deepest point. */
    public float waterAlphaAt(float distanceCells, float timeSeconds) {
        if (!(distanceCells >= 0f) || Float.isInfinite(distanceCells)) {
            throw new IllegalArgumentException("distanceCells must be finite and >= 0: " + distanceCells);
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
