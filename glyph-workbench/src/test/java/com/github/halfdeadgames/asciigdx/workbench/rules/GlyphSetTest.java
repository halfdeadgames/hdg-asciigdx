package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GlyphSetTest {

    @Test
    void singleCharacterMatchesOnlyItself() {
        var set = GlyphSet.parse(List.of("A"));
        assertTrue(set.contains('A'));
        assertFalse(set.contains('B'));
    }

    @Test
    void codeMatchesOnlyItself() {
        var set = GlyphSet.parse(List.of("U+0041"));
        assertTrue(set.contains(0x41));
        assertFalse(set.contains(0x42));
    }

    @Test
    void lowercasePrefixAndHexDigitsAreAccepted() {
        assertTrue(GlyphSet.parse(List.of("u+00e9")).contains(0xE9));
    }

    @Test
    void characterRangeIncludesBothEnds() {
        var set = GlyphSet.parse(List.of("A-Z"));
        assertTrue(set.contains('A'));
        assertTrue(set.contains('Z'));
        assertFalse(set.contains('@'));
        assertFalse(set.contains('['));
    }

    @Test
    void codeRangeIncludesBothEnds() {
        var set = GlyphSet.parse(List.of("U+2500-U+259F"));
        assertTrue(set.contains(0x2500));
        assertTrue(set.contains(0x259F));
        assertFalse(set.contains(0x24FF));
        assertFalse(set.contains(0x25A0));
    }

    @Test
    void mixedRangeEndsAreAccepted() {
        var set = GlyphSet.parse(List.of("A-U+005A"));
        assertTrue(set.contains('M'));
        assertFalse(set.contains('a'));
    }

    @Test
    void mixedListUnitesAllTokens() {
        var set = GlyphSet.parse(List.of("a", "0-9", "U+2588"));
        assertTrue(set.contains('a'));
        assertTrue(set.contains('5'));
        assertTrue(set.contains(0x2588));
        assertFalse(set.contains('b'));
    }

    @Test
    void commaDashAndSpaceAreSingleCharacters() {
        var set = GlyphSet.parse(List.of(",", "-", " "));
        assertTrue(set.contains(','));
        assertTrue(set.contains('-'));
        assertTrue(set.contains(' '));
        assertFalse(set.contains('.'));
    }

    @Test
    void rangeMayStartOrEndWithDash() {
        assertTrue(GlyphSet.parse(List.of("!--")).contains('#'));
        assertTrue(GlyphSet.parse(List.of("--/")).contains('.'));
        assertTrue(GlyphSet.parse(List.of("U+002D-U+002F")).contains('.'));
    }

    @Test
    void supplementaryCharacterCountsAsOne() {
        var set = GlyphSet.parse(List.of("😀"));
        assertTrue(set.contains(0x1F600));
        assertFalse(set.contains(0xD83D));
    }

    @Test
    void backwardsRangeIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> GlyphSet.parse(List.of("Z-A")));
        assertTrue(error.getMessage().contains("runs backwards"));
        assertTrue(error.getMessage().contains("\"Z-A\""));
    }

    @Test
    void badTokenMessageQuotesTokenAndListsAllowedForms() {
        var error = assertThrows(IllegalArgumentException.class, () -> GlyphSet.parse(List.of("ZZ")));
        assertEquals("\"ZZ\" is not a glyph: use one character like \"A\", a code like \"U+0041\", "
                + "or a range like \"A-Z\"", error.getMessage());
    }

    @Test
    void emptyTokenIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> GlyphSet.parse(List.of("")));
    }

    @Test
    void codeAboveUnicodeIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> GlyphSet.parse(List.of("U+110000")));
        assertTrue(error.getMessage().contains("out of Unicode"));
    }

    @Test
    void allMatchesEveryCodePoint() {
        var set = GlyphSet.all();
        assertTrue(set.contains(0));
        assertTrue(set.contains('A'));
        assertTrue(set.contains(0x10FFFF));
    }

    @Test
    void unionHoldsGlyphsOfEverySet() {
        var set = GlyphSet.union(List.of(GlyphSet.parse(List.of("A-C")), GlyphSet.parse(List.of("x"))));
        assertTrue(set.contains('B'));
        assertTrue(set.contains('x'));
        assertFalse(set.contains('D'));
    }

    @Test
    void unionOfNothingIsEmpty() {
        assertFalse(GlyphSet.union(List.of()).contains('A'));
    }

    @Test
    void fullUnicodeRangeIsCheap() {
        assertTrue(GlyphSet.parse(List.of("U+0000-U+10FFFF")).contains(0x10FFFF));
    }
}
