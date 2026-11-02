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
        Dialog.Layout layout = Dialog.layout("UNSAVED CHANGES",
            List.of("Save changes to 8.txt", "before leaving?"), buttons, 79, 41);
        int last = layout.buttonXs().get(2) + buttons.get(2).key().length() + 3 + buttons.get(2).label().length();
        assertTrue(layout.buttonXs().get(0) >= layout.x() + 3);
        assertTrue(last <= layout.x() + layout.width() - 3);
        assertEquals((79 - layout.width()) / 2, layout.x());
        assertEquals((41 - layout.height()) / 2, layout.y());
        assertTrue(layout.buttonY() > layout.textY() + 1);
        assertTrue(layout.buttonY() < layout.y() + layout.height() - 1);
    }

    @Test
    void messageStripKeepsTheNewest() {
        MessageStrip strip = new MessageStrip();
        strip.ok("one");
        strip.warn("two");
        strip.ok("three");
        List<MessageStrip.Message> recent = strip.recent(2);
        assertEquals("two", recent.get(0).text());
        assertTrue(recent.get(0).warning());
        assertEquals("three", recent.get(1).text());
        assertTrue(strip.recent(0).isEmpty());
    }
}
