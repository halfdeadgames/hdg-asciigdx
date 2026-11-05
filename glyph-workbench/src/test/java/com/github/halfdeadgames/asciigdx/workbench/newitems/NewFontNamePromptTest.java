package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewFontNamePromptTest {

    private final List<String> accepted = new ArrayList<>();
    private int cancels;

    private NewFontNamePrompt openPrompt(String typed, String... existing) {
        var prompt = new NewFontNamePrompt(accepted::add, () -> cancels++);
        prompt.open(List.of(existing));
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
        var prompt = new NewFontNamePrompt(accepted::add, () -> cancels++);
        assertFalse(prompt.isOpen());
        prompt.open(List.of());
        assertTrue(prompt.isOpen());
        prompt.close();
        assertFalse(prompt.isOpen());
    }

    @Test
    void ignoresTypingWhileClosed() {
        var prompt = new NewFontNamePrompt(accepted::add, () -> cancels++);
        prompt.type('a');
        prompt.open(List.of());
        assertEquals("", prompt.name());
    }

    @Test
    void reopeningClearsEarlierText() {
        var prompt = openPrompt("abc");
        prompt.close();
        prompt.open(List.of());
        assertEquals("", prompt.name());
    }

    @Test
    void opensWithInitialTextAlreadyTyped() {
        var prompt = new NewFontNamePrompt(accepted::add, () -> cancels++);
        prompt.open(List.of("taken"), "my-font");
        assertEquals("my-font", prompt.name());
        prompt.enter();
        assertEquals(List.of("my-font"), accepted);
    }

    @Test
    void uppercaseLettersAreLowercasedAndOthersKeptAsTyped() {
        assertEquals("my-font9", openPrompt("My-FONT9").name());
        assertEquals("a b_É", openPrompt("A B_É").name());
    }

    @Test
    void controlCharactersAreIgnored() {
        assertEquals("ab", openPrompt("a\n\t\u0085b").name());
    }

    @Test
    void backspaceRemovesOneCodePointAndIsSafeWhenEmpty() {
        var prompt = openPrompt("abc");
        prompt.backspace();
        assertEquals("ab", prompt.name());
        var emoji = openPrompt("a" + new String(Character.toChars(0x1F600)));
        emoji.backspace();
        assertEquals("a", emoji.name());
        var empty = openPrompt("");
        empty.backspace();
        assertEquals("", empty.name());
    }

    @Test
    void longNamesAreNotTruncatedSoTheErrorExplainsThem() {
        var prompt = openPrompt("a".repeat(33));
        assertEquals(33, prompt.name().length());
        assertEquals("Use at most 32 characters", prompt.error());
    }

    @Test
    void typingStopsAtTheInputLimit() {
        assertEquals(NewFontNamePrompt.MAX_TYPED, openPrompt("a".repeat(100)).name().length());
    }

    @Test
    void emptyNameShowsTheErrorAndEnterDoesNothing() {
        var prompt = openPrompt("");
        assertEquals(List.of(padded("_"), "", "Type a font name", ""), prompt.lines());
        prompt.enter();
        assertTrue(accepted.isEmpty());
    }

    @Test
    void invalidNameShowsTheErrorAndEnterDoesNothing() {
        var prompt = openPrompt("Bad Name");
        assertEquals(List.of(padded("bad name_"), "", "Use only a-z, 0-9, and", "hyphens"), prompt.lines());
        prompt.enter();
        assertTrue(accepted.isEmpty());
    }

    @Test
    void existingFolderIsAnErrorEvenWithOtherCase() {
        var prompt = openPrompt("saiksistt", "Saiksistt");
        assertEquals("A font with that name already exists", prompt.error());
        assertEquals(List.of(padded("saiksistt_"), "", "A font with that name", "already exists"), prompt.lines());
    }

    @Test
    void validNameShowsTheHintAndEnterHandsItOver() {
        var prompt = openPrompt("Tiny-8", "other");
        assertNull(prompt.error());
        assertEquals(List.of(padded("tiny-8_"), "", "Creates the folder", "tiny-8"), prompt.lines());
        prompt.enter();
        assertEquals(List.of("tiny-8"), accepted);
    }

    @Test
    void enterWhileClosedDoesNothing() {
        var prompt = openPrompt("good");
        prompt.close();
        prompt.enter();
        prompt.cancel();
        assertTrue(accepted.isEmpty());
        assertEquals(0, cancels);
    }

    @Test
    void cancelCallsTheCallback() {
        openPrompt("x").cancel();
        assertEquals(1, cancels);
    }

    @Test
    void alwaysReturnsFourLinesAndLineOneIsTwentySixCells() {
        for (String typed : new String[] {"", "ok", "BAD!", "a".repeat(40)}) {
            List<String> lines = openPrompt(typed).lines();
            assertEquals(4, lines.size());
            assertEquals("", lines.get(1));
            assertEquals(26, lines.get(0).length());
        }
    }

    @Test
    void longTextScrollsSoTheCursorStaysVisible() {
        String first = openPrompt("a".repeat(30)).lines().get(0);
        assertEquals("a".repeat(25) + "_", first);
    }

    @Test
    void nonBmpCodePointsShowAsOneQuestionMark() {
        var prompt = openPrompt(new String(Character.toChars(0x1F600)));
        assertEquals(padded("?_"), prompt.lines().get(0));
    }
}
