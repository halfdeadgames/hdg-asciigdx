package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextButtonTest {
    private static final int NORMAL_BG = 0x111111FF;
    private static final int OVER_BG = 0x222222FF;
    private static final int PRESSED_BG = 0x333333FF;
    private static final int DISABLED_BG = 0x444444FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final ButtonStyle STYLE = style();

    private final UiSurface surface = UiSurface.ofSize(40, 10);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> clicks = new ArrayList<>();

    private static ButtonStyle style() {
        return ButtonStyle.of(LabelStyle.of(WHITE, NORMAL_BG), LabelStyle.of(WHITE, OVER_BG),
                LabelStyle.of(WHITE, PRESSED_BG), LabelStyle.of(WHITE, DISABLED_BG));
    }

    private TextButton buttonAt(int x, int y, ButtonStyle style) {
        TextButton button = new TextButton("Go", style);
        button.setPosition(x, y);
        button.addClickListener(clicked -> clicks.add(clicked.getText()));
        ui.getRoot().addChild(button);
        return button;
    }

    @Test
    void preferredSizeIsTextCellsPlusPaddingByOneRow() {
        TextButton plain = new TextButton("Go", STYLE);
        TextButton wide = new TextButton("Go", STYLE.withHorizontalPadding(3));

        assertEquals(3, plain.getWidth());
        assertEquals(1, plain.getHeight());
        assertEquals(7, wide.getWidth());
        assertEquals(1, wide.getHeight());
    }

    @Test
    void evenLengthTextTakesHalfItsLettersInCells() {
        assertEquals(4, new TextButton("abcd", STYLE).getPreferredWidth());
    }

    @Test
    void oddLengthTextRoundsUpToWholeCells() {
        assertEquals(4, new TextButton("abc", STYLE).getPreferredWidth());
        assertEquals(5, new TextButton("abcde", STYLE).getPreferredWidth());
    }

    @Test
    void pressInsideThenReleaseInsideClicks() {
        TextButton button = buttonAt(2, 2, STYLE);

        assertTrue(ui.pointerDown(3, 2, 0, 0));
        assertTrue(button.isPressed());
        assertTrue(ui.pointerUp(3, 2, 0, 0));

        assertFalse(button.isPressed());
        assertEquals(List.of("Go"), clicks);
    }

    @Test
    void dragOutsideUnpressesAndDragBackPressesAgain() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.pointerDown(3, 2, 0, 0);

        ui.pointerDragged(20, 8, 0);
        assertFalse(button.isPressed());
        ui.pointerDragged(4, 2, 0);
        assertTrue(button.isPressed());
        ui.pointerUp(4, 2, 0, 0);

        assertEquals(1, clicks.size());
    }

    @Test
    void releaseOutsideDoesNotClick() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.pointerDown(3, 2, 0, 0);
        ui.pointerDragged(20, 8, 0);

        assertTrue(ui.pointerUp(20, 8, 0, 0));

        assertFalse(button.isPressed());
        assertTrue(clicks.isEmpty());
    }

    @Test
    void cancelDoesNotClick() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.pointerDown(3, 2, 0, 0);

        assertTrue(ui.pointerCancelled(0));

        assertFalse(button.isPressed());
        assertTrue(clicks.isEmpty());
    }

    @Test
    void secondaryButtonIsIgnored() {
        TextButton button = buttonAt(2, 2, STYLE);

        assertFalse(ui.pointerDown(3, 2, 0, 1));
        assertFalse(button.isPressed());
        ui.pointerUp(3, 2, 0, 1);

        assertTrue(clicks.isEmpty());
    }

    @Test
    void enterNumpadEnterAndSpaceClickWhenFocused() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.setKeyboardFocus(button);

        assertTrue(ui.keyDown(com.badlogic.gdx.Input.Keys.ENTER));
        assertTrue(ui.keyDown(com.badlogic.gdx.Input.Keys.NUMPAD_ENTER));
        assertTrue(ui.keyDown(com.badlogic.gdx.Input.Keys.SPACE));
        assertFalse(ui.keyDown(com.badlogic.gdx.Input.Keys.A));

        assertEquals(3, clicks.size());
    }

    @Test
    void secondaryButtonDuringPrimaryPressKeepsTheClick() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.pointerDown(3, 2, 0, 0);

        assertFalse(ui.pointerDown(3, 2, 0, 1));
        assertFalse(ui.pointerUp(3, 2, 0, 1));
        assertTrue(button.isPressed());
        assertTrue(ui.pointerUp(3, 2, 0, 0));

        assertEquals(List.of("Go"), clicks);
    }

    @Test
    void removedPressedButtonResetsAndNeverClicks() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.pointerDown(3, 2, 0, 0);
        assertTrue(button.isPressed());

        ui.getRoot().removeChild(button);
        assertFalse(ui.pointerUp(3, 2, 0, 0));

        assertFalse(button.isPressed());
        assertTrue(clicks.isEmpty());
    }

    @Test
    void pressFocusesTheButton() {
        TextButton button = buttonAt(2, 2, STYLE);

        ui.pointerDown(3, 2, 0, 0);

        assertEquals(button, ui.getKeyboardFocus());
    }

    @Test
    void disabledButtonIgnoresInputAndIsNotFocusable() {
        TextButton button = buttonAt(2, 2, STYLE);
        button.setDisabled(true);
        ui.setKeyboardFocus(button);

        assertFalse(ui.pointerDown(3, 2, 0, 0));
        assertFalse(ui.pointerUp(3, 2, 0, 0));
        assertFalse(ui.keyDown(com.badlogic.gdx.Input.Keys.ENTER));

        assertFalse(button.isFocusable());
        assertTrue(clicks.isEmpty());
    }

    @Test
    void disablingDuringPressEndsThePressWithoutClick() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.pointerDown(3, 2, 0, 0);

        button.setDisabled(true);
        ui.pointerUp(3, 2, 0, 0);

        assertFalse(button.isPressed());
        assertTrue(clicks.isEmpty());
    }

    @Test
    void stateLookPriorityIsDisabledPressedOverNormal() {
        TextButton button = buttonAt(2, 2, STYLE);
        assertEquals(NORMAL_BG, drawnBackground());

        ui.pointerMoved(3, 2);
        assertTrue(button.isPointerOver());
        assertEquals(OVER_BG, drawnBackground());

        ui.pointerDown(3, 2, 0, 0);
        assertEquals(PRESSED_BG, drawnBackground());

        button.setDisabled(true);
        assertEquals(DISABLED_BG, drawnBackground());
    }

    @Test
    void textStartsAtPaddingInTextColumnsOnTopRow() {
        buttonAt(2, 2, STYLE);
        ui.draw();

        TerminalGrid textGrid = surface.getTextGrid();
        assertEquals('G', textGrid.getCodePoint(6, 2));
        assertEquals('o', textGrid.getCodePoint(7, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(5, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(8, 2));
    }

    @Test
    void oddLengthTextLeavesLastHalfCellBlank() {
        TextButton button = new TextButton("abc", STYLE);
        button.setPosition(2, 2);
        ui.getRoot().addChild(button);
        ui.draw();

        TerminalGrid textGrid = surface.getTextGrid();
        assertEquals('c', textGrid.getCodePoint(8, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(9, 2));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(5, 2));
    }

    @Test
    void stateChangesAndStyleChangesMarkRedraw() {
        TextButton button = buttonAt(2, 2, STYLE);
        ui.draw();
        assertFalse(ui.getRoot().isRedrawNeeded());

        ui.pointerMoved(3, 2);
        assertTrue(ui.getRoot().isRedrawNeeded());
        ui.draw();

        button.setStyle(STYLE);
        assertFalse(ui.getRoot().isRedrawNeeded());
        button.setStyle(style().withHorizontalPadding(2));
        assertTrue(button.isLayoutNeeded());
        ui.draw();

        button.setDisabled(true);
        assertTrue(ui.getRoot().isRedrawNeeded());
        assertFalse(button.isLayoutNeeded());
    }

    @Test
    void setTextInvalidatesAndKeepsSizeUntilPack() {
        TextButton button = new TextButton("Go", STYLE);

        button.setText("Go on");

        assertEquals(3, button.getWidth());
        button.pack();
        assertEquals(5, button.getWidth());
    }

    private int drawnBackground() {
        ui.draw();
        return surface.getSquareGrid().getBackground(3, 2);
    }
}
