package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// A Layer can only be created by a LayerStack, which needs GL, so apply is checked in the sandbox.
class LabelEffectZonesTest {
    @Test
    void ofRejectsNullLabel() {
        assertThrows(NullPointerException.class, () -> LabelEffectZones.of(null));
    }

    @Test
    void applyRejectsNullLayer() {
        Label label = new Label("text", LabelStyle.of(0xFFFFFFFF, 0x0000FFFF));
        LabelEffectZones zones = LabelEffectZones.of(label);

        assertThrows(NullPointerException.class, () -> zones.apply(null, 0f));
    }

    @Test
    void ofCreatesHolderForLabel() {
        Label label = new Label("text", LabelStyle.of(0xFFFFFFFF, 0x0000FFFF));

        assertNotNull(LabelEffectZones.of(label));
    }
}
