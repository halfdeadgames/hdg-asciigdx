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
    private static final ButtonStyle TITLES = ButtonStyle.of(LabelStyle.of(WHITE, BLACK));
    private static final ButtonStyle ITEMS = ButtonStyle.of(LabelStyle.of(BLACK, WHITE));

    @Test
    void gettersReturnTheGivenStyles() {
        MenuBarStyle style = MenuBarStyle.of(TITLES, ITEMS);

        assertSame(TITLES, style.getTitles());
        assertSame(ITEMS, style.getItems());
    }

    @Test
    void nullStylesAreRejected() {
        assertThrows(NullPointerException.class, () -> MenuBarStyle.of(null, TITLES));
        assertThrows(NullPointerException.class, () -> MenuBarStyle.of(TITLES, null));
    }

    @Test
    void equalStylesHaveEqualHashCodesAndDifferentStylesDiffer() {
        MenuBarStyle style = MenuBarStyle.of(TITLES, ITEMS);
        MenuBarStyle same = MenuBarStyle.of(TITLES, ITEMS);

        assertEquals(style, same);
        assertEquals(style.hashCode(), same.hashCode());
        assertNotEquals(style, MenuBarStyle.of(ITEMS, TITLES));
        assertNotEquals(style, "style");
    }

    @Test
    void toStringNamesBothStyles() {
        String text = MenuBarStyle.of(TITLES, ITEMS).toString();

        assertTrue(text.startsWith("MenuBarStyle[titles=ButtonStyle["));
        assertTrue(text.contains(", items=ButtonStyle["));
        assertTrue(text.endsWith(", triggerRgba=none]"));
    }

    @Test
    void newStyleHasNoTriggerColor() {
        MenuBarStyle style = MenuBarStyle.of(TITLES, ITEMS);

        assertFalse(style.hasTriggerRgba());
        assertThrows(IllegalStateException.class, style::getTriggerRgba);
    }

    @Test
    void withTriggerRgbaReturnsACopyWithTheColor() {
        MenuBarStyle plain = MenuBarStyle.of(TITLES, ITEMS);

        MenuBarStyle colored = plain.withTriggerRgba(0xFF0000FF);

        assertTrue(colored.hasTriggerRgba());
        assertEquals(0xFF0000FF, colored.getTriggerRgba());
        assertSame(TITLES, colored.getTitles());
        assertSame(ITEMS, colored.getItems());
        assertFalse(plain.hasTriggerRgba());
    }

    @Test
    void triggerColorTakesPartInEqualsHashCodeAndToString() {
        MenuBarStyle plain = MenuBarStyle.of(TITLES, ITEMS);
        MenuBarStyle red = plain.withTriggerRgba(0xFF0000FF);

        assertEquals(red, plain.withTriggerRgba(0xFF0000FF));
        assertEquals(red.hashCode(), plain.withTriggerRgba(0xFF0000FF).hashCode());
        assertNotEquals(red, plain);
        assertNotEquals(red, plain.withTriggerRgba(0x00FF00FF));
        assertTrue(red.toString().endsWith(", triggerRgba=ff0000ff]"));
    }

    @Test
    void newStyleHasNoTriggerUnderline() {
        assertFalse(MenuBarStyle.of(TITLES, ITEMS).isTriggerUnderline());
    }

    @Test
    void withTriggerUnderlineReturnsACopyAndLeavesTheInputUntouched() {
        MenuBarStyle plain = MenuBarStyle.of(TITLES, ITEMS).withTriggerRgba(0xFF0000FF);

        MenuBarStyle underlined = plain.withTriggerUnderline(true);

        assertTrue(underlined.isTriggerUnderline());
        assertFalse(plain.isTriggerUnderline());
        assertEquals(0xFF0000FF, underlined.getTriggerRgba());
        assertSame(TITLES, underlined.getTitles());
        assertFalse(underlined.withTriggerUnderline(false).isTriggerUnderline());
    }

    @Test
    void triggerUnderlineTakesPartInEqualsHashCodeAndToString() {
        MenuBarStyle plain = MenuBarStyle.of(TITLES, ITEMS);
        MenuBarStyle underlined = plain.withTriggerUnderline(true);

        assertEquals(underlined, plain.withTriggerUnderline(true));
        assertEquals(underlined.hashCode(), plain.withTriggerUnderline(true).hashCode());
        assertNotEquals(underlined, plain);
        assertTrue(underlined.toString().contains(", triggerUnderline=true, "));
    }
}
