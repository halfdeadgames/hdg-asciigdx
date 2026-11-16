package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GlyphListTest {

    @Test
    void noneHasNothing() {
        assertFalse(GlyphList.NONE.usesCp437());
        assertTrue(GlyphList.NONE.plannedCodePoints().isEmpty());
    }

    @Test
    void emptyTextHasNothing() {
        GlyphList list = GlyphList.parse("");
        assertFalse(list.usesCp437());
        assertTrue(list.plannedCodePoints().isEmpty());
    }

    @Test
    void readsPreset() {
        assertTrue(GlyphList.parse("cp437").usesCp437());
    }

    @Test
    void presetIsNotExpanded() {
        assertTrue(GlyphList.parse("cp437").plannedCodePoints().isEmpty());
    }

    @Test
    void readsSingleCodePoint() {
        assertEquals(List.of(0x2588),
                new ArrayList<>(GlyphList.parse("U+2588").plannedCodePoints()));
    }

    @Test
    void readsRangeInclusive() {
        Set<Integer> planned = GlyphList.parse("U+2190..U+2193").plannedCodePoints();
        assertEquals(List.of(0x2190, 0x2191, 0x2192, 0x2193), new ArrayList<>(planned));
    }

    @Test
    void skipsBlankLinesAndWhitespace() {
        GlyphList list = GlyphList.parse("\n  U+0041  \r\n\t\r\n   cp437 \t\nU+0042\n\n");
        assertTrue(list.usesCp437());
        assertEquals(List.of(0x41, 0x42), new ArrayList<>(list.plannedCodePoints()));
    }

    @Test
    void acceptsByteOrderMarkBeforeFirstLine() {
        assertTrue(GlyphList.parse("﻿cp437\nU+0041").usesCp437());
        assertEquals(List.of(0x41),
                new ArrayList<>(GlyphList.parse("﻿U+0041").plannedCodePoints()));
    }

    @Test
    void ignoresCase() {
        GlyphList list = GlyphList.parse("CP437\nu+00e9\nU+00Ff..u+0100");
        assertTrue(list.usesCp437());
        assertEquals(List.of(0xE9, 0xFF, 0x100), new ArrayList<>(list.plannedCodePoints()));
    }

    @Test
    void duplicatesAndOverlapsCollapse() {
        GlyphList list =
                GlyphList.parse("U+0041\nU+0041\nU+0040..U+0042\nU+0041..U+0043\ncp437\ncp437");
        assertEquals(List.of(0x40, 0x41, 0x42, 0x43), new ArrayList<>(list.plannedCodePoints()));
    }

    @Test
    void outputIsSorted() {
        GlyphList list = GlyphList.parse("U+0100\nU+0041..U+0042\nU+0020");
        assertEquals(List.of(0x20, 0x41, 0x42, 0x100), new ArrayList<>(list.plannedCodePoints()));
    }

    @Test
    void outputIsUnmodifiable() {
        Set<Integer> planned = GlyphList.parse("U+0041").plannedCodePoints();
        assertThrows(UnsupportedOperationException.class, () -> planned.add(1));
        assertThrows(UnsupportedOperationException.class,
                () -> GlyphList.NONE.plannedCodePoints().add(1));
    }

    @Test
    void rejectsUnrecognizedLineWithLineNumber() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+0041\n\nhello"));
        assertEquals(3, error.getLine());
        assertTrue(error.getMessage().startsWith("Line 3: "));
        assertTrue(error.getMessage().contains("'hello'"));
    }

    @Test
    void rejectsCommentsAndMalformedEntries() {
        for (String bad : List.of("# note", "U+", "U+XYZ", "0041", "U+0041..", "U+0041..0043",
                "U+0041 U+0042",
                "cp437 extra")) {
            assertThrows(FontDataException.class, () -> GlyphList.parse(bad), bad);
        }
    }

    @Test
    void rejectsCodePointBeyondUnicode() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+0041\nU+110000"));
        assertEquals(2, error.getLine());
        assertTrue(error.getMessage().contains("'U+110000' goes past U+10FFFF"));
    }

    @Test
    void acceptsLeadingZerosUpToEightDigits() {
        assertEquals(Set.of(0x41, 0x42),
                GlyphList.parse("U+00000041\nU+0000042").plannedCodePoints());
    }

    @Test
    void eightDigitCodePastUnicodeNamesTheLine() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+FFFFFFFF"));
        assertEquals(1, error.getLine());
        assertTrue(error.getMessage().contains("'U+FFFFFFFF' goes past U+10FFFF"));
    }

    @Test
    void rejectsRangeEndBeyondUnicode() {
        FontDataException error = assertThrows(FontDataException.class,
                () -> GlyphList.parse("U+10FFFF..U+FFFFFFFF"));
        assertEquals(1, error.getLine());
    }

    @Test
    void rejectsSurrogate() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("\nU+D800"));
        assertEquals(2, error.getLine());
        assertTrue(error.getMessage().contains("cannot be used as a glyph"));
    }

    @Test
    void rejectsFiller() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+10FFFD"));
        assertEquals(1, error.getLine());
        assertTrue(error.getMessage().contains("cannot be used as a glyph"));
    }

    @Test
    void rejectsReversedRange() {
        FontDataException error = assertThrows(FontDataException.class,
                () -> GlyphList.parse("U+0041\nU+0043..U+0042"));
        assertEquals(2, error.getLine());
        assertTrue(error.getMessage().contains("backwards"));
    }

    @Test
    void rejectsSurrogateInsideRange() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+D7FF..U+E000"));
        assertEquals(1, error.getLine());
        assertTrue(error.getMessage().contains("U+D800"));
    }

    @Test
    void rejectsFillerInsideRange() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+10FFF0..U+10FFFF"));
        assertEquals(1, error.getLine());
        assertTrue(error.getMessage().contains("U+10FFFD"));
    }

    @Test
    void acceptsRangeExactlyAtLimit() {
        Set<Integer> planned = GlyphList.parse("U+10000..U+1FFFF").plannedCodePoints();
        assertEquals(GlyphList.MAX_PLANNED, planned.size());
    }

    @Test
    void overlapDoesNotCountTwiceAgainstLimit() {
        Set<Integer> planned =
                GlyphList.parse("U+10000..U+1FFFF\nU+10000..U+10010").plannedCodePoints();
        assertEquals(GlyphList.MAX_PLANNED, planned.size());
    }

    @Test
    void rejectsOneOverLimitAcrossLines() {
        FontDataException error = assertThrows(FontDataException.class,
                () -> GlyphList.parse("U+10000..U+1FFFF\nU+0041"));
        assertEquals(2, error.getLine());
        assertTrue(error.getMessage().contains("65536"));
    }

    @Test
    void rejectsHugeRangeQuickly() {
        FontDataException error =
                assertThrows(FontDataException.class, () -> GlyphList.parse("U+0000..U+10FFFF"));
        assertEquals(1, error.getLine());
        assertTrue(error.getMessage().contains("65536"));
    }
}
