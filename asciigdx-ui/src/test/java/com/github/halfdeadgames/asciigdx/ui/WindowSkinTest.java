package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import org.junit.jupiter.api.Test;

class WindowSkinTest {

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070", "gray": "#909090" },
            "labels": {
              "plain": { "font": "square", "foreground": "white", "background": "navy" },
              "dim": { "font": "square", "foreground": "gray", "background": "navy" },
              "tallPlain": { "font": "tall", "foreground": "white", "background": "navy" }
            },
            "buttons": {
              "close": { "normal": "plain", "pointerOver": "dim" },
              "tallClose": { "normal": "tallPlain" }
            }""";
    private static final String WINDOW_START = "\"windows\": {\"w\": {";

    private static String window(String body) {
        return "{" + BASE + "," + WINDOW_START + body + "}}}";
    }

    private static void assertSkinError(String json, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void minimalEntryUsesDefaults() {
        Skin skin = Skin.fromJson(window("""
                "box": "double", "border": "plain", "title": "dim", "background": "navy\""""));

        WindowStyle style = skin.getWindowStyle("w");
        assertEquals(BoxStyle.DOUBLE, style.getBox());
        assertEquals(skin.getLabelStyle("plain"), style.getBorder());
        assertEquals(skin.getLabelStyle("plain"), style.getFocusedBorder());
        assertEquals(skin.getLabelStyle("dim"), style.getTitle());
        assertEquals(0x203070FF, style.getBackgroundRgba());
        assertFalse(style.hasCloseButton());
        assertEquals("x", style.getCloseText());
        assertTrue(skin.hasWindowStyle("w"));
        assertFalse(skin.hasWindowStyle("other"));
    }

    @Test
    void everyBoxNameIsKnown() {
        String[] names = {"single", "double", "heavy", "rounded"};
        BoxStyle[] boxes = {BoxStyle.SINGLE, BoxStyle.DOUBLE, BoxStyle.HEAVY, BoxStyle.ROUNDED};
        for (int i = 0; i < names.length; i++) {
            Skin skin = Skin.fromJson(window("\"box\": \"" + names[i] + "\", \"border\": \"plain\","
                    + " \"title\": \"plain\", \"background\": \"navy\""));

            assertEquals(boxes[i], skin.getWindowStyle("w").getBox());
        }
    }

    @Test
    void optionalPartsAreRead() {
        Skin skin = Skin.fromJson(window("""
                "box": "heavy", "border": "plain", "title": "plain", "background": "#00000080",
                "focusedBorder": { "font": "square", "foreground": "navy", "background": "white" },
                "closeButton": "close", "closeText": "[x]\""""));

        WindowStyle style = skin.getWindowStyle("w");
        assertEquals(LabelStyle.of(UiFont.SQUARE, 0x203070FF, 0xFFFFFFFF),
                style.getFocusedBorder());
        assertTrue(style.hasCloseButton());
        assertEquals(skin.getButtonStyle("close"), style.getCloseButton());
        assertEquals("[x]", style.getCloseText());
        assertEquals(0x00000080, style.getBackgroundRgba());
    }

    @Test
    void invalidBoxNameIsAnError() {
        assertSkinError(window("""
                "box": "dotted", "border": "plain", "title": "plain", "background": "navy\""""),
                "windows.w.box", "single, double, heavy, rounded");
        assertSkinError(window("""
                "box": 3, "border": "plain", "title": "plain", "background": "navy\""""),
                "windows.w.box");
    }

    @Test
    void missingRequiredKeysAreErrors() {
        assertSkinError(window("\"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\""), "windows.w", "missing \"box\"");
        assertSkinError(window("\"box\": \"single\", \"title\": \"plain\", "
                + "\"background\": \"navy\""), "windows.w", "missing \"border\"");
        assertSkinError(window("\"box\": \"single\", \"border\": \"plain\", "
                + "\"background\": \"navy\""), "windows.w", "missing \"title\"");
        assertSkinError(window("\"box\": \"single\", \"border\": \"plain\", "
                + "\"title\": \"plain\""), "windows.w", "missing \"background\"");
    }

    @Test
    void unknownKeysAreErrors() {
        assertSkinError(window("""
                "box": "single", "border": "plain", "title": "plain", "background": "navy",
                "shadow": true"""), "windows.w", "unknown key \"shadow\"");
    }

    @Test
    void tallLooksAreErrorsThatNameThePath() {
        assertSkinError(window("""
                "box": "single", "border": "plain", "title": "tallPlain", "background": "navy\""""),
                "windows.w.title", "square");
        assertSkinError(window("""
                "box": "single", "border": "tallPlain", "title": "plain", "background": "navy\""""),
                "windows.w.border", "square");
        assertSkinError(window("""
                "box": "single", "border": "plain", "title": "plain", "background": "navy",
                "focusedBorder": "tallPlain\""""), "windows.w.focusedBorder", "square");
    }

    @Test
    void closeButtonMustBeAKnownSquareButtonName() {
        String required = "\"box\": \"single\", \"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\", ";
        assertSkinError(window(required + "\"closeButton\": \"nope\""),
                "windows.w.closeButton", "unknown button style \"nope\"");
        assertSkinError(window(required + "\"closeButton\": \"tallClose\""),
                "windows.w.closeButton", "square");
        assertSkinError(window(required + "\"closeButton\": { \"normal\": \"plain\" }"),
                "windows.w.closeButton", "name of a button style");
    }

    @Test
    void closeTextMustBeNonEmptyText() {
        String required = "\"box\": \"single\", \"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\", ";
        assertSkinError(window(required + "\"closeText\": \"\""), "windows.w.closeText");
        assertSkinError(window(required + "\"closeText\": 4"), "windows.w.closeText");
    }

    @Test
    void duplicateEntryNamesAreErrors() {
        String entry = "\"box\": \"single\", \"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\"";
        String json = "{" + BASE + ", \"windows\": {\"w\": {" + entry + "}, \"w\": {" + entry
                + "}}}";

        assertSkinError(json, "windows.w", "defined twice");
    }
}
