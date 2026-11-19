package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuBarTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final ButtonStyle TALL = style(UiFont.TALL);
    private static final ButtonStyle SQUARE = style(UiFont.SQUARE);
    private static final int POPUP_CATCHER_INDEX = 0;
    private static final int POPUP_DROP_DOWN_INDEX = 1;

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> events = new ArrayList<>();
    private final MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(TALL, TALL));
    private final Menu file = bar.addMenu("File");
    private final Menu edit = bar.addMenu("Edit");

    private static ButtonStyle style(UiFont font) {
        return ButtonStyle.of(LabelStyle.of(font, WHITE, 0x111111FF),
                LabelStyle.of(font, WHITE, 0x222222FF), LabelStyle.of(font, WHITE, 0x333333FF),
                LabelStyle.of(font, WHITE, 0x444444FF));
    }

    MenuBarTest() {
        file.addItem("New", () -> events.add("new"));
        file.addItem("Open file", () -> events.add("open " + bar.isOpen()));
        edit.addItem("Copy", () -> events.add("copy"));
        bar.setSize(40, 0);
        ui.getRoot().addChild(bar);
    }

    private void click(int column, int row) {
        ui.pointerDown(column, row, 0, 0);
        ui.pointerUp(column, row, 0, 0);
    }

    private Widget dropDown() {
        return ui.getPopupLayer().getChild(POPUP_DROP_DOWN_INDEX);
    }

    @Test
    void clickOnTitleOpensItsDropDownAndClickOnOpenTitleCloses() {
        assertTrue(ui.pointerDown(1, 0, 0, 0));
        ui.pointerUp(1, 0, 0, 0);

        assertTrue(bar.isOpen());
        assertEquals(2, ui.getPopupLayer().getChildCount());

        assertTrue(ui.pointerDown(1, 0, 0, 0));
        ui.pointerUp(1, 0, 0, 0);

        assertFalse(bar.isOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void pressOnBarBackgroundBesideTitlesClosesTheMenu() {
        click(1, 0);

        assertTrue(ui.pointerDown(30, 0, 0, 0));

        assertFalse(bar.isOpen());
    }

    @Test
    void secondaryButtonOnTitleDoesNotOpen() {
        assertFalse(ui.pointerDown(1, 0, 0, 1));

        assertFalse(bar.isOpen());
    }

    @Test
    void releasingOnTheSameItemRunsItsActionAfterClosing() {
        click(1, 0);

        ui.pointerDown(1, 4, 0, 0);
        ui.pointerUp(1, 5, 0, 0);

        assertEquals(List.of("open false"), events);
        assertFalse(bar.isOpen());
    }

    @Test
    void releasingOnAnotherItemOrOutsideRunsNothing() {
        click(1, 0);

        ui.pointerDown(1, 2, 0, 0);
        ui.pointerUp(1, 4, 0, 0);
        ui.pointerDown(1, 2, 0, 0);
        ui.pointerDragged(30, 8, 0);
        ui.pointerUp(30, 8, 0, 0);

        assertTrue(events.isEmpty());
        assertTrue(bar.isOpen());
    }

    @Test
    void movingOverAnotherTitleWhileOpenSwitchesMenus() {
        click(1, 0);
        assertEquals(0, dropDown().getX());

        ui.pointerMoved(7, 0);

        assertTrue(bar.isOpen());
        assertEquals(6, dropDown().getX());
        assertEquals(2, dropDown().getHeight());
        assertEquals(2, ui.getPopupLayer().getChildCount());
    }

    @Test
    void movingOverNothingWhileClosedOpensNothing() {
        ui.pointerMoved(7, 0);

        assertFalse(bar.isOpen());
    }

    private TextButton focusedButton() {
        TextButton button = new TextButton("Go", TALL);
        button.setPosition(20, 6);
        ui.getRoot().addChild(button);
        click(21, 6);
        assertSame(button, ui.getKeyboardFocus());
        return button;
    }

    @Test
    void escapeClosesAndRestoresTheFocusFromBeforeTheClick() {
        TextButton button = focusedButton();
        click(1, 0);
        assertNotSame(button, ui.getKeyboardFocus());
        assertNotSame(bar, ui.getKeyboardFocus());

        assertTrue(ui.keyDown(Input.Keys.ESCAPE));

        assertFalse(bar.isOpen());
        assertSame(button, ui.getKeyboardFocus());
    }

    @Test
    void choosingAnItemRestoresTheFocusFromBeforeTheClick() {
        TextButton button = focusedButton();
        click(1, 0);

        click(1, 2);

        assertSame(button, ui.getKeyboardFocus());
    }

    @Test
    void outsidePressRestoresTheFocusFromBeforeTheClick() {
        TextButton button = focusedButton();
        click(1, 0);

        ui.pointerDown(30, 10, 0, 0);

        assertFalse(bar.isOpen());
        assertSame(button, ui.getKeyboardFocus());
    }

    @Test
    void clickWithNothingFocusedBeforeLeavesNothingFocusedAfterClosing() {
        click(1, 0);

        bar.close();

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void otherKeysDoNotCloseTheMenu() {
        click(1, 0);

        assertFalse(ui.keyDown(Input.Keys.A));

        assertTrue(bar.isOpen());
    }

    @Test
    void closeClearsFocusWhenThePreviousOwnerLeftTheTree() {
        TextButton button = focusedButton();
        click(1, 0);
        ui.getRoot().removeChild(button);

        bar.close();

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void outsidePressClosesAndDoesNotClickTheWidgetUnderneath() {
        TextButton button = new TextButton("Go", TALL);
        button.setPosition(20, 6);
        button.addClickListener(clicked -> events.add("button"));
        ui.getRoot().addChild(button);
        click(1, 0);

        assertTrue(ui.pointerDown(21, 6, 0, 0));
        ui.pointerUp(21, 6, 0, 0);

        assertFalse(bar.isOpen());
        assertTrue(events.isEmpty());
        click(21, 6);
        assertEquals(List.of("button"), events);
    }

    @Test
    void outsidePressAfterTheBarLeftTheMainTreeClosesTheMenuWithoutAnAction() {
        click(1, 0);
        ui.getRoot().removeChild(bar);

        assertTrue(ui.pointerDown(7, 0, 0, 0));
        ui.pointerUp(7, 0, 0, 0);

        assertFalse(bar.isOpen());
        assertNull(ui.getOpenMenuBar());
        assertEquals(0, ui.getPopupLayer().getChildCount());
        assertTrue(events.isEmpty());
    }

    @Test
    void pressOnDropDownIsNotAnOutsidePress() {
        click(1, 0);

        ui.pointerDown(1, 2, 0, 0);

        assertTrue(bar.isOpen());
    }

    @Test
    void dropDownSitsBelowTheTitleAndIsAsWideAsTheWidestItem() {
        click(1, 0);

        assertEquals(0, dropDown().getX());
        assertEquals(2, dropDown().getY());
        assertEquals(11, dropDown().getWidth());
        assertEquals(4, dropDown().getHeight());
    }

    @Test
    void dropDownMovesLeftInsideTheSurface() {
        MenuBar rightBar = MenuBar.of(ui, MenuBarStyle.of(TALL, TALL));
        Menu menu = rightBar.addMenu("Menu");
        menu.addItem("A very long item", () -> { });
        rightBar.setPosition(30, 6);
        ui.getRoot().addChild(rightBar);

        click(31, 6);

        assertEquals(40 - 18, dropDown().getX());
    }

    @Test
    void dropDownMovesUpOntoAnEvenRowWhenItWouldLeaveTheSurface() {
        MenuBar lowBar = MenuBar.of(ui, MenuBarStyle.of(TALL, TALL));
        Menu menu = lowBar.addMenu("Menu");
        menu.addItem("One", () -> { });
        menu.addItem("Two", () -> { });
        lowBar.setPosition(0, 8);
        ui.getRoot().addChild(lowBar);

        click(1, 8);

        assertEquals(8, dropDown().getY());
    }

    @Test
    void addingAnItemWhileOpenGrowsTheDropDown() {
        click(1, 0);

        file.addItem("A much longer item", () -> { });

        assertEquals(20, dropDown().getWidth());
        assertEquals(6, dropDown().getHeight());
    }

    @Test
    void pressCatcherSitsUnderTheDropDownAndCoversTheSurface() {
        click(1, 0);

        Widget catcher = ui.getPopupLayer().getChild(POPUP_CATCHER_INDEX);

        assertEquals(40, catcher.getWidth());
        assertEquals(12, catcher.getHeight());
    }

    @Test
    void tallBarIsTwoRowsHighAndSnapsToEvenRows() {
        assertEquals(2, bar.getHeight());
        assertEquals(2, bar.getRowSnap());
        assertEquals(2, ui.getRoot().getChild(0).getRowSnap());
    }

    @Test
    void squareBarIsOneRowHighAndItsHeightArgumentIsIgnored() {
        MenuBar squareBar = MenuBar.of(ui, MenuBarStyle.of(SQUARE, SQUARE));
        squareBar.addMenu("File");

        squareBar.setSize(20, 7);

        assertEquals(1, squareBar.getHeight());
        assertEquals(1, squareBar.getRowSnap());
        assertEquals(20, squareBar.getWidth());
    }

    @Test
    void addingMenusWidensTheBarToFitTheTitlesButNeverNarrowsIt() {
        MenuBar fresh = MenuBar.of(ui, MenuBarStyle.of(SQUARE, SQUARE));
        assertEquals(0, fresh.getWidth());

        fresh.addMenu("File");
        fresh.addMenu("Edit");

        assertEquals(12, fresh.getWidth());
        fresh.setSize(30, 0);
        fresh.addMenu("View");
        assertEquals(30, fresh.getWidth());
    }

    @Test
    void getMenuReturnsMenusInOrderAndChecksBounds() {
        assertEquals(2, bar.getMenuCount());
        assertSame(file, bar.getMenu(0));
        assertEquals("Edit", bar.getMenu(1).getTitle());
        assertEquals(2, file.getItemCount());
        assertThrows(IndexOutOfBoundsException.class, () -> bar.getMenu(2));
        assertThrows(IndexOutOfBoundsException.class, () -> bar.getMenu(-1));
    }

    @Test
    void closeWhenClosedDoesNothing() {
        bar.close();

        assertFalse(bar.isOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void closeMethodRemovesTheDropDown() {
        click(1, 0);

        bar.close();

        assertFalse(bar.isOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void nullArgumentsAreRejected() {
        MenuBarStyle style = MenuBarStyle.of(TALL, TALL);

        assertThrows(NullPointerException.class, () -> MenuBar.of(null, style));
        assertThrows(NullPointerException.class, () -> MenuBar.of(ui, null));
        assertThrows(NullPointerException.class, () -> bar.addMenu(null));
        assertThrows(NullPointerException.class, () -> file.addItem(null, () -> { }));
        assertThrows(NullPointerException.class, () -> file.addItem("x", null));
    }

    @Test
    void actionCannotCallInputMethodsOfTheRoot() {
        file.addItem("Bad", () -> ui.pointerMoved(0, 0));
        click(1, 0);

        ui.pointerDown(1, 6, 0, 0);

        assertThrows(IllegalStateException.class, () -> ui.pointerUp(1, 6, 0, 0));
    }
}
