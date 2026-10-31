package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TransparencyModeTest {

    @Test
    void autoConvertsOnlyWithoutTransparentPixels() {
        assertTrue(TransparencyMode.AUTO.shouldConvert(false));
        assertFalse(TransparencyMode.AUTO.shouldConvert(true));
    }

    @Test
    void alwaysConvertsRegardless() {
        assertTrue(TransparencyMode.ALWAYS.shouldConvert(false));
        assertTrue(TransparencyMode.ALWAYS.shouldConvert(true));
    }

    @Test
    void neverConvertsRegardless() {
        assertFalse(TransparencyMode.NEVER.shouldConvert(false));
        assertFalse(TransparencyMode.NEVER.shouldConvert(true));
    }
}
