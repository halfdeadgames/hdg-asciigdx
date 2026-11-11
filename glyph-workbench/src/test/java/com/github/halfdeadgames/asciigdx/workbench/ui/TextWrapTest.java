package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextWrapTest {

    @Test
    void emptyTextIsOneEmptyLine() {
        assertEquals(List.of(""), TextWrap.wrap("", 10));
    }

    @Test
    void shortTextStaysOnOneLine() {
        assertEquals(List.of("hello world"), TextWrap.wrap("hello world", 11));
    }

    @Test
    void breaksAtTheLastSpaceThatFits() {
        assertEquals(List.of("the quick", "brown fox"), TextWrap.wrap("the quick brown fox", 10));
    }

    @Test
    void spaceExactlyAtTheLineWidthBreaksThere() {
        assertEquals(List.of("abcde", "fgh"), TextWrap.wrap("abcde fgh", 5));
    }

    @Test
    void longWordsAreCutAtTheWidth() {
        assertEquals(List.of("abcd", "efgh", "ij"), TextWrap.wrap("abcdefghij", 4));
    }

    @Test
    void textEndingRightAfterABreakGivesAnEmptyLastLine() {
        assertEquals(List.of("abcde", ""), TextWrap.wrap("abcde ", 5));
    }

    @Test
    void noLineIsLongerThanTheWidth() {
        String text = "a".repeat(30) + " b " + "c".repeat(7) + "  d";
        for (String line : TextWrap.wrap(text, 8)) {
            assertEquals(true, line.length() <= 8);
        }
    }

    @Test
    void cursorFollowsTheLastLineAndDropsDownWhenItIsFull() {
        List<String> partial = TextWrap.wrap("abc", 5);
        assertEquals(0, TextWrap.cursorRow(partial, 5));
        assertEquals(3, TextWrap.cursorColumn(partial, 5));
        List<String> full = TextWrap.wrap("abcde", 5);
        assertEquals(1, TextWrap.cursorRow(full, 5));
        assertEquals(0, TextWrap.cursorColumn(full, 5));
    }
}
