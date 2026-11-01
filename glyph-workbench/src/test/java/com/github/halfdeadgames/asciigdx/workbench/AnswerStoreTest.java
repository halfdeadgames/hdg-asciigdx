package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnswerStoreTest {

    @Test
    void formatFollowsSheetOrderAndRoundTrips() {
        AnswerStore store = new AnswerStore();
        store.put(new GlyphAnswer('B', "scale2x-fill", "needs-work", "off-center"));
        store.put(new GlyphAnswer('A', "none", "needs-hand-drawn", ""));
        String text = store.format(new int[] {'A', 'B'});
        assertEquals(
            "U+0041\tA\tnone\tneeds-hand-drawn\t\t\nU+0042\tB\tscale2x-fill\tneeds-work\toff-center\t\n", text);

        AnswerStore loaded = AnswerStore.parse(text);
        assertEquals(2, loaded.size());
        assertEquals(store.get('B'), loaded.get('B'));
        assertEquals(store.get('A'), loaded.get('A'));
    }

    @Test
    void oldJoinedPicksLoadAsTheirFirstId() {
        AnswerStore loaded = AnswerStore.parse("U+0042\tB\tnearest=scale2x-fill\tneeds-work\toff-center\n");
        assertEquals("nearest", loaded.get('B').pick());
    }

    @Test
    void replacingAnAnswerKeepsOneLine() {
        AnswerStore store = new AnswerStore();
        store.put(new GlyphAnswer('A', "nearest", "perfect", ""));
        store.put(new GlyphAnswer('A', "scale2x", "needs-work", "too-similar"));
        assertEquals(1, store.size());
        assertEquals("scale2x", store.get('A').pick());
    }

    @Test
    void removeClearsTheAnswer() {
        AnswerStore store = new AnswerStore();
        store.put(new GlyphAnswer('A', "nearest", "perfect", ""));
        store.remove('A');
        assertFalse(store.has('A'));
        assertEquals("", store.format(new int[] {'A'}));
    }

    @Test
    void answersOffTheSheetAreKeptAtTheEnd() {
        AnswerStore store = new AnswerStore();
        store.put(new GlyphAnswer('Z', "nearest", "perfect", ""));
        store.put(new GlyphAnswer('A', "nearest", "perfect", ""));
        String[] lines = store.format(new int[] {'A'}).split("\n");
        assertTrue(lines[0].startsWith("U+0041"));
        assertTrue(lines[1].startsWith("U+005A"));
    }

    @Test
    void parseRejectsMalformedLines() {
        assertThrows(IllegalArgumentException.class, () -> AnswerStore.parse("garbage\n"));
        assertThrows(IllegalArgumentException.class, () -> AnswerStore.parse("U+0041\tA\tnearest\tperfect\n"));
        assertThrows(IllegalArgumentException.class, () -> AnswerStore.parse("U+00ZZ\tA\tnearest\tperfect\t\n"));
        assertThrows(IllegalArgumentException.class, () -> AnswerStore.parse("U+0041\tA\tnearest\tgreat\t\n"));
        assertThrows(IllegalArgumentException.class, () -> AnswerStore.parse("U+0041\tA\tnone\tperfect\t\n"));
        assertThrows(IllegalArgumentException.class, () -> AnswerStore.parse("U+0041\tA\t\tperfect\t\n"));
        assertThrows(
            IllegalArgumentException.class,
            () -> AnswerStore.parse("U+0041\tA\tnearest\tperfect\t\nU+0041\tA\tnearest\tperfect\t\n"));
    }

    @Test
    void parseReadsOldNoneVerdictAsNeedsHandDrawn() {
        AnswerStore loaded = AnswerStore.parse("U+0041\tA\tnone\tnone\t\n");
        assertEquals("needs-hand-drawn", loaded.get('A').verdict());
        assertEquals("U+0041\tA\tnone\tneeds-hand-drawn\t\t\n", loaded.format(new int[] {'A'}));
    }

    @Test
    void noteWithTabsBackslashesAndNewlinesRoundTrips() {
        String note = "tab\there, back\\slash, line\nbreak, literal \\n and \\t, end\\";
        AnswerStore store = new AnswerStore();
        store.put(new GlyphAnswer('A', "nearest", "needs-work", "off-center", note));
        String text = store.format(new int[] {'A'});
        assertEquals(1, text.chars().filter(c -> c == '\n').count());
        assertEquals(6, text.substring(0, text.length() - 1).split("\t", -1).length);
        AnswerStore loaded = AnswerStore.parse(text);
        assertEquals(note, loaded.get('A').note());
        assertEquals("off-center", loaded.get('A').reason());
    }

    @Test
    void escapingUsesTheDocumentedSequences() {
        assertEquals("a\\\\b\\tc\\nd", AnswerStore.escapeNote("a\\b\tc\nd"));
        assertEquals("a\\b\tc\nd", AnswerStore.unescapeNote("a\\\\b\\tc\\nd"));
        assertEquals("\\x", AnswerStore.unescapeNote("\\x"));
    }

    @Test
    void oldFiveColumnLinesLoadWithAnEmptyNote() {
        AnswerStore loaded = AnswerStore.parse("U+0042\tB\tnearest\tneeds-work\toff-center\n");
        assertEquals("off-center", loaded.get('B').reason());
        assertEquals("", loaded.get('B').note());
    }

    @Test
    void sixColumnLineReadsTheNote() {
        AnswerStore loaded = AnswerStore.parse("U+0042\tB\tnearest\tperfect\t\tlooks good\n");
        assertEquals("looks good", loaded.get('B').note());
    }

    @Test
    void parseRejectsSevenColumns() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AnswerStore.parse("U+0042\tB\tnearest\tperfect\t\tnote\textra\n"));
    }

    @Test
    void parseAcceptsEmptyTextAndWindowsLineEndings() {
        assertEquals(0, AnswerStore.parse("").size());
        assertEquals(1, AnswerStore.parse("U+0041\tA\tnearest\tperfect\t\r\n").size());
    }

    @Test
    void nextUnansweredSkipsBlankAndAnsweredAndWraps() {
        int[] codePoints = {'A', 'B', 'C', 'D'};
        boolean[] blank = {false, true, false, false};
        AnswerStore store = new AnswerStore();
        store.put(new GlyphAnswer('C', "nearest", "perfect", ""));
        assertEquals(3, store.nextUnanswered(codePoints, blank, 0));
        assertEquals(0, store.nextUnanswered(codePoints, blank, 3));
        store.put(new GlyphAnswer('A', "nearest", "perfect", ""));
        store.put(new GlyphAnswer('D', "nearest", "perfect", ""));
        assertEquals(-1, store.nextUnanswered(codePoints, blank, 0));
    }
}
