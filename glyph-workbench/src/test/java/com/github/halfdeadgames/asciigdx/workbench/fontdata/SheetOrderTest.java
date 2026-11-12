package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.workbench.unicodenames.UnicodeFacts;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SheetOrderTest {

    private static final int FILLER = SheetOrder.FILLER;

    private final int[] slots = SheetOrder.slotsFor(
        Set.of(0x41, 0x263A, 0xE9, 0x391, 0x392, 0x410, 0xE000), true);

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
        int[] order = SheetOrder.slotsFor(Set.of(0xE000, 0xFF21), true);

        assertEquals(0xFF21, order[256]);
        assertEquals(0xE000, order[272]);
    }

    @Test
    void withoutReservationThereIsNoCp437Prefix() {
        int[] order = SheetOrder.slotsFor(Set.of(0x41, 0x263A), false);

        assertEquals(0x41, order[0]);
        assertEquals(FILLER, order[1]);
        assertEquals(32, order.length);
    }

    @Test
    void withoutReservationCp437CodePointsGoByBlockAndBlocksStartNewRows() {
        int[] order = SheetOrder.slotsFor(Set.of(0x41, 0x42, 0xE9, 0x263A, 0x391), false);

        assertEquals(0x41, order[0]);
        assertEquals(0x42, order[1]);
        assertEquals(FILLER, order[2]);
        assertEquals(0xE9, order[16]);
        assertEquals(0x391, order[32]);
        assertEquals(0x263A, order[48]);
        assertEquals(64, order.length);
    }

    @Test
    void withoutReservationPrivateUseStillComesLast() {
        int[] order = SheetOrder.slotsFor(Set.of(0xE000, 0xFF21, 0x41), false);

        assertEquals(0x41, order[0]);
        assertEquals(0xFF21, order[16]);
        assertEquals(0xE000, order[32]);
        assertEquals(48, order.length);
    }

    @Test
    void twoDifferentBlocksNewerThanTheJdkGetSeparateRows() {
        UnicodeFacts oldJdk = UnicodeFacts.fromTableText("""
            1FA8F So SHOVEL
            1FB00 So LEGACY ONE
            BLOCK 1FA80..1FAFF Symbols and Pictographs Extended-A
            BLOCK 1FB00..1FBFF Symbols for Legacy Computing
            """, codePoint -> Character.UNASSIGNED, codePoint -> null);

        int[] order = SheetOrder.slotsFor(Set.of(0x1FA8F, 0x1FB00), false, oldJdk);

        assertEquals(0x1FA8F, order[0]);
        assertEquals(FILLER, order[1]);
        assertEquals(0x1FB00, order[16]);
        assertEquals(32, order.length);
    }

    @Test
    void sameNewerBlockSharesARow() {
        UnicodeFacts oldJdk = UnicodeFacts.fromTableText("""
            1FA8F So SHOVEL
            1FA90 So OTHER
            BLOCK 1FA80..1FAFF Symbols and Pictographs Extended-A
            """, codePoint -> Character.UNASSIGNED, codePoint -> null);

        int[] order = SheetOrder.slotsFor(Set.of(0x1FA8F, 0x1FA90), false, oldJdk);

        assertEquals(0x1FA8F, order[0]);
        assertEquals(0x1FA90, order[1]);
        assertEquals(16, order.length);
    }

    @Test
    void withoutReservationNoGlyphsMeansNoSlots() {
        assertEquals(0, SheetOrder.slotsFor(Set.of(), false).length);
    }
}
