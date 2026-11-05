package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UiKitTest {

    @Test
    void panelEdgesHaveCornersAndAnInsetTitle() {
        assertEquals("┌─ SHEET ─┐", Panel.topEdge(11, "SHEET", false));
        assertEquals("╔═ A ═╗", Panel.topEdge(7, "A", true));
        assertEquals("└────┘", Panel.bottomEdge(6, false));
        assertEquals("╚════╝", Panel.bottomEdge(6, true));
    }

    @Test
    void longTitlesAreShortenedAndTinyPanelsHaveNone() {
        String edge = Panel.topEdge(10, "VERY LONG TITLE", false);
        assertEquals(10, edge.length());
        assertTrue(edge.endsWith("─┐"));
        assertEquals("", Panel.titleLabel(6, "X"));
        assertEquals(6, Panel.topEdge(6, "X", false).length());
    }

    @Test
    void breadcrumbMatchesTheMockupColumns() {
        List<TopBar.Segment> segments = TopBar.layout(List.of("GLYPH WORKBENCH", "SAIKSISTT", "PAINT 8"));
        assertEquals(5, segments.size());
        assertEquals(1, segments.get(0).x());
        assertEquals(17, segments.get(1).x());
        assertEquals(19, segments.get(2).x());
        assertEquals(29, segments.get(3).x());
        assertEquals(31, segments.get(4).x());
        assertEquals(TopBar.Kind.TITLE, segments.get(0).kind());
        assertEquals(TopBar.Kind.TEXT, segments.get(2).kind());
        assertEquals(TopBar.Kind.ACCENT, segments.get(4).kind());
        assertEquals(TopBar.Kind.SEPARATOR, segments.get(1).kind());
        assertEquals(79 - 10 - 1, TopBar.rightX(79, "● MODIFIED"));
    }

    @Test
    void singleCrumbIsTheTitle() {
        assertEquals(TopBar.Kind.TITLE, TopBar.layout(List.of("A")).get(0).kind());
    }

    @Test
    void keycapsFollowTheMockupSpacing() {
        List<StatusBar.Cap> caps = StatusBar.layout(List.of(
            new StatusBar.Key("ESC", "BACK"), new StatusBar.Key("F1", "RULES")));
        assertEquals(1, caps.get(0).capX());
        assertEquals(" ESC ", caps.get(0).capText());
        assertEquals(7, caps.get(0).labelX());
        assertEquals(13, caps.get(1).capX());
        assertEquals(18, caps.get(1).labelX());
    }

    @Test
    void listRowsMapTwoCellsPerRow() {
        assertEquals(0, SelectList.rowAt(5, 5, 3));
        assertEquals(0, SelectList.rowAt(6, 5, 3));
        assertEquals(2, SelectList.rowAt(9, 5, 3));
        assertEquals(-1, SelectList.rowAt(11, 5, 3));
        assertEquals(-1, SelectList.rowAt(4, 5, 3));
        List<SelectList.Row> rows = List.of(
            new SelectList.Row("HEAD", false, false, 0), new SelectList.Row("ONE", true, false, 0));
        assertFalse(SelectList.isSelectable(rows, 0));
        assertTrue(SelectList.isSelectable(rows, 1));
        assertFalse(SelectList.isSelectable(rows, 5));
    }

    @Test
    void wrapBreaksAtSpacesAndKeepsParagraphs() {
        assertEquals(List.of("aaa", "bbb", "cc"), ScrollText.wrap("aaa bbb cc", 3));
        assertEquals(List.of("abcd", "ef"), ScrollText.wrap("abcdef", 4));
        assertEquals(List.of("a", "b"), ScrollText.wrap("a\nb", 5));
        assertEquals(List.of(""), ScrollText.wrap("", 5));
    }

    @Test
    void scrollbarThumbMath() {
        assertEquals(10, ScrollText.thumbSize(10, 5, 8));
        assertEquals(5, ScrollText.thumbSize(10, 20, 10));
        assertEquals(1, ScrollText.thumbSize(10, 1000, 2));
        assertEquals(0, ScrollText.thumbStart(10, 20, 10, 0));
        assertEquals(5, ScrollText.thumbStart(10, 20, 10, 10));
        assertEquals(0, ScrollText.thumbStart(10, 5, 8, 0));
    }

    @Test
    void scrollOffsetStaysInRange() {
        ScrollText text = new ScrollText();
        text.setText("a b c d e f g h", 1);
        assertEquals(8, text.lineCount());
        text.scrollBy(100);
        assertTrue(text.offset() <= 7);
        text.scrollBy(-100);
        assertEquals(0, text.offset());
    }

    @Test
    void dialogKeepsButtonsAwayFromTheFrameAndIsCentred() {
        List<Dialog.Button> buttons = List.of(
            new Dialog.Button("S", "SAVE", Dialog.Tone.OK),
            new Dialog.Button("D", "DISCARD", Dialog.Tone.WARN),
            new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
        Dialog.Layout layout = Dialog.layout(
            DialogContent.of("UNSAVED CHANGES", List.of("Save changes to 8.txt", "before leaving?"), buttons),
            new CellRect(0, 0, 79, 41));
        int last = layout.buttonXs().get(2) + buttons.get(2).key().length() + 3 + buttons.get(2).label().length();
        assertTrue(layout.buttonXs().get(0) >= layout.x() + 3);
        assertTrue(last <= layout.x() + layout.width() - 3);
        assertEquals((79 - layout.width()) / 2, layout.x());
        assertEquals((41 - layout.height()) / 2, layout.y());
        assertTrue(layout.buttonY() > layout.textY() + 1);
        assertTrue(layout.buttonY() < layout.y() + layout.height() - 1);
    }

    @Test
    void messageStripResultsClearOnTheNextAction() {
        MessageStrip strip = new MessageStrip();
        strip.setCondition("c", false, "cond");
        strip.ok("saved");
        assertEquals(2, strip.visible(5).size());
        strip.nextAction();
        List<MessageStrip.Message> left = strip.visible(5);
        assertEquals(1, left.size());
        assertEquals("cond", left.get(0).text());
    }

    @Test
    void messageStripConditionGoesWhenCleared() {
        MessageStrip strip = new MessageStrip();
        strip.setCondition("gap", true, "ink");
        strip.setCondition("gap", true, "more ink");
        assertEquals(1, strip.visible(5).size());
        assertEquals("more ink", strip.visible(5).get(0).text());
        strip.clearCondition("gap");
        assertTrue(strip.visible(5).isEmpty());
    }

    @Test
    void messageStripListsWarningsFirstAndConditionsBeforeResults() {
        MessageStrip strip = new MessageStrip();
        strip.ok("result ok");
        strip.warn("result warn");
        strip.setCondition("a", false, "cond ok");
        strip.setCondition("b", true, "cond warn");
        List<MessageStrip.Message> all = strip.visible(10);
        assertEquals("cond warn", all.get(0).text());
        assertEquals("result warn", all.get(1).text());
        assertEquals("cond ok", all.get(2).text());
        assertEquals("result ok", all.get(3).text());
    }

    @Test
    void messageStripKeepsAWarningWhenLinesRunOut() {
        MessageStrip strip = new MessageStrip();
        strip.setCondition("a", false, "one");
        strip.ok("two");
        strip.ok("three");
        strip.warn("careful");
        List<MessageStrip.Message> shown = strip.visible(2);
        assertEquals(2, shown.size());
        assertEquals("careful", shown.get(0).text());
        assertTrue(shown.get(0).warning());
        assertEquals("one", shown.get(1).text());
        assertTrue(strip.visible(0).isEmpty());
    }

    @Test
    void messageStripScrollsToTheNewestResultWhenItWouldBeCutOff() {
        MessageStrip strip = fourMessagesWithNewestResultLast();
        List<MessageStrip.Message> shown = strip.visible(2);
        assertEquals(2, shown.size());
        assertEquals("third", shown.get(0).text());
        assertEquals("Undid move", shown.get(1).text());
        assertTrue(strip.hiddenAbove());
        assertFalse(strip.hiddenBelow());
    }

    private static MessageStrip fourMessagesWithNewestResultLast() {
        MessageStrip strip = new MessageStrip();
        strip.setCondition("a", false, "first");
        strip.setCondition("b", false, "second");
        strip.setCondition("c", false, "third");
        strip.ok("Undid move");
        return strip;
    }

    @Test
    void messageStripScrollOffsetStaysWithinTheList() {
        MessageStrip strip = fourMessagesWithNewestResultLast();
        strip.visible(2);
        strip.scrollBy(-100);
        assertEquals("first", strip.visible(2).get(0).text());
        strip.scrollBy(100);
        List<MessageStrip.Message> shown = strip.visible(2);
        assertEquals("third", shown.get(0).text());
        assertEquals(2, shown.size());
    }

    @Test
    void messageStripReportsMessagesHiddenAboveAndBelow() {
        MessageStrip strip = fourMessagesWithNewestResultLast();
        strip.visible(2);
        strip.scrollBy(-1);
        strip.visible(2);
        assertTrue(strip.hiddenAbove());
        assertTrue(strip.hiddenBelow());
        strip.scrollBy(-1);
        strip.visible(2);
        assertFalse(strip.hiddenAbove());
        assertTrue(strip.hiddenBelow());
        assertFalse(new MessageStrip().hiddenAbove());
        assertFalse(new MessageStrip().hiddenBelow());
    }

    @Test
    void messageStripJumpsToANewResultEvenWhenScrolledAway() {
        MessageStrip strip = fourMessagesWithNewestResultLast();
        strip.visible(2);
        strip.scrollBy(-100);
        assertEquals("first", strip.visible(2).get(0).text());
        strip.ok("Saved");
        List<MessageStrip.Message> shown = strip.visible(2);
        assertEquals("Saved", shown.get(shown.size() - 1).text());
        assertFalse(strip.hiddenBelow());
    }

    @Test
    void messageStripDoesNotJumpForANewCondition() {
        MessageStrip strip = fourMessagesWithNewestResultLast();
        strip.visible(2);
        strip.scrollBy(-100);
        strip.visible(2);
        strip.setCondition("d", false, "fourth");
        List<MessageStrip.Message> shown = strip.visible(2);
        assertEquals("first", shown.get(0).text());
        assertTrue(strip.hiddenBelow());
    }

    @Test
    void messageStripButtonsAreUsableOnlyTowardsHiddenMessages() {
        MessageStrip strip = fourMessagesWithNewestResultLast();
        strip.place(10, 20, 30, 4);
        strip.visible(2);
        // The list sits at its bottom: the up button is usable, the down button is not, but both can be hit.
        assertTrue(strip.hiddenAbove());
        assertFalse(strip.hiddenBelow());
        int buttonColumn = 10 + 30 - 2;
        strip.click(buttonColumn, 22);
        assertEquals("third", strip.visible(2).get(0).text());
        strip.click(buttonColumn, 21);
        assertEquals("second", strip.visible(2).get(0).text());
        strip.click(buttonColumn - 1, 21);
        assertEquals("second", strip.visible(2).get(0).text());
        strip.click(buttonColumn, 22);
        assertEquals("third", strip.visible(2).get(0).text());
    }

    @Test
    void messageStripKnowsWhichCellsBelongToIt() {
        MessageStrip strip = new MessageStrip();
        assertFalse(strip.contains(0, 0));
        strip.place(10, 20, 30, 4);
        assertTrue(strip.contains(10, 20));
        assertTrue(strip.contains(39, 23));
        assertFalse(strip.contains(40, 23));
        assertFalse(strip.contains(39, 24));
    }

    @Test
    void messageStripLeavesMessagesThatFitUntouchedAtAnyTime() {
        MessageStrip strip = new MessageStrip();
        strip.ok("short");
        MessageStrip.Message message = strip.visible(1).get(0);
        strip.advance(30f);
        assertEquals("short", strip.lineText(message, 5));
    }

    @Test
    void messageStripScrollsOverlongMessagesAndRestartsWhenTheTextChanges() {
        MessageStrip strip = new MessageStrip();
        strip.setCondition("long", false, "0123456789");
        MessageStrip.Message first = strip.visible(1).get(0);
        assertEquals("01234", strip.lineText(first, 5));
        strip.advance(30f);
        String scrolled = strip.lineText(first, 5);
        assertEquals(5, scrolled.length());
        assertTrue("0123456789".contains(scrolled));

        strip.setCondition("long", false, "abcdefghij");
        MessageStrip.Message replaced = strip.visible(1).get(0);
        assertEquals("abcde", strip.lineText(replaced, 5));
    }

    @Test
    void messageStripScrollsEachOverlongMessageOnItsOwnTimer() {
        MessageStrip strip = new MessageStrip();
        strip.setCondition("a", false, "0123456789");
        MessageStrip.Message early = strip.visible(1).get(0);
        strip.lineText(early, 5);
        strip.advance(3f);
        strip.setCondition("b", false, "abcdefghij");
        MessageStrip.Message late = strip.visible(2).get(1);
        assertEquals("abcde", strip.lineText(late, 5));
        assertEquals("56789", strip.lineText(early, 5));
    }
}
