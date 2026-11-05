package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class MenuBarTest {

    private static final int WIDE = 80;
    private static final int NONE = MenuBar.NOT_IN_DROP_DOWN;
    private static final Predicate<String> ALL = action -> true;

    private static MenuBar.Item<String> item(String name) {
        return new MenuBar.Item<>(name, "", name);
    }

    /** FILE at column 1 (new, open, save), EDIT at 7 (undo, redo), VIEW at 13 (zoom, grid). */
    private static MenuBar<String> bar() {
        return new MenuBar<>(List.of(
            new MenuBar.Menu<>("FILE", 'F', List.of(
                new MenuBar.Item<>("NEW", "CTRL+N", "new"),
                new MenuBar.Item<>("OPEN", "CTRL+O", "open"),
                new MenuBar.Item<>("SAVE", "", "save"))),
            new MenuBar.Menu<>("EDIT", 'E', List.of(item("undo"), item("redo"))),
            new MenuBar.Menu<>("VIEW", 'V', List.of(item("zoom"), item("grid")))));
    }

    private static Predicate<String> without(String... disabled) {
        List<String> blocked = List.of(disabled);
        return action -> !blocked.contains(action);
    }

    @Test
    void layoutPlacesTitlesTwoCellsApart() {
        MenuBar<String> bar = bar();
        assertEquals(1, bar.titleColumn(0));
        assertEquals(7, bar.titleColumn(1));
        assertEquals(13, bar.titleColumn(2));
        assertEquals(17, bar.barWidth());
    }

    @Test
    void titleHitTestingCoversTheLitFillOneCellEachSide() {
        // The lit fill spans titleColumn-1 through titleColumn+length, so a click on the fill counts.
        MenuBar<String> bar = bar();
        assertEquals(0, bar.titleAt(0));
        assertEquals(0, bar.titleAt(1));
        assertEquals(0, bar.titleAt(4));
        assertEquals(0, bar.titleAt(5));
        assertEquals(1, bar.titleAt(6));
        assertEquals(1, bar.titleAt(7));
        assertEquals(2, bar.titleAt(12));
        assertEquals(2, bar.titleAt(16));
        assertEquals(2, bar.titleAt(17));
        assertEquals(-1, bar.titleAt(18));
        assertEquals(-1, bar.titleAt(-1));
    }

    @Test
    void dropDownWidthUsesLongestLabelAndShortcut() {
        MenuBar<String> bar = bar();
        assertEquals(4 + 2 + 6 + 2, bar.dropDownWidth(0));
        assertEquals(4 + 2 + 0 + 2, bar.dropDownWidth(1));
    }

    @Test
    void dropDownIsClampedInsideTheTotalColumns() {
        MenuBar<String> bar = bar();
        assertEquals(13, bar.dropDownColumn(2, WIDE));
        assertEquals(20 - 8, bar.dropDownColumn(2, 20));
        assertEquals(0, bar.dropDownColumn(0, 5));
    }

    @Test
    void itemHitTestingHasExactBoundaries() {
        MenuBar<String> bar = bar();
        // FILE's panel spans columns 1..14; columns 1 and 14 are its border, so items are 2..13.
        assertEquals(-1, bar.itemAt(2, 0, WIDE));
        bar.openFirst(ALL);
        assertEquals(0, bar.itemAt(2, 0, WIDE));
        assertEquals(2, bar.itemAt(2, 2, WIDE));
        assertEquals(-1, bar.itemAt(2, 3, WIDE));
        assertEquals(-1, bar.itemAt(2, -1, WIDE));
        assertEquals(-1, bar.itemAt(2, NONE, WIDE));
        assertEquals(-1, bar.itemAt(0, 0, WIDE));
        assertEquals(-1, bar.itemAt(1, 0, WIDE));
        assertEquals(0, bar.itemAt(13, 0, WIDE));
        assertEquals(-1, bar.itemAt(14, 0, WIDE));
    }

    @Test
    void panelHitTestingIncludesBordersAndNothingBeyond() {
        // Rows -1 (top border) through itemCount (bottom border), columns 1..14 for FILE.
        MenuBar<String> bar = bar();
        assertFalse(bar.isOnDropDown(2, 0, WIDE));
        bar.openFirst(ALL);
        assertTrue(bar.isOnDropDown(1, -1, WIDE));
        assertTrue(bar.isOnDropDown(14, 3, WIDE));
        assertTrue(bar.isOnDropDown(1, 0, WIDE));
        assertTrue(bar.isOnDropDown(14, 1, WIDE));
        assertFalse(bar.isOnDropDown(0, 0, WIDE));
        assertFalse(bar.isOnDropDown(15, 0, WIDE));
        assertFalse(bar.isOnDropDown(2, -2, WIDE));
        assertFalse(bar.isOnDropDown(2, 4, WIDE));
        assertFalse(bar.isOnDropDown(2, NONE, WIDE));
    }

    @Test
    void itemHitTestingFollowsTheClampedColumn() {
        MenuBar<String> bar = bar();
        bar.openByAccessKey('v', ALL);
        // VIEW is 8 wide and shifts to column 12; its border columns are 12 and 19.
        assertEquals(-1, bar.itemAt(11, 0, 20));
        assertEquals(-1, bar.itemAt(12, 0, 20));
        assertEquals(0, bar.itemAt(13, 0, 20));
        assertEquals(0, bar.itemAt(18, 0, 20));
        assertEquals(-1, bar.itemAt(19, 0, 20));
        assertEquals(-1, bar.itemAt(20, 0, 20));
    }

    @Test
    void accessKeysOpenInEitherCase() {
        MenuBar<String> lower = bar();
        assertTrue(lower.openByAccessKey('e', ALL));
        assertEquals(1, lower.openMenu());
        MenuBar<String> upper = bar();
        assertTrue(upper.openByAccessKey('E', ALL));
        assertEquals(1, upper.openMenu());
    }

    @Test
    void unknownAccessKeyChangesNothing() {
        MenuBar<String> bar = bar();
        bar.openByAccessKey('f', ALL);
        assertFalse(bar.openByAccessKey('z', ALL));
        assertEquals(0, bar.openMenu());
    }

    @Test
    void lockedMenuCannotBeOpenedByAccessKey() {
        MenuBar<String> bar = bar();
        assertFalse(bar.openByAccessKey('e', without("undo", "redo")));
        assertFalse(bar.isOpen());
    }

    @Test
    void openingHighlightsTheFirstAvailableItem() {
        MenuBar<String> bar = bar();
        bar.openByAccessKey('f', without("new"));
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void openFirstSkipsLockedMenus() {
        MenuBar<String> bar = bar();
        bar.openFirst(without("new", "open", "save"));
        assertTrue(bar.isOpen());
        assertEquals(1, bar.openMenu());
    }

    @Test
    void allLockedBarCannotOpenAtAll() {
        MenuBar<String> bar = bar();
        Predicate<String> nothing = action -> false;
        bar.openFirst(nothing);
        assertFalse(bar.isOpen());
        assertFalse(bar.openByAccessKey('f', nothing));
        bar.pressTitle(0, nothing);
        bar.hover(1, NONE, WIDE, nothing);
        assertFalse(bar.isOpen());
        assertEquals(-1, bar.hoveredTitle());
        bar.moveMenu(1, nothing);
        assertFalse(bar.isOpen());
    }

    @Test
    void moveMenuWrapsBothWays() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveMenu(-1, ALL);
        assertEquals(2, bar.openMenu());
        bar.moveMenu(1, ALL);
        assertEquals(0, bar.openMenu());
        bar.moveMenu(1, ALL);
        assertEquals(1, bar.openMenu());
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void moveMenuSkipsLockedMenus() {
        MenuBar<String> bar = bar();
        Predicate<String> editLocked = without("undo", "redo");
        bar.openFirst(editLocked);
        bar.moveMenu(1, editLocked);
        assertEquals(2, bar.openMenu());
        bar.moveMenu(-1, editLocked);
        assertEquals(0, bar.openMenu());
    }

    @Test
    void moveMenuStaysPutWhenNoOtherMenuIsUsable() {
        MenuBar<String> bar = bar();
        Predicate<String> onlyFile = action -> action.equals("new");
        bar.openFirst(onlyFile);
        bar.moveMenu(1, onlyFile);
        assertEquals(0, bar.openMenu());
    }

    @Test
    void movingWhileClosedDoesNothing() {
        MenuBar<String> bar = bar();
        bar.moveMenu(1, ALL);
        bar.moveItem(1, ALL);
        assertFalse(bar.isOpen());
        assertEquals(-1, bar.highlightedItem());
    }

    @Test
    void moveItemWrapsBothWays() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(-1, ALL);
        assertEquals(2, bar.highlightedItem());
        bar.moveItem(1, ALL);
        assertEquals(0, bar.highlightedItem());
        bar.moveItem(1, ALL);
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void moveItemSkipsUnavailableItems() {
        MenuBar<String> bar = bar();
        Predicate<String> noOpen = without("open");
        bar.openFirst(noOpen);
        bar.moveItem(1, noOpen);
        assertEquals(2, bar.highlightedItem());
        bar.moveItem(-1, noOpen);
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void moveItemFromNoHighlightGoesToTheEnds() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.hover(2, 0, WIDE, without("new"));
        assertEquals(-1, bar.highlightedItem());
        bar.moveItem(-1, ALL);
        assertEquals(2, bar.highlightedItem());
        bar.hover(2, 0, WIDE, without("new"));
        bar.moveItem(1, ALL);
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void closeClearsOpenAndHighlight() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.close();
        assertEquals(-1, bar.openMenu());
        assertEquals(-1, bar.highlightedItem());
    }

    @Test
    void chooseReturnsTheActionAndCloses() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(1, ALL);
        assertEquals("open", bar.chooseHighlighted(ALL));
        assertFalse(bar.isOpen());
    }

    @Test
    void choosingWithNothingHighlightedGivesNullAndKeepsTheMenuOpen() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.hover(2, 0, WIDE, without("new"));
        assertNull(bar.chooseHighlighted(without("new")));
        assertTrue(bar.isOpen());
    }

    @Test
    void choosingRepicksAHighlightThatBecameUnavailable() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        assertEquals("open", bar.chooseHighlighted(without("new")));
        assertFalse(bar.isOpen());
    }

    @Test
    void movingRepicksAHighlightThatBecameUnavailableBeforeStepping() {
        MenuBar<String> bar = bar();
        Predicate<String> noNew = without("new");
        bar.openFirst(ALL);
        bar.moveItem(1, noNew);
        assertEquals(2, bar.highlightedItem());
    }

    @Test
    void refreshRepicksTheFirstAvailableItemWhenTheHighlightBecomesUnavailable() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(1, ALL);
        bar.refresh(without("open"));
        assertEquals(0, bar.highlightedItem());
        bar.refresh(without("new", "open"));
        assertEquals(2, bar.highlightedItem());
    }

    @Test
    void refreshKeepsAnAvailableHighlightAndAClearedOne() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(1, ALL);
        bar.refresh(without("new"));
        assertEquals(1, bar.highlightedItem());
        bar.hover(2, 0, WIDE, without("new"));
        bar.refresh(without("new"));
        assertEquals(-1, bar.highlightedItem());
    }

    @Test
    void refreshClosesAnOpenMenuThatBecameLocked() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.refresh(without("new", "open", "save"));
        assertFalse(bar.isOpen());
        assertEquals(-1, bar.highlightedItem());
    }

    @Test
    void everyEntryPointClosesAMenuThatBecameLocked() {
        Predicate<String> fileLocked = without("new", "open", "save");
        MenuBar<String> hover = bar();
        hover.openFirst(ALL);
        hover.hover(30, NONE, WIDE, fileLocked);
        assertFalse(hover.isOpen());
        MenuBar<String> moveItem = bar();
        moveItem.openFirst(ALL);
        moveItem.moveItem(1, fileLocked);
        assertFalse(moveItem.isOpen());
        MenuBar<String> moveMenu = bar();
        moveMenu.openFirst(ALL);
        moveMenu.moveMenu(1, fileLocked);
        assertFalse(moveMenu.isOpen());
        MenuBar<String> choose = bar();
        choose.openFirst(ALL);
        assertNull(choose.chooseHighlighted(fileLocked));
        assertFalse(choose.isOpen());
        MenuBar<String> release = bar();
        release.openFirst(ALL);
        assertNull(release.releaseOnItem(3, 0, WIDE, fileLocked));
        assertFalse(release.isOpen());
    }

    @Test
    void moveItemWithNothingAvailableClosesTheMenu() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(1, action -> false);
        assertFalse(bar.isOpen());
    }

    @Test
    void moveItemWithASingleAvailableItemStaysOnItBothWays() {
        MenuBar<String> bar = bar();
        Predicate<String> onlyOpen = action -> action.equals("open");
        bar.openFirst(onlyOpen);
        assertEquals(1, bar.highlightedItem());
        bar.moveItem(1, onlyOpen);
        assertEquals(1, bar.highlightedItem());
        bar.moveItem(-1, onlyOpen);
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void zeroDirectionMovesNothing() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(0, ALL);
        bar.moveMenu(0, ALL);
        assertEquals(0, bar.openMenu());
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void largeDirectionsAreTreatedAsOneStep() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(5, ALL);
        assertEquals(1, bar.highlightedItem());
        bar.moveItem(-7, ALL);
        assertEquals(0, bar.highlightedItem());
        bar.moveMenu(9, ALL);
        assertEquals(1, bar.openMenu());
        bar.moveMenu(-9, ALL);
        assertEquals(0, bar.openMenu());
    }

    @Test
    void menuWithoutItemsIsLocked() {
        MenuBar<String> bar = new MenuBar<>(List.of(
            new MenuBar.Menu<>("NONE", 'n', List.of()),
            new MenuBar.Menu<>("FILE", 'f', List.of(item("a")))));
        assertTrue(bar.isLocked(0, ALL));
        assertFalse(bar.openByAccessKey('n', ALL));
        bar.pressTitle(0, ALL);
        assertFalse(bar.isOpen());
        bar.openFirst(ALL);
        assertTrue(bar.isOpen());
        assertEquals(1, bar.openMenu());
        bar.moveMenu(1, ALL);
        assertEquals(1, bar.openMenu());
    }

    @Test
    void duplicateAccessKeyFallsThroughALockedMenu() {
        MenuBar<String> bar = new MenuBar<>(List.of(
            new MenuBar.Menu<>("FIRST", 'x', List.of(item("a"))),
            new MenuBar.Menu<>("SECOND", 'X', List.of(item("b")))));
        assertTrue(bar.openByAccessKey('x', without("a")));
        assertEquals(1, bar.openMenu());
        assertFalse(bar.openByAccessKey('x', action -> false));
    }

    @Test
    void itemRejectsANullAction() {
        assertThrows(IllegalArgumentException.class, () -> new MenuBar.Item<String>("L", "", null));
    }

    @Test
    void choosingWhileClosedGivesNull() {
        assertNull(bar().chooseHighlighted(ALL));
    }

    @Test
    void hoverLightsTitlesWithoutOpening() {
        MenuBar<String> bar = bar();
        bar.hover(8, NONE, WIDE, ALL);
        assertEquals(1, bar.hoveredTitle());
        assertFalse(bar.isOpen());
        bar.hover(18, NONE, WIDE, ALL);
        assertEquals(-1, bar.hoveredTitle());
        bar.hover(5, NONE, WIDE, ALL);
        assertEquals(0, bar.hoveredTitle());
    }

    @Test
    void hoverDoesNotLightLockedTitles() {
        MenuBar<String> bar = bar();
        bar.hover(8, NONE, WIDE, without("undo", "redo"));
        assertEquals(-1, bar.hoveredTitle());
    }

    @Test
    void hoveringAnotherTitleSwitchesTheOpenMenu() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(1, ALL);
        bar.hover(14, NONE, WIDE, ALL);
        assertEquals(2, bar.openMenu());
        assertEquals(0, bar.highlightedItem());
        assertEquals(2, bar.hoveredTitle());
    }

    @Test
    void hoveringTheOpenTitleKeepsTheHighlight() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.moveItem(1, ALL);
        bar.hover(2, NONE, WIDE, ALL);
        assertEquals(0, bar.openMenu());
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void hoveringALockedTitleDoesNotSwitchMenus() {
        MenuBar<String> bar = bar();
        Predicate<String> editLocked = without("undo", "redo");
        bar.openFirst(editLocked);
        bar.hover(8, NONE, WIDE, editLocked);
        assertEquals(0, bar.openMenu());
    }

    @Test
    void hoveringItemsHighlightsAvailableAndClearsUnavailable() {
        MenuBar<String> bar = bar();
        Predicate<String> noOpen = without("open");
        bar.openFirst(noOpen);
        bar.hover(2, 2, WIDE, noOpen);
        assertEquals(2, bar.highlightedItem());
        assertEquals(-1, bar.hoveredTitle());
        bar.hover(2, 1, WIDE, noOpen);
        assertEquals(-1, bar.highlightedItem());
    }

    @Test
    void hoveringOutsideTheItemsKeepsTheHighlight() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.hover(2, 5, WIDE, ALL);
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void hoveringAnUnavailableItemInspectsItWithoutHighlighting() {
        MenuBar<String> bar = bar();
        Predicate<String> noOpen = without("open");
        bar.openFirst(noOpen);
        bar.hover(2, 1, WIDE, noOpen);
        assertEquals(1, bar.inspectedItem());
        assertEquals(-1, bar.highlightedItem());
        assertEquals(-1, bar.hoveredTitle());
        assertEquals(-1, bar.inspectedTitle());
        assertNull(bar.releaseOnItem(2, 1, WIDE, noOpen));
        assertTrue(bar.isOpen());
    }

    @Test
    void hoveringAnAvailableItemInspectsNothing() {
        MenuBar<String> bar = bar();
        bar.openFirst(ALL);
        bar.hover(2, 1, WIDE, ALL);
        assertEquals(-1, bar.inspectedItem());
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void hoveringALockedTitleInspectsItWithoutHovering() {
        MenuBar<String> bar = bar();
        bar.hover(8, NONE, WIDE, without("undo", "redo"));
        assertEquals(1, bar.inspectedTitle());
        assertEquals(-1, bar.hoveredTitle());
        assertEquals(-1, bar.inspectedItem());
        assertFalse(bar.isOpen());
    }

    @Test
    void hoveringALockedTitleWithAMenuOpenKeepsThatMenu() {
        MenuBar<String> bar = bar();
        Predicate<String> editLocked = without("undo", "redo");
        bar.openFirst(editLocked);
        bar.hover(8, NONE, WIDE, editLocked);
        assertEquals(1, bar.inspectedTitle());
        assertEquals(0, bar.openMenu());
    }

    @Test
    void anyOtherPointerPositionClearsInspection() {
        Predicate<String> noOpen = without("open");
        Predicate<String> editLocked = without("open", "undo", "redo");
        MenuBar<String> bar = bar();
        bar.openFirst(editLocked);
        bar.hover(2, 1, WIDE, editLocked);
        bar.hover(2, 0, WIDE, editLocked);
        assertEquals(-1, bar.inspectedItem());
        bar.hover(2, 1, WIDE, editLocked);
        bar.hover(2, 5, WIDE, editLocked);
        assertEquals(-1, bar.inspectedItem());
        bar.hover(2, 1, WIDE, editLocked);
        bar.hover(60, 1, WIDE, editLocked);
        assertEquals(-1, bar.inspectedItem());
        bar.hover(2, 1, WIDE, editLocked);
        bar.hover(2, NONE, WIDE, editLocked);
        assertEquals(-1, bar.inspectedItem());
        bar.hover(8, NONE, WIDE, editLocked);
        assertEquals(1, bar.inspectedTitle());
        bar.hover(2, NONE, WIDE, editLocked);
        assertEquals(-1, bar.inspectedTitle());
        bar.hover(8, NONE, WIDE, editLocked);
        bar.hover(2, 1, WIDE, noOpen);
        assertEquals(-1, bar.inspectedTitle());
        assertEquals(1, bar.inspectedItem());
    }

    @Test
    void closingOrOpeningClearsInspection() {
        Predicate<String> noOpen = without("open");
        MenuBar<String> closed = bar();
        closed.openFirst(noOpen);
        closed.hover(2, 1, WIDE, noOpen);
        closed.close();
        assertEquals(-1, closed.inspectedItem());
        MenuBar<String> switched = bar();
        switched.openFirst(noOpen);
        switched.hover(2, 1, WIDE, noOpen);
        switched.hover(14, NONE, WIDE, noOpen);
        assertEquals(2, switched.openMenu());
        assertEquals(-1, switched.inspectedItem());
        MenuBar<String> reopened = bar();
        reopened.openFirst(noOpen);
        reopened.hover(2, 1, WIDE, noOpen);
        reopened.openByAccessKey('e', noOpen);
        assertEquals(-1, reopened.inspectedItem());
        MenuBar<String> lockedTitle = bar();
        Predicate<String> editLocked = without("undo", "redo");
        lockedTitle.hover(8, NONE, WIDE, editLocked);
        lockedTitle.openFirst(editLocked);
        assertEquals(-1, lockedTitle.inspectedTitle());
        MenuBar<String> pressed = bar();
        pressed.hover(8, NONE, WIDE, editLocked);
        pressed.pressTitle(0, editLocked);
        assertEquals(-1, pressed.inspectedTitle());
    }

    @Test
    void keyboardMovementClearsInspection() {
        Predicate<String> noOpen = without("open");
        MenuBar<String> item = bar();
        item.openFirst(noOpen);
        item.hover(2, 1, WIDE, noOpen);
        item.moveItem(1, noOpen);
        assertEquals(-1, item.inspectedItem());
        assertEquals(0, item.highlightedItem());
        MenuBar<String> menu = bar();
        menu.openFirst(noOpen);
        menu.hover(2, 1, WIDE, noOpen);
        menu.moveMenu(1, noOpen);
        assertEquals(-1, menu.inspectedItem());
        Predicate<String> editLocked = without("undo", "redo");
        MenuBar<String> title = bar();
        title.hover(8, NONE, WIDE, editLocked);
        title.moveItem(1, editLocked);
        assertEquals(-1, title.inspectedTitle());
        title.hover(8, NONE, WIDE, editLocked);
        title.moveMenu(1, editLocked);
        assertEquals(-1, title.inspectedTitle());
    }

    @Test
    void refreshClearsInspectionOfEntriesThatBecameAvailable() {
        Predicate<String> noOpen = without("open");
        MenuBar<String> item = bar();
        item.openFirst(noOpen);
        item.hover(2, 1, WIDE, noOpen);
        item.refresh(noOpen);
        assertEquals(1, item.inspectedItem());
        item.refresh(ALL);
        assertEquals(-1, item.inspectedItem());
        Predicate<String> editLocked = without("undo", "redo");
        MenuBar<String> title = bar();
        title.hover(8, NONE, WIDE, editLocked);
        title.refresh(editLocked);
        assertEquals(1, title.inspectedTitle());
        title.refresh(ALL);
        assertEquals(-1, title.inspectedTitle());
    }

    @Test
    void inspectionNeverMakesAnythingChoosable() {
        MenuBar<String> bar = bar();
        Predicate<String> noNewOrOpen = without("new", "open");
        bar.openFirst(noNewOrOpen);
        bar.hover(2, 0, WIDE, noNewOrOpen);
        assertEquals(0, bar.inspectedItem());
        assertNull(bar.chooseHighlighted(noNewOrOpen));
        assertNull(bar.releaseOnItem(2, 0, WIDE, noNewOrOpen));
        assertTrue(bar.isOpen());
    }

    @Test
    void pressTitleTogglesAndIgnoresLockedMenus() {
        MenuBar<String> bar = bar();
        bar.pressTitle(1, ALL);
        assertEquals(1, bar.openMenu());
        bar.pressTitle(1, ALL);
        assertFalse(bar.isOpen());
        bar.pressTitle(1, without("undo", "redo"));
        assertFalse(bar.isOpen());
    }

    @Test
    void releaseOnItemReturnsTheActionAndCloses() {
        MenuBar<String> bar = bar();
        bar.pressTitle(0, ALL);
        assertEquals("save", bar.releaseOnItem(3, 2, WIDE, ALL));
        assertFalse(bar.isOpen());
    }

    @Test
    void releaseOffItemsOrOnAnUnavailableItemGivesNullAndStaysOpen() {
        MenuBar<String> bar = bar();
        bar.pressTitle(0, ALL);
        assertNull(bar.releaseOnItem(3, 3, WIDE, ALL));
        assertNull(bar.releaseOnItem(3, NONE, WIDE, ALL));
        assertNull(bar.releaseOnItem(1, 0, WIDE, ALL));
        assertNull(bar.releaseOnItem(14, 0, WIDE, ALL));
        assertNull(bar.releaseOnItem(3, -1, WIDE, ALL));
        assertNull(bar.releaseOnItem(3, 1, WIDE, without("open")));
        assertTrue(bar.isOpen());
    }

    @Test
    void releaseWhileClosedGivesNull() {
        assertNull(bar().releaseOnItem(3, 0, WIDE, ALL));
    }

    @Test
    void menusAreDefensivelyCopied() {
        List<MenuBar.Item<String>> items = new ArrayList<>(List.of(item("a")));
        MenuBar.Menu<String> menu = new MenuBar.Menu<>("M", 'm', items);
        List<MenuBar.Menu<String>> menus = new ArrayList<>(List.of(menu));
        MenuBar<String> bar = new MenuBar<>(menus);
        items.add(item("b"));
        menus.clear();
        assertEquals(1, menu.items().size());
        assertEquals(1, bar.menuCount());
        assertThrows(UnsupportedOperationException.class, () -> menu.items().add(item("c")));
    }

    @Test
    void emptyBarHasOnlyTheLeadingBlank() {
        MenuBar<String> bar = new MenuBar<>(List.of());
        assertEquals(1, bar.barWidth());
        assertEquals(-1, bar.titleAt(1));
        bar.openFirst(ALL);
        assertFalse(bar.isOpen());
    }
}
