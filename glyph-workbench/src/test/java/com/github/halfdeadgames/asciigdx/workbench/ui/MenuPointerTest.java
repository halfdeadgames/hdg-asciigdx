package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar.Item;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar.Menu;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

// The FILE title sits at columns 1-4 and EDIT at 7-10; each drop-down has its items on rows 2 and 3.
class MenuPointerTest {

    private static final int COLUMNS = 80;

    private final MenuBar<String> bar = new MenuBar<>(List.of(
        new Menu<>("FILE", 'f', List.of(new Item<>("OPEN", "", "open"), new Item<>("QUIT", "", "quit"))),
        new Menu<>("EDIT", 'e', List.of(new Item<>("UNDO", "", "undo"), new Item<>("REDO", "", "redo")))));
    private final MenuPointer<String> pointer = new MenuPointer<>(0);
    private Predicate<String> available = action -> true;

    private String frame(int column, int row, boolean escape, boolean pressStarted, boolean down, boolean busy) {
        return pointer.update(bar, available, column, row, COLUMNS, escape, pressStarted, down, busy);
    }

    private String press(int column, int row) {
        return frame(column, row, false, true, true, false);
    }

    private String drag(int column, int row) {
        return frame(column, row, false, false, true, false);
    }

    private String idle(int column, int row) {
        return frame(column, row, false, false, false, false);
    }

    @Test
    void pressingATitleOpensItAndReleasingOnItKeepsItOpen() {
        press(2, 0);
        assertEquals(0, bar.openMenu());
        assertTrue(pointer.captured());
        assertNull(idle(2, 0));
        assertEquals(0, bar.openMenu());
        assertTrue(pointer.captured());
    }

    @Test
    void pressingTheOpenTitleClosesIt() {
        press(2, 0);
        idle(2, 0);
        press(2, 0);
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
        assertNull(idle(2, 0));
        assertFalse(bar.isOpen());
    }

    @Test
    void clickingAnItemRunsItOnReleaseAndCloses() {
        press(2, 0);
        idle(2, 0);
        assertNull(press(2, 3));
        assertTrue(bar.isOpen());
        assertEquals("quit", idle(2, 3));
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
        idle(2, 3);
        assertFalse(pointer.captured());
    }

    @Test
    void aPressAndReleaseInOneFrameStillRunsTheItem() {
        press(2, 0);
        idle(2, 0);
        assertEquals("open", frame(2, 2, false, true, false, false));
        assertFalse(bar.isOpen());
    }

    @Test
    void pressDragReleaseFromATitleOntoAnItemRunsIt() {
        press(2, 0);
        drag(2, 1);
        drag(2, 3);
        assertEquals(1, bar.highlightedItem());
        assertEquals("quit", idle(2, 3));
        assertFalse(bar.isOpen());
    }

    @Test
    void releasingOffTheItemsKeepsTheMenuOpen() {
        press(2, 0);
        drag(40, 20);
        assertNull(idle(40, 20));
        assertTrue(bar.isOpen());
        assertTrue(pointer.captured());
    }

    @Test
    void pressingOutsideClosesAndTheWholePressStaysCaptured() {
        press(2, 0);
        idle(2, 0);
        press(40, 10);
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
        assertTrue(pointer.isCapturing(bar));
        drag(41, 11);
        assertTrue(pointer.captured());
        assertNull(idle(41, 11));
        assertTrue(pointer.captured());
        assertFalse(pointer.isCapturing(bar));
        idle(41, 11);
        assertFalse(pointer.captured());
    }

    // FILE's panel covers columns 1..8 and rows 1..4: a border ring around the items on rows 2 and 3.
    @Test
    void pressingThePanelBordersOrPaddingKeepsTheMenuOpenAndRunsNothing() {
        press(2, 0);
        idle(2, 0);
        int[][] borderCells = {{2, 1}, {1, 2}, {8, 3}, {2, 4}, {1, 1}, {8, 4}};
        for (int[] cell : borderCells) {
            assertNull(press(cell[0], cell[1]), "press at " + cell[0] + "," + cell[1]);
            assertTrue(bar.isOpen(), "stays open at " + cell[0] + "," + cell[1]);
            assertTrue(pointer.captured());
            assertNull(idle(cell[0], cell[1]), "release at " + cell[0] + "," + cell[1]);
            assertTrue(bar.isOpen(), "release keeps open at " + cell[0] + "," + cell[1]);
        }
    }

    @Test
    void pressingJustOutsideThePanelClosesIt() {
        int[][] outsideCells = {{9, 2}, {0, 2}, {2, 5}, {50, 3}};
        for (int[] cell : outsideCells) {
            press(2, 0);
            idle(2, 0);
            press(cell[0], cell[1]);
            assertFalse(bar.isOpen(), "closes at " + cell[0] + "," + cell[1]);
            idle(cell[0], cell[1]);
            idle(cell[0], cell[1]);
        }
    }

    @Test
    void draggingFromATitleAndReleasingOnABorderKeepsTheMenuOpen() {
        press(2, 0);
        drag(1, 2);
        assertNull(idle(1, 2));
        assertTrue(bar.isOpen());
        assertTrue(pointer.captured());
    }

    @Test
    void pressingTheLitFillBesideATitleOpensIt() {
        press(0, 0);
        assertEquals(0, bar.openMenu());
        idle(0, 0);
        press(5, 0);
        assertFalse(bar.isOpen());
    }

    @Test
    void pressingBelowTheBarWhileClosedPassesThrough() {
        press(30, 10);
        assertFalse(pointer.captured());
        assertFalse(pointer.isCapturing(bar));
    }

    @Test
    void pressingEmptyBarSpaceIsTakenButOpensNothing() {
        press(30, 0);
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
        drag(30, 10);
        assertTrue(pointer.captured());
        idle(30, 10);
        assertTrue(pointer.captured());
        idle(30, 10);
        assertFalse(pointer.captured());
    }

    @Test
    void pressingALockedTitleClosesTheOpenMenu() {
        press(2, 0);
        idle(2, 0);
        available = action -> !action.equals("undo") && !action.equals("redo");
        press(8, 0);
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
    }

    @Test
    void aBusyPointerIgnoresPressesAndLightsNothing() {
        frame(2, 0, false, true, true, true);
        assertFalse(bar.isOpen());
        assertFalse(pointer.captured());
        assertEquals(-1, bar.hoveredTitle());
        frame(2, 0, false, false, true, true);
        assertEquals(-1, bar.hoveredTitle());
        assertFalse(pointer.captured());
        idle(2, 0);
        assertEquals(0, bar.hoveredTitle());
        assertFalse(pointer.captured());
    }

    @Test
    void escapeClosesTheMenuAndOwnsThatFrame() {
        press(2, 0);
        idle(2, 0);
        frame(2, 0, true, false, false, false);
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
        idle(2, 0);
        assertFalse(pointer.captured());
    }

    @Test
    void escapeWithNothingOpenIsNotTaken() {
        frame(20, 10, true, false, false, false);
        assertFalse(pointer.captured());
    }

    @Test
    void escapeDuringAHeldPressIsTakenToo() {
        press(2, 0);
        frame(2, 0, true, false, true, false);
        assertFalse(bar.isOpen());
        assertTrue(pointer.captured());
    }

    @Test
    void hoveringAnotherTitleSwitchesTheOpenMenu() {
        press(2, 0);
        idle(2, 0);
        idle(8, 0);
        assertEquals(1, bar.openMenu());
        assertTrue(pointer.captured());
    }

    @Test
    void hoveringWithNothingOpenIsNotCaptured() {
        idle(2, 0);
        assertEquals(0, bar.hoveredTitle());
        assertFalse(pointer.captured());
        assertFalse(bar.isOpen());
    }

    @Test
    void aStationaryPointerKeepsTheKeyboardHighlight() {
        press(2, 0);
        idle(2, 0);
        idle(2, 2);
        assertEquals(0, bar.highlightedItem());
        bar.moveItem(1, available);
        assertEquals(1, bar.highlightedItem());
        idle(2, 2);
        idle(2, 2);
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void aStationaryPointerOnATitleDoesNotSwitchTheMenuOpenedByKeyboard() {
        idle(8, 0);
        assertEquals(1, bar.hoveredTitle());
        bar.openByAccessKey('f', available);
        idle(8, 0);
        idle(8, 0);
        assertEquals(0, bar.openMenu());
    }

    @Test
    void movingToAnotherCellHoversAgain() {
        press(2, 0);
        idle(2, 0);
        idle(2, 2);
        bar.moveItem(1, available);
        idle(2, 2);
        idle(2, 3);
        assertEquals(1, bar.highlightedItem());
        idle(2, 2);
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void anActivePressAlwaysHovers() {
        press(2, 0);
        drag(2, 2);
        bar.moveItem(1, available);
        drag(2, 2);
        assertEquals(0, bar.highlightedItem());
    }

    @Test
    void aClosedMenuStillFollowsTheStationaryPointer() {
        idle(2, 0);
        assertEquals(0, bar.hoveredTitle());
        available = action -> !action.equals("open") && !action.equals("quit");
        idle(2, 0);
        assertEquals(-1, bar.hoveredTitle());
        assertEquals(0, bar.inspectedTitle());
    }

    @Test
    void theFirstFrameAlwaysHovers() {
        bar.openByAccessKey('f', available);
        bar.moveItem(1, available);
        idle(2, 2);
        assertEquals(0, bar.highlightedItem());
        assertEquals(-1, bar.hoveredTitle());
    }

    @Test
    void hoverRuleOnlySkipsAnOpenMenuWithAStillPointerAndNoPress() {
        assertTrue(MenuPointer.shouldHover(false, false, false));
        assertTrue(MenuPointer.shouldHover(true, true, false));
        assertTrue(MenuPointer.shouldHover(true, false, true));
        assertFalse(MenuPointer.shouldHover(true, false, false));
    }

    @Test
    void anUnavailableItemNeverRunsAndKeepsTheMenuOpen() {
        press(2, 0);
        idle(2, 0);
        available = action -> !action.equals("quit");
        press(2, 3);
        assertNull(idle(2, 3));
        assertTrue(bar.isOpen());
    }
}
