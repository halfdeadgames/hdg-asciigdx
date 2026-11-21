package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkinFileErrorTest {

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070" },
            "labels": {
              "plain": { "foreground": "white", "background": "navy" },
              "other": { "foreground": "navy", "background": "white" }
            }""";

    private static void assertSkinError(String json, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        String message = thrown.getMessage();
        for (String fragment : expectedFragments) {
            assertTrue(message.contains(fragment),
                    "Expected \"" + fragment + "\" in: " + message);
        }
    }

    private static String withBase(String moreSections) {
        return "{" + BASE + "," + moreSections + "}";
    }

    @Test
    void shortHexIsRejected() {
        assertSkinError("{\"colors\": {\"bad\": \"#12345\"}}", "colors.bad", "#12345");
    }

    @Test
    void longHexIsRejected() {
        assertSkinError("{\"colors\": {\"bad\": \"#123456789\"}}", "colors.bad");
    }

    @Test
    void nonHexDigitsAreRejected() {
        assertSkinError("{\"colors\": {\"bad\": \"#12345G\"}}", "colors.bad", "#12345G");
    }

    @Test
    void hexWithSignOrSpaceIsRejected() {
        assertSkinError("{\"colors\": {\"bad\": \"#+12345\"}}", "colors.bad");
        assertSkinError("{\"colors\": {\"bad\": \"# 12345\"}}", "colors.bad");
    }

    @Test
    void colorEntryCannotNameAnotherColor() {
        assertSkinError("{\"colors\": {\"a\": \"#FFFFFF\", \"b\": \"a\"}}", "colors.b",
                "another color");
    }

    @Test
    void colorEntryMustBeText() {
        assertSkinError("{\"colors\": {\"a\": 5}}", "colors.a");
    }

    @Test
    void badHexInsideLabelNamesThePath() {
        assertSkinError("""
                {"labels": {"x": {"foreground": "#12", "background": "#000000"}}}
                """, "labels.x.foreground");
    }

    @Test
    void unknownColorNameListsKnownColors() {
        assertSkinError("""
                {"colors": {"white": "#FFFFFF", "navy": "#203070"},
                 "labels": {"x": {"foreground": "red", "background": "navy"}}}
                """, "labels.x.foreground", "unknown color \"red\"", "Known colors: white, navy");
    }

    @Test
    void colorMustBeText() {
        assertSkinError("""
                {"labels": {"x": {"foreground": 1, "background": "#000000"}}}
                """, "labels.x.foreground");
    }

    @Test
    void unknownTopLevelKeyListsSections() {
        assertSkinError("{\"fonts\": {}}", "fonts",
                "Known sections: colors, labels, buttons, menuBars, checkBoxes, radioButtons, "
                        + "textFields, progressBars, sliders, scrollPanes");
    }

    @Test
    void unknownLabelKeyListsKnownKeys() {
        assertSkinError("""
                {"colors": {"c": "#000000"},
                 "labels": {"x": {"foreground": "c", "background": "c",
                   "size": 3}}}
                """, "labels.x", "unknown key \"size\"",
                "Known keys: foreground, background");
    }

    @Test
    void unknownButtonKeyListsKnownKeys() {
        assertSkinError(
                withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", \"hover\": 1}}"),
                "buttons.b", "unknown key \"hover\"",
                "Known keys: normal, pointerOver, pressed, disabled, focused, horizontalPadding");
    }

    @Test
    void unknownMenuBarKeyListsKnownKeys() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "plain"}},
                "menuBars": {"m": {"titles": "b", "items": "b", "color": "white"}}"""),
                "menuBars.m", "unknown key \"color\"",
                "Known keys: titles, items, triggerColor, triggerUnderline");
    }

    @Test
    void unknownKeyInInlineLookNamesItsPath() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": {"foreground": "white",
                  "background": "navy", "bold": true}}}"""), "buttons.b.normal",
                "unknown key \"bold\"");
    }

    @Test
    void missingLabelKeysAreReported() {
        assertSkinError("{\"labels\": {\"x\": {\"background\": \"#000000\"}}}",
                "labels.x", "missing \"foreground\"");
        assertSkinError("{\"labels\": {\"x\": {\"foreground\": \"#000000\"}}}",
                "labels.x", "missing \"background\"");
    }

    @Test
    void missingButtonNormalIsReported() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"pressed\": \"plain\"}}"),
                "buttons.b", "missing \"normal\"");
    }

    @Test
    void missingMenuBarKeysAreReported() {
        String buttons = "\"buttons\": {\"b\": {\"normal\": \"plain\"}},";
        assertSkinError(withBase(buttons + "\"menuBars\": {\"m\": {\"items\": \"b\"}}"),
                "menuBars.m", "missing \"titles\"");
        assertSkinError(withBase(buttons + "\"menuBars\": {\"m\": {\"titles\": \"b\"}}"),
                "menuBars.m", "missing \"items\"");
    }

    @Test
    void labelLookWithFontKeyIsRejectedWithRemovalMessage() {
        assertSkinError("""
                {"labels": {"x": {"font": "square", "foreground": "#000000",
                  "background": "#000000"}}}""", "labels.x.font", "\"font\" key was removed",
                "delete the key");
    }

    @Test
    void buttonStateLookWithFontKeyIsRejectedWithRemovalMessage() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "plain",
                  "pressed": {"font": "tall", "foreground": "white", "background": "navy"}}}"""),
                "buttons.b.pressed.font", "\"font\" key was removed");
    }

    @Test
    void nestedProgressBarLookWithFontKeyIsRejectedWithRemovalMessage() {
        assertSkinError(withBase("""
                "progressBars": {"p": {
                  "fill": {"font": "square", "foreground": "white", "background": "navy"},
                  "empty": "plain"}}"""),
                "progressBars.p.fill.font", "\"font\" key was removed");
    }

    @Test
    void caretWithBackgroundIsRejectedWithRemovalMessage() {
        assertSkinError(withBase("""
                "textFields": {"t": {"normal": "plain",
                  "caret": {"glyph": "|", "background": "white"}}}"""),
                "textFields.t.caret.background", "no background", "delete the \"background\" key");
    }

    @Test
    void caretWithoutGlyphIsReported() {
        assertSkinError(withBase("""
                "textFields": {"t": {"normal": "plain", "caret": {"foreground": "white"}}}"""),
                "textFields.t.caret", "missing \"glyph\"");
    }

    @Test
    void sectionMustBeAnObject() {
        assertSkinError("{\"colors\": [\"#000000\"]}", "colors", "must be an object");
        assertSkinError("{\"labels\": 3}", "labels", "must be an object");
    }

    @Test
    void entryMustBeAnObject() {
        assertSkinError("{\"labels\": {\"x\": \"plain\"}}", "labels.x", "must be");
        assertSkinError(withBase("\"buttons\": {\"b\": 1}"), "buttons.b", "must be");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\"}},"
                + "\"menuBars\": {\"m\": []}"), "menuBars.m", "must be");
    }

    @Test
    void lookMustBeNameOrObject() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": 4}}"), "buttons.b.normal",
                "label style");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", "
                + "\"pressed\": [1]}}"), "buttons.b.pressed", "label style");
    }

    @Test
    void menuBarStylesMustBeNameOrObject() {
        assertSkinError(withBase("\"menuBars\": {\"m\": {\"titles\": 1, \"items\": 1}}"),
                "menuBars.m.titles", "button style");
    }

    @Test
    void duplicateNameInSectionIsRejected() {
        assertSkinError("{\"colors\": {\"a\": \"#000000\", \"a\": \"#FFFFFF\"}}", "colors.a",
                "defined twice");
        assertSkinError("""
                {"labels": {
                  "x": {"foreground": "#000000", "background": "#000000"},
                  "x": {"foreground": "#000000", "background": "#000000"}}}
                """, "labels.x", "defined twice");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\"}, "
                + "\"b\": {\"normal\": \"plain\"}}"), "buttons.b", "defined twice");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\"}},"
                + "\"menuBars\": {\"m\": {\"titles\": \"b\", \"items\": \"b\"}, "
                + "\"m\": {\"titles\": \"b\", \"items\": \"b\"}}"), "menuBars.m", "defined twice");
    }

    @Test
    void duplicateKeyInObjectIsRejected() {
        assertSkinError("{\"colors\": {}, \"colors\": {}}", "colors", "twice");
        assertSkinError("""
                {"labels": {"x": {"foreground": "#000000", "foreground": "#FFFFFF",
                  "background": "#000000"}}}""", "labels.x", "\"foreground\" appears twice");
    }

    @Test
    void unknownLabelReferenceListsKnownLabels() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"missing\"}}"),
                "buttons.b.normal", "unknown label style \"missing\"",
                "Known label styles: plain, other");
    }

    @Test
    void unknownLabelReferenceInOptionalLookNamesThatLook() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", "
                + "\"disabled\": \"missing\"}}"), "buttons.b.disabled", "unknown label style");
    }

    @Test
    void unknownButtonReferenceListsKnownButtons() {
        assertSkinError(withBase("""
                "buttons": {"first": {"normal": "plain"}, "second": {"normal": "plain"}},
                "menuBars": {"m": {"titles": "first", "items": "missing"}}"""),
                "menuBars.m.items", "unknown button style \"missing\"",
                "Known button styles: first, second");
    }

    @Test
    void referencesToLaterSectionsFail() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\"}},"
                + "\"menuBars\": {\"m\": {\"titles\": \"b\", \"items\": \"b\"},"
                + "\"n\": {\"titles\": \"m\", \"items\": \"b\"}}"),
                "menuBars.n.titles", "unknown button style \"m\"");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\"},"
                + "\"c\": {\"normal\": \"b\"}}"), "buttons.c.normal",
                "unknown label style \"b\"");
        assertSkinError("""
                {"labels": {"x": {"foreground": "x", "background": "#000000"}}}
                """, "labels.x.foreground", "unknown color \"x\"");
    }

    @Test
    void menuBarCannotNameALabelStyle() {
        assertSkinError("""
                {"colors": {"a": "#000000"},
                 "labels": {"x": {"foreground": "a", "background": "a"}},
                 "menuBars": {"m": {"titles": "x", "items": "x"}}}
                """, "menuBars.m.titles", "unknown button style \"x\"");
    }

    @Test
    void negativePaddingIsRejected() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", "
                + "\"horizontalPadding\": -1}}"), "buttons.b.horizontalPadding",
                "must not be negative", "-1");
    }

    @Test
    void paddingMustBeAWholeNumber() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", "
                + "\"horizontalPadding\": 1.5}}"), "buttons.b.horizontalPadding", "whole number");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", "
                + "\"horizontalPadding\": \"2\"}}"), "buttons.b.horizontalPadding", "whole number");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"plain\", "
                + "\"horizontalPadding\": 99999999999}}"), "buttons.b.horizontalPadding");
    }

    @Test
    void triggerColorMustBeAKnownColor() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "plain"}},
                "menuBars": {"m": {"titles": "b", "items": "b", "triggerColor": "pink"}}"""),
                "menuBars.m.triggerColor", "unknown color \"pink\"", "Known colors: white, navy");
    }

    @Test
    void triggerUnderlineMustBeABoolean() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "plain"}},
                "menuBars": {"m": {"titles": "b", "items": "b", "triggerUnderline": "yes"}}"""),
                "menuBars.m.triggerUnderline", "true or false");
    }

    @Test
    void syntaxErrorIsReadable() {
        assertSkinError("{\"colors\": {\"white\": \"#FFFFFF\"", "not valid JSON");
    }

    @Test
    void rootMustBeAnObject() {
        assertSkinError("[]", "one object");
        assertSkinError("\"text\"", "one object");
    }
}
