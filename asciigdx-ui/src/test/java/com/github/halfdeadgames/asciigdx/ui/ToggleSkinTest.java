package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToggleSkinTest {

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070" },
            "labels": {
              "plain": { "foreground": "white", "background": "navy" }
            },
            "buttons": { "flat": { "normal": "plain" } }""";

    private static String withBase(String moreSections) {
        return "{" + BASE + "," + moreSections + "}";
    }

    private static void assertSkinError(String json, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void checkBoxAndRadioSectionsReadIntoToggleStyles() {
        Skin skin = Skin.fromJson(withBase("""
                "checkBoxes": { "option": {
                  "button": "flat", "unchecked": "[ ]", "checked": "[x]", "gap": 2 } },
                "radioButtons": { "choice": {
                  "button": "flat", "unchecked": "( )", "checked": "(*)" } }"""));

        ButtonStyle flat = skin.getButtonStyle("flat");
        assertEquals(ToggleStyle.of(flat, "[ ]", "[x]").withGap(2), skin.getCheckBoxStyle("option"));
        assertEquals(ToggleStyle.of(flat, "( )", "(*)"), skin.getRadioButtonStyle("choice"));
        assertTrue(skin.hasCheckBoxStyle("option"));
        assertFalse(skin.hasCheckBoxStyle("choice"));
        assertTrue(skin.hasRadioButtonStyle("choice"));
        assertFalse(skin.hasRadioButtonStyle("option"));
    }

    @Test
    void buttonMayBeInline() {
        Skin skin = Skin.fromJson(withBase("""
                "checkBoxes": { "option": {
                  "button": { "normal": "plain", "horizontalPadding": 0 },
                  "unchecked": "o", "checked": "x" } }"""));

        assertEquals(0, skin.getCheckBoxStyle("option").getButtonStyle().getHorizontalPadding());
    }

    @Test
    void unknownNamesListTheKnownOnes() {
        Skin skin = Skin.fromJson("{}");

        var thrown = assertThrows(IllegalArgumentException.class,
                () -> skin.getCheckBoxStyle("none"));
        assertTrue(thrown.getMessage().contains("Known check box styles: none"));
        assertThrows(IllegalArgumentException.class, () -> skin.getRadioButtonStyle("none"));
        assertThrows(NullPointerException.class, () -> skin.hasRadioButtonStyle(null));
    }

    @Test
    void missingKeysAreReported() {
        assertSkinError(withBase("\"checkBoxes\": {\"c\": {\"unchecked\": \"a\", "
                + "\"checked\": \"b\"}}"), "checkBoxes.c", "missing \"button\"");
        assertSkinError(withBase("\"radioButtons\": {\"c\": {\"button\": \"flat\", "
                + "\"checked\": \"b\"}}"), "radioButtons.c", "missing \"unchecked\"");
        assertSkinError(withBase("\"radioButtons\": {\"c\": {\"button\": \"flat\", "
                + "\"unchecked\": \"b\"}}"), "radioButtons.c", "missing \"checked\"");
    }

    @Test
    void unknownKeyIsReported() {
        assertSkinError(withBase("\"checkBoxes\": {\"c\": {\"button\": \"flat\", "
                + "\"unchecked\": \"a\", \"checked\": \"b\", \"color\": 1}}"),
                "checkBoxes.c", "unknown key \"color\"");
    }

    @Test
    void unequalMarksNameTheCheckedPath() {
        assertSkinError(withBase("\"checkBoxes\": {\"option\": {\"button\": \"flat\", "
                + "\"unchecked\": \"[ ]\", \"checked\": \"x\"}}"), "checkBoxes.option.checked");
    }

    @Test
    void emptyOrNonTextMarksAreReported() {
        assertSkinError(withBase("\"checkBoxes\": {\"o\": {\"button\": \"flat\", "
                + "\"unchecked\": \"\", \"checked\": \"\"}}"), "checkBoxes.o.unchecked");
        assertSkinError(withBase("\"radioButtons\": {\"o\": {\"button\": \"flat\", "
                + "\"unchecked\": \"a\", \"checked\": 1}}"), "radioButtons.o.checked");
    }

    @Test
    void badButtonReferenceIsReported() {
        assertSkinError(withBase("\"checkBoxes\": {\"o\": {\"button\": \"missing\", "
                + "\"unchecked\": \"a\", \"checked\": \"b\"}}"), "checkBoxes.o.button",
                "unknown button style \"missing\"", "Known button styles: flat");
        assertSkinError(withBase("\"checkBoxes\": {\"o\": {\"button\": 3, "
                + "\"unchecked\": \"a\", \"checked\": \"b\"}}"), "checkBoxes.o.button");
    }

    @Test
    void gapRulesMatchPaddingRules() {
        String head = "\"checkBoxes\": {\"o\": {\"button\": \"flat\", \"unchecked\": \"a\", "
                + "\"checked\": \"b\", \"gap\": ";
        assertSkinError(withBase(head + "-1}}"), "checkBoxes.o.gap", "must not be negative");
        assertSkinError(withBase(head + "1.5}}"), "checkBoxes.o.gap", "whole number");
        assertSkinError(withBase(head + "\"2\"}}"), "checkBoxes.o.gap", "whole number");
        assertEquals(0, Skin.fromJson(withBase(head + "0}}")).getCheckBoxStyle("o").getGap());
    }

    @Test
    void duplicateNamesAreReported() {
        assertSkinError(withBase("\"radioButtons\": {\"o\": {\"button\": \"flat\", "
                + "\"unchecked\": \"a\", \"checked\": \"b\"}, \"o\": {\"button\": \"flat\", "
                + "\"unchecked\": \"a\", \"checked\": \"b\"}}"), "radioButtons.o", "defined twice");
    }

    @Test
    void skinWithoutToggleSectionsStillLoads() {
        Skin skin = Skin.fromJson(withBase("\"menuBars\": {\"m\": {\"titles\": \"flat\", "
                + "\"items\": \"flat\"}}"));

        assertTrue(skin.hasMenuBarStyle("m"));
        assertFalse(skin.hasCheckBoxStyle("m"));
    }
}
