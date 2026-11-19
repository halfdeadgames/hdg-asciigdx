package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

class UiRootFocusNavigationTest {
    private static final LabelStyle NORMAL = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x111111FF);
    private static final LabelStyle OVER = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x222222FF);
    private static final LabelStyle FOCUSED = LabelStyle.of(UiFont.SQUARE, 0, 0x333333FF);
    private static final ButtonStyle STYLE = ButtonStyle.of(NORMAL, OVER, OVER, OVER)
            .withFocused(FOCUSED);

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(40, 10));
    private final TextButton first = buttonAt(2, 2);
    private final TextButton second = buttonAt(12, 2);
    private final TextButton third = buttonAt(2, 6);

    private TextButton buttonAt(int x, int y) {
        TextButton button = new TextButton("Go", STYLE);
        button.setPosition(x, y);
        ui.getRoot().addChild(button);
        return button;
    }

    @Test
    void tabMovesThroughButtonsAndWraps() {
        assertTrue(ui.keyDown(Input.Keys.TAB));
        assertSame(first, ui.getKeyboardFocus());
        assertTrue(ui.keyDown(Input.Keys.TAB));
        assertSame(second, ui.getKeyboardFocus());
        ui.keyDown(Input.Keys.TAB);
        assertSame(third, ui.getKeyboardFocus());
        ui.keyDown(Input.Keys.TAB);
        assertSame(first, ui.getKeyboardFocus());
    }

    @Test
    void shiftTabMovesBackwardAndStartsAtTheLastButton() {
        ui.keyDown(Input.Keys.SHIFT_LEFT);
        assertTrue(ui.keyDown(Input.Keys.TAB));
        assertSame(third, ui.getKeyboardFocus());
        ui.keyDown(Input.Keys.TAB);
        assertSame(second, ui.getKeyboardFocus());
        ui.keyUp(Input.Keys.SHIFT_LEFT);
        ui.keyDown(Input.Keys.TAB);
        assertSame(third, ui.getKeyboardFocus());
    }

    @Test
    void rightShiftAlsoReversesTab() {
        ui.keyDown(Input.Keys.SHIFT_RIGHT);

        ui.keyDown(Input.Keys.TAB);

        assertSame(third, ui.getKeyboardFocus());
    }

    @Test
    void arrowsMoveSpatiallyAndStopAtTheEdge() {
        ui.setKeyboardFocus(first);

        assertTrue(ui.keyDown(Input.Keys.RIGHT));
        assertSame(second, ui.getKeyboardFocus());
        assertFalse(ui.keyDown(Input.Keys.RIGHT));
        assertSame(second, ui.getKeyboardFocus());
        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertTrue(ui.keyDown(Input.Keys.DOWN));
        assertSame(third, ui.getKeyboardFocus());
        assertTrue(ui.keyDown(Input.Keys.UP));
        assertSame(first, ui.getKeyboardFocus());
    }

    @Test
    void arrowWithNothingFocusedPicksTheFirstButton() {
        assertTrue(ui.keyDown(Input.Keys.DOWN));

        assertSame(first, ui.getKeyboardFocus());
    }

    @Test
    void escapeAndOtherKeysAreNotHandled() {
        assertFalse(ui.keyDown(Input.Keys.ESCAPE));
        assertFalse(ui.keyDown(Input.Keys.A));
        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void aWidgetThatHandlesKeyDownKeepsTheArrows() {
        ui.setKeyboardFocus(first);
        first.addListener(event -> event.getType() == UiEventType.KEY_DOWN
                && event.getKeycode() == Input.Keys.RIGHT);

        assertTrue(ui.keyDown(Input.Keys.RIGHT));

        assertSame(first, ui.getKeyboardFocus());
    }

    @Test
    void unhandledKeyListenerRunsBeforeNavigation() {
        ui.addUnhandledKeyListener(event -> event.getKeycode() == Input.Keys.TAB);

        assertTrue(ui.keyDown(Input.Keys.TAB));

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void keyUpDoesNotNavigate() {
        assertFalse(ui.keyUp(Input.Keys.TAB));

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void emptyTreeIsNotHandled() {
        UiRoot empty = UiRoot.of(UiSurface.ofSize(10, 10));

        assertFalse(empty.keyDown(Input.Keys.TAB));
        assertFalse(empty.moveFocus(FocusDirection.RIGHT));
    }

    @Test
    void moveFocusReportsWhetherFocusMoved() {
        assertTrue(ui.moveFocus(FocusDirection.NEXT));
        assertSame(first, ui.getKeyboardFocus());
        assertTrue(ui.moveFocus(FocusDirection.PREVIOUS));
        assertSame(third, ui.getKeyboardFocus());
        assertFalse(ui.moveFocus(FocusDirection.DOWN));
        assertSame(third, ui.getKeyboardFocus());
    }

    @Test
    void moveFocusRejectsNull() {
        assertThrows(NullPointerException.class, () -> ui.moveFocus(null));
    }

    @Test
    void moveFocusIsRejectedWhileAnEventIsDelivered() {
        IllegalStateException[] thrown = new IllegalStateException[1];
        first.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_MOVED) {
                thrown[0] = assertThrows(IllegalStateException.class,
                        () -> ui.moveFocus(FocusDirection.NEXT));
            }
            return false;
        });

        ui.pointerMoved(3, 2);

        assertNotNull(thrown[0]);
    }

    @Test
    void navigationDoesNothingWhileAMenuIsOpen() {
        MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(STYLE, STYLE));
        bar.setSize(40, 0);
        bar.setPosition(0, 9);
        ui.getRoot().addChild(bar);
        Menu menu = bar.addMenu("File", 'F');
        menu.addItem("Open", 'O', () -> { });
        ui.pointerDown(1, 9, 0, 0);
        assertTrue(bar.isOpen());
        Widget before = ui.getKeyboardFocus();

        assertFalse(ui.moveFocus(FocusDirection.NEXT));
        assertSame(before, ui.getKeyboardFocus());
    }

    @Test
    void focusIsVisibleAfterNavigationAndHiddenByThePointerPress() {
        assertFalse(ui.isFocusVisible());

        ui.keyDown(Input.Keys.TAB);
        assertTrue(ui.isFocusVisible());
        assertTrue(first.isFocusShown());

        ui.pointerDown(13, 2, 0, 0);
        assertFalse(ui.isFocusVisible());
        assertFalse(first.isFocusShown());
        assertFalse(second.isFocusShown());
        assertSame(second, ui.getKeyboardFocus());
    }

    @Test
    void setKeyboardFocusLeavesVisibilityUnchanged() {
        ui.setKeyboardFocus(first);
        assertFalse(ui.isFocusVisible());

        ui.keyDown(Input.Keys.TAB);
        ui.setKeyboardFocus(third);

        assertTrue(ui.isFocusVisible());
        assertTrue(third.isFocusShown());
        assertFalse(first.isFocusShown());
    }

    @Test
    void focusedLookRequestsARedrawWhenFocusMoves() {
        ui.draw();
        assertFalse(first.isRedrawNeeded());

        ui.moveFocus(FocusDirection.NEXT);

        assertTrue(first.isRedrawNeeded());
        assertTrue(ui.getRoot().isRedrawNeeded());
    }

    @Test
    void focusedLookIsDrawnOnlyWhenFocusIsVisible() {
        UiSurface surface = UiSurface.ofSize(40, 10);
        UiRoot drawingRoot = UiRoot.of(surface);
        TextButton button = new TextButton("Go", STYLE);
        drawingRoot.getRoot().addChild(button);
        drawingRoot.setKeyboardFocus(button);
        drawingRoot.draw();
        assertEquals(NORMAL.getBackgroundRgba(), surface.getSquareGrid().getBackground(0, 0));

        drawingRoot.moveFocus(FocusDirection.NEXT);
        drawingRoot.draw();

        assertEquals(FOCUSED.getBackgroundRgba(), surface.getSquareGrid().getBackground(0, 0));
    }
}
