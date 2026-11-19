package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MenuBarStyleTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final ButtonStyle TALL =
            ButtonStyle.of(LabelStyle.of(UiFont.TALL, WHITE, BLACK));
    private static final ButtonStyle OTHER_TALL =
            ButtonStyle.of(LabelStyle.of(UiFont.TALL, BLACK, WHITE));
    private static final ButtonStyle SQUARE =
            ButtonStyle.of(LabelStyle.of(UiFont.SQUARE, WHITE, BLACK));

    @Test
    void gettersReturnTheGivenStylesAndTheirFont() {
        MenuBarStyle style = MenuBarStyle.of(TALL, OTHER_TALL);

        assertSame(TALL, style.getTitles());
        assertSame(OTHER_TALL, style.getItems());
        assertEquals(UiFont.TALL, style.getFont());
    }

    @Test
    void differentFontsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MenuBarStyle.of(TALL, SQUARE));
    }

    @Test
    void nullStylesAreRejected() {
        assertThrows(NullPointerException.class, () -> MenuBarStyle.of(null, TALL));
        assertThrows(NullPointerException.class, () -> MenuBarStyle.of(TALL, null));
    }

    @Test
    void equalStylesHaveEqualHashCodesAndDifferentStylesDiffer() {
        MenuBarStyle style = MenuBarStyle.of(TALL, OTHER_TALL);
        MenuBarStyle same = MenuBarStyle.of(TALL, OTHER_TALL);

        assertEquals(style, same);
        assertEquals(style.hashCode(), same.hashCode());
        assertNotEquals(style, MenuBarStyle.of(OTHER_TALL, TALL));
        assertNotEquals(style, "style");
    }

    @Test
    void toStringNamesBothStyles() {
        String text = MenuBarStyle.of(TALL, OTHER_TALL).toString();

        assertTrue(text.startsWith("MenuBarStyle[titles=ButtonStyle["));
        assertTrue(text.contains(", items=ButtonStyle["));
        assertTrue(text.endsWith(", triggerRgba=none]"));
    }

    @Test
    void newStyleHasNoTriggerColor() {
        MenuBarStyle style = MenuBarStyle.of(TALL, OTHER_TALL);

        assertFalse(style.hasTriggerRgba());
        assertThrows(IllegalStateException.class, style::getTriggerRgba);
    }

    @Test
    void withTriggerRgbaReturnsACopyWithTheColor() {
        MenuBarStyle plain = MenuBarStyle.of(TALL, OTHER_TALL);

        MenuBarStyle colored = plain.withTriggerRgba(0xFF0000FF);

        assertTrue(colored.hasTriggerRgba());
        assertEquals(0xFF0000FF, colored.getTriggerRgba());
        assertSame(TALL, colored.getTitles());
        assertSame(OTHER_TALL, colored.getItems());
        assertFalse(plain.hasTriggerRgba());
    }

    @Test
    void triggerColorTakesPartInEqualsHashCodeAndToString() {
        MenuBarStyle plain = MenuBarStyle.of(TALL, OTHER_TALL);
        MenuBarStyle red = plain.withTriggerRgba(0xFF0000FF);

        assertEquals(red, plain.withTriggerRgba(0xFF0000FF));
        assertEquals(red.hashCode(), plain.withTriggerRgba(0xFF0000FF).hashCode());
        assertNotEquals(red, plain);
        assertNotEquals(red, plain.withTriggerRgba(0x00FF00FF));
        assertTrue(red.toString().endsWith(", triggerRgba=ff0000ff]"));
    }
}
