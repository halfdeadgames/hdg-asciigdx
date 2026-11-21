package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The focused look of check boxes and radio buttons, and its priority among the other looks. */
class ToggleFocusedLookTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final LabelStyle NORMAL = look(0x111111FF);
    private static final LabelStyle OVER = look(0x222222FF);
    private static final LabelStyle PRESSED = look(0x333333FF);
    private static final LabelStyle DISABLED = look(0x444444FF);
    private static final LabelStyle FOCUSED = look(0x555555FF);
    private static final ToggleStyle STYLE = ToggleStyle.of(
            ButtonStyle.of(NORMAL, OVER, PRESSED, DISABLED).withFocused(FOCUSED), "[ ]", "[x]");

    private final UiSurface surface = UiSurface.ofSize(20, 3);
    private final UiRoot ui = UiRoot.of(surface);

    private static LabelStyle look(int background) {
        return LabelStyle.of(WHITE, background);
    }

    private int drawnBackground() {
        ui.draw();
        return surface.getSquareGrid().getBackground(0, 0);
    }

    @Test
    void checkBoxShowsFocusedLookAfterTabAndPriorityHolds() {
        CheckBox box = new CheckBox("Go", STYLE);
        ui.getRoot().addChild(box);
        assertEquals(NORMAL.getBackgroundRgba(), drawnBackground());

        ui.moveFocus(FocusDirection.NEXT);
        assertEquals(FOCUSED.getBackgroundRgba(), drawnBackground());

        ui.pointerMoved(1, 0);
        assertEquals(OVER.getBackgroundRgba(), drawnBackground());

        ui.pointerDown(1, 0, 0, 0);
        assertEquals(PRESSED.getBackgroundRgba(), drawnBackground());

        box.setDisabled(true);
        assertEquals(DISABLED.getBackgroundRgba(), drawnBackground());
    }

    @Test
    void checkBoxHidesFocusedLookAfterAPointerPress() {
        CheckBox box = new CheckBox("Go", STYLE);
        ui.getRoot().addChild(box);
        ui.moveFocus(FocusDirection.NEXT);

        ui.pointerDown(1, 0, 0, 0);
        ui.pointerUp(1, 0, 0, 0);
        ui.pointerMoved(-1, -1);

        assertEquals(NORMAL.getBackgroundRgba(), drawnBackground());
    }

    @Test
    void radioButtonShowsFocusedLookAfterTabAndPriorityHolds() {
        RadioButton radio = new RadioButton("Go", STYLE);
        ui.getRoot().addChild(radio);

        ui.moveFocus(FocusDirection.NEXT);
        assertEquals(FOCUSED.getBackgroundRgba(), drawnBackground());

        ui.pointerMoved(1, 0);
        assertEquals(OVER.getBackgroundRgba(), drawnBackground());

        ui.pointerDown(1, 0, 0, 0);
        assertEquals(PRESSED.getBackgroundRgba(), drawnBackground());

        radio.setDisabled(true);
        assertEquals(DISABLED.getBackgroundRgba(), drawnBackground());
    }

    @Test
    void radioButtonHidesFocusedLookAfterAPointerPress() {
        RadioButton radio = new RadioButton("Go", STYLE);
        ui.getRoot().addChild(radio);
        ui.moveFocus(FocusDirection.NEXT);

        ui.pointerDown(1, 0, 0, 0);
        ui.pointerUp(1, 0, 0, 0);
        ui.pointerMoved(-1, -1);

        assertEquals(NORMAL.getBackgroundRgba(), drawnBackground());
    }
}
