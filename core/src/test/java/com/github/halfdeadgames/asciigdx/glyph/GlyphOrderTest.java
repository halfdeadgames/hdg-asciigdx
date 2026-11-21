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
        assertEquals(256, order.getGlyphCount());
        assertEquals(65, order.findSlot('A'));
        assertEquals(219, order.findSlot(0x2588));
        assertEquals(1, order.findSlot(0x263A));
        assertEquals(32, order.findSlot(0x20));
        assertEquals(0xA0, order.getCodePoint(255));
        assertEquals(0, order.getCodePoint(0));
    }

    @Test
    void cp437HasNoDuplicates() {
        for (int slot = 0; slot < GlyphOrder.CP437.getGlyphCount(); slot++) {
            assertEquals(slot, GlyphOrder.CP437.findSlot(GlyphOrder.CP437.getCodePoint(slot)));
        }
    }

    @Test
    void absentCodePointReturnsMinusOne() {
        assertEquals(-1, GlyphOrder.CP437.findSlot(0x4E2D));
        assertFalse(GlyphOrder.CP437.contains(0x4E2D));
        assertTrue(GlyphOrder.CP437.contains('A'));
    }

    @Test
    void firstDuplicateWins() {
        var order = GlyphOrder.ofCodePoints('a', 'b', 'a');
        assertEquals(3, order.getGlyphCount());
        assertEquals(0, order.findSlot('a'));
        assertEquals('a', order.getCodePoint(2));
    }

    @Test
    void supplementaryCodePointRoundTrips() {
        var order = GlyphOrder.ofCodePoints('x', 0x1FB4B);
        assertEquals(1, order.findSlot(0x1FB4B));
        assertEquals(0x1FB4B, order.getCodePoint(1));
    }

    @Test
    void ofStringKeepsAsciiOrder() {
        var order = GlyphOrder.ofString("abc");
        assertEquals(3, order.getGlyphCount());
        assertEquals(0, order.findSlot('a'));
        assertEquals(2, order.findSlot('c'));
    }

    @Test
    void ofStringSkipsLineTerminators() {
        var order = GlyphOrder.ofString("ab\ncd");
        assertEquals(4, order.getGlyphCount());
        assertEquals(2, order.findSlot('c'));
    }

    @Test
    void ofStringHandlesCarriageReturnLineFeed() {
        var order = GlyphOrder.ofString("ab\r\ncd\r\n");
        assertEquals(4, order.getGlyphCount());
        assertEquals(3, order.findSlot('d'));
    }

    @Test
    void ofStringTreatsSupplementaryCodePointAsOneSlot() {
        var order = GlyphOrder.ofString("x🭋A");
        assertEquals(3, order.getGlyphCount());
        assertEquals(1, order.findSlot(0x1FB4B));
        assertEquals(2, order.findSlot('A'));
    }

    @Test
    void ofStringSkipsVariationSelectors() {
        var order = GlyphOrder.ofString("❤️" + "A");
        assertEquals(2, order.getGlyphCount());
        assertEquals(1, order.findSlot('A'));
        var supplementary = GlyphOrder.ofString("x󠄀y");
        assertEquals(2, supplementary.getGlyphCount());
        assertEquals(1, supplementary.findSlot('y'));
    }

    @Test
    void ofStringSkipsSupplementaryVariationSelectorAfterSupplementaryCharacter() {
        var order = GlyphOrder.ofString("x🭋󠄀A");
        assertEquals(3, order.getGlyphCount());
        assertEquals(1, order.findSlot(0x1FB4B));
        assertEquals(2, order.findSlot('A'));
    }

    @Test
    void ofStringKeepsLoneSurrogateAsOneSlot() {
        var order = GlyphOrder.ofString("a\uD800b");
        assertEquals(3, order.getGlyphCount());
        assertEquals(0xD800, order.getCodePoint(1));
    }

    @Test
    void ofStringKeepsSpaceAsSlot() {
        var order = GlyphOrder.ofString("a b");
        assertEquals(3, order.getGlyphCount());
        assertEquals(1, order.findSlot(' '));
        assertEquals(2, order.findSlot('b'));
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
        assertEquals('a', order.getCodePoint(0));
        assertEquals(-1, order.findSlot('z'));
    }

    @Test
    void badSlotAndInvalidCodePointThrow() {
        var order = GlyphOrder.ofCodePoints('a');
        assertThrows(IndexOutOfBoundsException.class, () -> order.getCodePoint(1));
        assertThrows(IllegalArgumentException.class, () -> GlyphOrder.ofCodePoints(-1));
    }
}
