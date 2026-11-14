package com.github.halfdeadgames.asciigdx.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellColorTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    @Test
    void rgbAddsFullAlpha() {
        assertEquals(0xFF4040FF, CellColor.rgb(0xFF4040));
    }

    @Test
    void rgbIgnoresBitsAbove24() {
        assertEquals(0xFF4040FF, CellColor.rgb(0x12FF4040));
    }

    @Test
    void rgbaPacksChannelsInOrder() {
        assertEquals(0x11223344, CellColor.rgba(0x11, 0x22, 0x33, 0x44));
    }

    @Test
    void rgbaRejectsOutOfRangeChannels() {
        for (int bad : new int[] {-1, 256}) {
            assertThrows(IllegalArgumentException.class, () -> CellColor.rgba(bad, 0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> CellColor.rgba(0, bad, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> CellColor.rgba(0, 0, bad, 0));
            assertThrows(IllegalArgumentException.class, () -> CellColor.rgba(0, 0, 0, bad));
        }
    }

    @Test
    void invertFlipsRgbAndKeepsAlpha() {
        assertEquals(0xEEDDCC44, CellColor.invert(0x11223344));
        assertEquals(0xEEDDCC00, CellColor.invert(0x11223300));
        assertEquals(0xEEDDCCFF, CellColor.invert(0x112233FF));
    }

    @Test
    void withAlphaReplacesOnlyAlpha() {
        assertEquals(0x11223380, CellColor.withAlpha(0x112233FF, 0x80));
    }

    @Test
    void withAlphaRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> CellColor.withAlpha(RED, -1));
        assertThrows(IllegalArgumentException.class, () -> CellColor.withAlpha(RED, 256));
    }

    @Test
    void mixAtZeroReturnsFromExactly() {
        assertEquals(RED, CellColor.mixOklab(RED, BLUE, 0f));
    }

    @Test
    void mixAtOneReturnsToExactly() {
        assertEquals(BLUE, CellColor.mixOklab(RED, BLUE, 1f));
    }

    @Test
    void mixClampsAmount() {
        assertEquals(RED, CellColor.mixOklab(RED, BLUE, -0.5f));
        assertEquals(BLUE, CellColor.mixOklab(RED, BLUE, 1.5f));
    }

    @Test
    void mixKeepsEqualOpaqueAlphaExactly() {
        assertEquals(0xFF, CellColor.mixOklab(RED, BLUE, 0.5f) & 0xFF);
    }

    @Test
    void mixInterpolatesAlpha() {
        int mixed = CellColor.mixOklab(0x11223300, 0x112233FF, 0.5f);

        assertEquals(Math.round(255 * 0.5f), mixed & 0xFF);
    }

    @Test
    void mixingColorWithItselfKeepsChannels() {
        int color = 0x4080C0FF;

        int mixed = CellColor.mixOklab(color, color, 0.5f);

        for (int shift = 8; shift <= 24; shift += 8) {
            int expected = (color >>> shift) & 0xFF;
            int actual = (mixed >>> shift) & 0xFF;
            assertTrue(Math.abs(expected - actual) <= 1, "channel at shift " + shift);
        }
    }

    @Test
    void blackToWhiteMidpointIsGray() {
        int mixed = CellColor.mixOklab(0x000000FF, 0xFFFFFFFF, 0.5f);

        int red = (mixed >>> 24) & 0xFF;
        int green = (mixed >>> 16) & 0xFF;
        int blue = (mixed >>> 8) & 0xFF;
        assertTrue(Math.abs(red - green) <= 1 && Math.abs(green - blue) <= 1);
        assertTrue(red > 0 && red < 255);
    }
}
