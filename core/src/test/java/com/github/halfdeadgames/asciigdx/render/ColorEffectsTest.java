package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

class ColorEffectsTest {

    private final ColorEffects effects = new ColorEffects();

    @Test
    void startsWithDefaults() {
        assertTint(1f, 1f, 1f, 1f);
        assertMix(1f, 1f, 1f, 0f);
        assertEquals(1f, effects.getSaturation());
        assertEquals(0f, effects.getInvert());
    }

    @Test
    void setTintStoresEveryComponent() {
        effects.setTint(0.1f, 0.2f, 0.3f, 0.4f);

        assertTint(0.1f, 0.2f, 0.3f, 0.4f);
    }

    @Test
    void setTintFromColorCopiesValues() {
        Color color = new Color(0.1f, 0.2f, 0.3f, 0.4f);
        effects.setTint(color);
        color.set(0.9f, 0.9f, 0.9f, 0.9f);

        assertTint(0.1f, 0.2f, 0.3f, 0.4f);
    }

    @Test
    void getTintFillsAndReturnsTheGivenColor() {
        effects.setTint(0.1f, 0.2f, 0.3f, 0.4f);
        Color out = new Color();

        assertSame(out, effects.getTint(out));
        assertEquals(new Color(0.1f, 0.2f, 0.3f, 0.4f), out);
    }

    @Test
    void setAlphaKeepsRedGreenAndBlue() {
        effects.setTint(0.1f, 0.2f, 0.3f, 1f);
        effects.setAlpha(0.5f);

        assertTint(0.1f, 0.2f, 0.3f, 0.5f);
    }

    @Test
    void setMixStoresColorAndAmount() {
        effects.setMix(0.1f, 0.2f, 0.3f, 0.4f);

        assertMix(0.1f, 0.2f, 0.3f, 0.4f);
    }

    @Test
    void setMixFromColorCopiesRgbAndIgnoresAlpha() {
        Color color = new Color(0.1f, 0.2f, 0.3f, 0.5f);
        effects.setMix(color, 0.6f);
        color.set(0.9f, 0.9f, 0.9f, 0.9f);

        assertMix(0.1f, 0.2f, 0.3f, 0.6f);
        assertTint(1f, 1f, 1f, 1f);
    }

    @Test
    void setSaturationAndInvertStoreValues() {
        effects.setSaturation(0.25f);
        effects.setInvert(0.75f);

        assertEquals(0.25f, effects.getSaturation());
        assertEquals(0.75f, effects.getInvert());
    }

    @Test
    void valuesAreNotClamped() {
        effects.setTint(2f, -1f, 3f, 5f);
        effects.setMix(4f, -2f, 6f, 7f);
        effects.setSaturation(3f);
        effects.setInvert(-0.5f);

        assertTint(2f, -1f, 3f, 5f);
        assertMix(4f, -2f, 6f, 7f);
        assertEquals(3f, effects.getSaturation());
        assertEquals(-0.5f, effects.getInvert());
    }

    @Test
    void settersThrowAfterDetached() {
        effects.markDetached();

        assertThrows(IllegalStateException.class, () -> effects.setTint(1f, 1f, 1f, 1f));
        assertThrows(IllegalStateException.class, () -> effects.setTint(Color.WHITE));
        assertThrows(IllegalStateException.class, () -> effects.setAlpha(0.5f));
        assertThrows(IllegalStateException.class, () -> effects.setMix(1f, 1f, 1f, 1f));
        assertThrows(IllegalStateException.class, () -> effects.setMix(Color.WHITE, 1f));
        assertThrows(IllegalStateException.class, () -> effects.setSaturation(0f));
        assertThrows(IllegalStateException.class, () -> effects.setInvert(1f));
    }

    @Test
    void gettersKeepWorkingAfterDetached() {
        effects.setTint(0.1f, 0.2f, 0.3f, 0.4f);
        effects.setInvert(0.5f);
        effects.markDetached();

        assertTint(0.1f, 0.2f, 0.3f, 0.4f);
        assertEquals(0.5f, effects.getInvert());
    }

    @Test
    void resetRestoresDefaults() {
        effects.setTint(0.1f, 0.2f, 0.3f, 0.4f);
        effects.setMix(0.5f, 0.6f, 0.7f, 0.8f);
        effects.setSaturation(0f);
        effects.setInvert(1f);

        effects.reset();

        assertTint(1f, 1f, 1f, 1f);
        assertMix(1f, 1f, 1f, 0f);
        assertEquals(1f, effects.getSaturation());
        assertEquals(0f, effects.getInvert());
    }

    @Test
    void resetAndCopyFromDoNotThrowWhenDetached() {
        ColorEffects source = new ColorEffects();
        source.setSaturation(0.5f);
        effects.markDetached();

        effects.copyFrom(source);
        assertEquals(0.5f, effects.getSaturation());
        effects.reset();
        assertEquals(1f, effects.getSaturation());
    }

    private void assertTint(float red, float green, float blue, float alpha) {
        assertEquals(red, effects.getTintRed());
        assertEquals(green, effects.getTintGreen());
        assertEquals(blue, effects.getTintBlue());
        assertEquals(alpha, effects.getTintAlpha());
    }

    private void assertMix(float red, float green, float blue, float amount) {
        assertEquals(red, effects.getMixRed());
        assertEquals(green, effects.getMixGreen());
        assertEquals(blue, effects.getMixBlue());
        assertEquals(amount, effects.getMixAmount());
    }
}
