package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NewGlyphPromptTest {

    private static NewGlyphPrompt openPrompt(String typed) {
        var prompt = new NewGlyphPrompt();
        prompt.open();
        for (int i = 0; i < typed.length(); i++) {
            prompt.type(typed.charAt(i));
        }
        return prompt;
    }

    private static String padded(String text) {
        return text + " ".repeat(26 - text.length());
    }

    @Test
    void startsClosedAndTracksOpenAndClose() {
        var prompt = new NewGlyphPrompt();
        assertFalse(prompt.isOpen());
        prompt.open();
        assertTrue(prompt.isOpen());
        prompt.close();
        assertFalse(prompt.isOpen());
    }

    @Test
    void ignoresTypingWhileClosed() {
        var prompt = new NewGlyphPrompt();
        prompt.type('A');
        prompt.open();
        assertEquals(padded("_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void reopeningClearsEarlierText() {
        var prompt = openPrompt("41");
        prompt.close();
        prompt.open();
        assertEquals(padded("_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void acceptsSpacesButNotControlCharacters() {
        var prompt = openPrompt("a \n\t\u0085b");
        assertEquals(padded("a b_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void capsTextAtTenChars() {
        var prompt = openPrompt("0123456789ABC");
        assertEquals(padded("0123456789_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void backspaceRemovesOneCharacter() {
        var prompt = openPrompt("U+41");
        prompt.backspace();
        assertEquals(padded("U+4_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void backspaceRemovesWholeSurrogatePair() {
        var prompt = openPrompt("a" + new String(Character.toChars(0x1F600)));
        prompt.backspace();
        assertEquals(padded("a_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void backspaceOnEmptyTextDoesNothing() {
        var prompt = openPrompt("");
        prompt.backspace();
        assertEquals(padded("_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void lineOneIsAlwaysTwentySixCells() {
        assertEquals(26, NewGlyphPrompt.LINE_CELLS);
        assertEquals(26, openPrompt("").lines(cp -> false).get(0).length());
        assertEquals(26, openPrompt("abcdefghij").lines(cp -> false).get(0).length());
    }

    @Test
    void alwaysReturnsThreeLines() {
        assertEquals(3, openPrompt("").lines(cp -> false).size());
        assertEquals(3, openPrompt("U+ZZ").lines(cp -> false).size());
        assertEquals(3, openPrompt("41").lines(cp -> true).size());
    }

    @Test
    void nonBmpCodePointsShowAsOneQuestionMark() {
        var prompt = openPrompt(new String(Character.toChars(0x1F600)));
        assertEquals(padded("?_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void loneSurrogateShowsAsQuestionMark() {
        var prompt = openPrompt("\uD83D");
        assertEquals(padded("?_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void emptyTextWrapsTheHintOverTwoLines() {
        List<String> lines = openPrompt("").lines(cp -> false);
        assertEquals("Type a character or hex", lines.get(1));
        assertEquals("code", lines.get(2));
    }

    @Test
    void wholeDialogTextIsExact() {
        assertEquals(List.of(padded("_"), "Type a character or hex", "code"),
            openPrompt("").lines(cp -> false));
        assertEquals(List.of(padded("2603_"), "U+2603 ALREADY IN FONT", "SNOWMAN"),
            openPrompt("2603").lines(cp -> true));
        assertEquals(List.of(padded("U+ZZ_"), "Use 1 to 6 hex digits,", "like 41 or U+0041"),
            openPrompt("U+ZZ").lines(cp -> false));
    }

    @Test
    void validTextShowsCodeAndName() {
        List<String> snowman = openPrompt("U+2603").lines(cp -> false);
        assertEquals("U+2603", snowman.get(1));
        assertEquals("SNOWMAN", snowman.get(2));
        List<String> letter = openPrompt("A").lines(cp -> false);
        assertEquals("U+0041", letter.get(1));
        assertEquals("LATIN CAPITAL LETTER A", letter.get(2));
    }

    @Test
    void longNamesAreCutToTheLineWidth() {
        assertEquals("LATIN SMALL LETTER A WITH ", openPrompt("1EA5").lines(cp -> false).get(2));
    }

    @Test
    void existingSlotIsMarked() {
        assertEquals("U+2603 ALREADY IN FONT", openPrompt("2603").lines(cp -> cp == 0x2603).get(1));
        assertEquals("U+2603", openPrompt("2603").lines(cp -> cp == 0x41).get(1));
    }

    @Test
    void invalidTextWrapsTheErrorOverTwoLines() {
        var prompt = openPrompt("U+ZZ");
        assertFalse(prompt.result().isValid());
        assertEquals("Use 1 to 6 hex digits, like 41 or U+0041", prompt.result().error());
        List<String> lines = prompt.lines(cp -> true);
        assertEquals("Use 1 to 6 hex digits,", lines.get(1));
        assertEquals("like 41 or U+0041", lines.get(2));
    }

    @Test
    void goesToExistingOnlyForValidTextWithASlot() {
        assertTrue(openPrompt("2603").goesToExisting(cp -> cp == 0x2603));
        assertFalse(openPrompt("2603").goesToExisting(cp -> false));
        assertFalse(openPrompt("U+ZZ").goesToExisting(cp -> true));
        assertFalse(openPrompt("").goesToExisting(cp -> true));
    }

    @Test
    void resultParsesTheTypedText() {
        var result = openPrompt("U+41").result();
        assertTrue(result.isValid());
        assertEquals(0x41, result.codePoint());
    }
}
