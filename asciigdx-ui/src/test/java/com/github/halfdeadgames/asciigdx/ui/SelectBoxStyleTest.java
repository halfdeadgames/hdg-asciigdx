package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SelectBoxStyleTest {
    private static final ButtonStyle BUTTON = ButtonStyle.of(LabelStyle.of(1, 2));
    private static final ButtonStyle LIST = ButtonStyle.of(LabelStyle.of(3, 4));
    private static final ScrollPaneStyle PANE = ScrollPaneStyle.of(5, 6);

    private static SelectBoxStyle plain() {
        return SelectBoxStyle.of(BUTTON, LIST, PANE);
    }

    @Test
    void newStyleHoldsItsPartsAndTheDefaultArrow() {
        SelectBoxStyle style = plain();

        assertSame(BUTTON, style.getBoxStyle());
        assertSame(LIST, style.getRowStyle());
        assertSame(PANE, style.getScrollPane());
        assertEquals(0x25BC, style.getArrowGlyph());
        assertEquals(SelectBoxStyle.DEFAULT_ARROW_GLYPH, style.getArrowGlyph());
        assertEquals(MarkFont.SQUARE, style.getArrowFont());
    }

    @Test
    void withMethodsReturnCopiesAndLeaveTheOriginalUnchanged() {
        SelectBoxStyle original = plain();

        SelectBoxStyle changed = original.withArrowGlyph('v').withArrowFont(MarkFont.TEXT);

        assertEquals('v', changed.getArrowGlyph());
        assertEquals(MarkFont.TEXT, changed.getArrowFont());
        assertEquals(SelectBoxStyle.DEFAULT_ARROW_GLYPH, original.getArrowGlyph());
        assertEquals(MarkFont.SQUARE, original.getArrowFont());
    }

    @Test
    void equalStylesHaveEqualHashCodes() {
        assertEquals(plain(), plain());
        assertEquals(plain().hashCode(), plain().hashCode());
        assertNotEquals(plain(), plain().withArrowGlyph('v'));
        assertNotEquals(plain(), plain().withArrowFont(MarkFont.TEXT));
        assertNotEquals(plain(), SelectBoxStyle.of(LIST, LIST, PANE));
        assertNotEquals(plain(), SelectBoxStyle.of(BUTTON, BUTTON, PANE));
        assertNotEquals(plain(), SelectBoxStyle.of(BUTTON, LIST, ScrollPaneStyle.of(7, 8)));
    }

    @Test
    void toStringNamesTheClassAndTheArrow() {
        assertTrue(plain().toString().startsWith("SelectBoxStyle["));
        assertTrue(plain().toString().contains("U+25BC"));
    }

    @Test
    void badArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> SelectBoxStyle.of(null, LIST, PANE));
        assertThrows(NullPointerException.class, () -> SelectBoxStyle.of(BUTTON, null, PANE));
        assertThrows(NullPointerException.class, () -> SelectBoxStyle.of(BUTTON, LIST, null));
        assertThrows(NullPointerException.class, () -> plain().withArrowFont(null));
        assertThrows(IllegalArgumentException.class, () -> plain().withArrowGlyph(-1));
        assertThrows(IllegalArgumentException.class, () -> plain().withArrowGlyph(0x110000));
        assertThrows(IllegalArgumentException.class, () -> PANE.withUpArrowGlyph(-1));
        assertThrows(IllegalArgumentException.class, () -> PANE.withDownArrowGlyph(-1));
        assertThrows(IllegalArgumentException.class, () -> PANE.withLeftArrowGlyph(0x110000));
        assertThrows(IllegalArgumentException.class, () -> PANE.withRightArrowGlyph(0x110000));
    }
}
