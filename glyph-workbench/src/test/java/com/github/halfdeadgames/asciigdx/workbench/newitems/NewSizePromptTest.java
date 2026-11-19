package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewSizePromptTest {

    private static final SizeOption SQUARE_8 = new SizeOption(new CellSize(8, 8), true);
    private static final SizeOption SQUARE_16 = new SizeOption(new CellSize(16, 16), false);
    private static final SizeOption SQUARE_24 = new SizeOption(new CellSize(24, 24), true);
    private static final SizeOption SQUARE_32 = new SizeOption(new CellSize(32, 32), false);
    private static final SizeOption SQUARE_40 = new SizeOption(new CellSize(40, 40), false);

    private final List<CellSize> chosen = new ArrayList<>();

    private NewSizePrompt openWith(SizeOption... options) {
        var prompt = new NewSizePrompt(chosen::add);
        prompt.open(List.of(options), "alpha");
        return prompt;
    }

    private static void typeText(NewSizePrompt prompt, String text) {
        for (int i = 0; i < text.length(); i++) {
            prompt.type(text.charAt(i));
        }
    }

    @Test
    void startsClosedAndTracksOpenAndClose() {
        var prompt = new NewSizePrompt(chosen::add);
        assertFalse(prompt.isOpen());
        prompt.open(List.of(SQUARE_16), "alpha");
        assertTrue(prompt.isOpen());
        prompt.close();
        assertFalse(prompt.isOpen());
        assertFalse(prompt.isTyping());
    }

    @Test
    void labelsTheCellAsWidthByHeight() {
        assertEquals("16x16", NewSizePrompt.label(SQUARE_16));
        assertEquals("16x32", NewSizePrompt.label(new SizeOption(new CellSize(16, 32), false)));
        assertEquals("16x32", NewSizePrompt.label(new CellSize(16, 32)));
    }

    @Test
    void endsWithAnOtherSizeRow() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        assertEquals(3, prompt.rowCount());
        assertEquals(NewSizePrompt.OTHER_SIZE_LABEL, prompt.list().rows().get(2).text());
        assertTrue(prompt.list().rows().get(2).selectable());
    }

    @Test
    void opensOnTheFirstFreeSize() {
        var prompt = openWith(SQUARE_8, SQUARE_16, SQUARE_32);
        assertEquals(1, prompt.selectedIndex());
    }

    @Test
    void movingSkipsTakenSizesAndLandsOnOtherSize() {
        var prompt = openWith(SQUARE_8, SQUARE_16, SQUARE_24, SQUARE_32, SQUARE_40);
        prompt.move(1);
        assertEquals(3, prompt.selectedIndex());
        prompt.move(1);
        assertEquals(4, prompt.selectedIndex());
        prompt.move(1);
        assertEquals(5, prompt.selectedIndex());
        prompt.move(1);
        assertEquals(5, prompt.selectedIndex());
        prompt.move(-1);
        prompt.move(-1);
        prompt.move(-1);
        assertEquals(1, prompt.selectedIndex());
        prompt.move(-1);
        assertEquals(1, prompt.selectedIndex());
    }

    @Test
    void movingFromAFreeRowOverATakenRowReachesOtherSize() {
        var prompt = openWith(SQUARE_16, SQUARE_24);
        prompt.move(1);
        assertEquals(2, prompt.selectedIndex());
    }

    @Test
    void hoverMovesTheHighlightOnlyOntoSelectableRows() {
        var prompt = openWith(SQUARE_8, SQUARE_16, SQUARE_24, SQUARE_32);
        prompt.list().hover(3);
        assertEquals(3, prompt.selectedIndex());
        prompt.list().hover(2);
        prompt.list().hover(-1);
        prompt.list().hover(5);
        assertEquals(3, prompt.selectedIndex());
        prompt.list().hover(4);
        assertEquals(4, prompt.selectedIndex());
    }

    @Test
    void takenRowsAreDimmedAndFreeRowsAreSelectable() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        var rows = prompt.list().rows();
        assertFalse(rows.get(0).selectable());
        assertTrue(rows.get(0).dimmed());
        assertTrue(rows.get(0).text().startsWith("  8x8"));
        assertEquals("16x16", rows.get(1).text());
        assertTrue(rows.get(1).selectable());
        assertFalse(rows.get(1).dimmed());
    }

    @Test
    void whenEverySizeIsTakenTheSelectionStartsOnOtherSize() {
        var prompt = openWith(SQUARE_8, SQUARE_24);
        assertEquals(2, prompt.selectedIndex());
        prompt.move(-1);
        prompt.list().hover(0);
        assertEquals(2, prompt.selectedIndex());
    }

    @Test
    void listLinesReserveTwoCellsPerRowAsWideAsTheWidestRow() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        assertEquals(5, prompt.lines().size());
        assertEquals(prompt.list().listWidth(), prompt.lines().get(0).length());
        assertEquals("  8x8  (EXISTS)".length(), prompt.list().listWidth());
    }

    @Test
    void enterOnAFreeSizeClosesAndHandsItOn() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        prompt.enter();
        assertEquals(List.of(new CellSize(16, 16)), chosen);
        assertFalse(prompt.isOpen());
    }

    @Test
    void choosingOtherSizeOpensTheTypedPageForTheFolder() {
        var prompt = openWith(SQUARE_16);
        prompt.move(1);
        prompt.enter();
        assertTrue(prompt.isOpen());
        assertTrue(prompt.isTyping());
        assertTrue(chosen.isEmpty());
        assertEquals("Size for alpha", prompt.lines().get(0));
    }

    @Test
    void aValidTypedSizeClosesAndIsHandedOn() {
        var prompt = openWith(SQUARE_8);
        prompt.chooseSelected();
        typeText(prompt, "24x48");
        prompt.enter();
        assertEquals(List.of(new CellSize(24, 48)), chosen);
        assertFalse(prompt.isOpen());
    }

    @Test
    void aTakenTypedSizeShowsAProblemAndStays() {
        var prompt = openWith(SQUARE_8, SQUARE_16);
        prompt.move(1);
        prompt.chooseSelected();
        typeText(prompt, "8x8");
        prompt.enter();
        assertTrue(prompt.isTyping());
        assertTrue(chosen.isEmpty());
        assertEquals("8x8 already exists", prompt.typedPage().problem());
    }

    @Test
    void aMalformedTypedSizeShowsTheInputProblemAndStays() {
        var prompt = openWith(SQUARE_8);
        prompt.chooseSelected();
        typeText(prompt, "8");
        prompt.enter();
        assertTrue(prompt.isTyping());
        assertFalse(prompt.typedPage().problem().isEmpty());
        assertTrue(chosen.isEmpty());
    }

    @Test
    void escOnTheTypedPageGoesBackToTheListOnOtherSize() {
        var prompt = openWith(SQUARE_16);
        prompt.move(1);
        prompt.chooseSelected();
        typeText(prompt, "5");
        prompt.cancel();
        assertTrue(prompt.isOpen());
        assertFalse(prompt.isTyping());
        assertEquals(1, prompt.selectedIndex());
        prompt.chooseSelected();
        assertEquals("", prompt.typedPage().typed());
    }

    @Test
    void escOnTheListCloses() {
        var prompt = openWith(SQUARE_16);
        prompt.cancel();
        assertFalse(prompt.isOpen());
        assertTrue(chosen.isEmpty());
    }

    @Test
    void escOnTheListReportsADismissalButChoosingDoesNot() {
        var dismissals = new ArrayList<String>();
        var prompt = new NewSizePrompt(chosen::add, () -> dismissals.add("dismissed"));
        prompt.open(List.of(SQUARE_16), "alpha");
        prompt.cancel();
        assertEquals(1, dismissals.size());
        prompt.open(List.of(SQUARE_16), "alpha");
        prompt.enter();
        assertEquals(1, dismissals.size());
        assertEquals(1, chosen.size());
    }

    @Test
    void escOnTheTypedPageOnlyDismissesWhenThereIsNoListToReturnTo() {
        var dismissals = new ArrayList<String>();
        var withList = new NewSizePrompt(chosen::add, () -> dismissals.add("dismissed"));
        withList.open(List.of(SQUARE_16), "alpha");
        withList.move(1);
        withList.chooseSelected();
        assertTrue(withList.isTyping());
        withList.cancel();
        assertTrue(withList.isOpen());
        assertTrue(dismissals.isEmpty());
        var withoutList = new NewSizePrompt(chosen::add, () -> dismissals.add("dismissed"));
        withoutList.open(List.of(), "alpha");
        withoutList.cancel();
        assertEquals(1, dismissals.size());
    }

    @Test
    void backspaceRemovesTypedCharacters() {
        var prompt = openWith();
        typeText(prompt, "16x");
        prompt.backspace();
        assertEquals("16", prompt.typedPage().typed());
    }

    @Test
    void typingIsIgnoredOnTheList() {
        var prompt = openWith(SQUARE_16);
        typeText(prompt, "16x16");
        assertEquals("", prompt.typedPage().typed());
    }

    @Test
    void withNoSizesItOpensOnTheTypedPageAndEscCloses() {
        var prompt = openWith();
        assertTrue(prompt.isOpen());
        assertTrue(prompt.isTyping());
        prompt.cancel();
        assertFalse(prompt.isOpen());
        assertFalse(prompt.isTyping());
    }

    @Test
    void reopeningStartsFresh() {
        var prompt = openWith(SQUARE_16, SQUARE_32);
        prompt.move(1);
        prompt.close();
        prompt.open(List.of(SQUARE_16, SQUARE_32), "alpha");
        assertEquals(0, prompt.selectedIndex());
    }
}
