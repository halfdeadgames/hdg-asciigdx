package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class TextDrawingWrapTest {

    private static final int FOREGROUND = 0xFF0000FF;
    private static final int BACKGROUND = 0x00FF00FF;

    private static String rowText(TerminalGrid grid, int row) {
        StringBuilder builder = new StringBuilder();
        for (int column = 0; column < grid.getColumns(); column++) {
            builder.appendCodePoint(grid.getCodePoint(column, row));
        }
        return builder.toString();
    }

    private static String areaText(TerminalGrid grid, int row, int startColumn, int endColumn) {
        StringBuilder builder = new StringBuilder();
        for (int column = startColumn; column <= endColumn; column++) {
            builder.appendCodePoint(grid.getCodePoint(column, row));
        }
        return builder.toString();
    }

    private static void draw(TerminalGrid grid, int topRow, String text, WrapRules rules) {
        WrapLayout layout = new WrapLayout();
        layout.layOut(text, rules);
        TextDrawing.drawWrapped(grid, topRow, layout, FOREGROUND, BACKGROUND);
    }

    @Test
    void drawsEachLineOnItsOwnRowAndFillsTheArea() {
        TerminalGrid grid = new TerminalGrid(10, 4);

        draw(grid, 1, "hello world", WrapRules.between(0, 5));

        assertEquals("hello ", areaText(grid, 1, 0, 5));
        assertEquals("world ", areaText(grid, 2, 0, 5));
        assertEquals(FOREGROUND, grid.getForeground(5, 1));
        assertEquals(BACKGROUND, grid.getBackground(5, 1));
        assertEquals(BACKGROUND, grid.getBackground(5, 2));
    }

    @Test
    void cellsOutsideTheAreaAndRowsBeyondTheLinesAreNotTouched() {
        TerminalGrid grid = new TerminalGrid(10, 4);
        TerminalGrid untouched = new TerminalGrid(10, 4);

        draw(grid, 1, "hi", WrapRules.between(2, 5));

        assertEquals(untouched.getCodePoint(0, 1), grid.getCodePoint(1, 1));
        assertEquals(untouched.getCodePoint(0, 1), grid.getCodePoint(6, 1));
        assertEquals(untouched.getCodePoint(0, 0), grid.getCodePoint(2, 0));
        assertEquals(untouched.getCodePoint(0, 2), grid.getCodePoint(2, 2));
        assertEquals('h', grid.getCodePoint(2, 1));
        assertEquals(' ', grid.getCodePoint(5, 1));
    }

    @Test
    void ellipsisIsDrawnRightAfterTheContent() {
        TerminalGrid grid = new TerminalGrid(10, 2);

        draw(grid, 0, "hello world", WrapRules.between(0, 5).withMaxLines(1));

        assertEquals("hello…", areaText(grid, 0, 0, 5));
        assertEquals(FOREGROUND, grid.getForeground(5, 0));
    }

    @Test
    void customEllipsisIsUsed() {
        TerminalGrid grid = new TerminalGrid(10, 2);

        draw(grid, 0, "hello world", WrapRules.between(0, 5).withMaxLines(1).withEllipsis('~'));

        assertEquals('~', grid.getCodePoint(5, 0));
    }

    @Test
    void columnsAndRowsOutsideTheGridAreClipped() {
        TerminalGrid grid = new TerminalGrid(10, 2);

        draw(grid, -1, "hello world", WrapRules.between(-2, 3));

        assertEquals('r', grid.getCodePoint(0, 0));
        assertEquals('l', grid.getCodePoint(1, 0));
        assertEquals('d', grid.getCodePoint(2, 0));
        assertEquals(' ', grid.getCodePoint(3, 0));
    }

    @Test
    void areaWiderThanTheGridIsClipped() {
        TerminalGrid grid = new TerminalGrid(4, 1);

        draw(grid, 0, "ab", WrapRules.between(2, 100));

        assertEquals('a', grid.getCodePoint(2, 0));
        assertEquals('b', grid.getCodePoint(3, 0));
    }

    @Test
    void rowsOutsideTheGridWriteNothing() {
        TerminalGrid grid = new TerminalGrid(10, 2);
        TerminalGrid untouched = new TerminalGrid(10, 2);

        draw(grid, 5, "hello world", WrapRules.between(0, 5));
        draw(grid, Integer.MAX_VALUE, "a\nb", WrapRules.between(0, 5));

        assertEquals(rowText(untouched, 0), rowText(grid, 0));
        assertEquals(rowText(untouched, 1), rowText(grid, 1));
    }

    @Test
    void layoutThatWasNeverLaidOutDrawsNothing() {
        TerminalGrid grid = new TerminalGrid(10, 2);
        TerminalGrid untouched = new TerminalGrid(10, 2);

        TextDrawing.drawWrapped(grid, 0, new WrapLayout(), FOREGROUND, BACKGROUND);

        assertEquals(rowText(untouched, 0), rowText(grid, 0));
    }

    @Test
    void drawingFollowsTheLatestLayOut() {
        TerminalGrid grid = new TerminalGrid(10, 1);
        StringBuilder text = new StringBuilder("abc");
        WrapLayout layout = new WrapLayout();
        layout.layOut(text, WrapRules.between(0, 5));
        text.setCharAt(0, 'x');
        layout.layOut(text, WrapRules.between(0, 5));

        TextDrawing.drawWrapped(grid, 0, layout, FOREGROUND, BACKGROUND);

        assertEquals("xbc   ", areaText(grid, 0, 0, 5));
    }

    private static WrapRules hyphenatingRules() {
        return WrapRules.between(0, 3).withBreakMode(BreakMode.HYPHENATE)
                .withHyphenator(Hyphenator.fromTexPatterns("\\patterns{ 1b 1c 1d 1e 1f }"));
    }

    @Test
    void hyphenIsDrawnRightAfterTheSplitPiece() {
        TerminalGrid grid = new TerminalGrid(10, 3);

        draw(grid, 0, "abcdefg", hyphenatingRules());

        assertEquals("abc-", areaText(grid, 0, 0, 3));
        assertEquals("defg", areaText(grid, 1, 0, 3));
        assertEquals(FOREGROUND, grid.getForeground(3, 0));
        assertEquals(BACKGROUND, grid.getBackground(3, 0));
    }

    @Test
    void customHyphenCodePointIsDrawn() {
        TerminalGrid grid = new TerminalGrid(10, 3);

        draw(grid, 0, "abcdefg", hyphenatingRules().withHyphen('~'));

        assertEquals("abc~", areaText(grid, 0, 0, 3));
    }

    @Test
    void nullArgumentsAreRejected() {
        TerminalGrid grid = new TerminalGrid(10, 2);
        WrapLayout layout = new WrapLayout();

        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawWrapped(null, 0, layout, FOREGROUND, BACKGROUND));
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawWrapped(grid, 0, null, FOREGROUND, BACKGROUND));
    }
}
