package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewSizePromptTest {

    private static final SizeOption SQUARE_8 = new SizeOption(8, 8, true);
    private static final SizeOption SQUARE_16 = new SizeOption(16, 16, false);
    private static final SizeOption SQUARE_24 = new SizeOption(24, 24, true);
    private static final SizeOption SQUARE_32 = new SizeOption(32, 32, false);
    private static final SizeOption SQUARE_40 = new SizeOption(40, 40, false);

    private static NewSizePrompt openWith(SizeOption... options) {
        var prompt = new NewSizePrompt();
        prompt.open(List.of(options));
        return prompt;
    }

    @Test
    void startsClosedAndTracksOpenAndClose() {
        var prompt = new NewSizePrompt();
        assertFalse(prompt.isOpen());
        prompt.open(List.of(SQUARE_16));
        assertTrue(prompt.isOpen());
        prompt.close();
        assertFalse(prompt.isOpen());
        assertNull(prompt.selected());
    }

    @Test
    void labelsTheCellAsWidthByHeight() {
        assertEquals("16X16", NewSizePrompt.label(SQUARE_16));
        assertEquals("16X32", NewSizePrompt.label(new SizeOption(32, 16, false)));
    }

    @Test
    void opensOnTheFirstFreeSize() {
        var prompt = openWith(SQUARE_8, SQUARE_16, SQUARE_32);
        assertEquals(1, prompt.selectedIndex());
        assertEquals(SQUARE_16, prompt.selected());
    }

    @Test
    void movingSkipsTakenSizesAndStopsAtTheEnds() {
        var prompt = openWith(SQUARE_8, SQUARE_16, SQUARE_24, SQUARE_32, SQUARE_40);
        prompt.move(1);
        assertEquals(SQUARE_32, prompt.selected());
        prompt.move(1);
        assertEquals(SQUARE_40, prompt.selected());
        prompt.move(1);
        assertEquals(SQUARE_40, prompt.selected());
        prompt.move(-1);
        prompt.move(-1);
        assertEquals(SQUARE_16, prompt.selected());
        prompt.move(-1);
        assertEquals(SQUARE_16, prompt.selected());
    }

    @Test
    void hoverMovesTheHighlightOnlyOntoFreeRows() {
        var prompt = openWith(SQUARE_8, SQUARE_16, SQUARE_24, SQUARE_32);
        prompt.hover(3);
        assertEquals(SQUARE_32, prompt.selected());
        prompt.hover(2);
        prompt.hover(-1);
        prompt.hover(4);
        assertEquals(SQUARE_32, prompt.selected());
    }

    @Test
    void takenRowsAreDimmedAndFreeRowsAreSelectable() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        assertFalse(prompt.rows().get(0).selectable());
        assertTrue(prompt.rows().get(0).dimmed());
        assertTrue(prompt.rows().get(0).text().startsWith("  8X8"));
        assertEquals("16X16", prompt.rows().get(1).text());
        assertTrue(prompt.rows().get(1).selectable());
        assertFalse(prompt.rows().get(1).dimmed());
    }

    @Test
    void whenEverySizeIsTakenTheDialogShowsOneLineAndHasNothingToChoose() {
        var prompt = openWith(SQUARE_8, SQUARE_24);
        assertFalse(prompt.hasChoices());
        assertNull(prompt.selected());
        assertEquals(List.of(NewSizePrompt.ALL_TAKEN_MESSAGE), prompt.lines());
        prompt.move(1);
        prompt.hover(0);
        assertNull(prompt.selected());
    }

    @Test
    void anEmptyListSaysThereAreNoSizes() {
        var prompt = openWith();
        assertFalse(prompt.hasChoices());
        assertEquals(List.of(NewSizePrompt.NO_SIZES_MESSAGE), prompt.lines());
    }

    @Test
    void listLinesReserveTwoCellsPerRowAsWideAsTheWidestRow() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        assertEquals(3, prompt.lines().size());
        assertEquals(prompt.listWidth(), prompt.lines().get(0).length());
        assertEquals("  8X8  (EXISTS)".length(), prompt.listWidth());
    }

    @Test
    void reopeningStartsFresh() {
        var prompt = openWith(SQUARE_16, SQUARE_32);
        prompt.move(1);
        prompt.close();
        prompt.open(List.of(SQUARE_16, SQUARE_32));
        assertEquals(0, prompt.selectedIndex());
    }
}
