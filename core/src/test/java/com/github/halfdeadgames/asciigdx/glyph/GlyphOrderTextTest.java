package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphOrderTextTest {
    @Test
    void escapeCodePointLeavesPlainCharactersAlone() {
        assertEquals("A", GlyphOrderText.escapeCodePoint('A'));
        assertEquals(" ", GlyphOrderText.escapeCodePoint(' '));
    }

    @Test
    void escapeCodePointEscapesHardToSeeCharacters() {
        assertEquals("\\u{E01F}", GlyphOrderText.escapeCodePoint(0xE01F));
        assertEquals("\\\\", GlyphOrderText.escapeCodePoint('\\'));
        assertEquals("\\u{9}", GlyphOrderText.escapeCodePoint('\t'));
    }


    private static int[] codePoints(GlyphOrder order) {
        int[] result = new int[order.size()];
        for (int slot = 0; slot < result.length; slot++) {
            result[slot] = order.codePointAt(slot);
        }
        return result;
    }

    @Test
    void cp437RoundTripsWithSixteenColumns() {
        String text = GlyphOrderText.write(GlyphOrder.CP437, 16);
        assertEquals(16, text.split("\n").length);
        assertArrayEquals(codePoints(GlyphOrder.CP437), codePoints(GlyphOrderText.read(text, 16)));
    }

    @Test
    void privateUseAndSupplementaryCodePointsRoundTrip() {
        GlyphOrder order = GlyphOrder.ofCodePoints(0xE000, 0xE08F, 0xF8FF, 0x1F600, 0x10FFFD, 'A');
        String text = GlyphOrderText.write(order, 4);
        assertArrayEquals(codePoints(order), codePoints(GlyphOrderText.read(text, 4)));
    }

    @Test
    void hardToSeeCharactersAreWrittenAsEscapes() {
        GlyphOrder order = GlyphOrder.ofCodePoints(0x0000, 0x00A0, 0xE001, '\\', 'A', ' ');
        assertEquals("\\u{0}\\u{A0}\\u{E001}\\\\A\\u{20}\n", GlyphOrderText.write(order, 6));
    }

    @Test
    void spaceInsideALineIsWrittenAsItself() {
        assertEquals("A B\n", GlyphOrderText.write(GlyphOrder.ofString("A B"), 3));
    }

    @Test
    void lastRowMayBeShorter() {
        GlyphOrder order = GlyphOrderText.read("ABC\nDE\n", 3);
        assertEquals(5, order.size());
        assertEquals('E', order.codePointAt(4));
    }

    @Test
    void shortRowBeforeTheLastIsRejected() {
        IllegalArgumentException error =
            assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.read("AB\nCDE\n", 3));
        assertTrue(error.getMessage().contains("Line 1"));
    }

    @Test
    void longRowIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.read("ABCD\n", 3));
    }

    @Test
    void windowsLineEndingsAndByteOrderMarkAreAccepted() {
        GlyphOrder order = GlyphOrderText.read("﻿AB\r\nCD\r\n", 2);
        assertArrayEquals(new int[] {'A', 'B', 'C', 'D'}, codePoints(order));
    }

    @Test
    void escapesAreAcceptedAnywhere() {
        GlyphOrder order = GlyphOrderText.read("\\u{263a}\\\\x\n", 3);
        assertArrayEquals(new int[] {0x263A, '\\', 'x'}, codePoints(order));
    }

    @Test
    void malformedEscapesAreRejected() {
        for (String bad : new String[] {"\\x", "\\u{}", "\\u{12", "\\u{GG}", "\\u{1234567}", "\\u{110000}", "\\"}) {
            assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.read(bad, 8), bad);
        }
    }

    @Test
    void emptyTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.read("\n\n", 4));
    }

    @Test
    void readAnyRowLengthKeepsEveryRowInOrder() {
        GlyphOrder order = GlyphOrderText.readAnyRowLength("AB\nCDEF\n\\u{E0D0}\n");
        assertArrayEquals(new int[] {'A', 'B', 'C', 'D', 'E', 'F', 0xE0D0}, codePoints(order));
    }

    @Test
    void readAnyRowLengthRejectsBadEscapesAndEmptyText() {
        assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.readAnyRowLength("\\x"));
        assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.readAnyRowLength("\n"));
    }

    @Test
    void nonPositiveColumnsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.read("A", 0));
        assertThrows(IllegalArgumentException.class, () -> GlyphOrderText.write(GlyphOrder.CP437, 0));
    }
}
