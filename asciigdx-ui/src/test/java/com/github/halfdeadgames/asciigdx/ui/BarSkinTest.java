package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BarSkinTest {

    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070", "gray": "#909090" },
            "labels": {
              "plain": { "font": "square", "foreground": "white", "background": "navy" },
              "dim": { "font": "square", "foreground": "gray", "background": "navy" },
              "tallPlain": { "font": "tall", "foreground": "white", "background": "navy" }
            }""";
    private static final String SLIDER_START = "\"sliders\": {\"s\": {";

    private static String withBase(String moreSections) {
        return "{" + BASE + "," + moreSections + "}";
    }

    private static String slider(String body) {
        return withBase(SLIDER_START + body + "}}");
    }

    private static void assertSkinError(String json, String... expectedFragments) {
        var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
        for (String fragment : expectedFragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void progressBarSectionReadsTheFillAndEmptyLooks() {
        Skin skin = Skin.fromJson(withBase("""
                "progressBars": { "loading": {
                  "fill": "plain",
                  "empty": { "font": "square", "foreground": "gray", "background": "#00000080" }
                } }"""));

        LabelStyle empty = LabelStyle.of(UiFont.SQUARE, 0x909090FF, 0x00000080);
        assertEquals(ProgressBarStyle.of(skin.getLabelStyle("plain"), empty),
                skin.getProgressBarStyle("loading"));
        assertTrue(skin.hasProgressBarStyle("loading"));
        assertFalse(skin.hasProgressBarStyle("other"));
    }

    @Test
    void sliderSectionCopiesMissingLooksFromTheNormalOnes() {
        Skin skin = Skin.fromJson(slider("""
                "track": { "normal": "dim" },
                "knob": {
                  "normal": "plain",
                  "focused": { "font": "square", "foreground": "navy", "background": "white" }
                }"""));

        LabelStyle dim = skin.getLabelStyle("dim");
        LabelStyle plain = skin.getLabelStyle("plain");
        LabelStyle focusedKnob = LabelStyle.of(UiFont.SQUARE, 0x203070FF, 0xFFFFFFFF);
        SliderStyle expected = SliderStyle.of(dim, plain).withFocused(dim, focusedKnob);
        assertEquals(expected, skin.getSliderStyle("s"));
        assertEquals(plain, skin.getSliderStyle("s").getKnobDisabled());
        assertFalse(skin.getSliderStyle("s").hasGlyphs());
        assertTrue(skin.hasSliderStyle("s"));
        assertFalse(skin.hasSliderStyle("other"));
    }

    @Test
    void sliderGlyphsAreReadTogether() {
        Skin skin = Skin.fromJson(slider("""
                "trackGlyph": "-", "knobGlyph": "#",
                "track": { "normal": "plain" }, "knob": { "normal": "plain" }"""));

        SliderStyle style = skin.getSliderStyle("s");
        assertTrue(style.hasGlyphs());
        assertEquals('-', style.resolveTrackGlyph(BarOrientation.VERTICAL));
        assertEquals('#', style.resolveKnobGlyph());
    }

    @Test
    void glyphMayBeOutsideTheBasicPlane() {
        String grinningFace = "😀";
        Skin skin = Skin.fromJson(slider("\"trackGlyph\": \"" + grinningFace
                + "\", \"knobGlyph\": \"#\", \"track\": {\"normal\": \"plain\"}, "
                + "\"knob\": {\"normal\": \"plain\"}"));

        assertEquals(0x1F600, skin.getSliderStyle("s").resolveTrackGlyph(
                BarOrientation.HORIZONTAL));
    }

    @Test
    void unknownNamesListTheKnownOnes() {
        Skin skin = Skin.fromJson("{}");

        var thrown = assertThrows(IllegalArgumentException.class,
                () -> skin.getSliderStyle("none"));
        assertTrue(thrown.getMessage().contains("Known slider styles: none"));
        assertThrows(IllegalArgumentException.class, () -> skin.getProgressBarStyle("none"));
        assertThrows(NullPointerException.class, () -> skin.hasSliderStyle(null));
    }

    @Test
    void progressBarErrorsNameThePlace() {
        assertSkinError(withBase("\"progressBars\": {\"p\": {\"fill\": \"plain\"}}"),
                "progressBars.p", "missing \"empty\"");
        assertSkinError(withBase("\"progressBars\": {\"p\": {\"fill\": \"plain\", "
                + "\"empty\": \"nope\"}}"), "progressBars.p.empty", "unknown label style");
        assertSkinError(withBase("\"progressBars\": {\"p\": {\"fill\": \"plain\", "
                + "\"empty\": \"plain\", \"font\": \"tall\"}}"), "progressBars.p",
                "unknown key \"font\"", "Known keys: fill, empty");
        assertSkinError(withBase("\"progressBars\": {\"p\": {\"fill\": \"plain\", "
                + "\"empty\": \"tallPlain\"}}"), "progressBars.p", "same font");
        assertSkinError(withBase("\"progressBars\": {\"p\": \"x\"}"), "progressBars.p",
                "progress bar object");
    }

    @Test
    void sliderErrorsNameThePlace() {
        assertSkinError(slider("\"track\": {\"normal\": \"plain\"}"), "sliders.s",
                "missing \"knob\"");
        assertSkinError(slider("\"track\": {\"focused\": \"plain\"}, "
                + "\"knob\": {\"normal\": \"plain\"}"), "sliders.s.track", "missing \"normal\"");
        assertSkinError(slider("\"track\": \"plain\", \"knob\": {\"normal\": \"plain\"}"),
                "sliders.s.track", "must be an object");
        assertSkinError(slider("\"track\": {\"normal\": \"nope\"}, "
                + "\"knob\": {\"normal\": \"plain\"}"), "sliders.s.track.normal",
                "unknown label style");
        assertSkinError(slider("\"track\": {\"normal\": \"plain\", \"hover\": \"plain\"}, "
                + "\"knob\": {\"normal\": \"plain\"}"), "sliders.s.track",
                "unknown key \"hover\"", "Known keys: normal, focused, disabled");
        assertSkinError(slider("\"track\": {\"normal\": \"plain\"}, "
                + "\"knob\": {\"normal\": \"tallPlain\"}"), "sliders.s", "same font");
        assertSkinError(slider("\"track\": {\"normal\": \"plain\"}, "
                + "\"knob\": {\"normal\": \"plain\", \"disabled\": \"tallPlain\"}"),
                "sliders.s", "same font");
        assertSkinError(slider("\"track\": {\"normal\": \"plain\"}, "
                + "\"knob\": {\"normal\": \"plain\"}, \"color\": 1"), "sliders.s",
                "unknown key \"color\"");
    }

    @Test
    void sliderGlyphErrorsNameThePlace() {
        String looks = "\"track\": {\"normal\": \"plain\"}, \"knob\": {\"normal\": \"plain\"}";

        assertSkinError(slider("\"trackGlyph\": \"--\", \"knobGlyph\": \"#\", " + looks),
                "sliders.s.trackGlyph", "exactly one code point");
        assertSkinError(slider("\"trackGlyph\": \"-\", \"knobGlyph\": \"\", " + looks),
                "sliders.s.knobGlyph", "exactly one code point");
        assertSkinError(slider("\"trackGlyph\": 5, \"knobGlyph\": \"#\", " + looks),
                "sliders.s.trackGlyph");
        assertSkinError(slider("\"trackGlyph\": \"-\", " + looks), "sliders.s",
                "must be given together");
        assertSkinError(slider("\"knobGlyph\": \"#\", " + looks), "sliders.s",
                "must be given together");
    }

    @Test
    void duplicateNamesAreRejected() {
        assertSkinError(withBase("\"progressBars\": {\"p\": {\"fill\": \"plain\", "
                + "\"empty\": \"plain\"}, \"p\": {\"fill\": \"plain\", \"empty\": \"plain\"}}"),
                "progressBars.p", "defined twice");
    }
}
