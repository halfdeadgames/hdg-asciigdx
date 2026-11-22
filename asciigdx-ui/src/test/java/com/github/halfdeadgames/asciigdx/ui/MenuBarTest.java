package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuBarTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final ButtonStyle LOOK = style();
    private static final int POPUP_CATCHER_INDEX = 0;
    private static final int POPUP_DROP_DOWN_INDEX = 1;

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> events = new ArrayList<>();
    private final MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(LOOK, LOOK));
    private final Menu file = bar.addMenu("File");
    private final Menu edit = bar.addMenu("Edit");

    private static ButtonStyle style() {
        return ButtonStyle.of(LabelStyle.of(WHITE, 0x111111FF), LabelStyle.of(WHITE, 0x222222FF),
                LabelStyle.of(WHITE, 0x333333FF), LabelStyle.of(WHITE, 0x444444FF));
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

        ui.pointerDown(1, 2, 0, 0);
        ui.pointerUp(3, 2, 0, 0);

        assertEquals(List.of("open false"), events);
        assertFalse(bar.isOpen());
    }

    @Test
    void releasingOnAnotherItemOrOutsideRunsNothing() {
        click(1, 0);

        ui.pointerDown(1, 1, 0, 0);
        ui.pointerUp(1, 2, 0, 0);
        ui.pointerDown(1, 1, 0, 0);
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
        assertEquals(4, dropDown().getX());
        assertEquals(1, dropDown().getHeight());
        assertEquals(2, ui.getPopupLayer().getChildCount());
    }

    @Test
    void movingOverNothingWhileClosedOpensNothing() {
        ui.pointerMoved(7, 0);

        assertFalse(bar.isOpen());
    }

    private TextButton focusedButton() {
        TextButton button = new TextButton("Go", LOOK);
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

        click(1, 1);

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
        TextButton button = new TextButton("Go", LOOK);
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
    void dropDownSitsBelowTheTitleAndIsAsWideAsTheWidestItemRoundedUpToCells() {
        click(1, 0);

        assertEquals(0, dropDown().getX());
        assertEquals(1, dropDown().getY());
        assertEquals(7, dropDown().getWidth());
        assertEquals(2, dropDown().getHeight());
    }

    @Test
    void titleWidthIsHalfTheLettersRoundedUpPlusPaddingOnBothSides() {
        MenuBar fresh = MenuBar.of(ui, MenuBarStyle.of(LOOK, LOOK));

        fresh.addMenu("Four");
        assertEquals(4, fresh.getPreferredWidth());
        fresh.addMenu("Odd");
        assertEquals(8, fresh.getPreferredWidth());
    }

    @Test
    void dropDownMovesLeftInsideTheSurface() {
        MenuBar rightBar = MenuBar.of(ui, MenuBarStyle.of(LOOK, LOOK));
        Menu menu = rightBar.addMenu("Menu");
        menu.addItem("A very long item", () -> { });
        rightBar.setPosition(36, 6);
        ui.getRoot().addChild(rightBar);

        click(37, 6);

        assertEquals(40 - 10, dropDown().getX());
    }

    @Test
    void dropDownMovesUpWhenItWouldLeaveTheSurface() {
        MenuBar lowBar = MenuBar.of(ui, MenuBarStyle.of(LOOK, LOOK));
        Menu menu = lowBar.addMenu("Menu");
        menu.addItem("One", () -> { });
        menu.addItem("Two", () -> { });
        lowBar.setPosition(0, 11);
        ui.getRoot().addChild(lowBar);

        click(1, 11);

        assertEquals(10, dropDown().getY());
    }

    @Test
    void addingAnItemWhileOpenGrowsTheDropDown() {
        click(1, 0);

        file.addItem("A much longer item", () -> { });

        assertEquals(11, dropDown().getWidth());
        assertEquals(3, dropDown().getHeight());
    }

    @Test
    void pressCatcherSitsUnderTheDropDownAndCoversTheSurface() {
        click(1, 0);

        Widget catcher = ui.getPopupLayer().getChild(POPUP_CATCHER_INDEX);

        assertEquals(40, catcher.getWidth());
        assertEquals(12, catcher.getHeight());
    }

    @Test
    void barIsOneRowHighAndItsHeightArgumentIsIgnored() {
        assertEquals(1, bar.getHeight());

        bar.setSize(20, 7);

        assertEquals(1, bar.getHeight());
        assertEquals(20, bar.getWidth());
    }

    @Test
    void addingMenusWidensTheBarToFitTheTitlesButNeverNarrowsIt() {
        MenuBar fresh = MenuBar.of(ui, MenuBarStyle.of(LOOK, LOOK));
        assertEquals(0, fresh.getWidth());

        fresh.addMenu("File");
        fresh.addMenu("Edit");

        assertEquals(8, fresh.getWidth());
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
        MenuBarStyle style = MenuBarStyle.of(LOOK, LOOK);

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

        ui.pointerDown(1, 3, 0, 0);

        assertThrows(IllegalStateException.class, () -> ui.pointerUp(1, 3, 0, 0));
    }

    private static final int RED = 0xFF0000FF;
    private static final int UNDERSCORE = '_';
    // Text column of the trigger letter: padding of 1 cell is 2 text columns, then the letter's
    // index in the text. 'i' in "File" and 'p' in "Open" are both at index 1, an odd text column.
    private static final int TITLE_TRIGGER_TEXT_COLUMN = 3;
    private static final int ITEM_TRIGGER_TEXT_COLUMN = 3;

    private MenuBar underlinedBar(ButtonStyle look, boolean underline, boolean colored) {
        MenuBarStyle style = MenuBarStyle.of(look, look).withTriggerUnderline(underline);
        MenuBar underlinedBar = MenuBar.of(ui, colored ? style.withTriggerRgba(RED) : style);
        Menu fileMenu = underlinedBar.addMenu("File", 'i');
        fileMenu.addItem("Open", 'p', () -> { });
        underlinedBar.setSize(40, 0);
        ui.getRoot().removeChild(bar);
        ui.getRoot().addChild(underlinedBar);
        return underlinedBar;
    }

    @Test
    void titleTriggerIsUnderlinedInTheTriggerColorAtItsTextColumnOnTheSameRow() {
        underlinedBar(LOOK, true, true).draw(surface);

        TerminalGrid overlay = surface.getOverlayGrid();
        assertEquals(UNDERSCORE, overlay.getCodePoint(TITLE_TRIGGER_TEXT_COLUMN, 0));
        assertEquals(RED, overlay.getForeground(TITLE_TRIGGER_TEXT_COLUMN, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT,
                overlay.getCodePoint(TITLE_TRIGGER_TEXT_COLUMN - 1, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT,
                overlay.getCodePoint(TITLE_TRIGGER_TEXT_COLUMN + 1, 0));
    }

    @Test
    void underlinedTitleLetterKeepsTheLookForegroundInTheTextGrid() {
        underlinedBar(LOOK, true, false).draw(surface);

        assertEquals('i', surface.getTextGrid().getCodePoint(TITLE_TRIGGER_TEXT_COLUMN, 0));
        assertEquals(WHITE, surface.getTextGrid().getForeground(TITLE_TRIGGER_TEXT_COLUMN, 0));
    }

    @Test
    void underlineUsesTheLookForegroundWithoutATriggerColor() {
        underlinedBar(LOOK, true, false).draw(surface);

        assertEquals(WHITE, surface.getOverlayGrid().getForeground(TITLE_TRIGGER_TEXT_COLUMN, 0));
        assertEquals(UNDERSCORE,
                surface.getOverlayGrid().getCodePoint(TITLE_TRIGGER_TEXT_COLUMN, 0));
    }

    @Test
    void noUnderlineIsWrittenWhenTheStyleHasItOff() {
        MenuBar plainBar = underlinedBar(LOOK, false, true);
        plainBar.draw(surface);
        click(1, 0);
        dropDown().draw(surface);

        for (int row = 0; row < surface.getRows(); row++) {
            for (int textColumn = 0; textColumn < surface.getTextColumns(); textColumn++) {
                assertEquals(TerminalGrid.EMPTY_CODE_POINT,
                        surface.getOverlayGrid().getCodePoint(textColumn, row));
            }
        }
    }

    @Test
    void itemTriggerIsUnderlinedAtItsTextColumnOnTheItemRow() {
        underlinedBar(LOOK, true, true);
        click(1, 0);
        MenuItemList openDropDown = (MenuItemList) dropDown();
        openDropDown.draw(surface);

        TerminalGrid overlay = surface.getOverlayGrid();
        int itemRow = openDropDown.getY();
        assertEquals(UNDERSCORE, overlay.getCodePoint(ITEM_TRIGGER_TEXT_COLUMN, itemRow));
        assertEquals(RED, overlay.getForeground(ITEM_TRIGGER_TEXT_COLUMN, itemRow));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT,
                overlay.getCodePoint(ITEM_TRIGGER_TEXT_COLUMN - 1, itemRow));
    }

    @Test
    void titlesAndItemsAreWrittenInTheTextGridStartingAfterThePadding() {
        click(1, 0);
        ui.draw();

        assertEquals('F', surface.getTextGrid().getCodePoint(2, 0));
        assertEquals('E', surface.getTextGrid().getCodePoint(10, 0));
        assertEquals('N', surface.getTextGrid().getCodePoint(2, 1));
        assertEquals('O', surface.getTextGrid().getCodePoint(2, 2));
    }
}
