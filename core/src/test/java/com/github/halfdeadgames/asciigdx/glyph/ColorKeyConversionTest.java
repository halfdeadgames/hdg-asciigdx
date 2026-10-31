package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColorKeyConversionTest {

    private static final int BLACK_KEY = 0x000000FF;
    private static final int MAGENTA_KEY = 0xFF00FFFF;

    @Test
    void blackKeyWithWhitePixelIsOpaqueWhite() {
        assertEquals(0xFFFFFFFF, ColorKeyConversion.toWhiteWithAlpha(0xFFFFFFFF, BLACK_KEY));
    }

    @Test
    void blackKeyUsesBrightestChannel() {
        assertEquals(0xFFFFFF80, ColorKeyConversion.toWhiteWithAlpha(0x808080FF, BLACK_KEY));
    }

    @Test
    void magentaKeyWithWhitePixelIsOpaque() {
        assertEquals(0xFFFFFFFF, ColorKeyConversion.toWhiteWithAlpha(0xFFFFFFFF, MAGENTA_KEY));
    }

    @Test
    void exactKeyIsFullyTransparent() {
        assertEquals(0xFFFFFF00, ColorKeyConversion.toWhiteWithAlpha(MAGENTA_KEY, MAGENTA_KEY));
    }

    @Test
    void edgePixelKeepsPartialAlpha() {
        assertEquals(0xFFFFFF80, ColorKeyConversion.toWhiteWithAlpha(0xFF80FFFF, MAGENTA_KEY));
    }

    @Test
    void pixelAndKeyAlphaAreIgnored() {
        assertEquals(0xFFFFFFFF, ColorKeyConversion.toWhiteWithAlpha(0xFFFFFF10, 0x00000020));
        assertEquals(0xFFFFFF00, ColorKeyConversion.toWhiteWithAlpha(0xFF00FF00, 0xFF00FFFF));
    }

    @Test
    void isTransparentChecksAlphaBelowOpaque() {
        assertFalse(ColorKeyConversion.isTransparent(0xFF));
        assertTrue(ColorKeyConversion.isTransparent(0xFE));
        assertTrue(ColorKeyConversion.isTransparent(0x00));
    }
}
