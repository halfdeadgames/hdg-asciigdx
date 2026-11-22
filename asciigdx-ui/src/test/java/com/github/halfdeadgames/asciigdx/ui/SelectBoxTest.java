package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SelectBoxTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int NORMAL_BG = 0x111111FF;
    private static final int OVER_BG = 0x222222FF;
    private static final int PRESSED_BG = 0x333333FF;
    private static final int DISABLED_BG = 0x444444FF;
    private static final int RED = 0xFF0000FF;
    private static final int GREEN = 0x00FF00FF;
    private static final int ELLIPSIS = 0x2026;
    private static final int SURFACE_COLUMNS = 40;
    private static final int SURFACE_ROWS = 12;
    private static final int BOX_COLUMN = 2;
    private static final int BOX_ROW = 2;
    private static final int LIST_INDEX = 1;
    private static final List<String> FRUIT = List.of("Apple", "Banana", "Cherry", "Date");
    private static final ButtonStyle LOOK = ButtonStyle.of(LabelStyle.of(WHITE, NORMAL_BG),
            LabelStyle.of(WHITE, OVER_BG), LabelStyle.of(WHITE, PRESSED_BG),
            LabelStyle.of(WHITE, DISABLED_BG));
    private static final SelectBoxStyle STYLE =
            SelectBoxStyle.of(LOOK, LOOK, ScrollPaneStyle.of(0x404040FF, 0x909090FF));

    private final UiSurface surface = UiSurface.ofSize(SURFACE_COLUMNS, SURFACE_ROWS);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> changes = new ArrayList<>();
    private final SelectBox<String> box = SelectBox.of(ui, STYLE);

    SelectBoxTest() {
        box.setItems(FRUIT);
        box.pack();
        box.setPosition(BOX_COLUMN, BOX_ROW);
        box.addChangeListener(source -> changes.add(source.getSelected()));
        ui.getRoot().addChild(box);
    }

    private ScrollPane list() {
        return (ScrollPane) ui.getPopupLayer().getChild(LIST_INDEX);
    }

    private SelectBoxRows rows() {
        return (SelectBoxRows) list().getContent();
    }

    private void click(int column, int row) {
        ui.pointerDown(column, row, 0, 0);
        ui.pointerUp(column, row, 0, 0);
    }

    private void openWithKeyboard() {
        ui.setKeyboardFocus(box);
        ui.keyDown(Input.Keys.ENTER);
    }

    private int rowOfItem(int index) {
        return list().getAbsoluteY() + index;
    }

    // ---- state ----

    @Test
    void newBoxHasNothingSelectedAndItsPreferredSize() {
        assertEquals(SelectBox.NO_SELECTION, box.getSelectedIndex());
        assertNull(box.getSelected());
        assertEquals(6, box.getWidth());
        assertEquals(1, box.getHeight());
    }

    @Test
    void itemsAreCopiedAndTheGetterIsUnmodifiable() {
        List<String> source = new ArrayList<>(FRUIT);
        box.setItems(source);
        source.add("Elderberry");

        assertEquals(FRUIT, box.getItems());
        assertThrows(UnsupportedOperationException.class, () -> box.getItems().add("x"));
    }

    @Test
    void settingItemsRejectsNullAndClearsTheSelection() {
        box.setSelectedIndex(2);

        box.setItems(FRUIT);

        assertEquals(SelectBox.NO_SELECTION, box.getSelectedIndex());
        assertThrows(NullPointerException.class, () -> box.setItems(null));
    }

    @Test
    void selectionByItemAndByIndexAgree() {
        box.setSelected("Cherry");
        assertEquals(2, box.getSelectedIndex());

        box.setSelectedIndex(0);
        assertEquals("Apple", box.getSelected());

        box.setSelected(null);
        assertEquals(SelectBox.NO_SELECTION, box.getSelectedIndex());
    }

    @Test
    void badSelectionArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> box.setSelected("Fig"));
        assertThrows(IndexOutOfBoundsException.class, () -> box.setSelectedIndex(4));
        assertThrows(IndexOutOfBoundsException.class, () -> box.setSelectedIndex(-2));
        assertThrows(IllegalArgumentException.class, () -> box.setMaxVisibleRows(0));
    }

    @Test
    void settingTheSelectionFromCodeCallsNoListener() {
        box.setSelectedIndex(1);
        box.setSelected("Date");

        assertTrue(changes.isEmpty());
    }

    @Test
    void preferredWidthDoesNotDependOnTheSelection() {
        int width = box.getPreferredWidth();

        box.setSelectedIndex(0);
        assertEquals(width, box.getPreferredWidth());
        box.setSelectedIndex(3);
        assertEquals(width, box.getPreferredWidth());
    }

    @Test
    void swatchesAndPlaceholderWidenThePreferredWidth() {
        int plain = box.getPreferredWidth();

        box.setItemSwatch(item -> RED);
        assertEquals(plain + 2, box.getPreferredWidth());

        box.setItemSwatch(null);
        box.setPlaceholder("A placeholder longer than any item");
        assertTrue(box.getPreferredWidth() > plain);
    }

    // ---- drawing ----

    @Test
    void closedBoxDrawsTheChoiceAndTheArrow() {
        box.setSelectedIndex(1);
        ui.draw();

        TerminalGrid textGrid = surface.getTextGrid();
        assertEquals('B', textGrid.getCodePoint(6, BOX_ROW));
        assertEquals('a', textGrid.getCodePoint(7, BOX_ROW));
        assertEquals(SelectBoxStyle.DEFAULT_ARROW_GLYPH,
                surface.getSquareGrid().getCodePoint(BOX_COLUMN + 5, BOX_ROW));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, BOX_ROW));
    }

    @Test
    void nothingSelectedShowsThePlaceholder() {
        box.setPlaceholder("#FF00FF");
        ui.draw();

        assertEquals('#', surface.getTextGrid().getCodePoint(6, BOX_ROW));
        assertEquals('F', surface.getTextGrid().getCodePoint(7, BOX_ROW));
    }

    @Test
    void swatchBlockComesFirstThenOneBlankColumnThenTheText() {
        box.setItemSwatch(item -> item.startsWith("A") ? RED : GREEN);
        box.pack();
        box.setSelectedIndex(0);
        ui.draw();

        assertEquals(RED, surface.getSquareGrid().getBackground(BOX_COLUMN + 1, BOX_ROW));
        TerminalGrid textGrid = surface.getTextGrid();
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(8, BOX_ROW));
        assertEquals('A', textGrid.getCodePoint(9, BOX_ROW));
    }

    @Test
    void boxUsesTheLookOfItsState() {
        ui.draw();
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, BOX_ROW));

        ui.pointerMoved(BOX_COLUMN, BOX_ROW);
        ui.draw();
        assertEquals(OVER_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, BOX_ROW));

        box.setDisabled(true);
        ui.draw();
        assertEquals(DISABLED_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, BOX_ROW));
    }

    @Test
    void openBoxShowsThePressedLook() {
        box.openList();
        ui.draw();

        assertEquals(PRESSED_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, BOX_ROW));
    }

    @Test
    void highlightedRowUsesThePointerOverLookAndOthersTheNormalLook() {
        box.setSelectedIndex(1);
        box.openList();
        ui.draw();

        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, rowOfItem(0)));
        assertEquals(OVER_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, rowOfItem(1)));
    }

    // ---- placement ----

    @Test
    void pressOnTheBoxOpensTheListRightUnderIt() {
        assertTrue(ui.pointerDown(BOX_COLUMN + 1, BOX_ROW, 0, 0));

        assertTrue(box.isListOpen());
        assertEquals(2, ui.getPopupLayer().getChildCount());
        assertEquals(BOX_COLUMN, list().getAbsoluteX());
        assertEquals(BOX_ROW + 1, list().getAbsoluteY());
        assertEquals(4, list().getHeight());
        assertEquals(6, list().getWidth());
    }

    @Test
    void listFlipsUpWhenItDoesNotFitBelow() {
        box.setPosition(BOX_COLUMN, SURFACE_ROWS - 2);

        box.openList();

        assertEquals(4, list().getHeight());
        assertEquals(SURFACE_ROWS - 2 - 4, list().getAbsoluteY());
    }

    @Test
    void listShrinksToTheSideWithMoreRoom() {
        box.setItems(numberedItems(20));
        box.setPosition(BOX_COLUMN, 5);

        box.openList();

        assertEquals(SURFACE_ROWS - 6, list().getHeight());
        assertEquals(6, list().getAbsoluteY());
    }

    @Test
    void listShowsAtMostTheMaximumVisibleRows() {
        box.setItems(numberedItems(20));

        box.openList();
        assertEquals(SelectBox.DEFAULT_MAX_VISIBLE_ROWS, list().getHeight());
        box.closeList();

        box.setMaxVisibleRows(3);
        box.openList();
        assertEquals(3, list().getHeight());
    }

    @Test
    void fitEntriesListIsWiderThanTheBoxAndShiftsLeftToStayOnTheSurface() {
        box.setItems(List.of("A very long entry name", "Short"));
        box.setSize(6, 0);
        box.setPosition(SURFACE_COLUMNS - 6, BOX_ROW);

        box.openList();

        assertEquals(13, list().getWidth());
        assertEquals(SURFACE_COLUMNS - 13, list().getAbsoluteX());
    }

    @Test
    void fitEntriesAddsTheScrollBarColumnWhenItIsShown() {
        box.setItems(List.of("A very long entry name", "Two", "Three"));
        box.setSize(6, 0);
        box.setMaxVisibleRows(2);

        box.openList();

        assertEquals(14, list().getWidth());
    }

    @Test
    void matchButtonListHasTheBoxWidthAndLongEntriesEndWithAnEllipsis() {
        box.setItems(List.of("Extraordinarily long", "Short"));
        box.setSize(6, 0);
        box.setListWidth(SelectBoxListWidth.MATCH_BUTTON);

        box.openList();
        ui.draw();

        assertEquals(6, list().getWidth());
        assertEquals('E', surface.getTextGrid().getCodePoint(6, rowOfItem(0)));
        assertEquals(ELLIPSIS, surface.getTextGrid().getCodePoint(13, rowOfItem(0)));
        assertEquals('S', surface.getTextGrid().getCodePoint(6, rowOfItem(1)));
    }

    @Test
    void listOpensWithTheChoiceHighlightedAndScrolledIntoView() {
        box.setItems(numberedItems(20));
        box.setMaxVisibleRows(5);
        box.setSelectedIndex(15);

        box.openList();

        assertEquals(15, rows().getHighlightedIndex());
        assertEquals(11, list().getScrollRow());
    }

    // ---- keyboard ----

    @Test
    void enterOpensTheFocusedBox() {
        openWithKeyboard();

        assertTrue(box.isListOpen());
    }

    @Test
    void spaceOpensTheFocusedBox() {
        ui.setKeyboardFocus(box);

        assertTrue(ui.keyDown(Input.Keys.SPACE));

        assertTrue(box.isListOpen());
    }

    @Test
    void upAndDownMoveTheHighlightAndStopAtTheEnds() {
        openWithKeyboard();

        ui.keyDown(Input.Keys.DOWN);
        assertEquals(0, rows().getHighlightedIndex());
        ui.keyDown(Input.Keys.DOWN);
        ui.keyDown(Input.Keys.DOWN);
        assertEquals(2, rows().getHighlightedIndex());
        ui.keyDown(Input.Keys.UP);
        assertEquals(1, rows().getHighlightedIndex());
        ui.keyDown(Input.Keys.UP);
        ui.keyDown(Input.Keys.UP);
        assertEquals(0, rows().getHighlightedIndex());
        for (int i = 0; i < 6; i++) {
            ui.keyDown(Input.Keys.DOWN);
        }
        assertEquals(3, rows().getHighlightedIndex());
    }

    @Test
    void movingTheHighlightScrollsItIntoView() {
        box.setItems(numberedItems(20));
        box.setMaxVisibleRows(3);
        openWithKeyboard();

        for (int i = 0; i < 5; i++) {
            ui.keyDown(Input.Keys.DOWN);
        }

        assertEquals(4, rows().getHighlightedIndex());
        assertEquals(2, list().getScrollRow());
    }

    @Test
    void enterPicksTheHighlightedItemClosesAndFiresTheListener() {
        openWithKeyboard();
        ui.keyDown(Input.Keys.DOWN);
        ui.keyDown(Input.Keys.DOWN);

        assertTrue(ui.keyDown(Input.Keys.ENTER));

        assertEquals(1, box.getSelectedIndex());
        assertFalse(box.isListOpen());
        assertEquals(List.of("Banana"), changes);
        assertSame(box, ui.getKeyboardFocus());
    }

    @Test
    void escapeClosesWithoutChangingAnything() {
        box.setSelectedIndex(1);
        openWithKeyboard();
        ui.keyDown(Input.Keys.DOWN);

        assertTrue(ui.keyDown(Input.Keys.ESCAPE));

        assertFalse(box.isListOpen());
        assertEquals(1, box.getSelectedIndex());
        assertTrue(changes.isEmpty());
        assertEquals(0, ui.getPopupLayer().getChildCount());
        assertSame(box, ui.getKeyboardFocus());
    }

    @Test
    void typingALetterJumpsToTheNextEntryThatStartsWithItAndWraps() {
        box.setItems(List.of("Apple", "Banana", "avocado", "Cherry"));
        openWithKeyboard();

        ui.keyTyped('c');
        assertEquals(3, rows().getHighlightedIndex());
        ui.keyTyped('A');
        assertEquals(0, rows().getHighlightedIndex());
        ui.keyTyped('a');
        assertEquals(2, rows().getHighlightedIndex());
        ui.keyTyped('a');
        assertEquals(0, rows().getHighlightedIndex());
    }

    @Test
    void typingALetterWithoutAMatchKeepsTheHighlight() {
        openWithKeyboard();
        ui.keyDown(Input.Keys.DOWN);

        assertFalse(ui.keyTyped('z'));

        assertEquals(0, rows().getHighlightedIndex());
    }

    // ---- pointer ----

    @Test
    void clickOnARowPicksIt() {
        box.openList();

        ui.pointerDown(BOX_COLUMN + 1, rowOfItem(2), 0, 0);
        ui.pointerUp(BOX_COLUMN + 1, rowOfItem(2), 0, 0);

        assertEquals(2, box.getSelectedIndex());
        assertFalse(box.isListOpen());
        assertEquals(List.of("Cherry"), changes);
    }

    @Test
    void pressedRowShowsThePressedLook() {
        box.openList();

        ui.pointerDown(BOX_COLUMN + 1, rowOfItem(1), 0, 0);
        ui.draw();

        assertEquals(PRESSED_BG, surface.getSquareGrid().getBackground(BOX_COLUMN, rowOfItem(1)));
    }

    @Test
    void releaseOnAnotherRowPicksNothing() {
        box.openList();

        ui.pointerDown(BOX_COLUMN + 1, rowOfItem(0), 0, 0);
        ui.pointerUp(BOX_COLUMN + 1, rowOfItem(1), 0, 0);

        assertTrue(box.isListOpen());
        assertEquals(SelectBox.NO_SELECTION, box.getSelectedIndex());
    }

    @Test
    void pointerOverARowHighlightsIt() {
        box.openList();

        ui.pointerMoved(BOX_COLUMN + 1, rowOfItem(3));

        assertEquals(3, rows().getHighlightedIndex());
    }

    @Test
    void pressOutsideTheListClosesItAndIsConsumed() {
        box.setSelectedIndex(1);
        box.openList();

        assertTrue(ui.pointerDown(30, 9, 0, 0));

        assertFalse(box.isListOpen());
        assertEquals(1, box.getSelectedIndex());
        assertTrue(changes.isEmpty());
    }

    @Test
    void pressOnTheBoxWhileOpenClosesTheList() {
        box.openList();

        assertTrue(ui.pointerDown(BOX_COLUMN + 1, BOX_ROW, 0, 0));
        ui.pointerUp(BOX_COLUMN + 1, BOX_ROW, 0, 0);

        assertFalse(box.isListOpen());
    }

    @Test
    void secondaryButtonOnTheBoxDoesNotOpen() {
        assertFalse(ui.pointerDown(BOX_COLUMN + 1, BOX_ROW, 0, 1));

        assertFalse(box.isListOpen());
    }

    @Test
    void pressOutsideDoesNotClickTheWidgetUnderneath() {
        List<String> clicks = new ArrayList<>();
        TextButton underneath = new TextButton("Go", LOOK);
        underneath.setPosition(20, 8);
        underneath.addClickListener(clicked -> clicks.add("go"));
        ui.getRoot().addChild(underneath);
        box.openList();

        click(21, 8);

        assertTrue(clicks.isEmpty());
        assertFalse(box.isListOpen());
    }

    // ---- change listener ----

    @Test
    void listenerFiresOnlyWhenTheUserPicksADifferentItem() {
        box.setSelectedIndex(1);

        box.openList();
        click(BOX_COLUMN + 1, rowOfItem(1));
        assertTrue(changes.isEmpty());

        box.openList();
        click(BOX_COLUMN + 1, rowOfItem(2));
        assertEquals(List.of("Cherry"), changes);
    }

    @Test
    void removedListenerIsNotCalled() {
        ChangeListener<SelectBox<String>> extra = source -> changes.add("extra");
        box.addChangeListener(extra);
        box.removeChangeListener(extra);

        box.openList();
        click(BOX_COLUMN + 1, rowOfItem(0));

        assertEquals(List.of("Apple"), changes);
    }

    // ---- wheel ----

    @Test
    void wheelOverTheClosedBoxIsNotConsumed() {
        ui.pointerMoved(BOX_COLUMN + 1, BOX_ROW);

        assertFalse(ui.scrolled(0f, 1f));
    }

    @Test
    void wheelOverTheOpenListScrollsIt() {
        box.setItems(numberedItems(20));
        box.setMaxVisibleRows(5);
        box.openList();
        ui.pointerMoved(BOX_COLUMN + 1, rowOfItem(1));

        assertTrue(ui.scrolled(0f, 1f));

        assertEquals(3, list().getScrollRow());
    }

    @Test
    void wheelOverTheClosedBoxScrollsAnOuterPane() {
        ui.getRoot().removeChild(box);
        WidgetGroup content = new WidgetGroup();
        content.addChild(box);
        box.setPosition(1, 1);
        TextButton spacer = new TextButton("x", LOOK);
        spacer.setPosition(1, 30);
        content.addChild(spacer);
        content.pack();
        ScrollPane pane = new ScrollPane(content, STYLE.getScrollPane());
        pane.setSize(10, 5);
        pane.setPosition(10, 2);
        ui.getRoot().addChild(pane);
        ui.getRoot().validate();
        ui.pointerMoved(11, 3);

        assertTrue(ui.scrolled(0f, 1f));

        assertTrue(pane.getScrollRow() > 0);
    }

    // ---- disabled, closing, lifetime ----

    @Test
    void disabledBoxDoesNotOpenByPointerKeyOrCall() {
        box.setDisabled(true);

        assertFalse(ui.pointerDown(BOX_COLUMN + 1, BOX_ROW, 0, 0));
        ui.keyDown(Input.Keys.ENTER);
        box.openList();

        assertFalse(box.isListOpen());
        assertFalse(box.isFocusable());
    }

    @Test
    void disablingTheBoxWhileOpenClosesTheList() {
        box.openList();

        box.setDisabled(true);

        assertFalse(box.isListOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void replacingTheItemsWhileOpenClosesTheList() {
        box.openList();

        box.setItems(List.of("One"));

        assertFalse(box.isListOpen());
    }

    @Test
    void emptyBoxDoesNotOpen() {
        box.setItems(List.of());

        box.openList();

        assertFalse(box.isListOpen());
    }

    @Test
    void removingTheBoxFromTheTreeWhileOpenClosesTheList() {
        box.openList();

        ui.getRoot().removeChild(box);
        ui.update(0f);

        assertFalse(box.isListOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void hidingTheBoxWhileOpenClosesTheList() {
        box.openList();

        box.setVisible(false);
        ui.update(0f);

        assertFalse(box.isListOpen());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void openingTwiceKeepsOnePopup() {
        box.openList();
        box.openList();

        assertEquals(2, ui.getPopupLayer().getChildCount());
    }

    @Test
    void pressOnTheScrollBarLeavesTheKeysWorkingAfterTheNextUpdate() {
        box.setItems(numberedItems(20));
        box.setMaxVisibleRows(5);
        box.openList();
        int barColumn = list().getAbsoluteX() + list().getWidth() - 1;

        ui.pointerDown(barColumn, list().getAbsoluteY(), 0, 0);
        ui.pointerUp(barColumn, list().getAbsoluteY(), 0, 0);
        ui.update(0f);
        ui.keyDown(Input.Keys.DOWN);

        assertEquals(0, rows().getHighlightedIndex());
    }

    @Test
    void styleChangeRedrawsWithoutResizingWhenPaddingIsSame() {
        box.setStyle(STYLE.withArrowGlyph('v'));

        assertEquals(6, box.getWidth());
        assertEquals('v', box.getStyle().getArrowGlyph());
    }

    private static List<String> numberedItems(int count) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add("Item " + i);
        }
        return items;
    }
}
