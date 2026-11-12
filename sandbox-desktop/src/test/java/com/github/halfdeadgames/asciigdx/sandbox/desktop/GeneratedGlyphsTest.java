package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratedGlyphsTest {

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

    @Test
    void ttfPicksAreAllMissingFromCp437() {
        GlyphOrder ttfPicks = GlyphOrder.ofString(SheetExporter.TTF_PICKS);
        for (int slot = 0; slot < ttfPicks.size(); slot++) {
            int codePoint = ttfPicks.codePointAt(slot);
            assertFalse(GlyphOrder.CP437.contains(codePoint), "CP437 already has U+" + Integer.toHexString(codePoint));
        }
    }

    @Test
    void generatedGlyphsAreNotHiddenByTtfPicks() {
        Map<Integer, boolean[][]> glyphs = GeneratedGlyphs.all();
        GlyphOrder ttfPicks = GlyphOrder.ofString(SheetExporter.TTF_PICKS);
        for (int slot = 0; slot < ttfPicks.size(); slot++) {
            int codePoint = ttfPicks.codePointAt(slot);
            assertFalse(glyphs.containsKey(codePoint), "TTF pick also generated: U+" + Integer.toHexString(codePoint));
        }
    }
}
