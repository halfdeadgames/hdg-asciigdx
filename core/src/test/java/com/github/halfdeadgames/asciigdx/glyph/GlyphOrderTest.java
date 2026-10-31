package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphOrderTest {

    @Test
    void cp437HasKnownSlots() {
        var order = GlyphOrder.CP437;
        assertEquals(256, order.size());
        assertEquals(65, order.slotOf('A'));
        assertEquals(219, order.slotOf(0x2588));
        assertEquals(1, order.slotOf(0x263A));
        assertEquals(32, order.slotOf(0x20));
        assertEquals(0xA0, order.codePointAt(255));
        assertEquals(0, order.codePointAt(0));
    }

    @Test
    void cp437HasNoDuplicates() {
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            assertEquals(slot, GlyphOrder.CP437.slotOf(GlyphOrder.CP437.codePointAt(slot)));
        }
    }

    @Test
    void absentCodePointReturnsMinusOne() {
        assertEquals(-1, GlyphOrder.CP437.slotOf(0x4E2D));
        assertFalse(GlyphOrder.CP437.contains(0x4E2D));
        assertTrue(GlyphOrder.CP437.contains('A'));
    }

    @Test
    void firstDuplicateWins() {
        var order = GlyphOrder.ofCodePoints('a', 'b', 'a');
        assertEquals(3, order.size());
        assertEquals(0, order.slotOf('a'));
        assertEquals('a', order.codePointAt(2));
    }

    @Test
    void supplementaryCodePointRoundTrips() {
        var order = GlyphOrder.ofCodePoints('x', 0x1FB4B);
        assertEquals(1, order.slotOf(0x1FB4B));
        assertEquals(0x1FB4B, order.codePointAt(1));
    }

    @Test
    void ofStringKeepsAsciiOrder() {
        var order = GlyphOrder.ofString("abc");
        assertEquals(3, order.size());
        assertEquals(0, order.slotOf('a'));
        assertEquals(2, order.slotOf('c'));
    }

    @Test
    void ofStringSkipsLineTerminators() {
        var order = GlyphOrder.ofString("ab\ncd");
        assertEquals(4, order.size());
        assertEquals(2, order.slotOf('c'));
    }

    @Test
    void ofStringHandlesCarriageReturnLineFeed() {
        var order = GlyphOrder.ofString("ab\r\ncd\r\n");
        assertEquals(4, order.size());
        assertEquals(3, order.slotOf('d'));
    }

    @Test
    void ofStringTreatsSupplementaryCodePointAsOneSlot() {
        var order = GlyphOrder.ofString("x🭋A");
        assertEquals(3, order.size());
        assertEquals(1, order.slotOf(0x1FB4B));
        assertEquals(2, order.slotOf('A'));
    }

    @Test
    void ofStringSkipsVariationSelectors() {
        var order = GlyphOrder.ofString("❤️" + "A");
        assertEquals(2, order.size());
        assertEquals(1, order.slotOf('A'));
        var supplementary = GlyphOrder.ofString("x󠄀y");
        assertEquals(2, supplementary.size());
        assertEquals(1, supplementary.slotOf('y'));
    }

    @Test
    void ofStringKeepsSpaceAsSlot() {
        var order = GlyphOrder.ofString("a b");
        assertEquals(3, order.size());
        assertEquals(1, order.slotOf(' '));
        assertEquals(2, order.slotOf('b'));
    }

    @Test
    void ofStringRejectsEmptyAndNull() {
        assertThrows(IllegalArgumentException.class, () -> GlyphOrder.ofString(""));
        assertThrows(IllegalArgumentException.class, () -> GlyphOrder.ofString("️\r\n"));
        assertThrows(NullPointerException.class, () -> GlyphOrder.ofString(null));
    }

    @Test
    void inputArrayIsCopied() {
        int[] source = {'a', 'b'};
        var order = GlyphOrder.ofCodePoints(source);
        source[0] = 'z';
        assertEquals('a', order.codePointAt(0));
        assertEquals(-1, order.slotOf('z'));
    }

    @Test
    void badSlotAndInvalidCodePointThrow() {
        var order = GlyphOrder.ofCodePoints('a');
        assertThrows(IndexOutOfBoundsException.class, () -> order.codePointAt(1));
        assertThrows(IllegalArgumentException.class, () -> GlyphOrder.ofCodePoints(-1));
    }
}
