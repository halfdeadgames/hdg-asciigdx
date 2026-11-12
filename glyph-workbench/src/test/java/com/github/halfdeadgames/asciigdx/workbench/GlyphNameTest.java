package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphNameTest {

    private static final int SHOVEL = 0x1FA8F;
    private static final int POST_13_NOT_IN_TABLE = 0x1FBCB;

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

    @Test
    void lookupUsesJavaNameFirst() {
        assertEquals("LATIN CAPITAL LETTER A", GlyphName.fullName(0x41));
    }

    @Test
    void shovelIsNamedWhetherOrNotTheJdkKnowsIt() {
        // A JDK that already knows U+1FA8F also says SHOVEL, so this holds either way.
        assertEquals("SHOVEL", GlyphName.fullName(SHOVEL));
    }

    @Test
    void tableFillsInWhenJdkHasNoName() {
        assertEquals("SHOVEL", GlyphName.fullName(SHOVEL, codePoint -> null,
            codePoint -> codePoint == SHOVEL ? "SHOVEL" : null));
    }

    @Test
    void missingEverywhereGivesFallback() {
        assertEquals(GlyphName.UNNAMED,
            GlyphName.fullName(POST_13_NOT_IN_TABLE, codePoint -> null, codePoint -> null));
    }

    @Test
    void runsLeadingAndTrailingSpacesCollapse() {
        assertEquals("A B-C D", GlyphName.fullName(0x41, codePoint -> "  a!!b-c   (d)  ", codePoint -> null));
    }

    @Test
    void nameOfOnlySymbolsIsEmpty() {
        assertEquals("", GlyphName.fullName(0x41, codePoint -> "(!)", codePoint -> null));
    }

    @Test
    void privateUseStaysCustomEvenWithoutJdkName() {
        assertEquals(GlyphName.CUSTOM, GlyphName.fullName(0xE000, codePoint -> null, codePoint -> null));
    }
}
