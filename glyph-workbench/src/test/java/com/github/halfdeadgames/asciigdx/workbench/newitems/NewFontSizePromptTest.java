package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.newitems.NewFontInput.Shape;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewFontSizePromptTest {

    private final List<String> created = new ArrayList<>();
    private int backs;

    private NewFontSizePrompt openPrompt(String name) {
        var prompt = new NewFontSizePrompt((shape, height) -> created.add(shape + " " + height), () -> backs++);
        prompt.open(name);
        return prompt;
    }

    private static List<String> texts(NewFontSizePrompt prompt) {
        return prompt.rows().stream().map(SelectList.Row::text).toList();
    }

    @Test
    void startsClosedAndTracksOpenAndClose() {
        var prompt = new NewFontSizePrompt((shape, height) -> { }, () -> { });
        assertFalse(prompt.isOpen());
        prompt.open("a");
        assertTrue(prompt.isOpen());
        assertEquals("a", prompt.name());
        prompt.close();
        assertFalse(prompt.isOpen());
        assertNull(prompt.selected());
    }

    @Test
    void groupsSquareThenTallUnderHeadings() {
        assertEquals(List.of("SQUARE", "8X8", "16X16", "24X24", "32X32", "40X40", "48X48", "56X56", "64X64",
            "TALL", "8X16", "16X32", "24X48", "32X64"), texts(openPrompt("a")));
    }

    @Test
    void headingsAreNotSelectableAndSizesAre() {
        var prompt = openPrompt("a");
        for (int i = 0; i < prompt.rows().size(); i++) {
            boolean heading = prompt.rows().get(i).text().equals("SQUARE") || prompt.rows().get(i).text().equals("TALL");
            assertEquals(!heading, prompt.rows().get(i).selectable());
            assertFalse(prompt.rows().get(i).dimmed());
        }
    }

    @Test
    void cursorStartsOnEightByEight() {
        var prompt = openPrompt("a");
        assertEquals(1, prompt.selectedIndex());
        assertEquals(new NewFontSizePrompt.Entry(Shape.SQUARE, 8), prompt.selected());
    }

    @Test
    void movingSkipsTheTallHeadingAndStopsAtTheEnds() {
        var prompt = openPrompt("a");
        prompt.move(-1);
        assertEquals(1, prompt.selectedIndex());
        for (int i = 0; i < 7; i++) {
            prompt.move(1);
        }
        assertEquals(new NewFontSizePrompt.Entry(Shape.SQUARE, 64), prompt.selected());
        prompt.move(1);
        assertEquals(new NewFontSizePrompt.Entry(Shape.TALL, 16), prompt.selected());
        prompt.move(-1);
        assertEquals(new NewFontSizePrompt.Entry(Shape.SQUARE, 64), prompt.selected());
        for (int i = 0; i < 20; i++) {
            prompt.move(1);
        }
        assertEquals(new NewFontSizePrompt.Entry(Shape.TALL, 64), prompt.selected());
    }

    @Test
    void enterHandsOverTheShapeAndHeight() {
        var prompt = openPrompt("a");
        prompt.enter();
        prompt.move(1);
        prompt.enter();
        for (int i = 0; i < 20; i++) {
            prompt.move(1);
        }
        prompt.enter();
        assertEquals(List.of("SQUARE 8", "SQUARE 16", "TALL 64"), created);
    }

    @Test
    void tallLabelsShowHalfWidth() {
        assertEquals("16X32", NewFontSizePrompt.label(new NewFontSizePrompt.Entry(Shape.TALL, 32)));
    }

    @Test
    void escapeCallsBackAndKeepsTheNameAvailable() {
        var prompt = openPrompt("my-font");
        assertEquals("my-font", prompt.name());
        prompt.back();
        assertEquals(1, backs);
    }

    @Test
    void closedPromptIgnoresEnterAndBack() {
        var prompt = openPrompt("a");
        prompt.close();
        prompt.enter();
        prompt.back();
        assertTrue(created.isEmpty());
        assertEquals(0, backs);
    }

    @Test
    void linesStartWithTheNameThenReserveTwoCellsPerRow() {
        var prompt = openPrompt("my-font");
        List<String> lines = prompt.lines();
        assertEquals("Size for my-font", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals(NewFontSizePrompt.LINES_ABOVE_LIST + prompt.rows().size() * SelectList.PITCH - 1, lines.size());
        assertEquals(prompt.listWidth(), lines.get(2).length());
        assertEquals("► ".length() + "64X64".length(), prompt.listWidth());
    }

    @Test
    void longNamesAreCutToTheLineWidth() {
        String line = openPrompt("a".repeat(32)).lines().get(0);
        assertEquals(NewFontSizePrompt.LINE_CELLS, line.length());
        assertTrue(line.endsWith(".."));
    }

    // 18 cells tall leaves room for five list rows: 18 - 7 (frame, buttons, shadow) = 11 lines = 2 + 5 * 2 - 1.
    private static final int FIVE_ROW_AREA = 18;

    @Test
    void roomyAreaShowsEveryRowWithoutScrolling() {
        var prompt = openPrompt("a");
        prompt.fitTo(40);
        assertFalse(prompt.scrolls());
        assertEquals(prompt.rows().size(), prompt.shownRows());
        assertEquals(prompt.rows(), prompt.visibleRows());
        assertEquals(1, prompt.visibleSelectedIndex());
        assertEquals(NewFontSizePrompt.LINES_ABOVE_LIST + prompt.rows().size() * SelectList.PITCH - 1,
            prompt.lines().size());
    }

    @Test
    void tightAreaShowsAWindowThatFitsTheDialog() {
        var prompt = openPrompt("a");
        prompt.fitTo(FIVE_ROW_AREA);
        assertTrue(prompt.scrolls());
        assertEquals(5, prompt.shownRows());
        assertEquals(List.of("SQUARE", "8X8", "16X16", "24X24", "32X32"),
            prompt.visibleRows().stream().map(SelectList.Row::text).toList());
        assertEquals(1, prompt.visibleSelectedIndex());
        assertEquals(com.github.halfdeadgames.asciigdx.workbench.ui.Dialog.maxLines(FIVE_ROW_AREA), prompt.lines().size());
    }

    @Test
    void tinyAreaStillShowsAHeadingAndASize() {
        var prompt = openPrompt("a");
        prompt.fitTo(0);
        assertEquals(2, prompt.shownRows());
        assertEquals(1, prompt.visibleSelectedIndex());
    }

    @Test
    void cursorMovingPastTheBottomScrollsJustEnough() {
        var prompt = openPrompt("a");
        prompt.fitTo(FIVE_ROW_AREA);
        for (int i = 0; i < 4; i++) {
            prompt.move(1);
        }
        assertEquals(5, prompt.selectedIndex());
        assertEquals(1, prompt.window().offset());
        assertEquals(4, prompt.visibleSelectedIndex());
    }

    @Test
    void reachingTheFirstTallSizeBringsItsHeadingIntoView() {
        var prompt = openPrompt("a");
        prompt.fitTo(FIVE_ROW_AREA);
        for (int i = 0; i < 8; i++) {
            prompt.move(1);
        }
        assertEquals(new NewFontSizePrompt.Entry(Shape.TALL, 16), prompt.selected());
        assertTrue(prompt.window().isVisible(prompt.selectedIndex() - 1));
        assertTrue(prompt.window().isVisible(prompt.selectedIndex()));
    }

    @Test
    void movingUpToTheFirstSizeShowsItsHeadingAboveIt() {
        var prompt = openPrompt("a");
        prompt.fitTo(FIVE_ROW_AREA);
        prompt.wheel(100);
        prompt.move(1);
        assertEquals(2, prompt.window().offset());
        prompt.move(-1);
        assertEquals(1, prompt.selectedIndex());
        assertEquals(0, prompt.window().offset());
    }

    @Test
    void wheelScrollsTheWindowAndClampsWithoutMovingTheCursor() {
        var prompt = openPrompt("a");
        prompt.fitTo(FIVE_ROW_AREA);
        prompt.wheel(-5);
        assertEquals(0, prompt.window().offset());
        prompt.wheel(100);
        assertEquals(prompt.rows().size() - 5, prompt.window().offset());
        assertEquals(1, prompt.selectedIndex());
        assertEquals(-1, prompt.visibleSelectedIndex());
    }

    @Test
    void hoverMovesTheCursorToSizesOnly() {
        var prompt = openPrompt("a");
        prompt.hover(0);
        assertEquals(1, prompt.selectedIndex());
        prompt.hover(9);
        assertEquals(1, prompt.selectedIndex());
        prompt.hover(99);
        assertEquals(1, prompt.selectedIndex());
        prompt.hover(3);
        assertEquals(3, prompt.selectedIndex());
    }

    @Test
    void pickingASizeCreatesItAndPickingAHeadingDoesNothing() {
        var prompt = openPrompt("a");
        prompt.pick(9);
        prompt.pick(-1);
        assertTrue(created.isEmpty());
        prompt.pick(3);
        assertEquals(List.of("SQUARE 24"), created);
        assertEquals(3, prompt.selectedIndex());
    }

    @Test
    void reopeningStartsFresh() {
        var prompt = openPrompt("a");
        prompt.move(1);
        prompt.close();
        prompt.open("b");
        assertEquals(1, prompt.selectedIndex());
        assertEquals("b", prompt.name());
    }
}
