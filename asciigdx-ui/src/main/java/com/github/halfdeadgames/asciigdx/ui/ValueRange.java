package com.github.halfdeadgames.asciigdx.ui;

/**
 * A closed range of float values with a step size, as used by {@link ProgressBar} and
 * {@link Slider}. The values that can be shown or picked are {@code min + n * stepSize} for whole
 * numbers n, as far as they do not pass {@code max}; the maximum itself is always allowed, even
 * when it does not lie on that grid. Immutable, so one instance can be shared freely.
 *
 * <p>Example:
 * <pre>{@code
 * ValueRange percent = ValueRange.of(0f, 100f, 1f);
 * ProgressBar loading = ProgressBar.horizontal(percent, 20, style);
 * }</pre>
 */
public final class ValueRange {

    // Absorbs float error when the range is an exact number of steps, such as 0 to 1 by 0.1.
    private static final double STEP_COUNT_TOLERANCE = 1e-6;

    private final float min;
    private final float max;
    private final float stepSize;

    private ValueRange(float min, float max, float stepSize) {
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
    }

    /**
     * Creates a range.
     *
     * @param min lowest value; must be finite and less than {@code max}
     * @param max highest value; must be finite
     * @param stepSize distance between neighboring values; must be finite and greater than 0
     * @return a new range
     * @throws IllegalArgumentException if {@code min} or {@code max} is NaN or infinite, if
     *         {@code min} is not less than {@code max}, or if {@code stepSize} is NaN, infinite,
     *         or not greater than 0
     */
    public static ValueRange of(float min, float max, float stepSize) {
        if (!Float.isFinite(min) || !Float.isFinite(max)) {
            throw new IllegalArgumentException(
                    "min and max must be finite, were " + min + " and " + max);
        }
        if (min >= max) {
            throw new IllegalArgumentException(
                    "min must be less than max, were " + min + " and " + max);
        }
        if (!Float.isFinite(stepSize) || stepSize <= 0f) {
            throw new IllegalArgumentException(
                    "stepSize must be finite and greater than 0, was " + stepSize);
        }
        return new ValueRange(min, max, stepSize);
    }

    /**
     * Returns the lowest value.
     *
     * @return the minimum, finite
     */
    public float getMin() {
        return min;
    }

    /**
     * Returns the highest value.
     *
     * @return the maximum, finite and greater than {@link #getMin()}
     */
    public float getMax() {
        return max;
    }

    /**
     * Returns the distance between neighboring values.
     *
     * @return the step size, finite and greater than 0
     */
    public float getStepSize() {
        return stepSize;
    }

    float snap(float value) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException("value must not be NaN");
        }
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        long stepCount = Math.round(((double) value - min) / stepSize);
        return valueAtStep(stepCount);
    }

    float stepUp(float value) {
        return snap((float) ((double) snap(value) + stepSize));
    }

    float stepDown(float value) {
        float snapped = snap(value);
        if (snapped < max) {
            return snap((float) ((double) snapped - stepSize));
        }
        long highestStep = highestStepCount();
        boolean maxIsOnGrid = (double) min + highestStep * (double) stepSize >= max;
        return valueAtStep(maxIsOnGrid ? highestStep - 1 : highestStep);
    }

    /** Returns the position of a value in the range, 0 at the minimum and 1 at the maximum. */
    float fractionOf(float value) {
        return (float) (((double) value - min) / ((double) max - min));
    }

    /** Returns the snapped value at a fraction, which is clamped to 0 to 1 first. */
    float valueAtFraction(float fraction) {
        double clamped = Math.max(0d, Math.min(1d, fraction));
        return snap((float) (min + clamped * ((double) max - min)));
    }

    private long highestStepCount() {
        return (long) Math.floor(((double) max - min) / stepSize + STEP_COUNT_TOLERANCE);
    }

    private float valueAtStep(long stepCount) {
        double value = (double) min + stepCount * (double) stepSize;
        return value >= max ? max : (float) value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValueRange)) {
            return false;
        }
        ValueRange that = (ValueRange) other;
        return Float.compare(min, that.min) == 0 && Float.compare(max, that.max) == 0
                && Float.compare(stepSize, that.stepSize) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Float.hashCode(min) + Float.hashCode(max)) + Float.hashCode(stepSize);
    }

    @Override
    public String toString() {
        return "ValueRange[min=" + min + ", max=" + max + ", stepSize=" + stepSize + "]";
    }
}
