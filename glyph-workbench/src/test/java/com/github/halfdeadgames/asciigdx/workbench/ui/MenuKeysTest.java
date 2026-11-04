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

class MenuKeysTest {

    private final MenuBar<String> bar = new MenuBar<>(List.of(
        new Menu<>("FILE", 'f', List.of(new Item<>("OPEN", "", "open"), new Item<>("QUIT", "", "quit"))),
        new Menu<>("EDIT", 'e', List.of(new Item<>("UNDO", "", "undo"), new Item<>("REDO", "", "redo")))));
    private final MenuKeys<String> keys = new MenuKeys<>();
    private Predicate<String> available = action -> true;

    private String route(boolean enabled, boolean alt, char accessKey, boolean f10, boolean enter,
                         int lefts, int rights, int ups, int downs) {
        return keys.route(bar, available, enabled, alt, accessKey, f10, enter, lefts, rights, ups, downs);
    }

    private void altKey(char key) {
        assertNull(route(true, true, key, false, false, 0, 0, 0, 0));
    }

    private String arrows(int lefts, int rights, int ups, int downs) {
        return route(true, false, MenuKeys.NO_KEY, false, false, lefts, rights, ups, downs);
    }

    private String press(boolean f10, boolean enter) {
        return route(true, false, MenuKeys.NO_KEY, f10, enter, 0, 0, 0, 0);
    }

    @Test
    void altLetterOpensThatMenuInAnyCase() {
        altKey('E');
        assertEquals(1, bar.openMenu());
        assertTrue(keys.consumed());
    }

    @Test
    void altLetterSwitchesAnOpenMenu() {
        altKey('f');
        altKey('e');
        assertEquals(1, bar.openMenu());
    }

    @Test
    void altOwnLetterClosesTheOpenMenu() {
        altKey('f');
        altKey('f');
        assertFalse(bar.isOpen());
        assertTrue(keys.consumed());
    }

    @Test
    void altLetterOnALockedMenuDoesNotOpenIt() {
        available = action -> !action.equals("undo") && !action.equals("redo");
        altKey('e');
        assertFalse(bar.isOpen());
        assertTrue(keys.consumed());
    }

    @Test
    void altLetterOnALockedMenuLeavesTheOpenMenuAlone() {
        altKey('f');
        available = action -> !action.equals("undo") && !action.equals("redo");
        altKey('e');
        assertEquals(0, bar.openMenu());
    }

    @Test
    void altWithAnUnknownLetterOpensNothingButStillOwnsTheFrame() {
        altKey('d');
        assertFalse(bar.isOpen());
        assertTrue(keys.consumed());
    }

    @Test
    void altHeldAloneOwnsTheFrame() {
        route(true, true, MenuKeys.NO_KEY, false, false, 0, 0, 0, 0);
        assertTrue(keys.consumed());
        assertFalse(bar.isOpen());
    }

    @Test
    void altIgnoresArrowsAndEnterOfAnOpenMenu() {
        altKey('f');
        assertNull(route(true, true, MenuKeys.NO_KEY, false, true, 1, 1, 0, 1));
        assertEquals(0, bar.openMenu());
        assertEquals(0, bar.highlightedItem());
        assertTrue(bar.isOpen());
    }

    @Test
    void f10OpensTheFirstUsableMenuAndTogglesItClosed() {
        available = action -> !action.equals("open") && !action.equals("quit");
        press(true, false);
        assertEquals(1, bar.openMenu());
        assertTrue(keys.consumed());
        press(true, false);
        assertFalse(bar.isOpen());
        assertTrue(keys.consumed());
    }

    @Test
    void f10OpensNothingWhenEveryMenuIsLocked() {
        available = action -> false;
        press(true, false);
        assertFalse(bar.isOpen());
    }

    @Test
    void arrowsDoNothingAndAreNotTakenWhileClosed() {
        assertNull(arrows(1, 1, 1, 1));
        assertFalse(keys.consumed());
        assertNull(press(false, true));
        assertFalse(keys.consumed());
        assertFalse(bar.isOpen());
    }

    @Test
    void leftAndRightMoveBetweenMenusWithWrap() {
        altKey('f');
        arrows(0, 1, 0, 0);
        assertEquals(1, bar.openMenu());
        arrows(0, 1, 0, 0);
        assertEquals(0, bar.openMenu());
        arrows(1, 0, 0, 0);
        assertEquals(1, bar.openMenu());
        assertTrue(keys.consumed());
    }

    @Test
    void upAndDownMoveTheHighlightWithWrap() {
        altKey('f');
        arrows(0, 0, 0, 1);
        assertEquals(1, bar.highlightedItem());
        arrows(0, 0, 0, 1);
        assertEquals(0, bar.highlightedItem());
        arrows(0, 0, 1, 0);
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void severalRepeatsInOneFrameAllApply() {
        altKey('f');
        arrows(0, 0, 0, 3);
        assertEquals(1, bar.highlightedItem());
    }

    @Test
    void movingSkipsLockedMenusAndUnavailableItems() {
        altKey('f');
        available = action -> !action.equals("undo") && !action.equals("redo");
        arrows(0, 1, 0, 0);
        assertEquals(0, bar.openMenu());
    }

    @Test
    void enterChoosesTheHighlightedItemAndClosesButStillOwnsTheFrame() {
        altKey('f');
        arrows(0, 0, 0, 1);
        assertEquals("quit", press(false, true));
        assertFalse(bar.isOpen());
        assertTrue(keys.consumed());
    }

    @Test
    void enterOnAnOpenMenuNeverRunsAnUnavailableItem() {
        altKey('f');
        available = action -> !action.equals("open");
        assertEquals("quit", press(false, true));
    }

    @Test
    void escapeIsLeftToThePointerSoNothingHappensHere() {
        altKey('f');
        press(false, false);
        assertTrue(bar.isOpen());
    }

    @Test
    void aDisabledRouterTakesNothing() {
        assertNull(route(false, true, 'f', false, false, 0, 0, 0, 0));
        assertFalse(keys.consumed());
        assertNull(route(false, false, MenuKeys.NO_KEY, true, false, 0, 0, 0, 0));
        assertFalse(keys.consumed());
        assertFalse(bar.isOpen());
    }

    @Test
    void aMenuThatBecameLockedClosesBeforeKeysAreRead() {
        altKey('f');
        available = action -> false;
        assertNull(press(false, true));
        assertFalse(bar.isOpen());
        assertFalse(keys.consumed());
    }

    @Test
    void consumedResetsEachFrame() {
        altKey('f');
        press(true, false);
        assertFalse(bar.isOpen());
        assertNull(arrows(1, 1, 1, 1));
        assertFalse(keys.consumed());
    }
}
