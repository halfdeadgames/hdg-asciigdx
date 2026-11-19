package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WrapLayoutTest {

    private static List<String> lineTexts(WrapLayout layout, String text) {
        List<String> lines = new ArrayList<>();
        for (int line = 0; line < layout.getLineCount(); line++) {
            lines.add(text.substring(layout.getLineStart(line), layout.getLineEnd(line)));
        }
        return lines;
    }

    private static List<String> wrap(String text, WrapRules rules) {
        WrapLayout layout = new WrapLayout();
        layout.layOut(text, rules);
        return lineTexts(layout, text);
    }

    @Test
    void beforeFirstLayOutThereAreNoLines() {
        WrapLayout layout = new WrapLayout();

        assertEquals(0, layout.getLineCount());
        assertNull(layout.getRules());
        assertFalse(layout.isTruncated());
    }

    @Test
    void wordsWrapAtSpaces() {
        assertEquals(List.of("the quick", "brown fox"),
                wrap("the quick brown fox", WrapRules.between(0, 9)));
    }

    @Test
    void longWordIsSplitAndFillsTheLine() {
        assertEquals(List.of("abcd", "efgh", "ij"), wrap("abcdefghij", WrapRules.between(0, 3)));
    }

    @Test
    void characterModeFillsEachLine() {
        WrapRules rules = WrapRules.between(0, 3).withBreakMode(BreakMode.CHARACTER);

        assertEquals(List.of("ab c", "d ef"), wrap("ab cd ef", rules));
    }

    @Test
    void spacesAtTheWrapPointAreDropped() {
        assertEquals(List.of("aaaa", "bbbb"), wrap("aaaa  bbbb", WrapRules.between(0, 3)));
        assertEquals(List.of("ab", "cd"), wrap("ab   cd", WrapRules.between(0, 3)));
        assertEquals(List.of("aaaa", "bbbb"), wrap("aaaa  \nbbbb", WrapRules.between(0, 3)));
    }

    @Test
    void paragraphIndentAfterNewlineIsKept() {
        assertEquals(List.of("ab", "  cd"), wrap("ab\n  cd", WrapRules.between(0, 9)));
    }

    @Test
    void newlineAndCarriageReturnLineFeedBothBreak() {
        assertEquals(List.of("ab", "cd", "ef"), wrap("ab\ncd\r\nef", WrapRules.between(0, 9)));
    }

    @Test
    void consecutiveNewlinesGiveEmptyLines() {
        assertEquals(List.of("a", "", "b"), wrap("a\n\nb", WrapRules.between(0, 9)));
    }

    @Test
    void trailingNewlineAddsNoLine() {
        assertEquals(List.of("a"), wrap("a\n", WrapRules.between(0, 9)));
        assertEquals(List.of("a"), wrap("a\r\n", WrapRules.between(0, 9)));
    }

    @Test
    void emptyTextGivesNoLines() {
        assertEquals(List.of(), wrap("", WrapRules.between(0, 9)));
    }

    @Test
    void onlyTheVeryFirstLineUsesTheFirstLineColumns() {
        WrapRules rules = WrapRules.between(5, 9).withWrappedLineColumns(0, 9);
        String text = "abcde fghij\nklm";
        WrapLayout layout = new WrapLayout();

        layout.layOut(text, rules);

        assertEquals(List.of("abcde", "fghij", "klm"), lineTexts(layout, text));
        assertEquals(5, layout.getLineStartColumn(0));
        assertEquals(9, layout.getLineEndColumn(0));
        assertEquals(0, layout.getLineStartColumn(1));
        assertEquals(0, layout.getLineStartColumn(2));
        assertEquals(9, layout.getLineEndColumn(2));
    }

    @Test
    void lineLimitCutsTextAndMarksTruncated() {
        String text = "one two three four";
        WrapLayout layout = new WrapLayout();

        layout.layOut(text, WrapRules.between(0, 7).withMaxLines(2));

        assertEquals(List.of("one two", "three"), lineTexts(layout, text));
        assertTrue(layout.isTruncated());
    }

    @Test
    void lastLineIsShortenedToLeaveRoomForTheEllipsisAndTrailingSpacesAreTrimmed() {
        String text = "abcd efgh";
        WrapRules rules = WrapRules.between(0, 5).withMaxLines(1)
                .withBreakMode(BreakMode.CHARACTER);
        WrapLayout layout = new WrapLayout();

        layout.layOut(text, rules);

        assertEquals(List.of("abcd"), lineTexts(layout, text));
        assertTrue(layout.isTruncated());
    }

    @Test
    void remainingSpacesAndNewlinesDoNotTruncate() {
        String text = "ab\n  \n";
        WrapLayout layout = new WrapLayout();

        layout.layOut(text, WrapRules.between(0, 9).withMaxLines(1));

        assertEquals(List.of("ab"), lineTexts(layout, text));
        assertFalse(layout.isTruncated());
    }

    @Test
    void widthOneLineKeepsOnlyTheEllipsis() {
        String text = "abc";
        WrapLayout layout = new WrapLayout();

        layout.layOut(text, WrapRules.between(0, 0).withMaxLines(1));

        assertEquals(List.of(""), lineTexts(layout, text));
        assertTrue(layout.isTruncated());
    }

    @Test
    void layingOutAgainReplacesTheEarlierResult() {
        WrapLayout layout = new WrapLayout();
        layout.layOut("one two three four", WrapRules.between(0, 2).withMaxLines(2));

        layout.layOut("ab", WrapRules.between(0, 9));

        assertEquals(List.of("ab"), lineTexts(layout, "ab"));
        assertFalse(layout.isTruncated());
    }

    @Test
    void manyLinesGrowTheStorage() {
        String text = "a\n".repeat(100) + "b";
        WrapLayout layout = new WrapLayout();

        layout.layOut(text, WrapRules.between(0, 9));

        assertEquals(101, layout.getLineCount());
        assertEquals(text.length() - 1, layout.getLineStart(100));
    }

    @Test
    void supplementaryCodePointsTakeOneCell() {
        String text = "😀😁😂";

        assertEquals(List.of("😀😁", "😂"),
                wrap(text, WrapRules.between(0, 1)));
    }

    @Test
    void badLineIndexIsRejected() {
        WrapLayout layout = new WrapLayout();
        layout.layOut("ab", WrapRules.between(0, 9));

        assertThrows(IndexOutOfBoundsException.class, () -> layout.getLineStart(1));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.getLineEnd(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.getLineStartColumn(1));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.getLineEndColumn(1));
    }

    @Test
    void nullArgumentsAreRejected() {
        WrapLayout layout = new WrapLayout();

        assertThrows(NullPointerException.class, () -> layout.layOut(null, WrapRules.between(0, 1)));
        assertThrows(NullPointerException.class, () -> layout.layOut("a", null));
    }
}
