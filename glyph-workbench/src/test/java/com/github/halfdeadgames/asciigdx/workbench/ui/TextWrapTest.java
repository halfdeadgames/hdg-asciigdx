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
    void widthZeroWrapsLikeWidthOne() {
        assertEquals(List.of("a", "b", "c"), TextWrap.wrap("abc", 0));
        assertEquals(TextWrap.wrap("ab cd", 1), TextWrap.wrap("ab cd", 0));
    }

    @Test
    void negativeWidthWrapsLikeWidthOne() {
        assertEquals(List.of("a", "b", "c"), TextWrap.wrap("abc", -3));
        assertEquals(TextWrap.wrap("ab cd", 1), TextWrap.wrap("ab cd", -3));
    }
}
