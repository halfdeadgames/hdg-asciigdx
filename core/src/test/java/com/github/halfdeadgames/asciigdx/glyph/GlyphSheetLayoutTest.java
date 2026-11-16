package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GlyphSheetLayoutTest {

    @Test
    void squareGridOfEightByEight() {
        var layout = GlyphSheetLayout.packed(8, 8, 16, 16);
        assertEquals(256, layout.getSlotCount());
        assertEquals(128, layout.getRequiredImageWidth());
        assertEquals(128, layout.getRequiredImageHeight());
        assertEquals(0, layout.getSlotX(0));
        assertEquals(0, layout.getSlotY(0));
        assertEquals(8, layout.getSlotX(1));
        assertEquals(0, layout.getSlotY(1));
        assertEquals(0, layout.getSlotX(16));
        assertEquals(8, layout.getSlotY(16));
        assertEquals(120, layout.getSlotX(255));
        assertEquals(120, layout.getSlotY(255));
    }

    @Test
    void horizontalStrip() {
        var layout = GlyphSheetLayout.packed(8, 8, 32, 1);
        assertEquals(32, layout.getSlotCount());
        assertEquals(256, layout.getRequiredImageWidth());
        assertEquals(8, layout.getRequiredImageHeight());
        assertEquals(248, layout.getSlotX(31));
        assertEquals(0, layout.getSlotY(31));
    }

    @Test
    void marginAndSpacing() {
        var layout = new GlyphSheetLayout(8, 10, 4, 3, 2, 1);
        assertEquals(2, layout.getSlotX(0));
        assertEquals(2, layout.getSlotY(0));
        assertEquals(11, layout.getSlotX(1));
        assertEquals(2, layout.getSlotY(1));
        assertEquals(2, layout.getSlotX(4));
        assertEquals(13, layout.getSlotY(4));
        assertEquals(29, layout.getSlotX(11));
        assertEquals(24, layout.getSlotY(11));
        assertEquals(4 * 8 + 3 + 4, layout.getRequiredImageWidth());
        assertEquals(3 * 10 + 2 + 4, layout.getRequiredImageHeight());
    }

    @Test
    void badSlotThrows() {
        var layout = GlyphSheetLayout.packed(8, 8, 4, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> layout.getSlotX(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.getSlotY(8));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.getSlotX(8));
    }

    @Test
    void slotCountOverflowThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> GlyphSheetLayout.packed(1, 1, 65536, 65536));
    }

    @Test
    void invalidArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.packed(0, 8, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.packed(8, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.packed(8, 8, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.packed(8, 8, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GlyphSheetLayout(8, 8, 1, 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GlyphSheetLayout(8, 8, 1, 1, 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> GlyphSheetLayout.packed(Integer.MAX_VALUE, 8, 2, 1));
    }
}
