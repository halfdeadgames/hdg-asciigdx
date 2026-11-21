package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

class WidgetVisibilityTest {
    private static final int NORMAL_BG = 0x111111FF;
    private static final ButtonStyle STYLE = ButtonStyle.of(
            LabelStyle.of(0xFFFFFFFF, NORMAL_BG));

    private final UiSurface surface = UiSurface.ofSize(40, 10);
    private final UiRoot ui = UiRoot.of(surface);
    private final TextButton button = buttonIn(ui.getRoot(), 2, 2);

    private static TextButton buttonIn(WidgetGroup parent, int x, int y) {
        TextButton created = new TextButton("Go", STYLE);
        created.setPosition(x, y);
        parent.addChild(created);
        return created;
    }

    private int drawnBackground() {
        ui.draw();
        return surface.getSquareGrid().getBackground(3, 2);
    }

    @Test
    void widgetsAreVisibleByDefault() {
        assertTrue(button.isVisible());
    }

    @Test
    void hiddenWidgetIsNotDrawnAndShowingDrawsItAgain() {
        assertEquals(NORMAL_BG, drawnBackground());

        button.setVisible(false);
        assertNotEquals(NORMAL_BG, drawnBackground());

        button.setVisible(true);
        assertEquals(NORMAL_BG, drawnBackground());
    }

    @Test
    void descendantsOfAHiddenGroupAreNotDrawn() {
        WidgetGroup group = new WidgetGroup();
        ui.getRoot().addChild(group);
        buttonIn(group, 20, 2);
        group.setVisible(false);

        ui.draw();

        assertNotEquals(NORMAL_BG, surface.getSquareGrid().getBackground(21, 2));
    }

    @Test
    void changingVisibilityRequestsARedrawAndSameValueDoesNot() {
        ui.draw();

        button.setVisible(true);
        assertFalse(button.isRedrawNeeded());

        button.setVisible(false);
        assertTrue(button.isRedrawNeeded());
        assertTrue(ui.getRoot().isRedrawNeeded());
    }

    @Test
    void hiddenWidgetKeepsItsLayoutSpace() {
        int widthBefore = ui.getRoot().getPreferredWidth();
        int heightBefore = ui.getRoot().getPreferredHeight();

        button.setVisible(false);

        assertEquals(widthBefore, ui.getRoot().getPreferredWidth());
        assertEquals(heightBefore, ui.getRoot().getPreferredHeight());
        assertEquals(3, button.getWidth());
    }

    @Test
    void hiddenWidgetAndItsDescendantsAreNotHit() {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(20, 2);
        ui.getRoot().addChild(group);
        TextButton inner = buttonIn(group, 0, 0);
        assertSame(button, ui.getRoot().hit(3, 2));
        assertSame(inner, ui.getRoot().hit(21, 2));

        button.setVisible(false);
        group.setVisible(false);

        assertNull(ui.getRoot().hit(3, 2));
        assertNull(ui.getRoot().hit(21, 2));
        assertFalse(ui.pointerDown(3, 2, 0, 0));
    }

    @Test
    void hiddenWidgetsAreSkippedByNavigation() {
        TextButton second = buttonIn(ui.getRoot(), 10, 2);
        WidgetGroup group = new WidgetGroup();
        ui.getRoot().addChild(group);
        buttonIn(group, 20, 2);
        TextButton last = buttonIn(ui.getRoot(), 30, 2);
        button.setVisible(false);
        group.setVisible(false);

        ui.moveFocus(FocusDirection.NEXT);
        assertSame(second, ui.getKeyboardFocus());
        ui.moveFocus(FocusDirection.NEXT);
        assertSame(last, ui.getKeyboardFocus());
        ui.moveFocus(FocusDirection.NEXT);
        assertSame(second, ui.getKeyboardFocus());
    }

    @Test
    void hiddenFocusOwnerHasNoFocus() {
        ui.setKeyboardFocus(button);
        assertSame(button, ui.getKeyboardFocus());

        button.setVisible(false);

        assertNull(ui.getKeyboardFocus());
        assertFalse(ui.keyDown(Input.Keys.ENTER));
        button.setVisible(true);
        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void removedAndReAddedFocusOwnerDoesNotGetFocusBack() {
        ui.setKeyboardFocus(button);
        ui.getRoot().removeChild(button);
        assertNull(ui.getKeyboardFocus());

        ui.getRoot().addChild(button);

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void settingFocusOnAHiddenWidgetIsAcceptedButReadsAsNoFocus() {
        button.setVisible(false);

        ui.setKeyboardFocus(button);

        assertNull(ui.getKeyboardFocus());
        button.setVisible(true);
        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void staleHoverIsClearedBeforeDrawingSoAReshownWidgetHasNoHoverLook() {
        ui.pointerMoved(3, 2);
        assertTrue(button.isPointerOver());

        button.setVisible(false);
        ui.draw();
        button.setVisible(true);

        assertFalse(button.isPointerOver());
        assertEquals(NORMAL_BG, drawnBackground());
    }

    @Test
    void staleCaptureIsCanceledBeforeUpdateSoAReshownWidgetIsNotPressed() {
        ui.pointerDown(3, 2, 0, 0);
        assertTrue(button.isPressed());

        button.setVisible(false);
        ui.update(0f);
        button.setVisible(true);

        assertFalse(button.isPressed());
        assertFalse(ui.pointerUp(3, 2, 0, 0));
        assertEquals(NORMAL_BG, drawnBackground());
    }

    @Test
    void hiddenChildIsSkippedByTheGroupEvenIfItOverridesHit() {
        WidgetGroup group = new WidgetGroup();
        ui.getRoot().addChild(group);
        TextButton sneaky = new TextButton("Go", STYLE) {
            @Override
            public Widget hit(int column, int row) {
                return this;
            }
        };
        group.addChild(sneaky);
        sneaky.setVisible(false);

        assertNull(group.hit(1, 0));
    }

    @Test
    void hiddenAncestorRemovesFocusAndHover() {
        WidgetGroup group = new WidgetGroup();
        ui.getRoot().addChild(group);
        TextButton inner = buttonIn(group, 20, 2);
        ui.setKeyboardFocus(inner);
        ui.pointerMoved(21, 2);
        assertSame(inner, ui.getHoveredWidget());

        group.setVisible(false);

        assertNull(ui.getKeyboardFocus());
        assertNull(ui.getHoveredWidget());
    }

    @Test
    void hoverMovesOffAHiddenWidgetOnTheNextPointerMove() {
        ui.pointerMoved(3, 2);
        assertTrue(button.isPointerOver());

        button.setVisible(false);
        ui.pointerMoved(3, 2);

        assertFalse(button.isPointerOver());
        assertNull(ui.getHoveredWidget());
    }

    @Test
    void hidingACapturingWidgetCancelsThePress() {
        ui.pointerDown(3, 2, 0, 0);
        assertTrue(button.isPressed());

        button.setVisible(false);
        boolean handled = ui.pointerDragged(20, 8, 0);

        assertFalse(handled);
        assertFalse(button.isPressed());
        assertFalse(ui.pointerUp(3, 2, 0, 0));
    }
}
