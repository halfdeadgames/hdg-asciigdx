package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckBoxTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final ToggleStyle TALL = style(UiFont.TALL);
    private static final ToggleStyle SQUARE = style(UiFont.SQUARE);

    private final UiSurface surface = UiSurface.ofSize(40, 10);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<Widget> changes = new ArrayList<>();

    private static ToggleStyle style(UiFont font) {
        ButtonStyle button = ButtonStyle.of(LabelStyle.of(font, WHITE, 0x111111FF),
                LabelStyle.of(font, WHITE, 0x222222FF), LabelStyle.of(font, WHITE, 0x333333FF),
                LabelStyle.of(font, WHITE, 0x444444FF));
        return ToggleStyle.of(button, "[ ]", "[x]");
    }

    private CheckBox boxAt(int x, int y) {
        CheckBox box = new CheckBox("Go", TALL);
        box.setPosition(x, y);
        box.addChangeListener(changes::add);
        ui.getRoot().addChild(box);
        return box;
    }

    @Test
    void preferredSizeIsPaddingMarkGapAndTextByFontHeight() {
        CheckBox tall = new CheckBox("Go", TALL);
        CheckBox square = new CheckBox("Go", SQUARE.withGap(3));

        assertEquals(8, tall.getWidth());
        assertEquals(2, tall.getHeight());
        assertEquals(2, tall.getRowSnap());
        assertEquals(10, square.getWidth());
        assertEquals(1, square.getHeight());
        assertEquals(1, square.getRowSnap());
    }

    @Test
    void drawsMarkThenGapThenText() {
        CheckBox box = boxAt(2, 2);
        ui.draw();

        assertEquals('[', surface.getTallGrid().getCodePoint(3, 1));
        assertEquals(']', surface.getTallGrid().getCodePoint(5, 1));
        assertEquals('G', surface.getTallGrid().getCodePoint(7, 1));
        assertEquals('o', surface.getTallGrid().getCodePoint(8, 1));

        box.setChecked(true);
        ui.draw();

        assertEquals('x', surface.getTallGrid().getCodePoint(4, 1));
    }

    @Test
    void tallBoxOnEvenRowDrawsBackgroundAndGlyphsInTheSameRows() {
        boxAt(2, 4);
        ui.draw();

        assertEquals(0x111111FF, surface.getSquareGrid().getBackground(3, 4));
        assertEquals(0x111111FF, surface.getSquareGrid().getBackground(3, 5));
        assertEquals('[', surface.getTallGrid().getCodePoint(3, 2));
    }

    // The surface snaps tall glyphs up to the even row, so an owner has to honor getRowSnap().
    // On an odd row the background starts one row below the glyphs.
    @Test
    void tallBoxOnOddRowShowsWhyOwnersMustHonorTheRowSnap() {
        CheckBox box = boxAt(2, 5);
        ui.draw();

        assertEquals(2, box.getRowSnap());
        assertEquals('[', surface.getTallGrid().getCodePoint(3, 2));
        assertEquals(0x111111FF, surface.getSquareGrid().getBackground(3, 6));
    }

    @Test
    void startsUncheckedWithItsTextAndStyle() {
        CheckBox box = new CheckBox("Go", TALL);

        assertFalse(box.isChecked());
        assertEquals("Go", box.getText());
        assertSame(TALL, box.getStyle());
    }

    @Test
    void clickTogglesAndNotifiesTheListener() {
        CheckBox box = boxAt(2, 2);

        ui.pointerDown(3, 2, 0, 0);
        assertTrue(box.isPressed());
        ui.pointerUp(3, 2, 0, 0);
        assertTrue(box.isChecked());
        ui.pointerDown(3, 2, 0, 0);
        ui.pointerUp(3, 2, 0, 0);

        assertFalse(box.isChecked());
        assertEquals(List.of(box, box), changes);
    }

    @Test
    void releaseOutsideCancelAndSecondaryButtonDoNotToggle() {
        CheckBox box = boxAt(2, 2);

        ui.pointerDown(3, 2, 0, 0);
        ui.pointerDragged(30, 8, 0);
        assertFalse(box.isPressed());
        ui.pointerUp(30, 8, 0, 0);
        ui.pointerDown(3, 2, 0, 0);
        ui.pointerCancelled(0);
        ui.pointerDown(3, 2, 0, 1);
        ui.pointerUp(3, 2, 0, 1);

        assertFalse(box.isChecked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void enterNumpadEnterAndSpaceToggleWhenFocused() {
        CheckBox box = boxAt(2, 2);
        ui.setKeyboardFocus(box);

        assertTrue(ui.keyDown(Input.Keys.ENTER));
        assertTrue(box.isChecked());
        assertTrue(ui.keyDown(Input.Keys.NUMPAD_ENTER));
        assertFalse(box.isChecked());
        assertTrue(ui.keyDown(Input.Keys.SPACE));
        assertTrue(box.isChecked());
        assertFalse(ui.keyDown(Input.Keys.A));
        assertEquals(3, changes.size());
    }

    @Test
    void setCheckedFromCodeNeverNotifies() {
        CheckBox box = boxAt(2, 2);

        box.setChecked(true);
        box.setChecked(false);
        box.setChecked(true);

        assertTrue(box.isChecked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void disabledBoxIgnoresInputAndTakesNoFocus() {
        CheckBox box = boxAt(2, 2);
        box.setDisabled(true);
        ui.setKeyboardFocus(box);

        assertFalse(box.isFocusable());
        assertFalse(ui.pointerDown(3, 2, 0, 0));
        assertFalse(box.isPressed());
        ui.keyDown(Input.Keys.SPACE);

        assertFalse(box.isChecked());
        assertTrue(changes.isEmpty());
        box.setDisabled(false);
        assertTrue(box.isFocusable());
    }

    @Test
    void disablingDuringAPressEndsItWithoutToggle() {
        CheckBox box = boxAt(2, 2);
        ui.pointerDown(3, 2, 0, 0);

        box.setDisabled(true);
        ui.pointerUp(3, 2, 0, 0);

        assertFalse(box.isPressed());
        assertFalse(box.isChecked());
    }

    @Test
    void removedListenerIsNotCalledAgain() {
        CheckBox box = boxAt(2, 2);
        ChangeListener<Widget> extra = changes::add;
        box.addChangeListener(extra);
        box.removeChangeListener(extra);
        ui.setKeyboardFocus(box);

        ui.keyDown(Input.Keys.SPACE);

        assertEquals(1, changes.size());
    }

    @Test
    void textAndStyleChangesMarkTheBox() {
        CheckBox box = new CheckBox("Go", TALL);
        box.validate();
        box.setText("Go");
        assertFalse(box.isLayoutNeeded());

        box.setText("Longer");
        assertTrue(box.isLayoutNeeded());
        box.validate();
        box.setStyle(TALL.withGap(4));
        assertTrue(box.isLayoutNeeded());
    }
}
