package com.github.halfdeadgames.asciigdx.workbench.newitems;

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
    void acceptsOnlyCharactersThatCanFormAHexCode() {
        var prompt = openPrompt("a \n\t\u0085bgéxz-0Fu+U");
        assertEquals(padded("ab0Fu+U_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void truncatedSupplementaryCharacterIsIgnored() {
        var prompt = openPrompt("🗡");
        assertEquals(padded("_"), prompt.lines(cp -> false).get(0));
    }

    @Test
    void typedHexGivesSupplementaryCodePoint() {
        assertEquals(0x1F5E1, openPrompt("1F5E1").result().codePoint());
        assertEquals(0x1F5E1, openPrompt("U+1F5E1").result().codePoint());
    }

    @Test
    void typedLiteralGlyphGivesNoGlyph() {
        var prompt = openPrompt("é");
        assertFalse(prompt.result().isValid());
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
    void backspaceRemovesOnePlusSignAtATime() {
        var prompt = openPrompt("U+");
        prompt.backspace();
        assertEquals(padded("U_"), prompt.lines(cp -> false).get(0));
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
        assertEquals(26, openPrompt("abcdef0123").lines(cp -> false).get(0).length());
    }

    @Test
    void alwaysReturnsFourLinesWithABlankLineAfterTheInput() {
        assertEquals(4, openPrompt("").lines(cp -> false).size());
        assertEquals(4, openPrompt("U+").lines(cp -> false).size());
        assertEquals(4, openPrompt("41").lines(cp -> true).size());
        assertEquals("", openPrompt("41").lines(cp -> true).get(1));
    }

    @Test
    void emptyTextWrapsTheHintOverTwoLines() {
        List<String> lines = openPrompt("").lines(cp -> false);
        assertEquals("TYPE A HEX CODE, E.G. 263A", lines.get(2));
        assertEquals("OR U+1F5E1", lines.get(3));
    }

    @Test
    void wholeDialogTextIsExact() {
        assertEquals(List.of(padded("_"), "", "TYPE A HEX CODE, E.G. 263A", "OR U+1F5E1"),
            openPrompt("").lines(cp -> false));
        assertEquals(List.of(padded("2603_"), "", "U+2603 ALREADY IN FONT", "SNOWMAN"),
            openPrompt("2603").lines(cp -> true));
        assertEquals(List.of(padded("U+_"), "", "Add hex digits after U+", ""),
            openPrompt("U+").lines(cp -> false));
    }

    @Test
    void validTextShowsCodeAndName() {
        List<String> snowman = openPrompt("U+2603").lines(cp -> false);
        assertEquals("U+2603", snowman.get(2));
        assertEquals("SNOWMAN", snowman.get(3));
        List<String> letter = openPrompt("41").lines(cp -> false);
        assertEquals("U+0041", letter.get(2));
        assertEquals("LATIN CAPITAL LETTER A", letter.get(3));
    }

    @Test
    void longNamesWrapOntoASecondLine() {
        List<String> lines = openPrompt("E9").lines(cp -> false);
        assertEquals(5, lines.size());
        assertEquals("LATIN SMALL LETTER E WITH", lines.get(3));
        assertEquals("ACUTE", lines.get(4));
    }

    @Test
    void shortNamesKeepOneLine() {
        assertEquals(4, openPrompt("41").lines(cp -> false).size());
    }

    @Test
    void namesLongerThanTwoLinesAreCut() {
        List<String> lines = openPrompt("1EA5").lines(cp -> false);
        assertEquals("LATIN SMALL LETTER A WITH", lines.get(3));
        assertTrue(lines.get(4).length() <= NewGlyphPrompt.LINE_CELLS);
    }

    @Test
    void existingSlotIsMarked() {
        assertEquals("U+2603 ALREADY IN FONT", openPrompt("2603").lines(cp -> cp == 0x2603).get(2));
        assertEquals("U+2603", openPrompt("2603").lines(cp -> cp == 0x41).get(2));
    }

    @Test
    void invalidTextWrapsTheErrorOverTwoLines() {
        var prompt = openPrompt("000000041");
        assertFalse(prompt.result().isValid());
        assertEquals("Use 1 to 8 hex digits, like 41 or U+0041", prompt.result().error());
        List<String> lines = prompt.lines(cp -> true);
        assertEquals("Use 1 to 8 hex digits,", lines.get(2));
        assertEquals("like 41 or U+0041", lines.get(3));
    }

    @Test
    void goesToExistingOnlyForValidTextWithASlot() {
        assertTrue(openPrompt("2603").goesToExisting(cp -> cp == 0x2603));
        assertFalse(openPrompt("2603").goesToExisting(cp -> false));
        assertFalse(openPrompt("U+").goesToExisting(cp -> true));
        assertFalse(openPrompt("").goesToExisting(cp -> true));
    }

    @Test
    void resultParsesTheTypedText() {
        var result = openPrompt("U+41").result();
        assertTrue(result.isValid());
        assertEquals(0x41, result.codePoint());
    }
}
