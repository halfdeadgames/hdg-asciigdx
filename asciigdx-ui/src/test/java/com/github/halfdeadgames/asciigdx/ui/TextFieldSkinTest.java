package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextFieldSkinTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int NAVY = 0x203070FF;
    private static final int GRAY = 0x909090FF;

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070", "gray": "#909090" },
            "labels": {
              "plain": { "foreground": "white", "background": "navy" },
              "dim": { "foreground": "gray", "background": "navy" }
            }""";

    private static Skin skinWith(String textFields) {
        return Skin.fromJson("{" + BASE + ", \"textFields\": {" + textFields + "}}");
    }

    private static void assertSkinError(String textFields, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> skinWith(textFields));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void minimalEntryCopiesNormalAndGetsSteadyBarCaretInTheNormalForeground() {
        Skin skin = skinWith("\"input\": { \"normal\": \"plain\" }");

        TextFieldStyle style = skin.getTextFieldStyle("input");

        LabelStyle plain = LabelStyle.of(WHITE, NAVY);
        assertTrue(skin.hasTextFieldStyle("input"));
        assertFalse(skin.hasTextFieldStyle("other"));
        assertEquals(plain, style.getFocused());
        assertEquals(plain, style.getDisabled());
        assertEquals(plain, style.getPlaceholder());
        assertEquals(TextFieldStyle.of(plain), style);
        assertEquals(TextFieldStyle.DEFAULT_CARET_GLYPH, style.getCaret().getGlyphCodePoint());
        assertEquals(WHITE, style.getCaret().getForegroundRgba());
        assertEquals(0f, style.getCaret().getBlinkSeconds());
    }

    @Test
    void looksCanBeNamesOrInlineObjects() {
        Skin skin = skinWith("""
                "input": {
                  "normal": "plain",
                  "focused": { "foreground": "navy", "background": "white" },
                  "placeholder": "dim"
                }""");

        TextFieldStyle style = skin.getTextFieldStyle("input");

        assertEquals(LabelStyle.of(NAVY, WHITE), style.getFocused());
        assertEquals(LabelStyle.of(GRAY, NAVY), style.getPlaceholder());
        assertEquals(LabelStyle.of(WHITE, NAVY), style.getDisabled());
    }

    @Test
    void underlineCaretHasGlyphAndNoColors() {
        Skin skin = skinWith("""
                "input": { "normal": "plain",
                  "caret": { "glyph": "_", "blinkSeconds": 0.5 } }""");

        CaretStyle caret = skin.getTextFieldStyle("input").getCaret();

        assertTrue(caret.hasGlyph());
        assertEquals('_', caret.getGlyphCodePoint());
        assertFalse(caret.hasForeground());
        assertEquals(0.5f, caret.getBlinkSeconds());
    }

    @Test
    void caretWithGlyphAndForegroundKeepsBoth() {
        Skin skin = skinWith("""
                "input": { "normal": "plain",
                  "caret": { "glyph": "|", "foreground": "gray" } }""");

        CaretStyle caret = skin.getTextFieldStyle("input").getCaret();

        assertEquals(GRAY, caret.getForegroundRgba());
        assertEquals('|', caret.getGlyphCodePoint());
    }

    @Test
    void unknownLookedUpNameListsKnownStyles() {
        Skin skin = skinWith("\"input\": { \"normal\": \"plain\" }");

        var thrown = assertThrows(IllegalArgumentException.class,
                () -> skin.getTextFieldStyle("nope"));

        assertTrue(thrown.getMessage().contains("input"));
    }

    @Test
    void missingNormalIsAnError() {
        assertSkinError("\"input\": { }", "textFields.input", "missing \"normal\"");
    }

    @Test
    void unknownKeysAreErrorsWithKnownKeysListed() {
        assertSkinError("\"input\": { \"normal\": \"plain\", \"hover\": \"plain\" }",
                "textFields.input", "unknown key \"hover\"",
                "Known keys: normal, focused, disabled, placeholder, caret");
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"_\","
                + " \"shape\": 1 } }", "textFields.input.caret", "unknown key \"shape\"",
                "Known keys: glyph, foreground, blinkSeconds");
    }

    @Test
    void unknownLookNameIsAnError() {
        assertSkinError("\"input\": { \"normal\": \"missing\" }", "textFields.input.normal",
                "unknown label style \"missing\"");
    }

    @Test
    void lookWithAFontKeyIsAnErrorNamingThePath() {
        assertSkinError("\"input\": { \"normal\": \"plain\", \"disabled\": { \"font\": \"tall\","
                + " \"foreground\": \"white\", \"background\": \"navy\" } }",
                "textFields.input.disabled.font", "\"font\" key was removed");
    }

    @Test
    void caretWithoutGlyphIsAnError() {
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"blinkSeconds\": 1 } }",
                "textFields.input.caret", "missing \"glyph\"");
    }

    @Test
    void caretWithBackgroundIsAnError() {
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"_\","
                + " \"background\": \"navy\" } }", "textFields.input.caret.background",
                "has no background", "delete the \"background\" key");
    }

    @Test
    void badCaretValuesAreErrorsNamingThePath() {
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"ab\" } }",
                "textFields.input.caret.glyph", "exactly one character");
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"\" } }",
                "textFields.input.caret.glyph");
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"_\","
                + " \"blinkSeconds\": -1 } }", "textFields.input.caret.blinkSeconds",
                "zero or greater");
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"_\","
                + " \"blinkSeconds\": \"fast\" } }", "textFields.input.caret.blinkSeconds",
                "number");
        assertSkinError("\"input\": { \"normal\": \"plain\", \"caret\": { \"glyph\": \"_\","
                + " \"foreground\": \"red\" } }", "textFields.input.caret.foreground", "unknown color");
    }

    @Test
    void entryMustBeAnObject() {
        assertSkinError("\"input\": \"plain\"", "textFields.input", "text field object");
    }
}
