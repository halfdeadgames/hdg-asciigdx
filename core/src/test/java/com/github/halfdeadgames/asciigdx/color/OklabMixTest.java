package com.github.halfdeadgames.asciigdx.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OklabMixTest {

    @Test
    void selfMixRoundTripStaysWithinOneAcrossSweep() {
        for (int red = 0; red <= 255; red += 17) {
            for (int green = 0; green <= 255; green += 17) {
                for (int blue = 0; blue <= 255; blue += 17) {
                    assertSelfMixWithinOne(red, green, blue);
                }
            }
        }
    }

    @Test
    void selfMixRoundTripKeepsPreviouslyDriftingColor() {
        assertSelfMixWithinOne(0x23, 0xE1, 0x8C);
    }

    @Test
    void resultHasZeroAlphaByte() {
        int mixed = OklabMix.mixRgb(0x4080C0FF, 0xC08040FF, 0.5f);

        assertEquals(0, mixed & 0xFF);
    }

    @Test
    void blackToWhiteMidpointIsMidGray() {
        int mixed = OklabMix.mixRgb(0x000000FF, 0xFFFFFFFF, 0.5f);

        int red = (mixed >>> 24) & 0xFF;
        int green = (mixed >>> 16) & 0xFF;
        int blue = (mixed >>> 8) & 0xFF;
        assertTrue(Math.abs(red - green) <= 1 && Math.abs(green - blue) <= 1);
        assertTrue(red > 0 && red < 255);
        assertTrue(Math.abs(red - 99) <= 2, "red was " + red);
    }

    @Test
    void redMixedWithItselfStaysRed() {
        int mixed = OklabMix.mixRgb(0xFF0000FF, 0xFF0000FF, 0.3f);

        assertTrue(Math.abs(((mixed >>> 24) & 0xFF) - 255) <= 1);
        assertTrue(((mixed >>> 16) & 0xFF) <= 1);
        assertTrue(((mixed >>> 8) & 0xFF) <= 1);
    }

    private static void assertSelfMixWithinOne(int red, int green, int blue) {
        int color = (red << 24) | (green << 16) | (blue << 8) | 0xFF;

        int mixed = OklabMix.mixRgb(color, color, 0.5f);

        String label = Integer.toHexString(color);
        assertTrue(Math.abs(red - ((mixed >>> 24) & 0xFF)) <= 1, "red of " + label);
        assertTrue(Math.abs(green - ((mixed >>> 16) & 0xFF)) <= 1, "green of " + label);
        assertTrue(Math.abs(blue - ((mixed >>> 8) & 0xFF)) <= 1, "blue of " + label);
    }
}
