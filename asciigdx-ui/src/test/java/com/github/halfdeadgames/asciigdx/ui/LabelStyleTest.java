package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LabelStyleTest {

    @Test
    void equalValuesGiveEqualStylesAndHashCodes() {
        LabelStyle first = LabelStyle.of(1, 2);
        LabelStyle second = LabelStyle.of(1, 2);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.toString(), second.toString());
    }

    @Test
    void differentValuesGiveDifferentStyles() {
        assertNotEquals(LabelStyle.of(1, 2), LabelStyle.of(2, 1));
        assertNotEquals(LabelStyle.of(1, 2), LabelStyle.of(1, 3));
    }

    @Test
    void toStringShowsColorsAndNoFont() {
        String text = LabelStyle.of(0xAA, 0xBB).toString();

        assertTrue(text.contains("foregroundRgba=aa"));
        assertTrue(text.contains("backgroundRgba=bb"));
        assertFalse(text.toLowerCase().contains("font"));
    }

    @Test
    void gettersReturnValues() {
        LabelStyle style = LabelStyle.of(0xAA, 0xBB);
        assertEquals(0xAA, style.getForegroundRgba());
        assertEquals(0xBB, style.getBackgroundRgba());
    }
}
