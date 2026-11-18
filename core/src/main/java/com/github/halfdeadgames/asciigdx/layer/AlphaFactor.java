package com.github.halfdeadgames.asciigdx.layer;

/**
 * The range rule shared by every temporary alpha on a {@link CellHandle}. Nothing here allocates.
 */
final class AlphaFactor {

    /**
     * The largest factor kept. The faintest visible alpha is 1/255, so a larger factor can never
     * change what is drawn. The cap also keeps the cell alpha well inside the range a mediump
     * shader float is guaranteed to hold.
     */
    static final float MAX_FACTOR = 255f;

    private AlphaFactor() {
    }

    /**
     * Checks an alpha factor and clamps it to 0..{@link #MAX_FACTOR}. Values between 1 and the
     * maximum are kept as given, because they make a translucent color less transparent; the drawn
     * alpha is capped at opaque where the factor is applied.
     *
     * @param factor the requested factor; 0 is invisible, 1 is unchanged
     * @return {@code factor} clamped to 0..{@link #MAX_FACTOR}
     * @throws IllegalArgumentException if {@code factor} is NaN or infinite
     */
    static float validated(float factor) {
        if (Float.isNaN(factor) || Float.isInfinite(factor)) {
            throw new IllegalArgumentException("Alpha must be a finite number, was " + factor);
        }
        return Math.max(0f, Math.min(MAX_FACTOR, factor));
    }
}
