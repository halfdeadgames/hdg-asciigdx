package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static Skin skin;

    @BeforeAll
    static void loadSkin() throws IOException {
        skin = Skin.fromJson(Files.readString(DEFAULT_SKIN_PATH, StandardCharsets.UTF_8));
    }

    @Test
    void transparentColorIsFullyTransparent() {
        assertEquals(0x00000000, skin.getRgba("transparent"));
    }

    @Test
    void labelStylesMatchTheSandboxValues() {
        assertEquals(LabelStyle.of(UiFont.TALL, GOLD, BLACK), skin.getLabelStyle("title"));
        assertEquals(LabelStyle.of(UiFont.SQUARE, WHITE, BLACK),
                skin.getLabelStyle("text"));
        assertEquals(LabelStyle.of(UiFont.SQUARE, GOLD, 0x00000000),
                skin.getLabelStyle("overlayText"));
    }

    @Test
    void buttonStylesMatchTheSandboxValues() {
        ButtonStyle tallButton = ButtonStyle.of(LabelStyle.of(UiFont.TALL, WHITE, DEEP_BLUE),
                LabelStyle.of(UiFont.TALL, BLACK, TEAL),
                LabelStyle.of(UiFont.TALL, BLACK, GOLD),
                LabelStyle.of(UiFont.TALL, GRAY, DARK_GRAY))
                .withFocused(LabelStyle.of(UiFont.TALL, BLACK, CYAN));
        ButtonStyle squareButton = ButtonStyle.of(
                LabelStyle.of(UiFont.SQUARE, WHITE, DEEP_BLUE),
                LabelStyle.of(UiFont.SQUARE, BLACK, TEAL),
                LabelStyle.of(UiFont.SQUARE, BLACK, GOLD),
                LabelStyle.of(UiFont.SQUARE, GRAY, DARK_GRAY))
                .withFocused(LabelStyle.of(UiFont.SQUARE, BLACK, CYAN));

        assertEquals(tallButton, skin.getButtonStyle("tallButton"));
        assertEquals(squareButton, skin.getButtonStyle("button"));
    }

    @Test
    void toggleStylesReferenceTheButtonStyles() {
        assertEquals(ToggleStyle.of(skin.getButtonStyle("button"), "[ ]", "[x]"),
                skin.getCheckBoxStyle("checkBox"));
        assertEquals(ToggleStyle.of(skin.getButtonStyle("tallButton"), "[ ]", "[x]"),
                skin.getCheckBoxStyle("tallCheckBox"));
        assertEquals(ToggleStyle.of(skin.getButtonStyle("button"), "( )", "(*)"),
                skin.getRadioButtonStyle("radioButton"));
        assertEquals(ToggleStyle.of(skin.getButtonStyle("tallButton"), "( )", "(*)"),
                skin.getRadioButtonStyle("tallRadioButton"));
    }

    @Test
    void menuBarStylesMatchTheSandboxValues() {
        MenuBarStyle tallMenuBar = MenuBarStyle.of(skin.getButtonStyle("tallButton"),
                skin.getButtonStyle("tallButton")).withTriggerRgba(HOT_PINK)
                .withTriggerUnderline(true);
        MenuBarStyle menuBar = MenuBarStyle.of(skin.getButtonStyle("button"),
                skin.getButtonStyle("button")).withTriggerRgba(HOT_PINK)
                .withTriggerUnderline(true);

        assertEquals(tallMenuBar, skin.getMenuBarStyle("tallMenuBar"));
        assertEquals(menuBar, skin.getMenuBarStyle("menuBar"));
        assertTrue(skin.getMenuBarStyle("tallMenuBar").isTriggerUnderline());
    }
}
