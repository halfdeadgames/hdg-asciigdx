package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GlyphSheetLayoutTest {

    @Test
    void squareGridOfEightByEight() {
        var layout = GlyphSheetLayout.grid(8, 8, 16, 16);
        assertEquals(256, layout.slotCount());
        assertEquals(128, layout.requiredImageWidth());
        assertEquals(128, layout.requiredImageHeight());
        assertEquals(0, layout.slotX(0));
        assertEquals(0, layout.slotY(0));
        assertEquals(8, layout.slotX(1));
        assertEquals(0, layout.slotY(1));
        assertEquals(0, layout.slotX(16));
        assertEquals(8, layout.slotY(16));
        assertEquals(120, layout.slotX(255));
        assertEquals(120, layout.slotY(255));
    }

    @Test
    void horizontalStrip() {
        var layout = GlyphSheetLayout.grid(8, 8, 32, 1);
        assertEquals(32, layout.slotCount());
        assertEquals(256, layout.requiredImageWidth());
        assertEquals(8, layout.requiredImageHeight());
        assertEquals(248, layout.slotX(31));
        assertEquals(0, layout.slotY(31));
    }

    @Test
    void marginAndSpacing() {
        var layout = new GlyphSheetLayout(8, 10, 4, 3, 2, 1);
        assertEquals(2, layout.slotX(0));
        assertEquals(2, layout.slotY(0));
        assertEquals(11, layout.slotX(1));
        assertEquals(2, layout.slotY(1));
        assertEquals(2, layout.slotX(4));
        assertEquals(13, layout.slotY(4));
        assertEquals(29, layout.slotX(11));
        assertEquals(24, layout.slotY(11));
        assertEquals(4 * 8 + 3 + 4, layout.requiredImageWidth());
        assertEquals(3 * 10 + 2 + 4, layout.requiredImageHeight());
    }

    @Test
    void badSlotThrows() {
        var layout = GlyphSheetLayout.grid(8, 8, 4, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> layout.slotX(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.slotY(8));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.slotX(8));
    }

    @Test
    void invalidArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.grid(0, 8, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.grid(8, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.grid(8, 8, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> GlyphSheetLayout.grid(8, 8, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GlyphSheetLayout(8, 8, 1, 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GlyphSheetLayout(8, 8, 1, 1, 0, -1));
        assertThrows(IllegalArgumentException.class,
            () -> GlyphSheetLayout.grid(Integer.MAX_VALUE, 8, 2, 1));
    }
}
