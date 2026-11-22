package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins which error comes first when one entry has two problems. */
class SkinErrorOrderTest {

    private static final String BASE_WITHOUT_WINDOWS = """
            "colors": { "c": "#000000" },
            "labels": { "l": { "foreground": "c", "background": "c" } },
            "buttons": { "b": { "normal": "l" } },
            "textFields": { "t": { "normal": "l" } }""";
    private static final String WINDOWS_LINE = """
            , "windows": { "w": { "box": "single", "border": "l", "title": "l",
                                "background": "c" } }""";
    private static final String DIALOG_REFERENCES = "\"window\": \"w\", \"message\": \"l\", "
            + "\"buttons\": \"b\", \"textField\": \"t\"";
    private static final String GOOD_LOOKS = "\"track\": {\"normal\": \"l\"}, "
            + "\"knob\": {\"normal\": \"l\"}";
    private static final String WINDOW_START = "\"box\": \"single\", \"border\": \"l\", "
            + "\"background\": \"c\"";

    private static void assertFirstError(String section, String entryBody, String expected) {
        String base = section.equals("windows")
                ? BASE_WITHOUT_WINDOWS
                : BASE_WITHOUT_WINDOWS + WINDOWS_LINE;
        String json = "{" + base + ", \"" + section + "\": {\"subject\": {" + entryBody + "}}}";
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        assertTrue(thrown.getMessage().contains(expected),
                "Expected \"" + expected + "\" in: " + thrown.getMessage());
    }

    @Test
    void dialogReportsAMissingDimPartBeforeReadingTheDimColor() {
        assertFirstError("dialogs", DIALOG_REFERENCES + ", \"dimColor\": \"nope\"",
                "\"dimColor\" and \"dimAmount\" must be given together");
    }

    @Test
    void sliderReportsAMissingGlyphPartBeforeReadingTheGlyph() {
        assertFirstError("sliders", "\"trackGlyph\": \"--\", " + GOOD_LOOKS,
                "\"trackGlyph\" and \"knobGlyph\" must be given together");
    }

    @Test
    void sliderReportsATrackProblemBeforeAGlyphProblem() {
        assertFirstError("sliders", "\"trackGlyph\": \"--\", \"knobGlyph\": \"x\", "
                + "\"track\": {\"normal\": \"nope\"}, \"knob\": {\"normal\": \"l\"}",
                "sliders.subject.track.normal");
        assertFirstError("sliders", "\"trackGlyph\": \"--\", \"knobGlyph\": \"x\", "
                + "\"track\": {\"normal\": \"l\"}", "missing \"knob\"");
    }

    @Test
    void windowReportsARequiredKeyProblemBeforeAnOptionalKeyProblem() {
        assertFirstError("windows", WINDOW_START + ", \"focusedBorder\": \"nope\"",
                "missing \"title\"");
        assertFirstError("windows", WINDOW_START + ", \"title\": \"nope\", "
                + "\"focusedBorder\": \"nope\"", "windows.subject.title");
    }

    @Test
    void toggleReportsUnequalMarksBeforeAGapProblem() {
        assertFirstError("checkBoxes", "\"button\": \"b\", \"unchecked\": \"ab\", "
                + "\"checked\": \"c\", \"gap\": -1", "checkBoxes.subject.checked");
        assertFirstError("radioButtons", "\"button\": \"b\", \"unchecked\": \"ab\", "
                + "\"checked\": \"c\", \"markFont\": \"tall\"", "radioButtons.subject.checked");
    }
}
