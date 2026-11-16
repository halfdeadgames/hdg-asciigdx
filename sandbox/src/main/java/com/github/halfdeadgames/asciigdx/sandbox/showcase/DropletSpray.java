package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.tween.Tween;

/**
 * Droplets thrown radially outward from one point in a top-down view. Every value is a pure
 * function of the droplet index and the time, so repeated runs match. The spray tween should be
 * linear: its progress is the spray's linear progress, and the outward easing is applied here.
 */
public final class DropletSpray {

    private static final float ANGLE_VARIATION_RADIANS = 0.12f;
    private static final float ANGLE_VARIATION_STEP = 2.1f;
    private static final float MIN_DISTANCE_MULTIPLIER = 0.6f;
    private static final float DISTANCE_MULTIPLIER_RANGE = 0.4f;
    private static final float GOLDEN_FRACTION = 0.618034f;
    private static final float FULL_ALPHA_UNTIL = 0.5f;

    private final Tween spray;
    private final int count;
    private final float maxDistanceCells;
    private final float peakScale;
    private final float[] directionsX;
    private final float[] directionsY;
    private final float[] distanceMultipliers;

    public DropletSpray(Tween spray, int count, float maxDistanceCells, float peakScale) {
        if (spray == null) {
            throw new IllegalArgumentException("spray must not be null");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1: " + count);
        }
        requireFinitePositive(maxDistanceCells, "maxDistanceCells");
        requireFinitePositive(peakScale, "peakScale");
        this.spray = spray;
        this.count = count;
        this.maxDistanceCells = maxDistanceCells;
        this.peakScale = peakScale;
        directionsX = new float[count];
        directionsY = new float[count];
        distanceMultipliers = new float[count];
        for (int index = 0; index < count; index++) {
            fillDroplet(index);
        }
    }

    private static void requireFinitePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
        }
    }

    private void fillDroplet(int index) {
        double angle = 2.0 * Math.PI * index / count + ANGLE_VARIATION_RADIANS * Math.sin(
                index * ANGLE_VARIATION_STEP);
        directionsX[index] = (float) Math.cos(angle);
        directionsY[index] = (float) Math.sin(angle);
        float fraction = (index * GOLDEN_FRACTION) % 1f;
        distanceMultipliers[index] = MIN_DISTANCE_MULTIPLIER + DISTANCE_MULTIPLIER_RANGE * fraction;
    }

    public int count() {
        return count;
    }

    public float directionX(int index) {
        return directionsX[index];
    }

    /** Positive is down the screen. */
    public float directionY(int index) {
        return directionsY[index];
    }

    public float distanceMultiplier(int index) {
        return distanceMultipliers[index];
    }

    public float distanceCellsAt(int index, float timeSeconds) {
        return Interpolation.pow2Out.apply(spray.getProgressAt(timeSeconds)) * maxDistanceCells
                * distanceMultipliers[index];
    }

    /** Rises from 1 to the peak at mid-spray, then falls back to 1. */
    public float scaleAt(float timeSeconds) {
        float progress = spray.getProgressAt(timeSeconds);
        return 1f + (peakScale - 1f) * 4f * progress * (1f - progress);
    }

    /** 0 outside the spray; 1 for the first half, then linear down to 0 at the end. */
    public float alphaAt(float timeSeconds) {
        if (!isVisibleAt(timeSeconds)) {
            return 0f;
        }
        float progress = spray.getProgressAt(timeSeconds);
        if (progress <= FULL_ALPHA_UNTIL) {
            return 1f;
        }
        return (1f - progress) / (1f - FULL_ALPHA_UNTIL);
    }

    public boolean isVisibleAt(float timeSeconds) {
        return spray.isActiveAt(timeSeconds);
    }
}
