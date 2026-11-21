package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckBoxTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final ToggleStyle STYLE = style();

    private final UiSurface surface = UiSurface.ofSize(40, 10);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<Widget> changes = new ArrayList<>();

    private static ToggleStyle style() {
        ButtonStyle button = ButtonStyle.of(LabelStyle.of(WHITE, 0x111111FF),
                LabelStyle.of(WHITE, 0x222222FF), LabelStyle.of(WHITE, 0x333333FF),
                LabelStyle.of(WHITE, 0x444444FF));
        return ToggleStyle.of(button, "[ ]", "[x]");
    }

    private CheckBox boxAt(int x, int y) {
        CheckBox box = new CheckBox("Go", STYLE);
        box.setPosition(x, y);
        box.addChangeListener(changes::add);
        ui.getRoot().addChild(box);
        return box;
    }

    @Test
    void preferredSizeIsPaddingMarkGapAndTextCellsByOneRow() {
        CheckBox plain = new CheckBox("Go", STYLE);
        CheckBox wide = new CheckBox("Go", STYLE.withGap(3));

        assertEquals(5, plain.getWidth());
        assertEquals(1, plain.getHeight());
        assertEquals(6, wide.getWidth());
        assertEquals(1, wide.getHeight());
    }

    @Test
    void oddLengthTextRoundsUpToWholeCellsInThePreferredWidth() {
        int threeLetters = new CheckBox("abc", STYLE).getPreferredWidth();

        assertEquals(threeLetters, new CheckBox("abcd", STYLE).getPreferredWidth());
        assertEquals(threeLetters + 1, new CheckBox("abcde", STYLE).getPreferredWidth());
    }

    @Test
    void markAndWordsAreBothInTheTextGrid() {
        CheckBox box = boxAt(2, 2);
        ui.draw();

        assertEquals('[', surface.getTextGrid().getCodePoint(6, 2));
        assertEquals(' ', surface.getTextGrid().getCodePoint(7, 2));
        assertEquals(']', surface.getTextGrid().getCodePoint(8, 2));
        assertEquals('G', surface.getTextGrid().getCodePoint(10, 2));
        assertEquals('o', surface.getTextGrid().getCodePoint(11, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getSquareGrid().getCodePoint(3, 2));

        box.setChecked(true);
        ui.draw();

        assertEquals('x', surface.getTextGrid().getCodePoint(7, 2));
    }

    @Test
    void wordsStartAfterTheGapCountedInTextColumns() {
        CheckBox box = new CheckBox("Go", STYLE.withGap(3));
        box.setPosition(2, 2);
        ui.getRoot().addChild(box);
        ui.draw();

        assertEquals('G', surface.getTextGrid().getCodePoint(2 * 2 + 2 * 1 + 3 + 3, 2));
    }

    @Test
    void startsUncheckedWithItsTextAndStyle() {
        CheckBox box = new CheckBox("Go", STYLE);

        assertFalse(box.isChecked());
        assertEquals("Go", box.getText());
        assertSame(STYLE, box.getStyle());
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
        CheckBox box = new CheckBox("Go", STYLE);
        box.validate();
        box.setText("Go");
        assertFalse(box.isLayoutNeeded());

        box.setText("Longer");
        assertTrue(box.isLayoutNeeded());
        box.validate();
        box.setStyle(STYLE.withGap(4));
        assertTrue(box.isLayoutNeeded());
    }
}
