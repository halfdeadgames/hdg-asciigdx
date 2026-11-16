package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DialogTextTest {

    @Test
    void shortMessageStaysOnTheFirstLine() {
        assertEquals(List.of("hello", ""), DialogText.wrapToTwoLines("hello"));
    }

    @Test
    void longMessageSplitsAtTheLastSpaceThatFits() {
        assertEquals(List.of("aaaaaaaaaaaaaaaaaaaaaaaaa", "bb"),
                DialogText.wrapToTwoLines("a".repeat(25) + " bb"));
    }

    @Test
    void longMessageWithoutASpaceSplitsAtTheLineWidth() {
        assertEquals(List.of("a".repeat(26), "a".repeat(4)),
                DialogText.wrapToTwoLines("a".repeat(30)));
    }

    @Test
    void supplementaryCharacterAtTheBoundaryIsNotSplit() {
        String dagger = "🗡";
        String message = "a".repeat(25) + dagger + "b".repeat(4);
        assertEquals(List.of("a".repeat(25) + dagger, "b".repeat(4)),
                DialogText.wrapToTwoLines(message));
        assertEquals("a".repeat(25) + dagger, DialogText.cut(message));
        assertEquals("a".repeat(23) + dagger + "..",
                DialogText.ellipsize("a".repeat(23) + dagger + "bbbb"));
    }

    @Test
    void secondLineIsCutWhenStillTooLong() {
        assertEquals("b".repeat(26), DialogText.wrapToTwoLines("a " + "b".repeat(40)).get(1));
    }

    @Test
    void cutKeepsAtMostTheLineWidth() {
        assertEquals("a".repeat(26), DialogText.cut("a".repeat(40)));
        assertEquals("abc", DialogText.cut("abc"));
    }

    @Test
    void ellipsizeEndsALongLineWithTwoDots() {
        assertEquals("a".repeat(24) + "..", DialogText.ellipsize("a".repeat(27)));
        assertEquals("a".repeat(26), DialogText.ellipsize("a".repeat(26)));
    }

    @Test
    void typedLineShowsTextCursorAndPadding() {
        assertEquals("ab_" + " ".repeat(23), DialogText.typedLine("ab", '_', 26));
    }

    @Test
    void typedLineScrollsToKeepTheCursorVisible() {
        assertEquals("b".repeat(25) + "_",
                DialogText.typedLine("a".repeat(5) + "b".repeat(25), '_', 26));
    }

    @Test
    void typedLineShowsUndrawableCharactersAsQuestionMarks() {
        assertEquals("a?_" + " ".repeat(23), DialogText.typedLine("a😀", '_', 26));
    }
}
