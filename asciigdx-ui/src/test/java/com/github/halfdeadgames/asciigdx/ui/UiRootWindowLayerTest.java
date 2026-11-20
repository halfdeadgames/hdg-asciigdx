package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiRootWindowLayerTest {

    private static final LabelStyle LOOK = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x111111FF);
    private static final LabelStyle PRESSED = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x222222FF);
    private static final ButtonStyle BUTTON = ButtonStyle.of(LOOK, LOOK, PRESSED, LOOK);
    private static final WindowStyle WINDOW_STYLE =
            WindowStyle.of(BoxStyle.SINGLE, LOOK, LOOK, 0x333333FF);

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final TerminalGrid grid = surface.getSquareGrid();

    private TextButton buttonIn(WidgetGroup group, String text, int x, int y) {
        TextButton button = new TextButton(text, BUTTON);
        button.setPosition(x, y);
        group.addChild(button);
        return button;
    }

    private Window windowIn(WidgetGroup group, int x, int y, int width, int height) {
        Window window = new Window("W", WINDOW_STYLE);
        window.setSize(width, height);
        window.setPosition(x, y);
        group.addChild(window);
        return window;
    }

    // Layer, draw order, hit order

    @Test
    void windowLayerIsAStableEmptyGroup() {
        assertNotNull(ui.getWindowLayer());
        assertSame(ui.getWindowLayer(), ui.getWindowLayer());
        assertEquals(0, ui.getWindowLayer().getChildCount());
    }

    @Test
    void layersDrawRootThenWindowsThenPopups() {
        buttonIn(ui.getRoot(), "R", 0, 0);
        ui.draw();
        assertEquals('R', grid.getCodePoint(1, 0));

        Window window = windowIn(ui.getWindowLayer(), 0, 0, 6, 3);
        ui.draw();
        assertEquals('┌', grid.getCodePoint(0, 0));
        assertEquals('─', grid.getCodePoint(1, 0));

        buttonIn(ui.getPopupLayer(), "P", 0, 0);
        ui.draw();
        assertEquals('P', grid.getCodePoint(1, 0));

        ui.getPopupLayer().removeChild(ui.getPopupLayer().getChild(0));
        ui.draw();
        assertEquals('─', grid.getCodePoint(1, 0));

        ui.getWindowLayer().removeChild(window);
        ui.draw();
        assertEquals('R', grid.getCodePoint(1, 0));
    }

    @Test
    void laterWindowsDrawOverEarlierOnes() {
        windowIn(ui.getWindowLayer(), 0, 0, 6, 3);
        windowIn(ui.getWindowLayer(), 3, 0, 6, 3);

        ui.draw();

        assertEquals(' ', grid.getCodePoint(2, 0));
        assertEquals('┌', grid.getCodePoint(3, 0));
        assertEquals('─', grid.getCodePoint(4, 0));
        assertEquals('┐', grid.getCodePoint(8, 0));
    }

    @Test
    void aChangeInsideAWindowTriggersARedraw() {
        Window window = windowIn(ui.getWindowLayer(), 0, 0, 12, 4);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        ui.draw();

        content.setText("NO");
        ui.draw();

        assertEquals('N', grid.getCodePoint(2, 1));
    }

    @Test
    void hitOrderIsPopupsThenWindowsFrontToBackThenTheMainTree() {
        TextButton rootButton = buttonIn(ui.getRoot(), "R", 0, 0);
        Window back = windowIn(ui.getWindowLayer(), 0, 0, 6, 3);
        Window front = windowIn(ui.getWindowLayer(), 0, 0, 6, 3);
        TextButton popup = buttonIn(ui.getPopupLayer(), "P", 0, 0);

        ui.pointerMoved(1, 0);
        assertSame(popup, ui.getHoveredWidget());

        ui.getPopupLayer().removeChild(popup);
        ui.pointerMoved(2, 0);
        assertSame(front, ui.getHoveredWidget());

        ui.getWindowLayer().removeChild(front);
        ui.pointerMoved(1, 0);
        assertSame(back, ui.getHoveredWidget());

        ui.getWindowLayer().removeChild(back);
        ui.pointerMoved(2, 0);
        assertSame(rootButton, ui.getHoveredWidget());
    }

    @Test
    void windowLayerChildrenAreHoveredAndTheirParentsGetEnterAndExit() {
        Window window = windowIn(ui.getWindowLayer(), 10, 2, 12, 5);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        ui.draw();

        ui.pointerMoved(12, 3);

        assertSame(content, ui.getHoveredWidget());
        assertTrue(content.isPointerOver());

        ui.pointerMoved(30, 9);
        assertNull(ui.getHoveredWidget());
        assertFalse(content.isPointerOver());
    }

    @Test
    void pressCaptureWorksForWindowLayerWidgets() {
        Window window = windowIn(ui.getWindowLayer(), 10, 2, 12, 5);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        ui.draw();

        assertTrue(ui.pointerDown(12, 3, 0, 0));
        assertTrue(content.isPressed());
        assertTrue(ui.pointerUp(30, 9, 0, 0));
        assertFalse(content.isPressed());
    }

    @Test
    void removingTheWindowCancelsACaptureInsideIt() {
        Window window = windowIn(ui.getWindowLayer(), 10, 2, 12, 5);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        ui.draw();
        ui.pointerDown(12, 3, 0, 0);

        ui.getWindowLayer().removeChild(window);
        ui.update(0f);

        assertFalse(content.isPressed());
        assertFalse(ui.pointerUp(12, 3, 0, 0));
    }

    @Test
    void removingTheWindowClearsFocusInsideIt() {
        Window window = windowIn(ui.getWindowLayer(), 10, 2, 12, 5);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        ui.setKeyboardFocus(content);
        assertSame(content, ui.getKeyboardFocus());

        ui.getWindowLayer().removeChild(window);

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void hidingTheWindowLayerHidesItsWidgetsFromHitTesting() {
        Window window = windowIn(ui.getWindowLayer(), 0, 0, 6, 3);
        ui.getWindowLayer().setVisible(false);

        ui.pointerMoved(2, 1);

        assertNull(ui.getHoveredWidget());
        assertFalse(window.isShown());
    }

    @Test
    void theDragBoundsLimitsADragOfAWindowInTheWindowLayer() {
        ui.setWindowDragBounds(new CellArea(10, 2, 20, 6));
        Window window = windowIn(ui.getWindowLayer(), 12, 3, 6, 3);

        ui.pointerDown(14, 3, 0, 0);
        ui.pointerDragged(500, 500, 0);
        assertEquals(24, window.getX());
        assertEquals(7, window.getY());

        ui.pointerDragged(-500, -500, 0);
        assertEquals(10, window.getX());
        assertEquals(2, window.getY());
    }

    // Focus scope

    private TextButton rootFirst;
    private TextButton rootSecond;
    private TextButton windowFirst;
    private TextButton windowSecond;
    private TextButton otherWindowButton;

    private void buildScopeFixture() {
        rootFirst = buttonIn(ui.getRoot(), "Go", 0, 8);
        rootSecond = buttonIn(ui.getRoot(), "Go", 10, 8);
        windowFirst = new TextButton("Go", BUTTON);
        windowSecond = new TextButton("Go", BUTTON);
        otherWindowButton = new TextButton("Go", BUTTON);
        Window window = new Window("A", WINDOW_STYLE);
        WidgetGroup content = new WidgetGroup();
        windowFirst.setPosition(0, 0);
        content.addChild(windowFirst);
        windowSecond.setPosition(5, 0);
        content.addChild(windowSecond);
        window.setContent(content);
        window.pack();
        window.setPosition(20, 3);
        ui.getWindowLayer().addChild(window);
        Window other = new Window("B", WINDOW_STYLE);
        other.setContent(otherWindowButton);
        other.pack();
        other.setPosition(30, 3);
        ui.getWindowLayer().addChild(other);
        ui.draw();
    }

    @Test
    void tabWithoutFocusStartsInTheMainTree() {
        buildScopeFixture();

        assertTrue(ui.keyDown(Input.Keys.TAB));

        assertSame(rootFirst, ui.getKeyboardFocus());
    }

    @Test
    void tabInTheMainTreeNeverEntersWindowsOrPopups() {
        buildScopeFixture();
        buttonIn(ui.getPopupLayer(), "Go", 0, 0);
        ui.setKeyboardFocus(rootFirst);

        ui.keyDown(Input.Keys.TAB);
        assertSame(rootSecond, ui.getKeyboardFocus());
        ui.keyDown(Input.Keys.TAB);
        assertSame(rootFirst, ui.getKeyboardFocus());
    }

    @Test
    void tabInsideAWindowStaysInsideThatWindowAndWraps() {
        buildScopeFixture();
        ui.setKeyboardFocus(windowFirst);

        ui.keyDown(Input.Keys.TAB);
        assertSame(windowSecond, ui.getKeyboardFocus());
        ui.keyDown(Input.Keys.TAB);
        assertSame(windowFirst, ui.getKeyboardFocus());
    }

    @Test
    void shiftTabInsideAWindowStaysInsideAndWrapsBackward() {
        buildScopeFixture();
        ui.setKeyboardFocus(windowFirst);

        ui.keyDown(Input.Keys.SHIFT_LEFT);
        ui.keyDown(Input.Keys.TAB);
        ui.keyUp(Input.Keys.SHIFT_LEFT);

        assertSame(windowSecond, ui.getKeyboardFocus());
    }

    @Test
    void arrowsInsideAWindowDoNotJumpToOtherWindowsOrTheMainTree() {
        buildScopeFixture();
        ui.setKeyboardFocus(windowSecond);

        assertFalse(ui.keyDown(Input.Keys.RIGHT));
        assertSame(windowSecond, ui.getKeyboardFocus());
        assertFalse(ui.keyDown(Input.Keys.DOWN));
        assertSame(windowSecond, ui.getKeyboardFocus());
        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertSame(windowFirst, ui.getKeyboardFocus());
    }

    @Test
    void moveFocusFollowsTheSameScope() {
        buildScopeFixture();
        ui.setKeyboardFocus(otherWindowButton);

        assertTrue(ui.moveFocus(FocusDirection.NEXT));

        assertSame(otherWindowButton, ui.getKeyboardFocus());
    }

    @Test
    void focusScopeUsesTheWindowsPositionOnTheSurface() {
        buildScopeFixture();
        ui.getWindowLayer().setPosition(1, 1);
        ui.setKeyboardFocus(windowFirst);

        assertTrue(ui.keyDown(Input.Keys.RIGHT));

        assertSame(windowSecond, ui.getKeyboardFocus());
    }
}
