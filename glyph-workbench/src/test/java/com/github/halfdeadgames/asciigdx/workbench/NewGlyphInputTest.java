package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.SheetOrder;
import org.junit.jupiter.api.Test;

class NewGlyphInputTest {

    private static void assertCodePoint(int expected, String typed) {
        var result = NewGlyphInput.parse(typed);
        assertTrue(result.isValid(), "expected valid for '" + typed + "' but got " + result.error());
        assertNull(result.error());
        assertEquals(expected, result.codePoint());
    }

    private static void assertRefused(String typed) {
        var result = NewGlyphInput.parse(typed);
        assertFalse(result.isValid(), "expected refusal for '" + typed + "'");
        assertNotNull(result.error());
        assertFalse(result.error().isEmpty());
    }

    @Test
    void singleCharacterIsItself() {
        assertCodePoint(0x41, "A");
        assertCodePoint(0x34, "4");
        assertCodePoint(0x20, " ");
    }

    @Test
    void singleHexLetterIsTheLetter() {
        assertCodePoint(0x66, "f");
    }

    @Test
    void nonBmpCharacterCountsAsOne() {
        assertCodePoint(0x1F600, new String(Character.toChars(0x1F600)));
    }

    @Test
    void bareHexIsParsed() {
        assertCodePoint(0x41, "41");
        assertCodePoint(0xE000, "e000");
        assertCodePoint(0x10FFFC, "10FFFC");
    }

    @Test
    void prefixedHexIsParsed() {
        assertCodePoint(0x41, "U+0041");
        assertCodePoint(0x41, "u+0041");
        assertCodePoint(0x2500, "U+2500");
        assertCodePoint(0xABC, "u+aBc");
        assertFalse(NewGlyphInput.parse("U+4").isValid());
    }

    @Test
    void surroundingWhitespaceIsStripped() {
        assertCodePoint(0x41, "  U+0041 ");
        assertCodePoint(0x41, " 41\t");
    }

    @Test
    void strippedSingleCharacterIsTheCharacter() {
        assertCodePoint(0x34, " 4 ");
        assertCodePoint(0x5A, " Z ");
    }

    @Test
    void emptyAndBlankAreRefused() {
        assertRefused("");
        assertRefused(null);
        assertRefused("  ");
    }

    @Test
    void nonHexTextIsRefused() {
        assertRefused("hello");
        assertRefused("U+00GG");
        assertRefused("0x41");
        assertRefused("4 1");
    }

    @Test
    void barePrefixIsRefused() {
        assertRefused("U+");
        assertRefused("u+");
        assertRefused(" U+ ");
    }

    @Test
    void tooManyDigitsIsRefused() {
        assertRefused("0000041");
    }

    @Test
    void aboveUnicodeRangeIsRefused() {
        assertRefused("110000");
        assertRefused("U+FFFFFF");
    }

    @Test
    void surrogatesAreRefused() {
        assertRefused("D800");
        assertRefused("U+DFFF");
        assertRefused(String.valueOf('\uD800'));
    }

    @Test
    void fillerIsRefused() {
        assertRefused("U+" + Integer.toHexString(SheetOrder.FILLER));
        assertRefused(new String(Character.toChars(SheetOrder.FILLER)));
    }

    @Test
    void controlCharactersAreRefused() {
        assertRefused("00");
        assertRefused("U+0000");
        assertRefused("1F");
        assertRefused("U+007F");
        assertRefused("U+0085");
        assertRefused("\n");
        assertRefused("\u0000");
    }
}
