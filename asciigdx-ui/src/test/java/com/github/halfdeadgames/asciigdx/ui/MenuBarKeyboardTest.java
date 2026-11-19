package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuBarKeyboardTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED = 0xFF0000FF;
    private static final int FILE_COLUMN = 0;
    private static final int FORMAT_COLUMN = 6;
    private static final int EDIT_COLUMN = 14;
    private static final int NO_HIGHLIGHT = -1;
    private static final ButtonStyle TALL = style(UiFont.TALL);
    private static final ButtonStyle SQUARE = style(UiFont.SQUARE);

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> events = new ArrayList<>();
    private final MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(TALL, TALL));

    private static ButtonStyle style(UiFont font) {
        return ButtonStyle.of(LabelStyle.of(font, WHITE, 0x111111FF));
    }

    MenuBarKeyboardTest() {
        Menu file = bar.addMenu("File", 'F');
        file.addItem("New", 'n', () -> events.add("new"));
        file.addItem("Open", 'O', () -> events.add("open"));
        file.addItem("Save", 's', () -> events.add("save"));
        file.addItem("Save as", 's', () -> events.add("save as"));
        Menu format = bar.addMenu("Format", 'f');
        format.addItem("Bold", () -> events.add("bold"));
        Menu edit = bar.addMenu("Edit", 'e');
        edit.addItem("Copy", 'c', () -> events.add("copy"));
        bar.setSize(40, 0);
        ui.getRoot().addChild(bar);
    }

    private void press(int keycode) {
        ui.keyDown(keycode);
        ui.keyUp(keycode);
    }

    private void altPress(int keycode) {
        ui.keyDown(Input.Keys.ALT_LEFT);
        ui.keyDown(keycode);
        ui.keyUp(keycode);
        ui.keyUp(Input.Keys.ALT_LEFT);
    }

    private MenuDropDown dropDown() {
        return (MenuDropDown) ui.getPopupLayer().getChild(1);
    }

    private void openFileMenu() {
        altPress(Input.Keys.F);
    }

    @Test
    void invalidTriggersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> bar.addMenu("File", 'x'));
        assertThrows(IllegalArgumentException.class, () -> bar.addMenu("a-b", '-'));
        assertThrows(IllegalArgumentException.class,
                () -> bar.getMenu(0).addItem("New", 'z', () -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> bar.getMenu(0).addItem("a b", ' ', () -> { }));
        assertThrows(NullPointerException.class,
                () -> bar.getMenu(0).addItem("New", 'n', null));
    }

    @Test
    void triggersMatchWithoutRegardToCase() {
        bar.addMenu("Window", 'W');
        bar.addMenu("Help", 'L');
        bar.getMenu(0).addItem("Quit", 'Q', () -> { });
        bar.getMenu(0).addItem("Exit", 'x', () -> { });

        assertEquals(5, bar.getMenuCount());
    }

    @Test
    void altPlusTriggerOpensTheMenuWithNoItemHighlighted() {
        ui.keyDown(Input.Keys.ALT_LEFT);

        assertTrue(ui.keyDown(Input.Keys.F));

        assertTrue(bar.isOpen());
        assertEquals(FILE_COLUMN, dropDown().getX());
        assertEquals(NO_HIGHLIGHT, dropDown().getHighlightedItem());
    }

    @Test
    void altPlusTheSameTriggerCyclesThroughTitlesAndLoops() {
        openFileMenu();
        assertEquals(FILE_COLUMN, dropDown().getX());

        altPress(Input.Keys.F);
        assertEquals(FORMAT_COLUMN, dropDown().getX());

        altPress(Input.Keys.F);
        assertEquals(FILE_COLUMN, dropDown().getX());
    }

    @Test
    void altPlusTriggerWhileClosedAlwaysOpensTheFirstMatchingTitle() {
        ui.pointerDown(15, 0, 0, 0);
        ui.pointerUp(15, 0, 0, 0);
        assertEquals(EDIT_COLUMN, dropDown().getX());
        bar.close();

        altPress(Input.Keys.F);
        assertEquals(FILE_COLUMN, dropDown().getX());
        altPress(Input.Keys.F);
        assertEquals(FORMAT_COLUMN, dropDown().getX());
        altPress(Input.Keys.F);
        assertEquals(FILE_COLUMN, dropDown().getX());

        ui.keyDown(Input.Keys.ESCAPE);
        assertFalse(bar.isOpen());
        altPress(Input.Keys.F);
        assertEquals(FILE_COLUMN, dropDown().getX());
    }

    @Test
    void plainLetterWhileClosedDoesNothing() {
        assertFalse(ui.keyDown(Input.Keys.F));

        assertFalse(bar.isOpen());
    }

    @Test
    void altReleasedClearsTheAltState() {
        ui.keyDown(Input.Keys.ALT_RIGHT);
        ui.keyUp(Input.Keys.ALT_RIGHT);

        assertFalse(ui.keyDown(Input.Keys.F));

        assertFalse(bar.isOpen());
    }

    @Test
    void digitTriggersWorkOnTheNumberRowAndTheNumpad() {
        bar.addMenu("Menu 2", '2');
        bar.addMenu("Page 3", '3');

        altPress(Input.Keys.NUM_2);
        assertTrue(bar.isOpen());
        bar.close();
        altPress(Input.Keys.NUMPAD_3);

        assertTrue(bar.isOpen());
    }

    @Test
    void uniqueItemTriggerWhileOpenRunsTheItemAndCloses() {
        openFileMenu();

        assertTrue(ui.keyDown(Input.Keys.N));

        assertEquals(List.of("new"), events);
        assertFalse(bar.isOpen());
    }

    @Test
    void itemTriggerIsMatchedWithoutRegardToCase() {
        openFileMenu();

        ui.keyDown(Input.Keys.O);

        assertEquals(List.of("open"), events);
    }

    @Test
    void sharedItemTriggerCyclesTheHighlightAndEnterRunsIt() {
        openFileMenu();

        ui.keyDown(Input.Keys.S);
        assertEquals(2, dropDown().getHighlightedItem());
        ui.keyDown(Input.Keys.S);
        assertEquals(3, dropDown().getHighlightedItem());
        ui.keyDown(Input.Keys.S);
        assertEquals(2, dropDown().getHighlightedItem());
        ui.keyDown(Input.Keys.S);
        assertTrue(ui.keyDown(Input.Keys.ENTER));

        assertEquals(List.of("save as"), events);
        assertFalse(bar.isOpen());
    }

    @Test
    void altPlusLetterWithoutItemMatchSwitchesToAnotherMenu() {
        openFileMenu();

        altPress(Input.Keys.E);

        assertTrue(bar.isOpen());
        assertEquals(EDIT_COLUMN, dropDown().getX());
    }

    @Test
    void plainLetterWithoutItemMatchDoesNothing() {
        openFileMenu();

        assertFalse(ui.keyDown(Input.Keys.E));

        assertEquals(FILE_COLUMN, dropDown().getX());
        assertTrue(events.isEmpty());
    }

    @Test
    void upAndDownMoveThroughAllItemsAndWrap() {
        openFileMenu();

        press(Input.Keys.DOWN);
        assertEquals(0, dropDown().getHighlightedItem());
        press(Input.Keys.UP);
        assertEquals(3, dropDown().getHighlightedItem());
        press(Input.Keys.UP);
        assertEquals(2, dropDown().getHighlightedItem());
        press(Input.Keys.DOWN);
        press(Input.Keys.DOWN);
        assertEquals(0, dropDown().getHighlightedItem());
    }

    @Test
    void upWithNoHighlightGoesToTheLastItem() {
        openFileMenu();

        press(Input.Keys.UP);

        assertEquals(3, dropDown().getHighlightedItem());
    }

    @Test
    void enterWithNoHighlightDoesNothing() {
        openFileMenu();

        assertFalse(ui.keyDown(Input.Keys.ENTER));

        assertTrue(bar.isOpen());
        assertTrue(events.isEmpty());
    }

    @Test
    void enterRunsTheHighlightedItem() {
        openFileMenu();
        press(Input.Keys.DOWN);
        press(Input.Keys.DOWN);

        press(Input.Keys.ENTER);

        assertEquals(List.of("open"), events);
    }

    @Test
    void leftAndRightSwitchMenusWrapAndHighlightTheFirstItem() {
        openFileMenu();

        press(Input.Keys.RIGHT);
        assertEquals(FORMAT_COLUMN, dropDown().getX());
        assertEquals(0, dropDown().getHighlightedItem());

        press(Input.Keys.LEFT);
        assertEquals(FILE_COLUMN, dropDown().getX());
        assertEquals(0, dropDown().getHighlightedItem());

        press(Input.Keys.LEFT);
        assertEquals(EDIT_COLUMN, dropDown().getX());
        press(Input.Keys.RIGHT);
        assertEquals(FILE_COLUMN, dropDown().getX());
    }

    @Test
    void pointerHoverMovesTheHighlight() {
        openFileMenu();
        press(Input.Keys.DOWN);

        ui.pointerMoved(1, 4);

        assertEquals(1, dropDown().getHighlightedItem());
    }

    @Test
    void escapeStillClosesTheMenu() {
        openFileMenu();

        assertTrue(ui.keyDown(Input.Keys.ESCAPE));

        assertFalse(bar.isOpen());
    }

    @Test
    void focusedButtonGetsHandledKeysFirstAndOtherKeysReachTheBar() {
        TextButton button = new TextButton("Go", TALL);
        button.setPosition(20, 6);
        button.addClickListener(clicked -> events.add("button"));
        ui.getRoot().addChild(button);
        ui.pointerDown(21, 6, 0, 0);
        ui.pointerUp(21, 6, 0, 0);
        events.clear();

        assertTrue(ui.keyDown(Input.Keys.ENTER));
        assertEquals(List.of("button"), events);
        assertFalse(bar.isOpen());

        altPress(Input.Keys.F);
        assertTrue(bar.isOpen());
    }

    @Test
    void barFoundOutsideTheMainTreeClosesItsMenuOnTheNextKeyWithoutRunningAnAction() {
        openFileMenu();
        press(Input.Keys.DOWN);

        ui.getRoot().removeChild(bar);
        assertTrue(bar.isOpen());

        assertFalse(ui.keyDown(Input.Keys.ENTER));

        assertFalse(bar.isOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
        assertTrue(events.isEmpty());
        altPress(Input.Keys.F);
        assertFalse(bar.isOpen());
    }

    @Test
    void barWorksAgainWhenAddedBackToTheMainTree() {
        ui.getRoot().removeChild(bar);
        altPress(Input.Keys.F);
        assertFalse(bar.isOpen());

        ui.getRoot().addChild(bar);
        altPress(Input.Keys.F);

        assertTrue(bar.isOpen());
    }

    @Test
    void barInThePopupLayerDoesNotReactToKeys() {
        ui.getRoot().removeChild(bar);
        ui.getPopupLayer().addChild(bar);

        altPress(Input.Keys.F);

        assertFalse(bar.isOpen());
    }

    @Test
    void nonAsciiTriggersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> bar.addMenu("Café", 'é'));
        assertThrows(IllegalArgumentException.class,
                () -> bar.getMenu(0).addItem("Café", 'é', () -> { }));
    }

    @Test
    void altReleaseThatAFocusedWidgetConsumesStillClearsTheAltState() {
        TextButton button = new TextButton("Go", TALL);
        button.setPosition(20, 6);
        button.addListener(event -> event.getType() == UiEventType.KEY_UP
                && event.getKeycode() == Input.Keys.ALT_LEFT);
        ui.getRoot().addChild(button);
        ui.pointerDown(21, 6, 0, 0);
        ui.pointerUp(21, 6, 0, 0);

        ui.keyDown(Input.Keys.ALT_LEFT);
        assertTrue(ui.keyUp(Input.Keys.ALT_LEFT));

        assertFalse(ui.keyDown(Input.Keys.F));
        assertFalse(bar.isOpen());
    }

    @Test
    void otherBarsIgnoreKeysWhileOneBarHasAMenuOpen() {
        MenuBar other = MenuBar.of(ui, MenuBarStyle.of(TALL, TALL));
        other.addMenu("Zoom", 'Z').addItem("In", () -> events.add("in"));
        other.setPosition(30, 6);
        ui.getRoot().addChild(other);
        openFileMenu();

        altPress(Input.Keys.Z);

        assertTrue(bar.isOpen());
        assertFalse(other.isOpen());
        bar.close();
        altPress(Input.Keys.Z);
        assertTrue(other.isOpen());
        altPress(Input.Keys.F);
        assertTrue(other.isOpen());
        assertFalse(bar.isOpen());
    }

    @Test
    void triggerLetterIsDrawnInTheTriggerColor() {
        MenuBarStyle colored = MenuBarStyle.of(SQUARE, SQUARE).withTriggerRgba(RED);
        MenuBar squareBar = MenuBar.of(ui, colored);
        squareBar.addMenu("File", 'I');
        squareBar.addMenu("Edit");

        squareBar.draw(surface);

        TerminalGridReader grid = new TerminalGridReader(surface);
        assertEquals(WHITE, grid.foreground(1, 0));
        assertEquals(RED, grid.foreground(2, 0));
        assertEquals(WHITE, grid.foreground(3, 0));
        assertEquals(WHITE, grid.foreground(8, 0));
    }

    @Test
    void withoutATriggerColorTheTriggerLetterLooksLikeTheOthers() {
        MenuBar squareBar = MenuBar.of(ui, MenuBarStyle.of(SQUARE, SQUARE));
        squareBar.addMenu("File", 'I');

        squareBar.draw(surface);

        assertEquals(WHITE, new TerminalGridReader(surface).foreground(2, 0));
    }

    /** Reads cells of the square grid. */
    private static final class TerminalGridReader {
        private final UiSurface readSurface;

        TerminalGridReader(UiSurface readSurface) {
            this.readSurface = readSurface;
        }

        int foreground(int column, int row) {
            return readSurface.getSquareGrid().getForeground(column, row);
        }
    }
}
