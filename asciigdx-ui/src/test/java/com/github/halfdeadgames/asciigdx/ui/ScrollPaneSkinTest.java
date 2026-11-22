package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollPaneSkinTest {

    private static final String COLORS = """
            "colors": { "dark": "#404040", "light": "#909090", "white": "#FFFFFF" }""";

    private static String withSection(String body) {
        return "{" + COLORS + ", \"scrollPanes\": {\"p\": {" + body + "}}}";
    }

    private static void assertSkinError(String json, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void requiredKeysAloneCopyThumbIntoTheOtherLooks() {
        Skin skin = Skin.fromJson(withSection("\"track\": \"dark\", \"thumb\": \"light\""));

        assertEquals(ScrollPaneStyle.of(0x404040FF, 0x909090FF), skin.getScrollPaneStyle("p"));
        assertEquals(0x909090FF, skin.getScrollPaneStyle("p").getThumbPointerOverRgba());
        assertEquals(0x909090FF, skin.getScrollPaneStyle("p").getThumbPressedRgba());
        assertTrue(skin.hasScrollPaneStyle("p"));
        assertFalse(skin.hasScrollPaneStyle("other"));
    }

    @Test
    void optionalKeysAndHexColorsAreRead() {
        Skin skin = Skin.fromJson(withSection("""
                "track": "#10203040", "thumb": "light",
                "thumbPointerOver": "white", "thumbPressed": "#FF000080" """));

        ScrollPaneStyle expected = ScrollPaneStyle.of(0x10203040, 0x909090FF)
                .withThumbPointerOver(0xFFFFFFFF).withThumbPressed(0xFF000080);
        assertEquals(expected, skin.getScrollPaneStyle("p"));
    }

    @Test
    void missingRequiredKeysAreReported() {
        assertSkinError(withSection("\"thumb\": \"light\""), "scrollPanes.p", "track");
        assertSkinError(withSection("\"track\": \"dark\""), "scrollPanes.p", "thumb");
    }

    @Test
    void unknownKeysAreRejectedWithTheKnownOnes() {
        assertSkinError(withSection("\"track\": \"dark\", \"thumb\": \"light\", \"size\": 1"),
                "scrollPanes.p", "unknown key \"size\"",
                "Known keys: track, thumb, thumbPointerOver, thumbPressed, stepButtons, "
                        + "stepArrow, stepArrowPointerOver, stepArrowPressed, upArrow, "
                        + "downArrow, leftArrow, rightArrow, arrowFont");
    }

    @Test
    void stepButtonKeysAreOffAndFollowTheThumbWhenMissing() {
        ScrollPaneStyle style = Skin.fromJson(withSection(
                "\"track\": \"dark\", \"thumb\": \"light\", \"thumbPressed\": \"white\""))
                .getScrollPaneStyle("p");

        assertFalse(style.isStepButtonsShown());
        assertEquals(0x909090FF, style.getStepArrowRgba());
        assertEquals(0x909090FF, style.getStepArrowPointerOverRgba());
        assertEquals(0xFFFFFFFF, style.getStepArrowPressedRgba());
        assertEquals(0x25B2, style.getUpArrowGlyph());
        assertEquals(0x25BC, style.getDownArrowGlyph());
        assertEquals(0x25C4, style.getLeftArrowGlyph());
        assertEquals(0x25BA, style.getRightArrowGlyph());
        assertEquals(MarkFont.SQUARE, style.getArrowFont());
    }

    @Test
    void stepButtonKeysAreRead() {
        ScrollPaneStyle style = Skin.fromJson(withSection("""
                "track": "dark", "thumb": "light", "stepButtons": true,
                "stepArrow": "white", "stepArrowPointerOver": "#FF0000", "stepArrowPressed": "dark",
                "upArrow": "^", "downArrow": "v", "leftArrow": "<", "rightArrow": ">",
                "arrowFont": "text" """)).getScrollPaneStyle("p");

        assertTrue(style.isStepButtonsShown());
        assertEquals(0xFFFFFFFF, style.getStepArrowRgba());
        assertEquals(0xFF0000FF, style.getStepArrowPointerOverRgba());
        assertEquals(0x404040FF, style.getStepArrowPressedRgba());
        assertEquals('^', style.getUpArrowGlyph());
        assertEquals('v', style.getDownArrowGlyph());
        assertEquals('<', style.getLeftArrowGlyph());
        assertEquals('>', style.getRightArrowGlyph());
        assertEquals(MarkFont.TEXT, style.getArrowFont());
    }

    @Test
    void badStepButtonValuesNameThePath() {
        String base = "\"track\": \"dark\", \"thumb\": \"light\", ";
        assertSkinError(withSection(base + "\"stepButtons\": \"yes\""),
                "scrollPanes.p.stepButtons");
        assertSkinError(withSection(base + "\"stepArrow\": \"nope\""),
                "scrollPanes.p.stepArrow", "unknown color \"nope\"");
        assertSkinError(withSection(base + "\"upArrow\": \"ab\""), "scrollPanes.p.upArrow");
        assertSkinError(withSection(base + "\"arrowFont\": \"huge\""),
                "scrollPanes.p.arrowFont");
    }

    @Test
    void unsetArrowColorsFollowALaterThumbChange() {
        ScrollPaneStyle style = ScrollPaneStyle.of(1, 2).withThumbPressed(3);

        assertEquals(3, style.getStepArrowPressedRgba());
        assertEquals(ScrollPaneStyle.of(1, 2).withThumbPressed(3).withStepArrowPressed(3), style);
        assertNotEquals(style, style.withStepArrowPressed(4));
        assertFalse(style.isStepButtonsShown());
        assertNotEquals(style, style.withStepButtonsShown(true));
    }

    @Test
    void badColorsNameThePath() {
        assertSkinError(withSection("\"track\": \"nope\", \"thumb\": \"light\""),
                "scrollPanes.p.track", "unknown color \"nope\"");
        assertSkinError(withSection(
                "\"track\": \"dark\", \"thumb\": \"light\", \"thumbPressed\": 5"),
                "scrollPanes.p.thumbPressed");
    }

    @Test
    void unknownStyleNameListsKnownNames() {
        Skin skin = Skin.fromJson(withSection("\"track\": \"dark\", \"thumb\": \"light\""));

        var thrown = assertThrows(IllegalArgumentException.class,
                () -> skin.getScrollPaneStyle("zzz"));

        assertTrue(thrown.getMessage().contains("scroll pane style \"zzz\""));
        assertTrue(thrown.getMessage().contains("p"));
    }

    @Test
    void sectionIsOptionalAndToStringCountsStyles() {
        assertFalse(Skin.fromJson("{}").hasScrollPaneStyle("p"));
        assertTrue(Skin.fromJson(withSection("\"track\": \"dark\", \"thumb\": \"light\""))
                .toString().contains("scrollPaneStyles=1"));
    }

    @Test
    void styleHasValueEquality() {
        ScrollPaneStyle base = ScrollPaneStyle.of(1, 2);

        assertEquals(base, ScrollPaneStyle.of(1, 2));
        assertEquals(base.hashCode(), ScrollPaneStyle.of(1, 2).hashCode());
        assertEquals(base, base.withThumbPointerOver(2));
        assertNotEquals(base, base.withThumbPointerOver(3));
        assertNotEquals(base, base.withThumbPressed(3));
        assertNotEquals(base, ScrollPaneStyle.of(1, 3));
        assertTrue(base.toString().startsWith("ScrollPaneStyle["));
    }
}
