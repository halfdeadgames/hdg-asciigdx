package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class SkinTest {

    private static final String COLORS_AND_LABELS = """
            {
              "colors": { "white": "#FFFFFF", "navy": "#203070", "teal": "#00A0A0" },
              "labels": {
                "plain": { "foreground": "white", "background": "navy" },
                "tealLook": { "foreground": "#000000", "background": "teal" }
              }
            """;

    private static Skin skinWithButtons(String buttonsAndAfter) {
        return Skin.fromJson(COLORS_AND_LABELS + "," + buttonsAndAfter + "}");
    }

    @Test
    void emptyObjectGivesEmptySkin() {
        Skin skin = Skin.fromJson("{}");

        var color = assertThrows(IllegalArgumentException.class, () -> skin.getRgba("any"));
        assertTrue(color.getMessage().contains("Known colors: none"), color.getMessage());
        assertThrows(IllegalArgumentException.class, () -> skin.getLabelStyle("any"));
        assertThrows(IllegalArgumentException.class, () -> skin.getButtonStyle("any"));
        assertThrows(IllegalArgumentException.class, () -> skin.getMenuBarStyle("any"));
    }

    @Test
    void sixDigitHexIsOpaque() {
        Skin skin = Skin.fromJson("{\"colors\": {\"navy\": \"#203070\"}}");

        assertEquals(0x203070FF, skin.getRgba("navy"));
    }

    @Test
    void eightDigitHexKeepsAlpha() {
        Skin skin = Skin.fromJson(
                "{\"colors\": {\"clear\": \"#00000000\", \"half\": \"#FF000080\"}}");

        assertEquals(0x00000000, skin.getRgba("clear"));
        assertEquals(0xFF000080, skin.getRgba("half"));
    }

    @Test
    void hexDigitsAreCaseInsensitive() {
        Skin skin = Skin.fromJson(
                "{\"colors\": {\"lower\": \"#aabbcc\", \"mixed\": \"#AaBbCcDd\"}}");

        assertEquals(0xAABBCCFF, skin.getRgba("lower"));
        assertEquals(0xAABBCCDD, skin.getRgba("mixed"));
    }

    @Test
    void colorNamesKeepFileOrderInMessage() {
        Skin skin = Skin.fromJson(
                "{\"colors\": {\"zeta\": \"#000000\", \"alpha\": \"#FFFFFF\"}}");

        var missing = assertThrows(IllegalArgumentException.class, () -> skin.getRgba("beta"));
        assertTrue(missing.getMessage().contains("Known colors: zeta, alpha"),
                missing.getMessage());
    }

    @Test
    void labelUsesHexAndColorNames() {
        Skin skin = Skin.fromJson(COLORS_AND_LABELS + "}");

        assertEquals(LabelStyle.of(0xFFFFFFFF, 0x203070FF), skin.getLabelStyle("plain"));
        assertEquals(LabelStyle.of(0x000000FF, 0x00A0A0FF), skin.getLabelStyle("tealLook"));
    }

    @Test
    void buttonWithOnlyNormalCopiesItToOtherStates() {
        Skin skin = skinWithButtons("\"buttons\": { \"flat\": { \"normal\": \"plain\" } }");

        LabelStyle plain = skin.getLabelStyle("plain");
        assertEquals(ButtonStyle.of(plain), skin.getButtonStyle("flat"));
        assertEquals(ButtonStyle.DEFAULT_HORIZONTAL_PADDING,
                skin.getButtonStyle("flat").getHorizontalPadding());
    }

    @Test
    void buttonMixesNamedAndInlineLooks() {
        Skin skin = skinWithButtons("""
                "buttons": {
                  "mixed": {
                    "normal": "tealLook",
                    "pointerOver": { "foreground": "white", "background": "navy" },
                    "pressed": "tealLook",
                    "disabled": { "foreground": "#808080", "background": "#101010" }
                  }
                }""");

        LabelStyle teal = skin.getLabelStyle("tealLook");
        ButtonStyle expected = ButtonStyle.of(teal,
                LabelStyle.of(0xFFFFFFFF, 0x203070FF), teal,
                LabelStyle.of(0x808080FF, 0x101010FF));
        assertEquals(expected, skin.getButtonStyle("mixed"));
    }

    @Test
    void buttonInlineNormalLookWorks() {
        Skin skin = skinWithButtons("""
                "buttons": { "inline": { "normal":
                  { "foreground": "white", "background": "teal" } } }""");

        assertEquals(LabelStyle.of(0xFFFFFFFF, 0x00A0A0FF),
                skin.getButtonStyle("inline").getNormal());
    }

    @Test
    void explicitHorizontalPaddingIsUsed() {
        Skin skin = skinWithButtons("""
                "buttons": {
                  "wide": { "normal": "plain", "horizontalPadding": 3 },
                  "tight": { "normal": "plain", "horizontalPadding": 0 }
                }""");

        assertEquals(3, skin.getButtonStyle("wide").getHorizontalPadding());
        assertEquals(0, skin.getButtonStyle("tight").getHorizontalPadding());
    }

    @Test
    void menuBarReferencesButtonStylesByName() {
        Skin skin = skinWithButtons("""
                "buttons": { "flat": { "normal": "plain" }, "other": { "normal": "plain",
                  "horizontalPadding": 2 } },
                "menuBars": { "bar": { "titles": "flat", "items": "other" } }""");

        MenuBarStyle bar = skin.getMenuBarStyle("bar");
        assertEquals(skin.getButtonStyle("flat"), bar.getTitles());
        assertEquals(skin.getButtonStyle("other"), bar.getItems());
    }

    @Test
    void menuBarAcceptsInlineButtons() {
        Skin skin = skinWithButtons("""
                "menuBars": { "bar": {
                  "titles": { "normal": "plain", "horizontalPadding": 2 },
                  "items": { "normal": { "foreground": "white",
                    "background": "navy" } }
                } }""");

        MenuBarStyle bar = skin.getMenuBarStyle("bar");
        assertEquals(ButtonStyle.of(skin.getLabelStyle("plain")).withHorizontalPadding(2),
                bar.getTitles());
        assertEquals(ButtonStyle.of(skin.getLabelStyle("plain")), bar.getItems());
    }

    @Test
    void menuBarTriggerSettingsDefaultToNone() {
        Skin skin = skinWithButtons("""
                "buttons": { "flat": { "normal": "plain" } },
                "menuBars": { "bar": { "titles": "flat", "items": "flat" } }""");

        MenuBarStyle bar = skin.getMenuBarStyle("bar");
        assertFalse(bar.hasTriggerRgba());
        assertFalse(bar.isTriggerUnderline());
    }

    @Test
    void menuBarTriggerSettingsAreApplied() {
        Skin skin = skinWithButtons("""
                "buttons": { "flat": { "normal": "plain" } },
                "menuBars": {
                  "byName": { "titles": "flat", "items": "flat", "triggerColor": "teal",
                    "triggerUnderline": true },
                  "byHex": { "titles": "flat", "items": "flat", "triggerColor": "#FF40C0",
                    "triggerUnderline": false }
                }""");

        MenuBarStyle byName = skin.getMenuBarStyle("byName");
        assertEquals(0x00A0A0FF, byName.getTriggerRgba());
        assertTrue(byName.isTriggerUnderline());
        MenuBarStyle byHex = skin.getMenuBarStyle("byHex");
        assertEquals(0xFF40C0FF, byHex.getTriggerRgba());
        assertFalse(byHex.isTriggerUnderline());
    }

    @Test
    void sectionsMayAppearInAnyOrderInTheFile() {
        Skin skin = Skin.fromJson("""
                {
                  "labels": { "plain": { "foreground": "white",
                    "background": "white" } },
                  "colors": { "white": "#FFFFFF" }
                }""");

        assertEquals(LabelStyle.of(0xFFFFFFFF, 0xFFFFFFFF), skin.getLabelStyle("plain"));
    }

    @Test
    void missingNamesListKnownNames() {
        Skin skin = skinWithButtons("""
                "buttons": { "flat": { "normal": "plain" } },
                "menuBars": { "bar": { "titles": "flat", "items": "flat" } }""");

        assertMissingNameMessage(() -> skin.getLabelStyle("nope"), "label style",
                "plain, tealLook");
        assertMissingNameMessage(() -> skin.getButtonStyle("nope"), "button style", "flat");
        assertMissingNameMessage(() -> skin.getMenuBarStyle("nope"), "menu bar style", "bar");
        assertMissingNameMessage(() -> skin.getRgba("nope"), "color", "white, navy, teal");
    }

    private static void assertMissingNameMessage(Executable lookup, String kind,
            String knownNames) {
        var thrown = assertThrows(IllegalArgumentException.class, lookup);
        String message = thrown.getMessage();
        assertTrue(message.contains("Unknown " + kind + " \"nope\""), message);
        assertTrue(message.contains("Known " + kind + "s: " + knownNames), message);
    }

    @Test
    void hasMethodsReportPresentAndAbsentNames() {
        Skin skin = skinWithButtons("""
                "buttons": { "flat": { "normal": "plain" } },
                "menuBars": { "bar": { "titles": "flat", "items": "flat" } }""");

        assertTrue(skin.hasColor("navy"));
        assertFalse(skin.hasColor("nope"));
        assertTrue(skin.hasLabelStyle("plain"));
        assertFalse(skin.hasLabelStyle("flat"));
        assertTrue(skin.hasButtonStyle("flat"));
        assertFalse(skin.hasButtonStyle("plain"));
        assertTrue(skin.hasMenuBarStyle("bar"));
        assertFalse(skin.hasMenuBarStyle("flat"));
    }

    @Test
    void hasMethodsRejectNull() {
        Skin skin = Skin.fromJson("{}");

        assertThrows(NullPointerException.class, () -> skin.hasColor(null));
        assertThrows(NullPointerException.class, () -> skin.hasLabelStyle(null));
        assertThrows(NullPointerException.class, () -> skin.hasButtonStyle(null));
        assertThrows(NullPointerException.class, () -> skin.hasMenuBarStyle(null));
    }

    @Test
    void leadingByteOrderMarkIsSkipped() {
        Skin skin = Skin.fromJson("﻿{\"colors\": {\"navy\": \"#203070\"}}");

        assertEquals(0x203070FF, skin.getRgba("navy"));
    }

    @Test
    void fromFileSkipsByteOrderMark() throws IOException {
        File file = File.createTempFile("skin", ".json");
        try {
            Files.write(file.toPath(),
                    "﻿{\"colors\": {\"navy\": \"#203070\"}}"
                            .getBytes(StandardCharsets.UTF_8));

            Skin skin = Skin.fromFile(new FileHandle(file));

            assertEquals(0x203070FF, skin.getRgba("navy"));
        } finally {
            file.delete();
        }
    }

    @Test
    void fromFilePrefixesErrorsWithThePath() throws IOException {
        File file = File.createTempFile("skin", ".json");
        try {
            Files.write(file.toPath(),
                    "{\"fonts\": {}}".getBytes(StandardCharsets.UTF_8));
            var handle = new FileHandle(file);

            var thrown = assertThrows(SkinFileException.class, () -> Skin.fromFile(handle));

            assertTrue(thrown.getMessage().startsWith(handle.path() + ": "), thrown.getMessage());
            assertTrue(thrown.getMessage().contains("unknown section \"fonts\""));
        } finally {
            file.delete();
        }
    }

    @Test
    void nullArgumentsFailWithNullPointerException() {
        Skin skin = Skin.fromJson("{}");

        assertThrows(NullPointerException.class, () -> Skin.fromJson(null));
        assertThrows(NullPointerException.class, () -> Skin.fromFile(null));
        assertThrows(NullPointerException.class, () -> skin.getRgba(null));
        assertThrows(NullPointerException.class, () -> skin.getLabelStyle(null));
        assertThrows(NullPointerException.class, () -> skin.getButtonStyle(null));
        assertThrows(NullPointerException.class, () -> skin.getMenuBarStyle(null));
    }
}
