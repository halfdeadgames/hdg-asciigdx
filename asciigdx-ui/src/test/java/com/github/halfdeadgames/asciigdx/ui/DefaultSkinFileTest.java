package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Loads the real {@code assets/default-skin.json}, which the sandbox scene depends on. */
class DefaultSkinFileTest {

    private static final Path DEFAULT_SKIN_PATH = Path.of("..", "assets", "default-skin.json");
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final int GOLD = 0xFFD700FF;
    private static final int DEEP_BLUE = 0x203070FF;
    private static final int TEAL = 0x00A0A0FF;
    private static final int CYAN = 0x00FFFFFF;
    private static final int HOT_PINK = 0xFF40C0FF;
    private static final int GRAY = 0x909090FF;
    private static final int DARK_GRAY = 0x404040FF;
    private static final int CHARCOAL = 0x303030FF;
    private static final int BAR_CARET_GLYPH = 0x258F;

    private static Skin skin;

    @BeforeAll
    static void loadSkin() throws IOException {
        skin = Skin.fromJson(Files.readString(DEFAULT_SKIN_PATH, StandardCharsets.UTF_8));
    }

    @Test
    void defaultSkinLoads() {
        assertNotNull(skin);
        assertTrue(skin.hasButtonStyle("button"));
    }

    @Test
    void transparentColorIsFullyTransparent() {
        assertEquals(0x00000000, skin.getRgba("transparent"));
    }

    @Test
    void tallStylesAreGone() {
        assertFalse(skin.hasLabelStyle("tallText"));
        assertFalse(skin.hasLabelStyle("tallOverlayText"));
        assertFalse(skin.hasLabelStyle("tallTooltip"));
        assertFalse(skin.hasButtonStyle("tallButton"));
        assertFalse(skin.hasMenuBarStyle("tallMenuBar"));
        assertFalse(skin.hasCheckBoxStyle("tallCheckBox"));
        assertFalse(skin.hasRadioButtonStyle("tallRadioButton"));
        assertFalse(skin.hasProgressBarStyle("tallProgressBar"));
        assertFalse(skin.hasSliderStyle("tallSlider"));
    }

    @Test
    void titleStillExistsWithGoldForeground() {
        assertTrue(skin.hasLabelStyle("title"));
        assertEquals(GOLD, skin.getLabelStyle("title").getForegroundRgba());
    }

    @Test
    void labelStylesMatchTheSandboxValues() {
        assertEquals(LabelStyle.of(GOLD, BLACK), skin.getLabelStyle("title"));
        assertEquals(LabelStyle.of(WHITE, BLACK), skin.getLabelStyle("text"));
        assertEquals(LabelStyle.of(GOLD, 0x00000000), skin.getLabelStyle("overlayText"));
    }

    @Test
    void buttonStyleMatchesTheSandboxValues() {
        ButtonStyle expected = ButtonStyle.of(LabelStyle.of(WHITE, DEEP_BLUE),
                LabelStyle.of(BLACK, TEAL),
                LabelStyle.of(BLACK, GOLD),
                LabelStyle.of(GRAY, DARK_GRAY))
                .withFocused(LabelStyle.of(BLACK, CYAN));

        assertEquals(expected, skin.getButtonStyle("button"));
    }

    @Test
    void toggleStylesReferenceTheButtonStyle() {
        assertEquals(ToggleStyle.of(skin.getButtonStyle("button"), "[ ]", "[x]"),
                skin.getCheckBoxStyle("checkBox"));
        assertEquals(ToggleStyle.of(skin.getButtonStyle("button"), "( )", "(*)"),
                skin.getRadioButtonStyle("radioButton"));
    }

    @Test
    void defaultTogglesUseTheTextFontAndTheDefaultWindowUsesTheSquareCloseFont() {
        assertEquals(MarkFont.TEXT, skin.getCheckBoxStyle("checkBox").getMarkFont());
        assertEquals(MarkFont.TEXT, skin.getRadioButtonStyle("radioButton").getMarkFont());
        assertEquals(MarkFont.SQUARE, skin.getWindowStyle("window").getCloseFont());
    }

    @Test
    void menuBarStyleMatchesTheSandboxValues() {
        MenuBarStyle menuBar = MenuBarStyle.of(skin.getButtonStyle("button"),
                skin.getButtonStyle("button")).withTriggerRgba(HOT_PINK)
                .withTriggerUnderline(true);

        assertEquals(menuBar, skin.getMenuBarStyle("menuBar"));
        assertTrue(skin.getMenuBarStyle("menuBar").isTriggerUnderline());
    }

    @Test
    void defaultTextFieldColorsAreUnchanged() {
        TextFieldStyle style = skin.getTextFieldStyle("default");

        assertEquals(LabelStyle.of(WHITE, DEEP_BLUE), style.getNormal());
        assertEquals(LabelStyle.of(WHITE, TEAL), style.getFocused());
        assertEquals(LabelStyle.of(GRAY, DARK_GRAY), style.getDisabled());
        assertEquals(LabelStyle.of(GRAY, DEEP_BLUE), style.getPlaceholder());
    }

    @Test
    void defaultTextFieldCaretIsABlinkingWhiteBarGlyph() {
        CaretStyle caret = skin.getTextFieldStyle("default").getCaret();

        assertTrue(caret.hasGlyph());
        assertEquals(BAR_CARET_GLYPH, caret.getGlyphCodePoint());
        assertTrue(caret.hasForeground());
        assertEquals(WHITE, caret.getForegroundRgba());
        assertEquals(0.5f, caret.getBlinkSeconds());
    }

    @Test
    void progressBarStyleMatchesTheSandboxValues() {
        LabelStyle goldOnGray = LabelStyle.of(GOLD, DARK_GRAY);

        assertEquals(ProgressBarStyle.of(goldOnGray, goldOnGray),
                skin.getProgressBarStyle("progressBar"));
    }

    @Test
    void scrollPaneStyleUsesTheGrayTrackAndAGoldPressedThumb() {
        ScrollPaneStyle expected = ScrollPaneStyle.of(DARK_GRAY, GRAY)
                .withThumbPointerOver(WHITE).withThumbPressed(GOLD)
                .withStepButtonsShown(true);

        assertEquals(expected, skin.getScrollPaneStyle("scrollPane"));
    }

    @Test
    void defaultWindowStyleMatchesTheSandboxValues() {
        WindowStyle expected = WindowStyle.of(BoxStyle.ROUNDED,
                LabelStyle.of(GRAY, CHARCOAL),
                LabelStyle.of(GOLD, CHARCOAL), CHARCOAL)
                .withFocusedBorder(LabelStyle.of(CYAN, CHARCOAL))
                .withCloseButton(skin.getButtonStyle("button"))
                .withCloseText("x");

        assertEquals(expected, skin.getWindowStyle("window"));
        assertTrue(skin.hasWindowStyle("window"));
    }

    @Test
    void defaultDialogStyleUsesTheDefaultPartsAndDimsHalfBlack() {
        DialogStyle expected = DialogStyle.of(skin.getWindowStyle("window"),
                skin.getLabelStyle("windowText"), skin.getButtonStyle("button"),
                skin.getTextFieldStyle("default")).withDim(BLACK, 0.5f);

        assertEquals(expected, skin.getDialogStyle("dialog"));
        assertTrue(skin.hasDialogStyle("dialog"));
    }

    @Test
    void windowTextIsTextOnTheWindowBackground() {
        assertEquals(LabelStyle.of(WHITE, CHARCOAL), skin.getLabelStyle("windowText"));
        assertEquals(CHARCOAL, skin.getWindowStyle("window").getBackgroundRgba());
    }

    @Test
    void sliderStyleUsesTheDefaultGlyphsForEachOrientation() {
        SliderStyle slider = skin.getSliderStyle("slider");

        assertFalse(slider.hasGlyphs());
        assertEquals(0x2500, slider.resolveTrackGlyph(BarOrientation.HORIZONTAL));
        assertEquals(0x2502, slider.resolveTrackGlyph(BarOrientation.VERTICAL));
        assertEquals(0x2588, slider.resolveKnobGlyph());
        assertEquals(LabelStyle.of(CYAN, BLACK), slider.getKnobFocused());
        assertEquals(LabelStyle.of(DARK_GRAY, BLACK), slider.getTrackDisabled());
    }
}
