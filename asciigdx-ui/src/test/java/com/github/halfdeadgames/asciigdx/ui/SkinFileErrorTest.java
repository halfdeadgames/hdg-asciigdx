package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkinFileErrorTest {

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070" },
            "labels": {
              "tallPlain": { "font": "tall", "foreground": "white", "background": "navy" },
              "squarePlain": { "font": "square", "foreground": "white", "background": "navy" }
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
                {"labels": {"x": {"font": "tall", "foreground": "#12", "background": "#000000"}}}
                """, "labels.x.foreground");
    }

    @Test
    void unknownColorNameListsKnownColors() {
        assertSkinError("""
                {"colors": {"white": "#FFFFFF", "navy": "#203070"},
                 "labels": {"x": {"font": "tall", "foreground": "red", "background": "navy"}}}
                """, "labels.x.foreground", "unknown color \"red\"", "Known colors: white, navy");
    }

    @Test
    void colorMustBeText() {
        assertSkinError("""
                {"labels": {"x": {"font": "tall", "foreground": 1, "background": "#000000"}}}
                """, "labels.x.foreground");
    }

    @Test
    void unknownTopLevelKeyListsSections() {
        assertSkinError("{\"fonts\": {}}", "fonts",
                "Known sections: colors, labels, buttons, menuBars, checkBoxes, radioButtons");
    }

    @Test
    void unknownLabelKeyListsKnownKeys() {
        assertSkinError("""
                {"colors": {"c": "#000000"},
                 "labels": {"x": {"font": "tall", "foreground": "c", "background": "c",
                   "size": 3}}}
                """, "labels.x", "unknown key \"size\"",
                "Known keys: font, foreground, background");
    }

    @Test
    void unknownButtonKeyListsKnownKeys() {
        assertSkinError(
                withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", \"hover\": 1}}"),
                "buttons.b", "unknown key \"hover\"",
                "Known keys: normal, pointerOver, pressed, disabled, focused, horizontalPadding");
    }

    @Test
    void unknownMenuBarKeyListsKnownKeys() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "tallPlain"}},
                "menuBars": {"m": {"titles": "b", "items": "b", "color": "white"}}"""),
                "menuBars.m", "unknown key \"color\"",
                "Known keys: titles, items, triggerColor, triggerUnderline");
    }

    @Test
    void unknownKeyInInlineLookNamesItsPath() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": {"font": "tall", "foreground": "white",
                  "background": "navy", "bold": true}}}"""), "buttons.b.normal",
                "unknown key \"bold\"");
    }

    @Test
    void missingLabelKeysAreReported() {
        assertSkinError("{\"labels\": {\"x\": {\"foreground\": \"#000000\", "
                + "\"background\": \"#000000\"}}}", "labels.x", "missing \"font\"");
        assertSkinError("{\"labels\": {\"x\": {\"font\": \"tall\", "
                + "\"background\": \"#000000\"}}}", "labels.x", "missing \"foreground\"");
        assertSkinError("{\"labels\": {\"x\": {\"font\": \"tall\", "
                + "\"foreground\": \"#000000\"}}}", "labels.x", "missing \"background\"");
    }

    @Test
    void missingButtonNormalIsReported() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"pressed\": \"tallPlain\"}}"),
                "buttons.b", "missing \"normal\"");
    }

    @Test
    void missingMenuBarKeysAreReported() {
        String buttons = "\"buttons\": {\"b\": {\"normal\": \"tallPlain\"}},";
        assertSkinError(withBase(buttons + "\"menuBars\": {\"m\": {\"items\": \"b\"}}"),
                "menuBars.m", "missing \"titles\"");
        assertSkinError(withBase(buttons + "\"menuBars\": {\"m\": {\"titles\": \"b\"}}"),
                "menuBars.m", "missing \"items\"");
    }

    @Test
    void unknownFontListsKnownFonts() {
        assertSkinError("""
                {"labels": {"x": {"font": "wide", "foreground": "#000000",
                  "background": "#000000"}}}""", "labels.x.font", "unknown font \"wide\"",
                "Known fonts: tall, square");
    }

    @Test
    void fontMustBeText() {
        assertSkinError("""
                {"labels": {"x": {"font": 1, "foreground": "#000000",
                  "background": "#000000"}}}""", "labels.x.font");
    }

    @Test
    void sectionMustBeAnObject() {
        assertSkinError("{\"colors\": [\"#000000\"]}", "colors", "must be an object");
        assertSkinError("{\"labels\": 3}", "labels", "must be an object");
    }

    @Test
    void entryMustBeAnObject() {
        assertSkinError("{\"labels\": {\"x\": \"tall\"}}", "labels.x", "must be");
        assertSkinError(withBase("\"buttons\": {\"b\": 1}"), "buttons.b", "must be");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\"}},"
                + "\"menuBars\": {\"m\": []}"), "menuBars.m", "must be");
    }

    @Test
    void lookMustBeNameOrObject() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": 4}}"), "buttons.b.normal",
                "label style");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
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
                  "x": {"font": "tall", "foreground": "#000000", "background": "#000000"},
                  "x": {"font": "tall", "foreground": "#000000", "background": "#000000"}}}
                """, "labels.x", "defined twice");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\"}, "
                + "\"b\": {\"normal\": \"tallPlain\"}}"), "buttons.b", "defined twice");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\"}},"
                + "\"menuBars\": {\"m\": {\"titles\": \"b\", \"items\": \"b\"}, "
                + "\"m\": {\"titles\": \"b\", \"items\": \"b\"}}"), "menuBars.m", "defined twice");
    }

    @Test
    void duplicateKeyInObjectIsRejected() {
        assertSkinError("{\"colors\": {}, \"colors\": {}}", "colors", "twice");
        assertSkinError("""
                {"labels": {"x": {"font": "tall", "font": "square", "foreground": "#000000",
                  "background": "#000000"}}}""", "labels.x", "\"font\" appears twice");
    }

    @Test
    void unknownLabelReferenceListsKnownLabels() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"missing\"}}"),
                "buttons.b.normal", "unknown label style \"missing\"",
                "Known label styles: tallPlain, squarePlain");
    }

    @Test
    void unknownLabelReferenceInOptionalLookNamesThatLook() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
                + "\"disabled\": \"missing\"}}"), "buttons.b.disabled", "unknown label style");
    }

    @Test
    void unknownButtonReferenceListsKnownButtons() {
        assertSkinError(withBase("""
                "buttons": {"first": {"normal": "tallPlain"}, "second": {"normal": "tallPlain"}},
                "menuBars": {"m": {"titles": "first", "items": "missing"}}"""),
                "menuBars.m.items", "unknown button style \"missing\"",
                "Known button styles: first, second");
    }

    @Test
    void referencesToLaterSectionsFail() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\"}},"
                + "\"menuBars\": {\"m\": {\"titles\": \"b\", \"items\": \"b\"},"
                + "\"n\": {\"titles\": \"m\", \"items\": \"b\"}}"),
                "menuBars.n.titles", "unknown button style \"m\"");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\"},"
                + "\"c\": {\"normal\": \"b\"}}"), "buttons.c.normal",
                "unknown label style \"b\"");
        assertSkinError("""
                {"labels": {"x": {"font": "tall", "foreground": "x", "background": "#000000"}}}
                """, "labels.x.foreground", "unknown color \"x\"");
    }

    @Test
    void menuBarCannotNameALabelStyle() {
        assertSkinError("""
                {"colors": {"a": "#000000"},
                 "labels": {"x": {"font": "tall", "foreground": "a", "background": "a"}},
                 "menuBars": {"m": {"titles": "x", "items": "x"}}}
                """, "menuBars.m.titles", "unknown button style \"x\"");
    }

    @Test
    void negativePaddingIsRejected() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
                + "\"horizontalPadding\": -1}}"), "buttons.b.horizontalPadding",
                "must not be negative", "-1");
    }

    @Test
    void paddingMustBeAWholeNumber() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
                + "\"horizontalPadding\": 1.5}}"), "buttons.b.horizontalPadding", "whole number");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
                + "\"horizontalPadding\": \"2\"}}"), "buttons.b.horizontalPadding", "whole number");
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
                + "\"horizontalPadding\": 99999999999}}"), "buttons.b.horizontalPadding");
    }

    @Test
    void buttonLooksWithDifferentFontsAreRejected() {
        assertSkinError(withBase("\"buttons\": {\"b\": {\"normal\": \"tallPlain\", "
                + "\"pressed\": \"squarePlain\"}}"), "buttons.b", "same font");
    }

    @Test
    void menuBarStylesWithDifferentFontsAreRejected() {
        assertSkinError(withBase("""
                "buttons": {"tallButton": {"normal": "tallPlain"},
                  "squareButton": {"normal": "squarePlain"}},
                "menuBars": {"m": {"titles": "tallButton", "items": "squareButton"}}"""),
                "menuBars.m", "same font");
    }

    @Test
    void triggerColorMustBeAKnownColor() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "tallPlain"}},
                "menuBars": {"m": {"titles": "b", "items": "b", "triggerColor": "pink"}}"""),
                "menuBars.m.triggerColor", "unknown color \"pink\"", "Known colors: white, navy");
    }

    @Test
    void triggerUnderlineMustBeABoolean() {
        assertSkinError(withBase("""
                "buttons": {"b": {"normal": "tallPlain"}},
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
