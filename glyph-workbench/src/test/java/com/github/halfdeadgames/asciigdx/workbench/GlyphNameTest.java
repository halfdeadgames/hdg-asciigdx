package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphNameTest {

    @Test
    void namesAreUppercase() {
        assertEquals("WHITE SMILING FACE", GlyphName.describe(0x263A, 40));
    }

    @Test
    void privateUseIsCustom() {
        assertEquals(GlyphName.CUSTOM, GlyphName.describe(0xE000, 40));
    }

    @Test
    void longNamesKeepTheirLastWholeWords() {
        assertEquals("FACE", GlyphName.describe(0x263A, 10));
        assertEquals("CAPITAL LETTER A", GlyphName.describe('A', 20));
    }

    @Test
    void wordThatFitsExactlyIsKept() {
        assertEquals("SMILING FACE", GlyphName.describe(0x263A, 12));
    }

    @Test
    void lastWordLongerThanTheLimitIsCut() {
        assertEquals("FACE", GlyphName.describe(0x263A, 4));
        assertEquals("ACE", GlyphName.describe(0x263A, 3));
    }

    @Test
    void onlyFontCharactersRemain() {
        String shown = GlyphName.describe(0x2500, 60);
        assertTrue(shown.matches("[A-Z0-9 \\-]+"));
    }
}
