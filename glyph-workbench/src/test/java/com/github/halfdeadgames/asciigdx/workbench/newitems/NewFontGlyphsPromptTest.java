package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewFontGlyphsPromptTest {

    private final List<NewFontGlyphs> created = new ArrayList<>();
    private int backs;

    private NewFontGlyphsPrompt openPrompt(String name) {
        var prompt = new NewFontGlyphsPrompt(created::add, () -> backs++);
        prompt.open(name);
        return prompt;
    }

    @Test
    void startsClosedAndTracksOpenAndClose() {
        var prompt = new NewFontGlyphsPrompt(glyphs -> {
        }, () -> {
        });
        assertFalse(prompt.isOpen());
        prompt.open("a");
        assertTrue(prompt.isOpen());
        assertEquals("a", prompt.name());
        prompt.close();
        assertFalse(prompt.isOpen());
    }

    @Test
    void opensOnCp437() {
        assertEquals(NewFontGlyphs.CP437, openPrompt("a").selected());
    }

    @Test
    void reopeningResetsTheHighlightAndProblem() {
        var prompt = openPrompt("a");
        prompt.move(1);
        prompt.showProblem("boom");
        prompt.close();
        prompt.open("b");
        assertEquals(NewFontGlyphs.CP437, prompt.selected());
        assertEquals("", prompt.problem());
        assertEquals("b", prompt.name());
    }

    @Test
    void downMovesToEmptyAndDoesNotWrap() {
        var prompt = openPrompt("a");
        prompt.move(1);
        assertEquals(NewFontGlyphs.EMPTY, prompt.selected());
        prompt.move(1);
        assertEquals(NewFontGlyphs.EMPTY, prompt.selected());
    }

    @Test
    void upMovesBackAndDoesNotWrap() {
        var prompt = openPrompt("a");
        prompt.move(-1);
        assertEquals(NewFontGlyphs.CP437, prompt.selected());
        prompt.move(1);
        prompt.move(-1);
        assertEquals(NewFontGlyphs.CP437, prompt.selected());
        prompt.move(-1);
        assertEquals(NewFontGlyphs.CP437, prompt.selected());
    }

    @Test
    void enterHandsOverTheSelectedChoice() {
        var prompt = openPrompt("a");
        prompt.enter();
        prompt.move(1);
        prompt.enter();
        assertEquals(List.of(NewFontGlyphs.CP437, NewFontGlyphs.EMPTY), created);
    }

    @Test
    void backCallsBackAndKeepsTheNameAvailable() {
        var prompt = openPrompt("my-font");
        prompt.back();
        assertEquals(1, backs);
        assertEquals("my-font", prompt.name());
    }

    @Test
    void closedPromptIgnoresEnterBackAndMoves() {
        var prompt = openPrompt("a");
        prompt.close();
        prompt.move(1);
        prompt.enter();
        prompt.back();
        assertTrue(created.isEmpty());
        assertEquals(0, backs);
        assertEquals(NewFontGlyphs.CP437, prompt.selected());
    }

    @Test
    void problemIsShownOnTheTwoLinesUnderTheList() {
        var prompt = openPrompt("a");
        prompt.showProblem("Cannot create font: disk full");
        List<String> lines = prompt.lines();
        assertEquals(8, lines.size());
        assertEquals("Cannot create font: disk", lines.get(6));
        assertEquals("full", lines.get(7));
        assertEquals("Cannot create font: disk full", prompt.problem());
    }

    @Test
    void listRowsSitOnTheLinesReservedForThem() {
        var prompt = openPrompt("my-font");
        List<String> lines = prompt.lines();
        assertEquals("Glyphs for my-font", lines.get(0));
        int firstRowLine = NewFontGlyphsPrompt.LIST_TOP_LINE;
        assertEquals(" ".repeat(NewFontGlyphsPrompt.LINE_CELLS), lines.get(firstRowLine));
        assertEquals(" ".repeat(NewFontGlyphsPrompt.LINE_CELLS),
                lines.get(firstRowLine + SelectList.PITCH));
        assertEquals(2, prompt.rows().size());
    }

    @Test
    void allLinesAndRowsFitTheDialogWidth() {
        var prompt = openPrompt("a".repeat(40));
        prompt.showProblem("x".repeat(100));
        for (String line : prompt.lines()) {
            assertTrue(line.length() <= NewFontGlyphsPrompt.LINE_CELLS);
        }
        assertTrue(prompt.lines().get(0).endsWith(".."));
        assertTrue(prompt.listWidth() <= NewFontGlyphsPrompt.LINE_CELLS);
    }
}
