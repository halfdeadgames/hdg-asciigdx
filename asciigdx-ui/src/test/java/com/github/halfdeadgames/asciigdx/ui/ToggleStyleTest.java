package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToggleStyleTest {
    private static final ButtonStyle BUTTON =
            ButtonStyle.of(LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x000000FF));

    @Test
    void newStyleHasDefaultGapAndMarks() {
        ToggleStyle style = ToggleStyle.of(BUTTON, "[ ]", "[x]");

        assertEquals(ToggleStyle.DEFAULT_GAP, style.getGap());
        assertEquals("[ ]", style.getUncheckedMark());
        assertEquals("[x]", style.getCheckedMark());
        assertEquals(3, style.getMarkLength());
        assertEquals(UiFont.SQUARE, style.getFont());
        assertEquals(BUTTON, style.getButtonStyle());
    }

    @Test
    void marksAreMeasuredInCodePoints() {
        String emoji = new String(Character.toChars(0x1F600));

        ToggleStyle style = ToggleStyle.of(BUTTON, emoji, "x");

        assertEquals(1, style.getMarkLength());
    }

    @Test
    void unequalMarkLengthsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ToggleStyle.of(BUTTON, "[ ]", "x"));
    }

    @Test
    void emptyMarksAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ToggleStyle.of(BUTTON, "", ""));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> ToggleStyle.of(null, "a", "b"));
        assertThrows(NullPointerException.class, () -> ToggleStyle.of(BUTTON, null, "b"));
        assertThrows(NullPointerException.class, () -> ToggleStyle.of(BUTTON, "a", null));
    }

    @Test
    void gapMustNotBeNegative() {
        ToggleStyle style = ToggleStyle.of(BUTTON, "a", "b");

        assertEquals(0, style.withGap(0).getGap());
        assertThrows(IllegalArgumentException.class, () -> style.withGap(-1));
    }

    @Test
    void equalsHashCodeAndToStringUseAllFields() {
        ToggleStyle style = ToggleStyle.of(BUTTON, "a", "b");

        assertEquals(style, ToggleStyle.of(BUTTON, "a", "b"));
        assertEquals(style.hashCode(), ToggleStyle.of(BUTTON, "a", "b").hashCode());
        assertNotEquals(style, style.withGap(3));
        assertNotEquals(style, ToggleStyle.of(BUTTON, "a", "c"));
        assertEquals(true, style.toString().contains("gap=1"));
    }
}
