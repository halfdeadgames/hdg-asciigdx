package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UiRootModalTest {

    private static final LabelStyle LOOK = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x111111FF);
    private static final LabelStyle TALL_LOOK = LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x111111FF);
    private static final ButtonStyle BUTTON = ButtonStyle.of(LOOK, LOOK, LOOK, LOOK);
    private static final ButtonStyle TALL_BUTTON =
            ButtonStyle.of(TALL_LOOK, TALL_LOOK, TALL_LOOK, TALL_LOOK);
    private static final WindowStyle WINDOW_STYLE =
            WindowStyle.of(BoxStyle.SINGLE, LOOK, LOOK, 0x333333FF);
    private static final int NO_DIM = 0x000000FF;

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final TerminalGrid grid = surface.getSquareGrid();
    private final TextButton rootFirst = new TextButton("Go", BUTTON);
    private final TextButton rootSecond = new TextButton("Go", BUTTON);
    private final TextButton windowFirst = new TextButton("Go", BUTTON);
    private final TextButton windowSecond = new TextButton("Go", BUTTON);
    private final Window modal = new Window("A", WINDOW_STYLE);

    UiRootModalTest() {
        rootFirst.setPosition(0, 8);
        rootSecond.setPosition(10, 8);
        ui.getRoot().addChild(rootFirst);
        ui.getRoot().addChild(rootSecond);
        WidgetGroup content = new WidgetGroup();
        windowSecond.setPosition(5, 0);
        content.addChild(windowFirst);
        content.addChild(windowSecond);
        modal.setContent(content);
        modal.setSize(14, 6);
        modal.setPosition(20, 3);
    }

    private void show() {
        ui.showModal(modal, null, NO_DIM, 0f);
        ui.draw();
    }

    private static Window windowWith(Widget content, int x) {
        Window window = new Window("B", WINDOW_STYLE);
        window.setContent(content);
        window.pack();
        window.setPosition(x, 3);
        return window;
    }

    // Presses and hover

    @Test
    void pressOutsideIsSwallowedAndFocusStaysUnchanged() {
        show();
        ui.setKeyboardFocus(windowSecond);

        assertTrue(ui.pointerDown(1, 8, 0, 0));

        assertFalse(rootFirst.isPressed());
        assertSame(windowSecond, ui.getKeyboardFocus());
        assertFalse(ui.pointerUp(1, 8, 0, 0));
    }

    @Test
    void pressOutsideTheSurfaceIsSwallowedToo() {
        show();

        assertTrue(ui.pointerDown(-5, -5, 0, 0));
    }

    @Test
    void pressInsideTheModalWorks() {
        show();

        assertTrue(ui.pointerDown(22, 4, 0, 0));

        assertTrue(windowFirst.isPressed());
    }

    @Test
    void pressOnAPopupStillWorks() {
        TextButton popup = new TextButton("P", BUTTON);
        ui.getPopupLayer().addChild(popup);
        show();

        assertTrue(ui.pointerDown(1, 0, 0, 0));

        assertTrue(popup.isPressed());
    }

    @Test
    void widgetsOutsideNeverBecomeHovered() {
        show();

        ui.pointerMoved(1, 8);

        assertNull(ui.getHoveredWidget());
        assertFalse(rootFirst.isPointerOver());
    }

    @Test
    void showingTheModalClearsTheHoverOfWidgetsOutside() {
        ui.pointerMoved(1, 8);
        assertTrue(rootFirst.isPointerOver());

        show();

        assertFalse(rootFirst.isPointerOver());
        assertNull(ui.getHoveredWidget());
    }

    @Test
    void widgetsInsideTheModalAreHovered() {
        show();

        ui.pointerMoved(22, 4);

        assertSame(windowFirst, ui.getHoveredWidget());
    }

    @Test
    void scrollOutsideIsIgnoredAndScrollInsideWorks() {
        AtomicInteger outside = new AtomicInteger();
        AtomicInteger inside = new AtomicInteger();
        rootFirst.addListener(event -> countScroll(event, outside));
        windowFirst.addListener(event -> countScroll(event, inside));
        ui.pointerMoved(1, 8);
        show();

        ui.pointerMoved(1, 8);
        ui.scrolled(0f, 1f);
        ui.pointerMoved(22, 4);
        ui.scrolled(0f, 1f);

        assertEquals(0, outside.get());
        assertEquals(1, inside.get());
    }

    private static boolean countScroll(UiEvent event, AtomicInteger counter) {
        if (event.getType() == UiEventType.SCROLLED) {
            counter.incrementAndGet();
        }
        return true;
    }

    // Keys and focus navigation

    @Test
    void tabStaysInsideTheModalAndWraps() {
        ui.setKeyboardFocus(rootFirst);
        show();
        assertSame(windowFirst, ui.getKeyboardFocus());

        ui.keyDown(Input.Keys.TAB);
        assertSame(windowSecond, ui.getKeyboardFocus());
        ui.keyDown(Input.Keys.TAB);
        assertSame(windowFirst, ui.getKeyboardFocus());
    }

    @Test
    void moveFocusFromOutsideTheModalEntersIt() {
        show();
        ui.setKeyboardFocus(rootSecond);

        assertTrue(ui.moveFocus(FocusDirection.NEXT));

        assertSame(windowFirst, ui.getKeyboardFocus());
    }

    @Test
    void unhandledKeyListenersAreNotCalledWhileAModalIsShown() {
        AtomicInteger calls = new AtomicInteger();
        ui.addUnhandledKeyListener(event -> {
            calls.incrementAndGet();
            return false;
        });
        ui.keyDown(Input.Keys.A);
        assertEquals(1, calls.get());

        show();
        ui.keyDown(Input.Keys.A);
        assertEquals(1, calls.get());

        ui.hideModal(modal);
        ui.keyDown(Input.Keys.A);
        assertEquals(2, calls.get());
    }

    @Test
    void keyObserversStillSeeKeysWhileAModalIsShown() {
        AtomicInteger calls = new AtomicInteger();
        ui.addKeyObserver(event -> {
            calls.incrementAndGet();
            return false;
        });
        show();

        ui.keyDown(Input.Keys.A);

        assertEquals(1, calls.get());
    }

    @Test
    void anAltShortcutCannotOpenAMenuWhileAModalIsShown() {
        MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(TALL_BUTTON, TALL_BUTTON));
        bar.addMenu("File", 'F').addItem("New", () -> { });
        bar.setSize(40, 0);
        ui.getRoot().addChild(bar);
        show();

        ui.keyDown(Input.Keys.ALT_LEFT);
        ui.keyDown(Input.Keys.F);

        assertFalse(bar.isOpen());
    }

    @Test
    void anOpenMenuIsClosedWhenTheModalIsShown() {
        MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(TALL_BUTTON, TALL_BUTTON));
        bar.addMenu("File").addItem("New", () -> { });
        bar.setSize(40, 0);
        ui.getRoot().addChild(bar);
        ui.pointerDown(1, 0, 0, 0);
        ui.pointerUp(1, 0, 0, 0);
        assertTrue(bar.isOpen());

        show();

        assertFalse(bar.isOpen());
        assertNull(ui.getOpenMenuBar());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    // Initial focus and restore

    @Test
    void focusMovesToTheInitialWidgetWhenItIsInsideTheModal() {
        ui.showModal(modal, windowSecond, NO_DIM, 0f);

        assertSame(windowSecond, ui.getKeyboardFocus());
    }

    @Test
    void focusMovesToTheFirstFocusableWidgetWhenTheInitialOneIsOutside() {
        ui.setKeyboardFocus(rootSecond);

        ui.showModal(modal, rootFirst, NO_DIM, 0f);

        assertSame(windowFirst, ui.getKeyboardFocus());
    }

    @Test
    void focusIsClearedWhenTheModalHasNothingFocusable() {
        Window empty = new Window("E", WINDOW_STYLE);
        empty.setSize(8, 4);
        ui.setKeyboardFocus(rootFirst);

        ui.showModal(empty, null, NO_DIM, 0f);

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void hidingRestoresTheFocusFromBeforeTheModal() {
        ui.setKeyboardFocus(rootSecond);
        show();

        ui.hideModal(modal);

        assertSame(rootSecond, ui.getKeyboardFocus());
        assertFalse(ui.isModalShown());
        assertEquals(0, ui.getWindowLayer().getChildCount());
    }

    @Test
    void hidingClearsFocusWhenTheSavedWidgetIsGone() {
        ui.setKeyboardFocus(rootSecond);
        show();
        ui.getRoot().removeChild(rootSecond);

        ui.hideModal(modal);

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void aModalRemovedByHandCanBeShownAgain() {
        show();
        ui.getWindowLayer().removeChild(modal);

        ui.showModal(modal, null, NO_DIM, 0f);

        assertSame(modal, ui.getTopModal());
        assertSame(ui.getWindowLayer(), modal.getParent());
    }

    @Test
    void scrollOverEmptySpaceOutsideTheModalIsConsumed() {
        show();
        ui.pointerMoved(30, 10);
        assertNull(ui.getHoveredWidget());

        assertTrue(ui.scrolled(0f, 1f));
    }

    @Test
    void scrollWithoutAWholeStepIsNotConsumedWhileAModalIsShown() {
        show();
        ui.pointerMoved(30, 10);

        assertFalse(ui.scrolled(0f, 0.25f));
    }

    @Test
    void nestedModalsRestoreFocusInOrder() {
        TextButton innerButton = new TextButton("In", BUTTON);
        Window inner = windowWith(innerButton, 2);
        ui.setKeyboardFocus(rootFirst);
        ui.showModal(modal, null, NO_DIM, 0f);
        ui.showModal(inner, null, NO_DIM, 0f);
        assertSame(innerButton, ui.getKeyboardFocus());
        assertSame(inner, ui.getTopModal());

        ui.hideModal(inner);
        assertSame(windowFirst, ui.getKeyboardFocus());
        assertSame(modal, ui.getTopModal());

        ui.hideModal(modal);
        assertSame(rootFirst, ui.getKeyboardFocus());
        assertFalse(ui.isModalShown());
    }

    @Test
    void aModalRemovedFromTheLayerIsDroppedFromTheStack() {
        ui.setKeyboardFocus(rootFirst);
        show();

        ui.getWindowLayer().removeChild(modal);
        ui.update(0f);

        assertFalse(ui.isModalShown());
        assertSame(rootFirst, ui.getKeyboardFocus());
        assertTrue(ui.pointerDown(1, 8, 0, 0));
        assertTrue(rootFirst.isPressed());
    }

    @Test
    void closingTheModalDropsItFromTheStack() {
        show();

        modal.close();
        ui.update(0f);

        assertFalse(ui.isModalShown());
    }

    // Drawing

    @Test
    void theDimPassCoversEverythingBelowTheModalAndNotTheModal() {
        ui.draw();
        assertEquals(0x111111FF, grid.getBackground(0, 8));

        ui.showModal(modal, null, NO_DIM, 1f);
        ui.draw();

        assertEquals(0x000000FF, grid.getBackground(0, 8));
        assertEquals('┌', grid.getCodePoint(20, 3));
    }

    @Test
    void noDimPassRunsWhenTheAmountIsZero() {
        show();

        assertEquals(0x111111FF, grid.getBackground(0, 8));
    }

    // Arguments

    @Test
    void argumentsAreChecked() {
        assertThrows(NullPointerException.class, () -> ui.showModal(null, null, NO_DIM, 0f));
        assertThrows(NullPointerException.class, () -> ui.hideModal(null));
        assertThrows(IllegalArgumentException.class,
                () -> ui.showModal(modal, null, NO_DIM, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ui.showModal(modal, null, NO_DIM, -0.1f));
        assertThrows(IllegalArgumentException.class,
                () -> ui.showModal(modal, null, NO_DIM, 1.1f));
        assertFalse(ui.isModalShown());
        assertThrows(IllegalStateException.class, ui::getTopModal);
    }

    @Test
    void showingTheSameModalTwiceIsRejectedAndHidingAnUnknownOneIsIgnored() {
        show();

        assertThrows(IllegalArgumentException.class, this::show);
        ui.hideModal(windowWith(new TextButton("X", BUTTON), 2));

        assertSame(modal, ui.getTopModal());
    }

    @Test
    void showingMovesAModalOutOfItsPreviousParent() {
        WidgetGroup other = new WidgetGroup();
        ui.getRoot().addChild(other);
        other.addChild(modal);

        show();

        assertSame(ui.getWindowLayer(), modal.getParent());
        assertEquals(0, other.getChildCount());
    }
}
