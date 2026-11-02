package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HandDrawnGlyphsTest {

    private static final String DEFAULT_LIST =
        "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended.txt";

    @Test
    void everyGlyphIsSixteenBySixteen() {
        Map<Integer, boolean[][]> glyphs = HandDrawnGlyphs.all();
        assertFalse(glyphs.isEmpty());
        for (Map.Entry<Integer, boolean[][]> glyph : glyphs.entrySet()) {
            assertEquals(16, glyph.getValue().length, "rows of U+" + Integer.toHexString(glyph.getKey()));
            for (boolean[] row : glyph.getValue()) {
                assertEquals(16, row.length, "columns of U+" + Integer.toHexString(glyph.getKey()));
            }
        }
    }

    @Test
    void everyCodePointExistsInTheDefaultSheet() throws IOException {
        GlyphOrder order = readDefaultOrder();
        for (int codePoint : HandDrawnGlyphs.all().keySet()) {
            assertTrue(order.contains(codePoint), "default sheet lacks U+" + Integer.toHexString(codePoint));
        }
    }

    @Test
    void removedHourglassFrameAndSandGlyphsAreAbsent() {
        Map<Integer, boolean[][]> glyphs = HandDrawnGlyphs.all();
        for (int codePoint = 0xE0E8; codePoint <= 0xE0EB; codePoint++) {
            assertFalse(glyphs.containsKey(codePoint), "unexpected U+" + Integer.toHexString(codePoint));
        }
        for (int codePoint = 0xE0F0; codePoint <= 0xE0F7; codePoint++) {
            assertFalse(glyphs.containsKey(codePoint), "unexpected U+" + Integer.toHexString(codePoint));
        }
    }

    @Test
    void movedShieldMeterEndsHaveNoHandVersion() {
        Map<Integer, boolean[][]> glyphs = HandDrawnGlyphs.all();
        assertFalse(glyphs.containsKey(GeneratedGlyphs.SHIELD_EMPTY));
        assertFalse(glyphs.containsKey(GeneratedGlyphs.SHIELD_FULL));
    }

    @Test
    void turnedShogiPiecesAreTheShogiPiecesRotated() {
        Map<Integer, boolean[][]> glyphs = HandDrawnGlyphs.all();
        assertArrayEquals(GeneratedGlyphs.rotated180(glyphs.get(0x2616)), glyphs.get(0x26C9));
        assertArrayEquals(GeneratedGlyphs.rotated180(glyphs.get(0x2617)), glyphs.get(0x26CA));
    }

    @Test
    void generatedTurnedShogiPiecesAreTheShogiPiecesRotated() {
        Map<Integer, boolean[][]> glyphs = GeneratedGlyphs.all();
        assertArrayEquals(GeneratedGlyphs.rotated180(glyphs.get(0x2616)), glyphs.get(0x26C9));
        assertArrayEquals(GeneratedGlyphs.rotated180(glyphs.get(0x2617)), glyphs.get(0x26CA));
    }

    @Test
    void generatedGlyphsDropTheHourglassPartsAndGainTheShieldEnds() {
        Map<Integer, boolean[][]> glyphs = GeneratedGlyphs.all();
        for (int codePoint = 0xE0E8; codePoint <= 0xE0EB; codePoint++) {
            assertFalse(glyphs.containsKey(codePoint), "unexpected U+" + Integer.toHexString(codePoint));
        }
        for (int codePoint = 0xE0F0; codePoint <= 0xE0F7; codePoint++) {
            assertFalse(glyphs.containsKey(codePoint), "unexpected U+" + Integer.toHexString(codePoint));
        }
        assertTrue(glyphs.containsKey(GeneratedGlyphs.SHIELD_EMPTY));
        assertTrue(glyphs.containsKey(GeneratedGlyphs.SHIELD_FULL));
    }

    private static GlyphOrder readDefaultOrder() throws IOException {
        try (InputStream stream = HandDrawnGlyphsTest.class.getClassLoader().getResourceAsStream(DEFAULT_LIST)) {
            assertNotNull(stream, "missing resource " + DEFAULT_LIST);
            return GlyphOrderText.read(new String(stream.readAllBytes(), StandardCharsets.UTF_8), 16);
        }
    }
}
