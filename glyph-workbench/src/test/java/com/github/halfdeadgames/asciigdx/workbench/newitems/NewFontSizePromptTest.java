package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewFontSizePromptTest {

    private final List<CellSize> created = new ArrayList<>();
    private int backs;

    private NewFontSizePrompt openPrompt(String name) {
        var prompt = new NewFontSizePrompt(created::add, () -> backs++);
        prompt.open(name);
        return prompt;
    }

    private static void typeAll(NewFontSizePrompt prompt, String text) {
        for (int i = 0; i < text.length(); i++) {
            prompt.type(text.charAt(i));
        }
    }

    @Test
    void startsClosedAndTracksOpenAndClose() {
        var prompt = new NewFontSizePrompt(size -> {
        }, () -> {
        });
        assertFalse(prompt.isOpen());
        prompt.open("a");
        assertTrue(prompt.isOpen());
        assertEquals("a", prompt.name());
        assertEquals("", prompt.typed());
        prompt.close();
        assertFalse(prompt.isOpen());
    }

    @Test
    void onlyDigitsAndXAreAccepted() {
        var prompt = openPrompt("a");
        typeAll(prompt, "1a6 -x.3,");
        assertEquals("16x3", prompt.typed());
    }

    @Test
    void capitalXIsStoredLowercase() {
        var prompt = openPrompt("a");
        typeAll(prompt, "16X32");
        assertEquals("16x32", prompt.typed());
    }

    @Test
    void typingStopsAtNineCharacters() {
        var prompt = openPrompt("a");
        typeAll(prompt, "1234567890123");
        assertEquals("123456789", prompt.typed());
    }

    @Test
    void backspaceRemovesTheLastCharacterAndIgnoresAnEmptyField() {
        var prompt = openPrompt("a");
        prompt.backspace();
        typeAll(prompt, "16x");
        prompt.backspace();
        assertEquals("16", prompt.typed());
    }

    @Test
    void closedPromptIgnoresTypingEnterAndBack() {
        var prompt = openPrompt("a");
        prompt.close();
        typeAll(prompt, "16x16");
        prompt.enter();
        prompt.back();
        assertEquals("", prompt.typed());
        assertTrue(created.isEmpty());
        assertEquals(0, backs);
    }

    @Test
    void enterOnAnInvalidSizeShowsTheProblemAndStays() {
        var prompt = openPrompt("a");
        typeAll(prompt, "16");
        prompt.enter();
        assertEquals(NewFontInput.typedSizeError("16"), prompt.problem());
        assertTrue(created.isEmpty());
        assertTrue(prompt.isOpen());
    }

    @Test
    void enterOnAnEmptyFieldAsksForASize() {
        var prompt = openPrompt("a");
        prompt.enter();
        assertEquals("Type a size like 16x16", prompt.problem());
        assertTrue(created.isEmpty());
    }

    @Test
    void enterOnAValidSizeHandsOverTheCellSize() {
        var prompt = openPrompt("a");
        typeAll(prompt, "7x14");
        prompt.enter();
        assertEquals(List.of(new CellSize(7, 14)), created);
    }

    @Test
    void typingAndBackspaceClearTheProblem() {
        var prompt = openPrompt("a");
        prompt.enter();
        prompt.type('1');
        assertEquals("", prompt.problem());
        prompt.enter();
        prompt.backspace();
        assertEquals("", prompt.problem());
    }

    @Test
    void escapeCallsBackAndKeepsTheNameAvailable() {
        var prompt = openPrompt("my-font");
        prompt.back();
        assertEquals(1, backs);
        assertEquals("my-font", prompt.name());
    }

    @Test
    void hintShowsOnlyWhileOpenAndEmpty() {
        var prompt = new NewFontSizePrompt(size -> {
        }, () -> {
        });
        assertFalse(prompt.showsHint());
        prompt.open("a");
        assertTrue(prompt.showsHint());
        prompt.type('1');
        assertFalse(prompt.showsHint());
    }

    @Test
    void linesAreNameBlankTypedFieldAndTwoProblemLines() {
        var prompt = openPrompt("my-font");
        typeAll(prompt, "16x");
        prompt.enter();
        List<String> lines = prompt.lines();
        assertEquals(5, lines.size());
        assertEquals("Size for my-font", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals("16x_" + " ".repeat(NewFontSizePrompt.LINE_CELLS - 4),
                lines.get(NewFontSizePrompt.INPUT_LINE));
        assertEquals("Write the size as WxH,", lines.get(3));
        assertEquals("e.g. 16x16", lines.get(4));
    }

    @Test
    void longCreationProblemsAreCutToTwoLines() {
        var prompt = openPrompt("a");
        prompt.showProblem("x".repeat(100));
        for (String line : prompt.lines()) {
            assertTrue(line.length() <= NewFontSizePrompt.LINE_CELLS);
        }
    }

    @Test
    void longNamesAreCutToTheLineWidth() {
        String line = openPrompt("a".repeat(32)).lines().get(0);
        assertEquals(NewFontSizePrompt.LINE_CELLS, line.length());
        assertTrue(line.endsWith(".."));
    }

    @Test
    void reopeningWithTextKeepsTheTypedSize() {
        var prompt = openPrompt("a");
        typeAll(prompt, "16x32");
        prompt.close();
        prompt.open("a", "16x32");
        assertEquals("16x32", prompt.typed());
        assertEquals("", prompt.problem());
        assertFalse(prompt.showsHint());
        prompt.enter();
        assertEquals(List.of(new CellSize(16, 32)), created);
    }

    @Test
    void reopeningStartsFresh() {
        var prompt = openPrompt("a");
        typeAll(prompt, "16");
        prompt.showProblem("boom");
        prompt.close();
        prompt.open("b");
        assertEquals("", prompt.typed());
        assertEquals("", prompt.problem());
        assertEquals("b", prompt.name());
    }
}
