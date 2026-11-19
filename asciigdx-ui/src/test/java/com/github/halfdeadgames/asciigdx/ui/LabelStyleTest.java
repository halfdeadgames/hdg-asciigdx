package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LabelStyleTest {

    @Test
    void equalValuesGiveEqualStylesAndHashCodes() {
        LabelStyle first = LabelStyle.of(UiFont.SQUARE, 1, 2);
        LabelStyle second = LabelStyle.of(UiFont.SQUARE, 1, 2);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.toString(), second.toString());
    }

    @Test
    void differentValuesGiveDifferentStyles() {
        assertNotEquals(LabelStyle.of(UiFont.SQUARE, 1, 2), LabelStyle.of(UiFont.SQUARE, 2, 1));
        assertNotEquals(LabelStyle.of(UiFont.SQUARE, 1, 2), LabelStyle.of(UiFont.SQUARE, 1, 3));
        assertNotEquals(LabelStyle.of(UiFont.SQUARE, 1, 2), LabelStyle.of(UiFont.TALL, 1, 2));
    }

    @Test
    void fontIsPartOfToString() {
        assertEquals(true, LabelStyle.of(UiFont.TALL, 1, 2).toString().contains("TALL"));
    }

    @Test
    void gettersReturnValues() {
        LabelStyle style = LabelStyle.of(UiFont.TALL, 0xAA, 0xBB);
        assertEquals(UiFont.TALL, style.getFont());
        assertEquals(0xAA, style.getForegroundRgba());
        assertEquals(0xBB, style.getBackgroundRgba());
    }

    @Test
    void nullFontThrows() {
        assertThrows(NullPointerException.class, () -> LabelStyle.of(null, 1, 2));
    }
}
