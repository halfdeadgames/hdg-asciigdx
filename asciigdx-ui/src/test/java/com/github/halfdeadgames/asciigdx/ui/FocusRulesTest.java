package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Edge rules of keyboard navigation and of when the focused look shows. */
class FocusRulesTest {
    private static final ButtonStyle STYLE = ButtonStyle.of(
            LabelStyle.of(0xFFFFFFFF, 0x111111FF));

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(40, 10));

    private TextButton buttonIn(WidgetGroup parent, int x, int y) {
        TextButton button = new TextButton("Go", STYLE);
        button.setPosition(x, y);
        parent.addChild(button);
        return button;
    }

    private static final class ShowingBox extends Widget {
        boolean focusShownWhenDrawn;

        @Override
        public boolean isFocusable() {
            return true;
        }

        @Override
        public int getPreferredWidth() {
            return 4;
        }

        @Override
        public int getPreferredHeight() {
            return 1;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            focusShownWhenDrawn = isFocusShown();
        }
    }

    private static final class SkippedBox extends Widget {
        @Override
        public boolean isFocusable() {
            return true;
        }

        @Override
        protected boolean isInFocusOrder() {
            return false;
        }

        @Override
        public int getPreferredWidth() {
            return 4;
        }

        @Override
        public int getPreferredHeight() {
            return 1;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }
    }

    @Test
    void textInputListenersRunInAddOrderOnlyOnChangeAndCanBeRemoved() {
        List<String> log = new ArrayList<>();
        UiRoot.TextInputWantedListener first = wanted -> log.add("first " + wanted);
        UiRoot.TextInputWantedListener second = wanted -> log.add("second " + wanted);
        ui.addTextInputWantedListener(first);
        ui.addTextInputWantedListener(second);
        TextField field = new TextField(TextFieldStyle.of(STYLE.getNormal()), 10);
        TextButton button = buttonIn(ui.getRoot(), 2, 2);
        field.setPosition(2, 5);
        ui.getRoot().addChild(field);

        ui.setKeyboardFocus(button);
        assertEquals(List.of(), log);
        ui.setKeyboardFocus(field);
        assertTrue(ui.isTextInputWanted());
        ui.removeTextInputWantedListener(first);
        ui.removeTextInputWantedListener(first);
        ui.setKeyboardFocus(button);

        assertEquals(List.of("first true", "second true", "second false"), log);
        assertFalse(ui.isTextInputWanted());
    }

    @Test
    void textInputListenerMayRemoveItselfWhileBeingNotified() {
        List<String> log = new ArrayList<>();
        UiRoot.TextInputWantedListener[] selfRemoving = new UiRoot.TextInputWantedListener[1];
        selfRemoving[0] = wanted -> {
            log.add("self");
            ui.removeTextInputWantedListener(selfRemoving[0]);
        };
        ui.addTextInputWantedListener(selfRemoving[0]);
        ui.addTextInputWantedListener(wanted -> log.add("other"));
        TextField field = new TextField(TextFieldStyle.of(STYLE.getNormal()), 10);
        ui.getRoot().addChild(field);

        ui.setKeyboardFocus(field);
        ui.setKeyboardFocus(null);

        assertEquals(List.of("self", "other", "other"), log);
    }

    @Test
    void changedFiresOnlyOnRealChangesAndNeverForRequestedAgain() {
        List<String> log = new ArrayList<>();
        ui.addTextInputWantedListener(new UiRoot.TextInputWantedListener() {
            @Override
            public void textInputWantedChanged(boolean wanted) {
                log.add("changed " + wanted);
            }

            @Override
            public void textInputRequestedAgain() {
                log.add("again");
            }
        });
        TextField first = new TextField(TextFieldStyle.of(STYLE.getNormal()), 10);
        TextField second = new TextField(TextFieldStyle.of(STYLE.getNormal()), 10);
        second.setPosition(2, 5);
        ui.getRoot().addChild(first);
        ui.getRoot().addChild(second);

        ui.setKeyboardFocus(first);
        ui.setKeyboardFocus(first);
        ui.setKeyboardFocus(second);
        ui.setKeyboardFocus(null);

        assertEquals(List.of("changed true", "changed false"), log);
    }

    @Test
    void textInputListenersRejectNull() {
        assertThrows(NullPointerException.class, () -> ui.addTextInputWantedListener(null));
        assertThrows(NullPointerException.class, () -> ui.removeTextInputWantedListener(null));
    }

    @Test
    void subclassSeesTheFocusLookWhileDrawing() {
        ShowingBox box = new ShowingBox();
        box.setSize(4, 1);
        ui.getRoot().addChild(box);
        ui.setKeyboardFocus(box);
        ui.keyDown(Input.Keys.TAB);

        ui.draw();

        assertTrue(box.focusShownWhenDrawn);
    }

    @Test
    void subclassCanOptOutOfNavigation() {
        TextButton first = buttonIn(ui.getRoot(), 2, 2);
        SkippedBox skipped = new SkippedBox();
        skipped.setPosition(12, 2);
        skipped.setSize(4, 1);
        ui.getRoot().addChild(skipped);
        TextButton last = buttonIn(ui.getRoot(), 22, 2);
        ui.setKeyboardFocus(first);

        ui.moveFocus(FocusDirection.NEXT);

        assertSame(last, ui.getKeyboardFocus());
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
