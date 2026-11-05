package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroupedPickListTest {

    private static SelectList.Row heading(String text) {
        return new SelectList.Row(text, false, false, 0);
    }

    private static SelectList.Row item(String text) {
        return new SelectList.Row(text, true, false, 0);
    }

    private static SelectList.Row dimmed(String text) {
        return new SelectList.Row(text, false, true, 0);
    }

    /** Rows: 0 H1, 1 a, 2 b, 3 H2, 4 c, 5 d, 6 e. */
    private static GroupedPickList<String> sample() {
        var list = new GroupedPickList<String>();
        list.setRows(
            List.of(heading("H1"), item("a"), item("b"), heading("H2"), item("c"), item("d"), item("e")),
            Arrays.asList(null, "a", "b", null, "c", "d", "e"));
        return list;
    }

    @Test
    void setRowsStartsWithoutSelection() {
        var list = sample();
        assertFalse(list.hasSelection());
        assertEquals(-1, list.selectedIndex());
        assertNull(list.selected());
    }

    @Test
    void moveDownFromNothingStartsAtTheTopAndSkipsHeadings() {
        var list = sample();
        list.move(1);
        assertEquals(1, list.selectedIndex());
        assertEquals("a", list.selected());
        list.move(1);
        list.move(1);
        assertEquals("c", list.selected());
    }

    @Test
    void moveStaysPutAtBothEnds() {
        var list = sample();
        list.move(1);
        list.move(-1);
        assertEquals(1, list.selectedIndex());
        for (int i = 0; i < 20; i++) {
            list.move(1);
        }
        assertEquals(6, list.selectedIndex());
        list.move(1);
        assertEquals(6, list.selectedIndex());
    }

    @Test
    void moveWithZeroStepDoesNothing() {
        var list = sample();
        list.move(0);
        assertFalse(list.hasSelection());
    }

    @Test
    void moveSkipsDimmedRowsWithoutEntries() {
        var list = new GroupedPickList<String>();
        list.setRows(List.of(item("a"), dimmed("x"), item("b")), Arrays.asList("a", null, "b"));
        list.move(1);
        list.move(1);
        assertEquals(2, list.selectedIndex());
        list.move(-1);
        assertEquals(0, list.selectedIndex());
    }

    @Test
    void hoverIgnoresHeadingsAndOutOfRangeRows() {
        var list = sample();
        list.move(1);
        list.hover(0);
        list.hover(3);
        list.hover(-1);
        list.hover(99);
        assertEquals(1, list.selectedIndex());
        list.hover(5);
        assertEquals(5, list.selectedIndex());
    }

    @Test
    void isChoosableNeedsARangeAndAnEntry() {
        var list = sample();
        assertTrue(list.isChoosable(1));
        assertFalse(list.isChoosable(0));
        assertFalse(list.isChoosable(-1));
        assertFalse(list.isChoosable(7));
    }

    @Test
    void capacityLimitsShownAndVisibleRows() {
        var list = sample();
        assertEquals(7, list.shownRows());
        assertFalse(list.scrolls());
        list.setCapacity(3);
        assertEquals(3, list.shownRows());
        assertTrue(list.scrolls());
        assertEquals(List.of("H1", "a", "b"), texts(list.visibleRows()));
    }

    @Test
    void cursorFollowKeepsTheHeadingAboveInView() {
        var list = sample();
        list.setCapacity(3);
        list.move(1);
        list.move(1);
        list.move(1);
        assertEquals(4, list.selectedIndex());
        assertTrue(list.window().isVisible(3));
        assertTrue(list.window().isVisible(4));
        assertEquals(List.of("b", "H2", "c"), texts(list.visibleRows()));
    }

    @Test
    void cursorFollowDoesNotTreatADimmedRowAsAHeading() {
        var list = new GroupedPickList<String>();
        list.setRows(List.of(item("a"), item("b"), dimmed("x"), item("c"), item("d")),
            Arrays.asList("a", "b", null, "c", "d"));
        list.setCapacity(2);
        list.move(1);
        list.move(1);
        list.move(1);
        assertEquals(3, list.selectedIndex());
        list.wheel(1);
        list.move(1);
        list.move(-1);
        assertEquals(3, list.selectedIndex());
        assertEquals(3, list.window().offset());
        assertEquals(List.of("c", "d"), texts(list.visibleRows()));
    }

    @Test
    void visibleSelectedIndexIsMinusOneWhenScrolledOut() {
        var list = sample();
        list.setCapacity(3);
        list.move(1);
        assertEquals(1, list.visibleSelectedIndex());
        list.wheel(100);
        assertEquals(-1, list.visibleSelectedIndex());
    }

    @Test
    void wheelMovesTheWindowNotTheCursor() {
        var list = sample();
        list.setCapacity(3);
        list.move(1);
        list.wheel(2);
        assertEquals(2, list.window().offset());
        assertEquals(1, list.selectedIndex());
        list.wheel(-100);
        assertEquals(0, list.window().offset());
    }

    @Test
    void listWidthCountsTheMarkerOnSelectableRows() {
        var list = sample();
        assertEquals("► ".length() + 1, list.listWidth());
    }

    @Test
    void setRowsRejectsSizeMismatch() {
        var list = new GroupedPickList<String>();
        List<SelectList.Row> rows = new ArrayList<>(List.of(item("a"), item("b")));
        assertThrows(IllegalArgumentException.class, () -> list.setRows(rows, Arrays.asList("a")));
    }

    @Test
    void setRowsResetsSelectionAndWindow() {
        var list = sample();
        list.setCapacity(3);
        list.move(1);
        list.wheel(2);
        list.setRows(List.of(item("z")), List.of("z"));
        assertFalse(list.hasSelection());
        assertEquals(0, list.window().offset());
        assertEquals(1, list.rows().size());
    }

    @Test
    void clearResetsEverything() {
        var list = sample();
        list.setCapacity(3);
        list.move(1);
        list.clear();
        assertTrue(list.rows().isEmpty());
        assertFalse(list.hasSelection());
        assertNull(list.selected());
        assertEquals(0, list.shownRows());
        list.setRows(List.of(item("a"), item("b"), item("c"), item("d")), List.of("a", "b", "c", "d"));
        assertEquals(4, list.shownRows());
        assertFalse(list.scrolls());
    }

    private static List<String> texts(List<SelectList.Row> rows) {
        return rows.stream().map(SelectList.Row::text).toList();
    }
}
