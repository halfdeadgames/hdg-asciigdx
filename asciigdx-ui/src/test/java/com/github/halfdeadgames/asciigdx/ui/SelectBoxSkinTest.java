package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SelectBoxSkinTest {

    private static final Path DEFAULT_SKIN_PATH = Path.of("..", "assets", "default-skin.json");
    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070" },
            "labels": { "plain": { "foreground": "white", "background": "navy" } },
            "buttons": { "action": { "normal": "plain" }, "rows": { "normal": "plain" } },
            "scrollPanes": { "bar": { "track": "navy", "thumb": "white" } }""";
    private static final String REFERENCES =
            "\"button\": \"action\", \"list\": \"rows\", \"scrollPane\": \"bar\"";

    private static String skinWith(String body) {
        return "{" + BASE + ", \"selectBoxes\": {\"s\": {" + body + "}}}";
    }

    private static void assertSkinError(String body, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(skinWith(body)));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void entryReferencesTheThreeStylesAndUsesTheDefaultArrow() {
        Skin skin = Skin.fromJson(skinWith(REFERENCES));

        SelectBoxStyle style = skin.getSelectBoxStyle("s");
        assertEquals(skin.getButtonStyle("action"), style.getBoxStyle());
        assertEquals(skin.getButtonStyle("rows"), style.getRowStyle());
        assertEquals(skin.getScrollPaneStyle("bar"), style.getScrollPane());
        assertEquals(SelectBoxStyle.DEFAULT_ARROW_GLYPH, style.getArrowGlyph());
        assertEquals(MarkFont.SQUARE, style.getArrowFont());
        assertTrue(skin.hasSelectBoxStyle("s"));
        assertFalse(skin.hasSelectBoxStyle("other"));
    }

    @Test
    void buttonAndListMayBeInlineObjects() {
        Skin skin = Skin.fromJson(skinWith("\"button\": {\"normal\": \"plain\"}, "
                + "\"list\": \"rows\", \"scrollPane\": \"bar\""));

        assertEquals(skin.getButtonStyle("action"), skin.getSelectBoxStyle("s").getBoxStyle());
    }

    @Test
    void arrowAndArrowFontAreOptionalAndRead() {
        Skin skin = Skin.fromJson(skinWith(REFERENCES + ", \"arrow\": \"v\", "
                + "\"arrowFont\": \"text\""));

        assertEquals('v', skin.getSelectBoxStyle("s").getArrowGlyph());
        assertEquals(MarkFont.TEXT, skin.getSelectBoxStyle("s").getArrowFont());
    }

    @Test
    void missingRequiredKeysAreReported() {
        assertSkinError("\"list\": \"rows\", \"scrollPane\": \"bar\"", "selectBoxes.s",
                "missing \"button\"");
        assertSkinError("\"button\": \"action\", \"scrollPane\": \"bar\"", "missing \"list\"");
        assertSkinError("\"button\": \"action\", \"list\": \"rows\"", "missing \"scrollPane\"");
    }

    @Test
    void unknownReferencesNameTheKnownStyles() {
        assertSkinError("\"button\": \"nope\", \"list\": \"rows\", \"scrollPane\": \"bar\"",
                "selectBoxes.s.button", "unknown button style \"nope\"", "Known button styles");
        assertSkinError("\"button\": \"action\", \"list\": \"rows\", \"scrollPane\": \"nope\"",
                "selectBoxes.s.scrollPane", "unknown scroll pane style \"nope\"", "bar");
    }

    @Test
    void scrollPaneMustBeTheNameOfAStyle() {
        assertSkinError("\"button\": \"action\", \"list\": \"rows\", "
                + "\"scrollPane\": {\"track\": \"navy\", \"thumb\": \"white\"}",
                "selectBoxes.s.scrollPane", "the name of a scroll pane style");
    }

    @Test
    void badArrowValuesAreReported() {
        assertSkinError(REFERENCES + ", \"arrow\": \"vv\"", "selectBoxes.s.arrow",
                "exactly one character");
        assertSkinError(REFERENCES + ", \"arrowFont\": \"Square\"", "selectBoxes.s.arrowFont",
                "text, square");
    }

    @Test
    void unknownKeysListTheKnownKeys() {
        assertSkinError(REFERENCES + ", \"size\": 3", "unknown key \"size\"",
                "Known keys: button, list, scrollPane, arrow, arrowFont");
    }

    @Test
    void unknownStyleNameFailsWithTheKnownNames() {
        Skin skin = Skin.fromJson(skinWith(REFERENCES));

        var thrown = assertThrows(IllegalArgumentException.class,
                () -> skin.getSelectBoxStyle("zzz"));

        assertTrue(thrown.getMessage().contains("select box style"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("s"), thrown.getMessage());
        assertThrows(NullPointerException.class, () -> skin.getSelectBoxStyle(null));
    }

    @Test
    void skinWithoutTheSectionHasNoSelectBoxStyles() {
        assertFalse(Skin.fromJson("{}").hasSelectBoxStyle("s"));
    }

    @Test
    void sectionComesAfterScrollPanesInTheSectionNames() {
        var names = SkinSchema.getSectionNames();

        assertEquals(names.indexOf("scrollPanes") + 1, names.indexOf("selectBoxes"));
        assertTrue(SkinFileReader.isBuiltInSection("selectBoxes"));
    }

    @Test
    void defaultSkinFileDefinesTheSelectBoxStyle() throws IOException {
        Skin skin = Skin.fromJson(Files.readString(DEFAULT_SKIN_PATH, StandardCharsets.UTF_8));

        SelectBoxStyle expected = SelectBoxStyle.of(skin.getButtonStyle("button"),
                skin.getButtonStyle("button"), skin.getScrollPaneStyle("scrollPane"))
                .withArrowGlyph(0x25BC).withArrowFont(MarkFont.SQUARE);
        assertEquals(expected, skin.getSelectBoxStyle("selectBox"));
    }
}
