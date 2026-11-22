package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.List;
import org.junit.jupiter.api.Test;

class PanelValueTextTest {

    private static final JsonValue TREE = new JsonReader().parse("""
            { "colors": { "gold": "#ffd700", "clear": "#00000080" },
              "labels": { "text": {}, "title": {} },
              "buttons": { "button": {} } }
            """);

    @Test
    void glyphTextRoundTrips() {
        assertEquals("█", GlyphText.toSkinText("U+2588"));
        assertEquals("█", GlyphText.toSkinText("u+2588"));
        assertEquals("█", GlyphText.toSkinText("2588"));
        assertEquals("U+2588", GlyphText.toTypedText("█"));
        assertEquals("U+1F600", GlyphText.toTypedText("😀"));
        assertEquals("😀", GlyphText.toSkinText("U+1F600"));
    }

    @Test
    void glyphTextRefusesWhatIsNotADrawableCodePoint() {
        assertThrows(IllegalArgumentException.class, () -> GlyphText.toSkinText(""));
        assertThrows(IllegalArgumentException.class, () -> GlyphText.toSkinText("U+"));
        assertThrows(IllegalArgumentException.class, () -> GlyphText.toSkinText("U+110000"));
        assertThrows(IllegalArgumentException.class, () -> GlyphText.toSkinText("U+0007"));
    }

    @Test
    void hexColorTextAcceptsSixOrEightDigits() {
        assertTrue(HexColorText.isValid("ffd700"));
        assertTrue(HexColorText.isValid("#FFD70080"));
        assertFalse(HexColorText.isValid("ffd70"));
        assertFalse(HexColorText.isValid("gggggg"));
        assertEquals("#FFD700", HexColorText.toSkinText(" ffd700 "));
    }

    @Test
    void hexDigitsOfAColorFollowPaletteNames() {
        assertEquals("FFD700", HexColorText.digitsOf(TREE, "gold"));
        assertEquals("00000080", HexColorText.digitsOf(TREE, "clear"));
        assertEquals("112233", HexColorText.digitsOf(TREE, "#112233"));
        assertEquals("", HexColorText.digitsOf(TREE, "unknown"));
    }

    @Test
    void styleChoicesListTheStylesOfTheGivenSectionsInOrder() {
        List<StyleChoice> choices = StyleChoice.listIn(TREE,
                List.of("buttons", "missing", "labels"));

        assertEquals(List.of(new StyleChoice("buttons", "button"),
                new StyleChoice("labels", "text"), new StyleChoice("labels", "title")), choices);
    }
}
