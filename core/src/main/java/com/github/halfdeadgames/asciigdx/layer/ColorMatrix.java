package com.github.halfdeadgames.asciigdx.layer;

import java.util.Arrays;

/**
 * An affine transform of an rgb color, stored as 16 floats in libGDX {@code Matrix4} column-major
 * layout (element at column c, row r is at index c * 4 + r) so it uploads as a mat4 uniform. The
 * last row is always 0, 0, 0, 1; the translation sits in indices 12 to 14. Values are not clamped.
 */
final class ColorMatrix {

    static final int SIZE = 16;
    private static final int TRANSLATION = 12;
    private static final float[] LUMA = {0.2126f, 0.7152f, 0.0722f};

    private final float[] values = new float[SIZE];

    ColorMatrix() {
        setIdentity();
    }

    float[] getValues() {
        return values;
    }

    void setIdentity() {
        Arrays.fill(values, 0f);
        values[0] = 1f;
        values[5] = 1f;
        values[10] = 1f;
        values[15] = 1f;
    }

    void set(ColorMatrix other) {
        System.arraycopy(other.values, 0, values, 0, SIZE);
    }

    /** Replaces this transform with a saturation change: 0 gives the luma gray, 1 keeps the color. */
    void setSaturation(float saturation) {
        setIdentity();
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                float identity = column == row ? 1f : 0f;
                values[column * 4 + row] = (1f - saturation) * LUMA[column] + saturation * identity;
            }
        }
    }

    /** Runs the colors through this transform first, then through {@code next}. */
    void then(ColorMatrix next) {
        for (int column = 0; column < 4; column++) {
            int base = column * 4;
            float x = values[base];
            float y = values[base + 1];
            float z = values[base + 2];
            float w = values[base + 3];
            for (int row = 0; row < 3; row++) {
                float sum = next.values[row] * x + next.values[4 + row] * y + next.values[8 + row] * z
                        + next.values[12 + row] * w;
                values[base + row] = sum;
            }
        }
    }

    /** Appends a per-channel multiply. */
    void thenScale(float r, float g, float b) {
        thenScaleAndOffset(r, g, b, 0f, 0f, 0f);
    }

    /** Appends a blend toward a color: amount 0 keeps the color, 1 replaces it. */
    void thenMixToward(float r, float g, float b, float amount) {
        float keep = 1f - amount;
        thenScaleAndOffset(keep, keep, keep, r * amount, g * amount, b * amount);
    }

    /** Appends a blend toward the negative: amount 0 keeps the color, 1 inverts it. */
    void thenInvert(float amount) {
        float scale = 1f - 2f * amount;
        thenScaleAndOffset(scale, scale, scale, amount, amount, amount);
    }

    /** Writes the transformed color of (r, g, b) into the first three floats of {@code out}. */
    void apply(float r, float g, float b, float[] out) {
        for (int row = 0; row < 3; row++) {
            out[row] = values[row] * r + values[4 + row] * g + values[8 + row] * b + values[TRANSLATION + row];
        }
    }

    private void thenScaleAndOffset(float scaleR, float scaleG, float scaleB,
            float offsetR, float offsetG, float offsetB) {
        for (int column = 0; column < 4; column++) {
            int base = column * 4;
            values[base] *= scaleR;
            values[base + 1] *= scaleG;
            values[base + 2] *= scaleB;
        }
        values[TRANSLATION] += offsetR;
        values[TRANSLATION + 1] += offsetG;
        values[TRANSLATION + 2] += offsetB;
    }
}
