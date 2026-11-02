package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SheetOrderTest {

    private static final int FILLER = SheetOrder.FILLER;

    private final int[] slots = SheetOrder.slotsFor(
        Set.of(0x41, 0x263A, 0xE9, 0x391, 0x392, 0x410, 0xE000));

    @Test
    void cp437GlyphsKeepTheirStandardSlots() {
        assertEquals(0x41, slots[GlyphOrder.CP437.slotOf(0x41)]);
        assertEquals(0x263A, slots[1]);
        assertEquals(0xE9, slots[GlyphOrder.CP437.slotOf(0xE9)]);
    }

    @Test
    void missingCp437GlyphsLeaveFillerSlots() {
        assertEquals(FILLER, slots[0]);
        assertEquals(FILLER, slots[GlyphOrder.CP437.slotOf(0x42)]);
    }

    @Test
    void otherBlocksStartOnNewRowsAfterCp437() {
        assertEquals(0x391, slots[256]);
        assertEquals(0x392, slots[257]);
        assertEquals(FILLER, slots[258]);
        assertEquals(0x410, slots[272]);
    }

    @Test
    void privateUseGlyphsComeLast() {
        assertEquals(304, slots.length);
        assertEquals(0xE000, slots[288]);
    }

    @Test
    void privateUseStaysAfterHigherCodePoints() {
        int[] order = SheetOrder.slotsFor(Set.of(0xE000, 0xFF21));

        assertEquals(0xFF21, order[256]);
        assertEquals(0xE000, order[272]);
    }
}
