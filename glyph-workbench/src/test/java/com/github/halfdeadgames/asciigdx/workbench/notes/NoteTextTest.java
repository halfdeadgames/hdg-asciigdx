package com.github.halfdeadgames.asciigdx.workbench.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class NoteTextTest {

    @Test
    void emptyTextIsOneEmptyLine() {
        assertEquals(List.of(""), NoteText.wrap("", 10));
    }

    @Test
    void shortTextStaysOnOneLine() {
        assertEquals(List.of("hello world"), NoteText.wrap("hello world", 11));
    }

    @Test
    void breaksAtTheLastSpaceThatFits() {
        assertEquals(List.of("the quick", "brown fox"), NoteText.wrap("the quick brown fox", 10));
    }

    @Test
    void spaceExactlyAtTheLineWidthBreaksThere() {
        assertEquals(List.of("abcde", "fgh"), NoteText.wrap("abcde fgh", 5));
    }

    @Test
    void longWordsAreCutAtTheWidth() {
        assertEquals(List.of("abcd", "efgh", "ij"), NoteText.wrap("abcdefghij", 4));
    }

    @Test
    void textEndingRightAfterABreakGivesAnEmptyLastLine() {
        assertEquals(List.of("abcde", ""), NoteText.wrap("abcde ", 5));
    }

    @Test
    void noLineIsLongerThanTheWidth() {
        String text = "a".repeat(30) + " b " + "c".repeat(7) + "  d";
        for (String line : NoteText.wrap(text, 8)) {
            assertEquals(true, line.length() <= 8);
        }
    }

    @Test
    void cursorFollowsTheLastLineAndDropsDownWhenItIsFull() {
        List<String> partial = NoteText.wrap("abc", 5);
        assertEquals(0, NoteText.cursorRow(partial, 5));
        assertEquals(3, NoteText.cursorColumn(partial, 5));
        List<String> full = NoteText.wrap("abcde", 5);
        assertEquals(1, NoteText.cursorRow(full, 5));
        assertEquals(0, NoteText.cursorColumn(full, 5));
    }
}
