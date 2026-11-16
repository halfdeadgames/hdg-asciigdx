package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.file;
import static com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataTexts.glyph;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import org.junit.jupiter.api.Test;

class FontSheetTest {

    private static final GlyphList CP437 = GlyphList.parse("cp437");
    private static final FontData DRAWN = FontDataParser.parse(
        file(glyph("U+0041 A", "#......."), glyph("U+0042 B", "##......")));

    @Test
    void withoutAGlyphListTheSheetHoldsOnlyDrawnGlyphs() {
        FontSheet sheet = FontSheet.of(DRAWN, GlyphList.NONE);

        int[] slots = sheet.getSlots();
        assertEquals(16, slots.length);
        assertEquals(0x41, slots[0]);
        assertEquals(0x42, slots[1]);
        assertEquals(SheetOrder.FILLER, slots[2]);
    }

    @Test
    void plannedGlyphsAreNotExported() {
        FontSheet sheet = FontSheet.of(DRAWN, GlyphList.parse("U+0041..U+0050\nU+2603"));

        assertArrayEquals(FontSheet.of(DRAWN, GlyphList.NONE).getSlots(), sheet.getSlots());
    }

    @Test
    void theCp437PresetKeepsTheReservedPositions() {
        FontSheet sheet = FontSheet.of(DRAWN, CP437);

        int[] slots = sheet.getSlots();
        assertEquals(256, slots.length);
        assertEquals(0x41, slots[GlyphOrder.CP437.findSlot(0x41)]);
        assertEquals(0x42, slots[GlyphOrder.CP437.findSlot(0x42)]);
        assertEquals(SheetOrder.FILLER, slots[GlyphOrder.CP437.findSlot(0x43)]);
        assertEquals(SheetOrder.FILLER, slots[0]);
    }

    @Test
    void glyphPixelsFollowTheirSlot() {
        FontSheet sheet = FontSheet.of(DRAWN, GlyphList.NONE);

        assertTrue(sheet.cellAt(1).isLit(1, 0));
        assertFalse(sheet.cellAt(0).isLit(1, 0));
    }
}
