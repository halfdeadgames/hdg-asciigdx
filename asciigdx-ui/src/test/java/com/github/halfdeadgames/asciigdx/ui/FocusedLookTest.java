package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The focused look of {@link ButtonStyle}, its skin file key, and its priority in the button. */
class FocusedLookTest {
    private static final LabelStyle NORMAL = LabelStyle.of(0xFFFFFFFF, 0x111111FF);
    private static final LabelStyle OVER = LabelStyle.of(0xFFFFFFFF, 0x222222FF);
    private static final LabelStyle PRESSED = LabelStyle.of(0xFFFFFFFF, 0x333333FF);
    private static final LabelStyle DISABLED = LabelStyle.of(0xFFFFFFFF, 0x444444FF);
    private static final LabelStyle FOCUSED = LabelStyle.of(0xFFFFFFFF, 0x555555FF);
    private static final String BASE = """
            "colors": { "white": "#FFFFFF", "navy": "#203070", "teal": "#00A0A0" },
            "labels": {
              "plain": { "foreground": "white", "background": "navy" },
              "bright": { "foreground": "white", "background": "teal" }
            }""";

    private static Skin skinWithButtons(String buttons) {
        return Skin.fromJson("{" + BASE + ", \"buttons\": {" + buttons + "}}");
    }

    private static void assertSkinError(String buttons, String... fragments) {
        var thrown = assertThrows(SkinFileException.class, () -> skinWithButtons(buttons));
        for (String fragment : fragments) {
            assertTrue(thrown.getMessage().contains(fragment),
                    "Expected \"" + fragment + "\" in: " + thrown.getMessage());
        }
    }

    @Test
    void focusedLookDefaultsToThePointerOverLook() {
        ButtonStyle style = ButtonStyle.of(NORMAL, OVER, PRESSED, DISABLED);

        assertEquals(OVER, style.getFocused());
        assertEquals(NORMAL, ButtonStyle.of(NORMAL).getFocused());
    }

    @Test
    void withFocusedReturnsAChangedCopyAndKeepsTheOriginal() {
        ButtonStyle original = ButtonStyle.of(NORMAL, OVER, PRESSED, DISABLED)
                .withHorizontalPadding(3);

        ButtonStyle changed = original.withFocused(FOCUSED);

        assertEquals(OVER, original.getFocused());
        assertEquals(FOCUSED, changed.getFocused());
        assertEquals(3, changed.getHorizontalPadding());
        assertEquals(PRESSED, changed.getPressed());
    }

    @Test
    void withHorizontalPaddingKeepsTheFocusedLook() {
        ButtonStyle style = ButtonStyle.of(NORMAL, OVER, PRESSED, DISABLED)
                .withFocused(FOCUSED).withHorizontalPadding(2);

        assertEquals(FOCUSED, style.getFocused());
    }

    @Test
    void withFocusedRejectsNull() {
        ButtonStyle style = ButtonStyle.of(NORMAL);

        assertThrows(NullPointerException.class, () -> style.withFocused(null));
    }

    @Test
    void focusedLookTakesPartInEqualsHashCodeAndToString() {
        ButtonStyle plain = ButtonStyle.of(NORMAL, OVER, PRESSED, DISABLED);
        ButtonStyle withFocus = plain.withFocused(FOCUSED);

        assertNotEquals(plain, withFocus);
        assertEquals(withFocus, plain.withFocused(FOCUSED));
        assertEquals(withFocus.hashCode(), plain.withFocused(FOCUSED).hashCode());
        assertEquals(plain, plain.withFocused(OVER));
        assertTrue(withFocus.toString().contains("focused="));
    }

    @Test
    void skinFocusedKeyAcceptsAReferenceOrAnInlineLook() {
        Skin skin = skinWithButtons("""
                "byName": { "normal": "plain", "focused": "bright" },
                "inline": { "normal": "plain",
                  "focused": { "foreground": "white", "background": "teal" } }
                """);
        LabelStyle teal = LabelStyle.of(0xFFFFFFFF, 0x00A0A0FF);

        assertEquals(teal, skin.getButtonStyle("byName").getFocused());
        assertEquals(teal, skin.getButtonStyle("inline").getFocused());
    }

    @Test
    void skinMissingFocusedCopiesPointerOver() {
        Skin skin = skinWithButtons("""
                "withOver": { "normal": "plain", "pointerOver": "bright" },
                "bare": { "normal": "plain" }
                """);

        assertEquals(skin.getButtonStyle("withOver").getPointerOver(),
                skin.getButtonStyle("withOver").getFocused());
        assertEquals(skin.getButtonStyle("bare").getNormal(),
                skin.getButtonStyle("bare").getFocused());
    }

    @Test
    void skinFileWithoutFocusedKeysStillLoadsAsBefore() {
        Skin skin = skinWithButtons("""
                "old": { "normal": "plain", "pointerOver": "bright", "pressed": "bright",
                  "disabled": "plain", "horizontalPadding": 2 }
                """);

        assertEquals(ButtonStyle.of(skin.getLabelStyle("plain"), skin.getLabelStyle("bright"),
                skin.getLabelStyle("bright"), skin.getLabelStyle("plain"))
                .withHorizontalPadding(2), skin.getButtonStyle("old"));
    }

    @Test
    void skinBadFocusedReferenceNamesThePath() {
        assertSkinError("\"x\": { \"normal\": \"plain\", \"focused\": \"missing\" }",
                "buttons.x.focused", "missing");
    }

    @Test
    void skinUnknownButtonKeyListsFocused() {
        assertSkinError("\"x\": { \"normal\": \"plain\", \"glow\": \"plain\" }", "glow",
                "focused");
    }

    @Test
    void buttonLookPriorityIsDisabledPressedPointerOverFocusedNormal() {
        var surface = UiSurface.ofSize(10, 3);
        var ui = UiRoot.of(surface);
        ButtonStyle style = ButtonStyle.of(NORMAL, OVER, PRESSED, DISABLED).withFocused(FOCUSED);
        TextButton button = new TextButton("Go", style);
        ui.getRoot().addChild(button);

        ui.moveFocus(FocusDirection.NEXT);
        assertEquals(FOCUSED.getBackgroundRgba(), drawnBackground(ui, surface));

        ui.pointerMoved(1, 0);
        assertEquals(OVER.getBackgroundRgba(), drawnBackground(ui, surface));

        ui.pointerDown(1, 0, 0, 0);
        assertEquals(PRESSED.getBackgroundRgba(), drawnBackground(ui, surface));

        button.setDisabled(true);
        assertEquals(DISABLED.getBackgroundRgba(), drawnBackground(ui, surface));
    }

    private static int drawnBackground(UiRoot ui, UiSurface surface) {
        ui.draw();
        return surface.getSquareGrid().getBackground(0, 0);
    }
}
