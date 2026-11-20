package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

class StackColorEffectsTest {

    private static final float TOLERANCE = 1e-5f;
    private static final float RED = 0.2f;
    private static final float GREEN = 0.6f;
    private static final float BLUE = 0.9f;

    private final LayerStack stack =
            TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final ResolvedEffects resolved = new ResolvedEffects();

    private LayerGroup groupWith(ColorEffects effects) {
        LayerGroup group = stack.addGroup();
        group.getColorEffects().copyFrom(effects);
        return group;
    }

    private static ColorEffects invert(float amount) {
        ColorEffects effects = new ColorEffects();
        effects.setInvert(amount);
        return effects;
    }

    private static ColorEffects darken(float amount) {
        ColorEffects effects = new ColorEffects();
        effects.setMix(0f, 0f, 0f, amount);
        return effects;
    }

    private static ColorEffects tint(float value, float alpha) {
        ColorEffects effects = new ColorEffects();
        effects.setTint(value, value, value, alpha);
        return effects;
    }

    private static Array<LayerGroup> groupsOf(LayerGroup... groups) {
        Array<LayerGroup> array = new Array<>();
        for (LayerGroup group : groups) {
            array.add(group);
        }
        return array;
    }

    /** Runs the sample color through the resolved matrix and returns the red, green, and blue. */
    private float[] resolvedSample() {
        float[] out = new float[3];
        resolved.getMatrix().apply(RED, GREEN, BLUE, out);
        return out;
    }

    @Test
    void stackEffectsRunAfterTheGroupEffects() {
        LayerGroup group = groupWith(darken(0.5f));
        resolved.resolve(new ColorEffects(), groupsOf(group));

        resolved.foldStack(invert(1f));

        assertEquals(1f - 0.5f * RED, resolvedSample()[0], TOLERANCE);
    }

    @Test
    void stackEffectsRunAfterTheLayersOwnEffects() {
        ColorEffects own = tint(0.5f, 1f);

        resolved.resolve(own, groupsOf());
        resolved.foldStack(invert(1f));

        assertEquals(1f - 0.5f * RED, resolvedSample()[0], TOLERANCE);
    }

    @Test
    void stackEffectsRunAfterOwnAndGroupEffectsInThatOrder() {
        ColorEffects own = invert(1f);
        LayerGroup group = groupWith(darken(0.5f));

        resolved.resolve(own, groupsOf(group));
        resolved.foldStack(tint(0.5f, 1f));

        float expectedRed = 0.5f * (0.5f * (1f - RED));
        assertEquals(expectedRed, resolvedSample()[0], TOLERANCE);
    }

    @Test
    void stackAlphaMultipliesWithLayerAndGroupAlphas() {
        ColorEffects own = tint(1f, 0.5f);
        LayerGroup group = groupWith(tint(1f, 0.5f));

        resolved.resolve(own, groupsOf(group));
        resolved.foldStack(tint(1f, 0.4f));

        assertEquals(0.1f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void stackAlphaZeroZeroesTheResolvedAlpha() {
        resolved.resolve(new ColorEffects(), groupsOf());

        resolved.foldStack(tint(1f, 0f));

        assertEquals(0f, resolved.getAlpha(), 0f);
    }

    @Test
    void defaultStackEffectsLeaveTheResolvedResultUnchanged() {
        LayerGroup group = groupWith(darken(0.5f));
        resolved.resolve(invert(0.25f), groupsOf(group));
        ColorMatrix before = new ColorMatrix();
        before.set(resolved.getMatrix());
        float alphaBefore = resolved.getAlpha();

        resolved.foldStack(new ColorEffects());

        assertArrayEquals(before.getValues(), resolved.getMatrix().getValues(), TOLERANCE);
        assertEquals(alphaBefore, resolved.getAlpha(), 0f);
    }

    @Test
    void stackEffectsStayTheSameInstanceAndReadableAfterDispose() {
        ColorEffects effects = stack.getColorEffects();

        stack.dispose();

        assertSame(effects, stack.getColorEffects());
        assertEquals(1f, effects.getTintAlpha(), 0f);
    }

    @Test
    void stackSettersThrowAfterDispose() {
        ColorEffects effects = stack.getColorEffects();

        stack.dispose();

        assertThrows(IllegalStateException.class, () -> effects.setAlpha(0.5f));
        assertThrows(IllegalStateException.class, () -> effects.setTint(1f, 1f, 1f, 0f));
        assertThrows(IllegalStateException.class, () -> effects.setMix(1f, 1f, 1f, 0.5f));
        assertThrows(IllegalStateException.class, () -> effects.setSaturation(0f));
        assertThrows(IllegalStateException.class, () -> effects.setInvert(1f));
    }
}
