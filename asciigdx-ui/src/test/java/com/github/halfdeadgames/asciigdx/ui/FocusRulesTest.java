package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

/** Edge rules of keyboard navigation and of when the focused look shows. */
class FocusRulesTest {
    private static final ButtonStyle STYLE = ButtonStyle.of(
            LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x111111FF));

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(40, 10));

    private TextButton buttonIn(WidgetGroup parent, int x, int y) {
        TextButton button = new TextButton("Go", STYLE);
        button.setPosition(x, y);
        parent.addChild(button);
        return button;
    }

    @Test
    void tabWithASingleFocusedWidgetIsHandled() {
        TextButton only = buttonIn(ui.getRoot(), 2, 2);
        ui.setKeyboardFocus(only);

        assertTrue(ui.keyDown(Input.Keys.TAB));
        assertTrue(ui.moveFocus(FocusDirection.NEXT));
        assertSame(only, ui.getKeyboardFocus());
    }

    @Test
    void nextAndPreviousContinueFromANonCandidateTreePosition() {
        TextButton before = buttonIn(ui.getRoot(), 2, 2);
        TextButton disabled = buttonIn(ui.getRoot(), 12, 2);
        TextButton after = buttonIn(ui.getRoot(), 22, 2);
        disabled.setDisabled(true);

        ui.setKeyboardFocus(disabled);
        ui.moveFocus(FocusDirection.NEXT);
        assertSame(after, ui.getKeyboardFocus());

        ui.setKeyboardFocus(disabled);
        ui.moveFocus(FocusDirection.PREVIOUS);
        assertSame(before, ui.getKeyboardFocus());
    }

    @Test
    void arrowsDoNothingWhenTheFocusOwnerIsInThePopupLayerButTabStillWorks() {
        TextButton mainButton = buttonIn(ui.getRoot(), 2, 2);
        buttonIn(ui.getRoot(), 20, 2);
        TextButton popupButton = buttonIn(ui.getPopupLayer(), 10, 6);
        ui.setKeyboardFocus(popupButton);

        assertFalse(ui.keyDown(Input.Keys.RIGHT));
        assertSame(popupButton, ui.getKeyboardFocus());
        assertTrue(ui.keyDown(Input.Keys.TAB));
        assertSame(mainButton, ui.getKeyboardFocus());
    }

    @Test
    void menuBarsAreSkippedByNavigationButStillTakeFocus() {
        MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(STYLE, STYLE));
        bar.setSize(40, 0);
        ui.getRoot().addChild(bar);
        bar.addMenu("File", 'F');
        TextButton button = buttonIn(ui.getRoot(), 2, 4);

        ui.moveFocus(FocusDirection.NEXT);
        assertSame(button, ui.getKeyboardFocus());
        ui.moveFocus(FocusDirection.PREVIOUS);
        assertSame(button, ui.getKeyboardFocus());

        ui.setKeyboardFocus(bar);
        assertSame(bar, ui.getKeyboardFocus());
    }

    @Test
    void focusIsHiddenBeforeAnyInput() {
        TextButton button = buttonIn(ui.getRoot(), 2, 2);
        ui.setKeyboardFocus(button);

        assertFalse(ui.isFocusVisible());
        assertFalse(button.isFocusShown());
    }

    @Test
    void aKeyThatTheFocusedChainHandledShowsFocusAndAPointerPressHidesIt() {
        TextButton button = buttonIn(ui.getRoot(), 2, 2);
        ui.setKeyboardFocus(button);

        assertTrue(ui.keyDown(Input.Keys.ENTER));
        assertTrue(ui.isFocusVisible());
        assertTrue(button.isFocusShown());

        ui.pointerDown(3, 2, 0, 0);
        assertFalse(ui.isFocusVisible());
        assertFalse(button.isFocusShown());
    }

    @Test
    void unhandledKeysKeyUpsModifiersAndTypedCharactersDoNotShowFocus() {
        TextButton button = buttonIn(ui.getRoot(), 2, 2);
        ui.pointerDown(3, 2, 0, 0);
        ui.pointerUp(3, 2, 0, 0);
        assertSame(button, ui.getKeyboardFocus());

        ui.keyDown(Input.Keys.W);
        ui.keyDown(Input.Keys.A);
        ui.keyDown(Input.Keys.SHIFT_LEFT);
        ui.keyUp(Input.Keys.ENTER);
        ui.keyTyped('w');

        assertFalse(ui.isFocusVisible());
        assertFalse(button.isFocusShown());
    }

    @Test
    void navigationShowsFocusEvenAfterAClick() {
        TextButton button = buttonIn(ui.getRoot(), 2, 2);
        buttonIn(ui.getRoot(), 12, 2);
        ui.pointerDown(3, 2, 0, 0);
        ui.pointerUp(3, 2, 0, 0);
        assertFalse(ui.isFocusVisible());

        ui.keyDown(Input.Keys.RIGHT);

        assertTrue(ui.isFocusVisible());
        assertFalse(button.isFocusShown());
    }

    @Test
    void focusIsNotVisibleWithoutAFocusOwner() {
        ui.keyDown(Input.Keys.ENTER);

        assertFalse(ui.isFocusVisible());
        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void passedShiftStateOverridesTheTrackedState() {
        TextButton first = buttonIn(ui.getRoot(), 2, 2);
        TextButton last = buttonIn(ui.getRoot(), 12, 2);

        ui.keyDown(Input.Keys.TAB, true);
        assertSame(last, ui.getKeyboardFocus());

        ui.keyDown(Input.Keys.SHIFT_LEFT);
        ui.keyDown(Input.Keys.TAB, false);
        assertSame(first, ui.getKeyboardFocus());
    }
}
