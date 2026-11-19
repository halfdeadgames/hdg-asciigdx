package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class TextDrawingTest {

    private static final int FOREGROUND = 0x11223344;
    private static final int BACKGROUND = 0x55667788;
    private static final int ELLIPSIS = TextDrawing.DEFAULT_ELLIPSIS;
    private static final String LONG_TEXT = "ABCDEFGHIJ";

    @Test
    void truncatedTextThatFitsIsDrawnAndRestIsBlank() {
        TerminalGrid grid = filledGrid(8, 1, 'x');
        truncated(grid, 1, 0, 4, "AB");
        assertEquals("xAB  xxx", rowText(grid, 0));
    }

    @Test
    void truncatedTextThatExactlyFitsHasNoEllipsis() {
        TerminalGrid grid = new TerminalGrid(8, 1);
        truncated(grid, 0, 0, 4, "ABCD");
        assertEquals("ABCD", rowText(grid, 0).substring(0, 4));
    }

    @Test
    void truncatedTextThatIsTooLongEndsWithEllipsis() {
        TerminalGrid grid = filledGrid(8, 1, 'x');
        truncated(grid, 0, 0, 4, LONG_TEXT);
        assertEquals("ABC…xxxx", rowText(grid, 0));
    }

    @Test
    void truncatedWidthOneDrawsOnlyEllipsis() {
        TerminalGrid grid = filledGrid(4, 1, 'x');
        truncated(grid, 1, 0, 1, LONG_TEXT);
        assertEquals("x…xx", rowText(grid, 0));
    }

    @Test
    void truncatedWidthZeroDrawsNothingAndNegativeThrows() {
        TerminalGrid grid = filledGrid(4, 1, 'x');
        truncated(grid, 0, 0, 0, LONG_TEXT);
        assertEquals("xxxx", rowText(grid, 0));
        assertThrows(IllegalArgumentException.class, () -> truncated(grid, 0, 0, -1, "A"));
        assertThrows(IllegalArgumentException.class,
                () -> scrolling(grid, 0, 0, -1, "A", TextScroll.loop(1f, 0f, 0), 0f));
    }

    @Test
    void nullArgumentsThrow() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        TextScroll scroll = TextScroll.loop(1f, 0f, 0);
        assertThrows(NullPointerException.class, () -> TextDrawing.drawTruncated(null, 0, 0, 2,
                "A", ELLIPSIS, FOREGROUND, BACKGROUND));
        assertThrows(NullPointerException.class, () -> TextDrawing.drawTruncated(grid, 0, 0, 2,
                null, ELLIPSIS, FOREGROUND, BACKGROUND));
        assertThrows(NullPointerException.class, () -> TextDrawing.drawScrolling(null, 0, 0, 2,
                "A", scroll, 0f, ELLIPSIS, FOREGROUND, BACKGROUND));
        assertThrows(NullPointerException.class, () -> TextDrawing.drawScrolling(grid, 0, 0, 2,
                null, scroll, 0f, ELLIPSIS, FOREGROUND, BACKGROUND));
        assertThrows(NullPointerException.class, () -> TextDrawing.drawScrolling(grid, 0, 0, 2,
                "A", null, 0f, ELLIPSIS, FOREGROUND, BACKGROUND));
    }

    @Test
    void cellsWithoutTextUseGivenColors() {
        TerminalGrid grid = new TerminalGrid(6, 1);
        truncated(grid, 0, 0, 4, "A");
        assertEquals(' ', grid.getCodePoint(3, 0));
        assertEquals(FOREGROUND, grid.getForeground(3, 0));
        assertEquals(BACKGROUND, grid.getBackground(3, 0));
    }

    @Test
    void truncatedClipsAtLeftEdgeButKeepsCellPositions() {
        TerminalGrid grid = new TerminalGrid(5, 1);
        truncated(grid, -2, 0, 6, LONG_TEXT);
        assertEquals("CDE…", rowText(grid, 0).substring(0, 4));
    }

    @Test
    void truncatedClipsAtRightEdge() {
        TerminalGrid grid = new TerminalGrid(5, 1);
        truncated(grid, 3, 0, 6, LONG_TEXT);
        assertEquals("AB", rowText(grid, 0).substring(3));
    }

    @Test
    void rowOutsideGridWritesNothingAndDoesNotThrow() {
        TerminalGrid grid = filledGrid(4, 2, 'x');
        truncated(grid, 0, -1, 4, LONG_TEXT);
        truncated(grid, 0, 2, 4, LONG_TEXT);
        scrolling(grid, 0, 5, 4, LONG_TEXT, TextScroll.loop(1f, 0f, 1), 1f);
        assertEquals("xxxx", rowText(grid, 0));
        assertEquals("xxxx", rowText(grid, 1));
    }

    @Test
    void supplementaryCodePointTakesOneCell() {
        String emoji = new String(Character.toChars(0x1F600));
        TerminalGrid grid = new TerminalGrid(6, 1);
        truncated(grid, 0, 0, 3, emoji + "BC");
        assertEquals(0x1F600, grid.getCodePoint(0, 0));
        assertEquals('B', grid.getCodePoint(1, 0));
        assertEquals('C', grid.getCodePoint(2, 0));
        truncated(grid, 0, 0, 3, emoji + "BCD");
        assertEquals(0x1F600, grid.getCodePoint(0, 0));
        assertEquals('B', grid.getCodePoint(1, 0));
        assertEquals(ELLIPSIS, grid.getCodePoint(2, 0));
    }

    @Test
    void loneSurrogateTakesOneCell() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        truncated(grid, 0, 0, 3, "\uD83DBC");
        assertEquals(0xD83D, grid.getCodePoint(0, 0));
        assertEquals('B', grid.getCodePoint(1, 0));
        assertEquals('C', grid.getCodePoint(2, 0));
    }

    @Test
    void customEllipsisIsUsed() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        TextDrawing.drawTruncated(grid, 0, 0, 4, LONG_TEXT, '~', FOREGROUND, BACKGROUND);
        assertEquals("ABC~", rowText(grid, 0));
    }

    @Test
    void scrollingTextThatFitsHasNoEllipsis() {
        TerminalGrid grid = filledGrid(6, 1, 'x');
        scrolling(grid, 0, 0, 4, "AB", TextScroll.backAndForth(2f, 1f), 3f);
        assertEquals("AB  xx", rowText(grid, 0));
    }

    @Test
    void backAndForthShowsEllipsesWhereTextIsHidden() {
        TextScroll scroll = TextScroll.backAndForth(2f, 1f);
        assertEquals("ABC…", scrolledRow(scroll, 4, 0f));
        assertEquals("…DE…", scrolledRow(scroll, 4, 2f));
        assertEquals("…HIJ", scrolledRow(scroll, 4, 4.5f));
    }

    @Test
    void backAndForthWidthOneShowsEllipsisWhenEitherSideHidden() {
        TextScroll scroll = TextScroll.backAndForth(2f, 1f);
        assertEquals("…", scrolledRow(scroll, 1, 0f));
        assertEquals("…", scrolledRow(scroll, 1, 2f));
        assertEquals("…", scrolledRow(scroll, 1, 4.5f));
    }

    @Test
    void loopShowsGapAndWrapsAround() {
        TextScroll scroll = TextScroll.loop(2f, 1f, 2);
        assertEquals("ABC…", scrolledRow(scroll, 4, 0f));
        assertEquals("…FG…", scrolledRow(scroll, 4, 3f));
        assertEquals("…J …", scrolledRow(scroll, 4, 5f));
        assertEquals(" AB…", scrolledRow(scroll, 4, 6.9f));
    }

    @Test
    void loopWithZeroGapRunsIntoItsOwnStart() {
        TextScroll scroll = TextScroll.loop(1f, 0f, 0);
        assertEquals("…JA…", scrolledRow(scroll, 4, 8f));
        assertEquals("…AB…", scrolledRow(scroll, 4, 9f));
    }

    @Test
    void scrollingClipsLikeTruncated() {
        TerminalGrid grid = new TerminalGrid(5, 1);
        scrolling(grid, -2, 0, 6, LONG_TEXT, TextScroll.backAndForth(2f, 1f), 2f);
        assertEquals("EFG…", rowText(grid, 0).substring(0, 4));
    }

    @Test
    void scrollingUsesCustomEllipsis() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        TextDrawing.drawScrolling(grid, 0, 0, 4, LONG_TEXT, TextScroll.backAndForth(2f, 1f), 2f,
                '~', FOREGROUND, BACKGROUND);
        assertEquals("~DE~", rowText(grid, 0));
    }

    @Test
    void invalidEllipsisCodePointThrows() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        TextScroll scroll = TextScroll.loop(1f, 0f, 0);
        assertThrows(IllegalArgumentException.class, () -> TextDrawing.drawTruncated(grid, 0, 0,
                4, "A", -1, FOREGROUND, BACKGROUND));
        assertThrows(IllegalArgumentException.class, () -> TextDrawing.drawTruncated(grid, 0, 0,
                4, "A", 0x110000, FOREGROUND, BACKGROUND));
        assertThrows(IllegalArgumentException.class, () -> TextDrawing.drawScrolling(grid, 0, 0,
                4, "A", scroll, 0f, -1, FOREGROUND, BACKGROUND));
    }

    @Test
    void overloadsWithoutEllipsisUseDefaultEllipsis() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        TextDrawing.drawTruncated(grid, 0, 0, 4, LONG_TEXT, FOREGROUND, BACKGROUND);
        assertEquals("ABC…", rowText(grid, 0));
        TextDrawing.drawScrolling(grid, 0, 0, 4, LONG_TEXT, TextScroll.backAndForth(2f, 1f), 2f,
                FOREGROUND, BACKGROUND);
        assertEquals("…DE…", rowText(grid, 0));
    }

    @Test
    void columnNearIntegerMaximumWritesNothing() {
        TerminalGrid grid = filledGrid(5, 1, 'x');
        truncated(grid, Integer.MAX_VALUE, 0, 10, LONG_TEXT);
        truncated(grid, Integer.MAX_VALUE - 2, 0, 10, LONG_TEXT);
        scrolling(grid, Integer.MAX_VALUE - 1, 0, 4, LONG_TEXT, TextScroll.loop(1f, 0f, 1), 3f);
        assertEquals("xxxxx", rowText(grid, 0));
    }

    @Test
    void veryNegativeColumnWithHugeWidthFinishesAndFillsBlanks() {
        TerminalGrid grid = filledGrid(5, 1, 'x');
        truncated(grid, -2_000_000_000, 0, Integer.MAX_VALUE, LONG_TEXT);
        assertEquals("     ", rowText(grid, 0));
    }

    @Test
    void areaEndingBeforeTheGridWritesNothing() {
        TerminalGrid grid = filledGrid(5, 1, 'x');
        truncated(grid, -Integer.MAX_VALUE, 0, Integer.MAX_VALUE, LONG_TEXT);
        assertEquals("xxxxx", rowText(grid, 0));
    }

    private static String scrolledRow(TextScroll scroll, int width, float seconds) {
        TerminalGrid grid = filledGrid(width, 1, 'x');
        scrolling(grid, 0, 0, width, LONG_TEXT, scroll, seconds);
        return rowText(grid, 0);
    }

    private static void truncated(TerminalGrid grid, int column, int row, int width,
            String text) {
        TextDrawing.drawTruncated(grid, column, row, width, text, ELLIPSIS, FOREGROUND,
                BACKGROUND);
    }

    private static void scrolling(TerminalGrid grid, int column, int row, int width,
            String text, TextScroll scroll, float seconds) {
        TextDrawing.drawScrolling(grid, column, row, width, text, scroll, seconds, ELLIPSIS,
                FOREGROUND, BACKGROUND);
    }

    private static TerminalGrid filledGrid(int columns, int rows, int codePoint) {
        TerminalGrid grid = new TerminalGrid(columns, rows);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                grid.set(x, y, codePoint, FOREGROUND, BACKGROUND);
            }
        }
        return grid;
    }

    private static String rowText(TerminalGrid grid, int row) {
        StringBuilder text = new StringBuilder();
        for (int x = 0; x < grid.getColumns(); x++) {
            text.appendCodePoint(grid.getCodePoint(x, row));
        }
        return text.toString();
    }
}
