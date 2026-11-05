package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReviewFileTest {

    @Test
    void formatsOneSortedLinePerGlyph() {
        ReviewFile file = new ReviewFile();
        file.put(new ReviewFile.Answer(0x62, "mmpx", "round"));
        file.put(new ReviewFile.Answer(0x41, "nearest@8", ""));
        file.put(new ReviewFile.Answer(0x1F600, "none", "x"));
        assertEquals("U+0041\tnearest@8\t\nU+0062\tmmpx\tround\nU+1F600\tnone\tx\n", file.format());
    }

    @Test
    void roundTripsNotesWithSpecialCharacters() {
        String note = "back\\slash, tab\t, new\nline, and a literal \\n";
        ReviewFile file = new ReviewFile();
        file.put(new ReviewFile.Answer(0x41, "light", note));
        ReviewFile reread = ReviewFile.parse(file.format());
        assertEquals(note, reread.get(0x41).note());
        assertEquals("light", reread.get(0x41).pick());
        assertEquals(file.format(), reread.format());
    }

    @Test
    void escapedNoteStaysOnOneLine() {
        assertEquals("a\\tb\\nc\\\\d", ReviewFile.escape("a\tb\nc\\d"));
        assertEquals("a\tb\nc\\d", ReviewFile.unescape("a\\tb\\nc\\\\d"));
    }

    @Test
    void longNotesAreKeptWhole() {
        String note = "word ".repeat(2000);
        ReviewFile file = new ReviewFile();
        file.put(new ReviewFile.Answer(0x41, "fill", note));
        assertEquals(note, ReviewFile.parse(file.format()).get(0x41).note());
    }

    @Test
    void laterAnswersReplaceEarlierOnes() {
        ReviewFile file = new ReviewFile();
        file.put(new ReviewFile.Answer(0x41, "fill", "a"));
        file.put(new ReviewFile.Answer(0x41, "trim", "b"));
        assertEquals("trim", file.get(0x41).pick());
        assertEquals(1, file.all().size());
    }

    @Test
    void parseSortsUnsortedInputAndSkipsBlankLines() {
        ReviewFile file = ReviewFile.parse("U+0062\tfill\t\n\nU+0041\ttrim\tn\n");
        assertEquals("U+0041\ttrim\tn\nU+0062\tfill\t\n", file.format());
    }

    @Test
    void badLinesReportTheirLineNumber() {
        IllegalArgumentException missingTabs = assertThrows(IllegalArgumentException.class,
            () -> ReviewFile.parse("U+0041\tfill\t\nU+0042 fill\n"));
        assertTrue(missingTabs.getMessage().startsWith("Line 2"));
        assertThrows(IllegalArgumentException.class, () -> ReviewFile.parse("0041\tfill\t\n"));
        assertThrows(IllegalArgumentException.class, () -> ReviewFile.parse("U+ZZ\tfill\t\n"));
        assertThrows(IllegalArgumentException.class, () -> new ReviewFile.Answer(0x41, "", ""));
    }

    @Test
    void emptyFileHasNoAnswers() {
        assertTrue(ReviewFile.parse("").isEmpty());
        assertEquals("", new ReviewFile().format());
        assertNull(new ReviewFile().get(0x41));
        assertFalse(new ReviewFile().has(0x41));
    }

    @Test
    void nextUnansweredSkipsBlankAndAnsweredSlotsAndWraps() {
        int[] codePoints = {0x41, 0x42, 0x43, 0x44};
        boolean[] blank = {false, false, true, false};
        ReviewFile file = new ReviewFile();
        file.put(new ReviewFile.Answer(0x42, "fill", ""));
        assertEquals(3, file.nextUnanswered(codePoints, blank, 0));
        assertEquals(0, file.nextUnanswered(codePoints, blank, 3));
        file.put(new ReviewFile.Answer(0x44, "none", ""));
        file.put(new ReviewFile.Answer(0x41, "fill", ""));
        assertEquals(-1, file.nextUnanswered(codePoints, blank, 0));
    }

    @Test
    void pickIdsCarryTheSourceSizeOnlyWithSeveralSources() {
        assertEquals("nearest", Upscaler.pickId(Upscaler.Method.NEAREST, 8, false));
        assertEquals("scale2x@16", Upscaler.pickId(Upscaler.Method.SCALE2X, 16, true));
        assertEquals("light", Upscaler.Method.LIGHT.id());
    }
}
