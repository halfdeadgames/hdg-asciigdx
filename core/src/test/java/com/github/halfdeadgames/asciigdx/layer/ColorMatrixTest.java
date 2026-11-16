package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import java.util.List;
import org.junit.jupiter.api.Test;

class ColorMatrixTest {

    private static final float TOLERANCE = 1e-5f;
    private static final List<Rgb> SAMPLE_COLORS = List.of(
            new Rgb(0f, 0f, 0f), new Rgb(1f, 1f, 1f), new Rgb(1f, 0f, 0f),
            new Rgb(0.2f, 0.6f, 0.9f), new Rgb(0.75f, 0.1f, 0.4f), new Rgb(0.5f, 0.5f, 0.5f));

    private record Rgb(float red, float green, float blue) {
    }

    private static ColorEffects identity() {
        return new ColorEffects();
    }

    private static ColorEffects saturation(float value) {
        ColorEffects effects = identity();
        effects.setSaturation(value);
        return effects;
    }

    private static ColorEffects tint() {
        ColorEffects effects = identity();
        effects.setTint(0.5f, 0.8f, 1.2f, 0.6f);
        return effects;
    }

    private static ColorEffects mixWhite() {
        ColorEffects effects = identity();
        effects.setMix(1f, 1f, 1f, 0.7f);
        return effects;
    }

    private static ColorEffects invert(float value) {
        ColorEffects effects = identity();
        effects.setInvert(value);
        return effects;
    }

    private static ColorEffects everything() {
        ColorEffects effects = new ColorEffects();
        effects.setSaturation(0.5f);
        effects.setTint(0.9f, 0.4f, 0.7f, 0.3f);
        effects.setMix(0.2f, 0.9f, 0.1f, 0.35f);
        effects.setInvert(0.85f);
        return effects;
    }

    private static float[] oldPipeline(ColorEffects e, Rgb rgb) {
        float[] color = {rgb.red(), rgb.green(), rgb.blue()};
        float gray = color[0] * 0.2126f + color[1] * 0.7152f + color[2] * 0.0722f;
        float[] tintValues = {e.getTintRed(), e.getTintGreen(), e.getTintBlue()};
        float[] mixValues = {e.getMixRed(), e.getMixGreen(), e.getMixBlue()};
        float[] result = new float[3];
        for (int channel = 0; channel < 3; channel++) {
            float value = gray + (color[channel] - gray) * e.getSaturation();
            value *= tintValues[channel];
            value = value + (mixValues[channel] - value) * e.getMixAmount();
            result[channel] = value + ((1f - value) - value) * e.getInvert();
        }
        return result;
    }

    private static void assertMatchesOldPipeline(ColorEffects effects) {
        ColorMatrix matrix = new ColorMatrix();
        effects.writeColorMatrix(matrix);
        float[] actual = new float[3];
        for (Rgb color : SAMPLE_COLORS) {
            matrix.apply(color.red(), color.green(), color.blue(), actual);
            assertArrayClose(oldPipeline(effects, color), actual);
        }
    }

    private static void assertArrayClose(float[] expected, float[] actual) {
        for (int channel = 0; channel < 3; channel++) {
            assertEquals(expected[channel], actual[channel], TOLERANCE);
        }
    }

    @Test
    void identityLeavesColorsAlone() {
        assertMatchesOldPipeline(identity());
    }

    @Test
    void saturationMatchesOldPipeline() {
        assertMatchesOldPipeline(saturation(0f));
        assertMatchesOldPipeline(saturation(0.5f));
    }

    @Test
    void tintMatchesOldPipeline() {
        assertMatchesOldPipeline(tint());
    }

    @Test
    void mixMatchesOldPipeline() {
        assertMatchesOldPipeline(mixWhite());
    }

    @Test
    void invertMatchesOldPipeline() {
        assertMatchesOldPipeline(invert(1f));
        assertMatchesOldPipeline(invert(0.85f));
    }

    @Test
    void allEffectsCombinedMatchOldPipeline() {
        assertMatchesOldPipeline(everything());
    }

    @Test
    void alphaStaysOutsideTheMatrix() {
        ColorMatrix matrix = new ColorMatrix();
        everything().writeColorMatrix(matrix);
        float[] values = matrix.getValues();
        assertEquals(0f, values[3], 0f);
        assertEquals(0f, values[7], 0f);
        assertEquals(0f, values[11], 0f);
        assertEquals(1f, values[15], 0f);
        assertEquals(0.3f, everything().getTintAlpha(), 0f);
    }

    @Test
    void thenEqualsApplyingInSequence() {
        ColorMatrix first = new ColorMatrix();
        saturation(0.5f).writeColorMatrix(first);
        ColorMatrix second = new ColorMatrix();
        everything().writeColorMatrix(second);
        ColorMatrix composed = new ColorMatrix();
        composed.set(first);
        composed.then(second);

        float[] middle = new float[3];
        float[] expected = new float[3];
        float[] actual = new float[3];
        for (Rgb color : SAMPLE_COLORS) {
            first.apply(color.red(), color.green(), color.blue(), middle);
            second.apply(middle[0], middle[1], middle[2], expected);
            composed.apply(color.red(), color.green(), color.blue(), actual);
            assertArrayClose(expected, actual);
        }
    }

    @Test
    void layoutMatchesLibgdxMatrix4AsUploadedToTheShader() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        matrix.thenMixToward(0.9f, 0.3f, 0.1f, 0.4f);
        Matrix4 uploaded = new Matrix4(matrix.getValues());

        float[] expected = new float[3];
        for (Rgb color : SAMPLE_COLORS) {
            matrix.apply(color.red(), color.green(), color.blue(), expected);
            Vector3 actual = new Vector3(color.red(), color.green(), color.blue()).mul(uploaded);
            assertArrayClose(expected, new float[]{actual.x, actual.y, actual.z});
        }
    }
}
