package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphNameTest {

    @Test
    void namesAreUppercase() {
        assertEquals("WHITE SMILING FACE", GlyphName.fullName(0x263A));
    }

    @Test
    void privateUseIsCustom() {
        assertEquals(GlyphName.CUSTOM, GlyphName.fullName(0xE000));
    }

    @Test
    void longNamesKeepAllWords() {
        assertEquals("DOUBLE EXCLAMATION MARK", GlyphName.fullName(0x203C));
    }

    @Test
    void onlyFontCharactersRemain() {
        String shown = GlyphName.fullName(0x2500);
        assertTrue(shown.matches("[A-Z0-9 \\-]+"));
    }
}
