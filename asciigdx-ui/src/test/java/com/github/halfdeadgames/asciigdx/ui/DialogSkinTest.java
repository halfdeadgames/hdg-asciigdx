package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DialogSkinTest {

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070", "black": "#000000" },
            "labels": {
              "plain": { "font": "square", "foreground": "white", "background": "navy" }
            },
            "buttons": { "action": { "normal": "plain" } },
            "textFields": { "field": { "normal": "plain" } },
            "windows": {
              "frame": { "box": "single", "border": "plain", "title": "plain",
                         "background": "navy" }
            }""";
    private static final String REFS = "\"window\": \"frame\", \"message\": \"plain\","
            + " \"buttons\": \"action\", \"textField\": \"field\"";

    private static String dialog(String body) {
        return "{" + BASE + ", \"dialogs\": {\"d\": {" + body + "}}}";
    }

    private static void assertSkinError(String json, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void entryWithoutDimReferencesTheFourStylesAndDoesNotDim() {
        Skin skin = Skin.fromJson(dialog(REFS));

        DialogStyle style = skin.getDialogStyle("d");
        assertEquals(skin.getWindowStyle("frame"), style.getWindow());
        assertEquals(skin.getLabelStyle("plain"), style.getMessage());
        assertEquals(skin.getButtonStyle("action"), style.getButtons());
        assertEquals(skin.getTextFieldStyle("field"), style.getTextField());
        assertFalse(style.hasDim());
        assertTrue(skin.hasDialogStyle("d"));
        assertFalse(skin.hasDialogStyle("other"));
    }

    @Test
    void dimColorByPaletteNameAndDimAmountAreRead() {
        Skin skin = Skin.fromJson(dialog(REFS + ", \"dimColor\": \"navy\", \"dimAmount\": 0.25"));

        DialogStyle style = skin.getDialogStyle("d");
        assertEquals(0x203070FF, style.getDimRgba());
        assertEquals(0.25f, style.getDimAmount());
    }

    @Test
    void dimColorAlphaIsIgnored() {
        Skin skin = Skin.fromJson(dialog(REFS
                + ", \"dimColor\": \"#10203040\", \"dimAmount\": 1"));

        assertEquals(0x102030FF, skin.getDialogStyle("d").getDimRgba());
    }

    @Test
    void dimAmountZeroMeansNoDim() {
        Skin skin = Skin.fromJson(dialog(REFS + ", \"dimColor\": \"black\", \"dimAmount\": 0"));

        assertFalse(skin.getDialogStyle("d").hasDim());
    }

    @Test
    void aDimKeyAloneIsAnError() {
        assertSkinError(dialog(REFS + ", \"dimColor\": \"black\""), "dialogs.d",
                "\"dimColor\" and \"dimAmount\" must be given together");
        assertSkinError(dialog(REFS + ", \"dimAmount\": 0.5"), "dialogs.d",
                "\"dimColor\" and \"dimAmount\" must be given together");
    }

    @Test
    void dimAmountMustBeANumberFromZeroToOne() {
        assertSkinError(dialog(REFS + ", \"dimColor\": \"black\", \"dimAmount\": 1.5"),
                "dialogs.d.dimAmount");
        assertSkinError(dialog(REFS + ", \"dimColor\": \"black\", \"dimAmount\": -0.1"),
                "dialogs.d.dimAmount");
        assertSkinError(dialog(REFS + ", \"dimColor\": \"black\", \"dimAmount\": \"half\""),
                "dialogs.d.dimAmount", "number");
    }

    @Test
    void unknownColorNameIsAnError() {
        assertSkinError(dialog(REFS + ", \"dimColor\": \"nope\", \"dimAmount\": 0.5"),
                "dialogs.d.dimColor", "nope");
    }

    @Test
    void everyReferenceIsRequired() {
        assertSkinError(dialog("\"message\": \"plain\", \"buttons\": \"action\","
                + " \"textField\": \"field\""), "dialogs.d", "missing \"window\"");
        assertSkinError(dialog("\"window\": \"frame\", \"buttons\": \"action\","
                + " \"textField\": \"field\""), "missing \"message\"");
        assertSkinError(dialog("\"window\": \"frame\", \"message\": \"plain\","
                + " \"textField\": \"field\""), "missing \"buttons\"");
        assertSkinError(dialog("\"window\": \"frame\", \"message\": \"plain\","
                + " \"buttons\": \"action\""), "missing \"textField\"");
    }

    @Test
    void unknownReferencesNameTheKnownOnes() {
        assertSkinError(dialog(REFS.replace("\"frame\"", "\"nope\"")), "dialogs.d.window",
                "nope", "frame");
        assertSkinError(dialog(REFS.replace("\"plain\"", "\"nope\"")), "dialogs.d.message",
                "nope");
    }

    @Test
    void referencesMustBeNames() {
        assertSkinError(dialog(REFS.replace("\"action\"", "{}")), "dialogs.d.buttons",
                "name of a button style");
    }

    @Test
    void unknownKeysAndNonObjectsAreErrors() {
        assertSkinError(dialog(REFS + ", \"extra\": 1"), "dialogs.d", "extra");
        assertSkinError("{" + BASE + ", \"dialogs\": {\"d\": \"text\"}}", "dialogs.d");
    }

    @Test
    void dialogsIsAReservedSectionName() {
        assertTrue(SkinFileReader.isBuiltInSection("dialogs"));
    }
}
