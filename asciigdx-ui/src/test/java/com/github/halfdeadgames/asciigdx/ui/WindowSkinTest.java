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
              "plain": { "foreground": "white", "background": "navy" },
              "dim": { "foreground": "gray", "background": "navy" }
            },
            "buttons": {
              "close": { "normal": "plain", "pointerOver": "dim" }
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
                "focusedBorder": { "foreground": "navy", "background": "white" },
                "closeButton": "close", "closeText": "[x]\""""));

        WindowStyle style = skin.getWindowStyle("w");
        assertEquals(LabelStyle.of(0x203070FF, 0xFFFFFFFF), style.getFocusedBorder());
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
    void looksWithAFontKeyAreErrorsThatNameThePath() {
        assertSkinError(window("""
                "box": "single", "border": "plain", "background": "navy",
                "title": {"font": "square", "foreground": "white", "background": "navy"}"""),
                "windows.w.title.font", "\"font\" key was removed");
        assertSkinError(window("""
                "box": "single", "title": "plain", "background": "navy",
                "border": {"font": "square", "foreground": "white", "background": "navy"}"""),
                "windows.w.border.font", "\"font\" key was removed");
        assertSkinError(window("""
                "box": "single", "border": "plain", "title": "plain", "background": "navy",
                "focusedBorder": {"font": "tall", "foreground": "white", "background": "navy"}"""),
                "windows.w.focusedBorder.font", "\"font\" key was removed");
    }

    @Test
    void closeButtonMustBeAKnownButtonName() {
        String required = "\"box\": \"single\", \"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\", ";
        assertSkinError(window(required + "\"closeButton\": \"nope\""),
                "windows.w.closeButton", "unknown button style \"nope\"");
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
    void closeFontIsReadAndDefaultsToSquare() {
        String required = "\"box\": \"single\", \"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\", \"closeButton\": \"close\"";

        assertEquals(MarkFont.TEXT, Skin.fromJson(window(required + ", \"closeFont\": \"text\""))
                .getWindowStyle("w").getCloseFont());
        assertEquals(MarkFont.SQUARE,
                Skin.fromJson(window(required + ", \"closeFont\": \"square\""))
                        .getWindowStyle("w").getCloseFont());
        assertEquals(MarkFont.SQUARE, Skin.fromJson(window(required)).getWindowStyle("w")
                .getCloseFont());
    }

    @Test
    void unknownCloseFontNamesTheKeyAndListsTheAllowedValues() {
        String required = "\"box\": \"single\", \"border\": \"plain\", \"title\": \"plain\", "
                + "\"background\": \"navy\", ";
        assertSkinError(window(required + "\"closeFont\": \"tall\""),
                "windows.w.closeFont", "text, square");
        assertSkinError(window(required + "\"closeFont\": \"TEXT\""),
                "windows.w.closeFont", "text, square");
        assertSkinError(window(required + "\"closeFont\": true"),
                "windows.w.closeFont", "text, square");
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
