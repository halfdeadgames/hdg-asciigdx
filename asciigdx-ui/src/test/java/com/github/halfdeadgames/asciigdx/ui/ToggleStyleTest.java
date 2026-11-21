package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToggleStyleTest {
    private static final ButtonStyle BUTTON =
            ButtonStyle.of(LabelStyle.of(0xFFFFFFFF, 0x000000FF));

    @Test
    void newStyleHasDefaultGapAndMarks() {
        ToggleStyle style = ToggleStyle.of(BUTTON, "[ ]", "[x]");

        assertEquals(ToggleStyle.DEFAULT_GAP, style.getGap());
        assertEquals("[ ]", style.getUncheckedMark());
        assertEquals("[x]", style.getCheckedMark());
        assertEquals(3, style.getMarkLength());
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

    @Test
    void markFontDefaultsToText() {
        assertEquals(MarkFont.TEXT, ToggleStyle.of(BUTTON, "[ ]", "[x]").getMarkFont());
    }

    @Test
    void squareMarkFontDoublesTheMarkLengthInTextColumns() {
        ToggleStyle square = ToggleStyle.of(BUTTON, "[ ]", "[x]").withMarkFont(MarkFont.SQUARE);

        assertEquals(MarkFont.SQUARE, square.getMarkFont());
        assertEquals(6, square.getMarkLength());
        assertEquals(3, square.withMarkFont(MarkFont.TEXT).getMarkLength());
    }

    @Test
    void withMarkFontRejectsNullAndKeepsTheOtherParts() {
        ToggleStyle style = ToggleStyle.of(BUTTON, "a", "b").withGap(3);

        assertThrows(NullPointerException.class, () -> style.withMarkFont(null));
        ToggleStyle square = style.withMarkFont(MarkFont.SQUARE);
        assertEquals(3, square.getGap());
        assertEquals("a", square.getUncheckedMark());
        assertEquals(MarkFont.TEXT, style.getMarkFont());
    }

    @Test
    void markFontTakesPartInEqualsHashCodeAndToString() {
        ToggleStyle text = ToggleStyle.of(BUTTON, "a", "b");
        ToggleStyle square = text.withMarkFont(MarkFont.SQUARE);

        assertNotEquals(text, square);
        assertEquals(square, ToggleStyle.of(BUTTON, "a", "b").withMarkFont(MarkFont.SQUARE));
        assertEquals(square.hashCode(),
                ToggleStyle.of(BUTTON, "a", "b").withMarkFont(MarkFont.SQUARE).hashCode());
        assertEquals(true, square.toString().contains("markFont=SQUARE"));
    }
}
