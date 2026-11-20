package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class TextPaintTest {

    private static final int FOREGROUND = 0xFF0000FF;
    private static final int GRADIENT_END = 0x0000FFFF;
    private static final int BACKGROUND = 0x00FF00FF;
    private static final int SCENERY = 0x123456FF;
    private static final int ELLIPSIS = TextDrawing.DEFAULT_ELLIPSIS;

    private static TextPaint paint() {
        return TextPaint.of(FOREGROUND, BACKGROUND);
    }

    private static String rowText(TerminalGrid grid, int row) {
        StringBuilder builder = new StringBuilder();
        for (int column = 0; column < grid.getColumns(); column++) {
            builder.appendCodePoint(grid.getCodePoint(column, row));
        }
        return builder.toString();
    }

    private static TerminalGrid sceneryGrid(int columns, int rows) {
        TerminalGrid grid = new TerminalGrid(columns, rows);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                grid.set(column, row, '.', 0x01020304, SCENERY);
            }
        }
        return grid;
    }

    private static WrapLayout layOut(String text, WrapRules rules) {
        WrapLayout layout = new WrapLayout();
        layout.layOut(text, rules);
        return layout;
    }

    @Test
    void paintStartsPlainAndShowsAllLetters() {
        TextPaint paint = paint();

        assertEquals(FOREGROUND, paint.getForeground());
        assertEquals(BACKGROUND, paint.getBackground());
        assertFalse(paint.isKeepBackground());
        assertFalse(paint.hasGradient());
        assertEquals(TextPaint.ALL_LETTERS, paint.getVisibleLetterCount());
    }

    @Test
    void gradientCanBeSetReadAndCleared() {
        TextPaint paint = paint();

        paint.setGradient(GRADIENT_END, GradientSpan.EACH_LINE);

        assertTrue(paint.hasGradient());
        assertEquals(GRADIENT_END, paint.getGradientEnd());
        assertSame(GradientSpan.EACH_LINE, paint.getGradientSpan());
        paint.clearGradient();
        assertFalse(paint.hasGradient());
        assertEquals(GRADIENT_END, paint.getGradientEnd());
        assertNull(paint.getGradientSpan());
    }

    @Test
    void gradientGettersNeverThrowBeforeAnyGradientIsSet() {
        TextPaint paint = paint();

        assertFalse(paint.hasGradient());
        assertNull(paint.getGradientSpan());
        assertEquals(FOREGROUND, paint.getGradientEnd());
        paint.setForeground(SCENERY);
        assertEquals(SCENERY, paint.getGradientEnd());
    }

    @Test
    void paintRejectsInvalidArguments() {
        TextPaint paint = paint();

        assertThrows(NullPointerException.class, () -> paint.setGradient(GRADIENT_END, null));
        assertThrows(IllegalArgumentException.class, () -> paint.setVisibleLetterCount(-1));
        paint.setVisibleLetterCount(0);
        assertEquals(0, paint.getVisibleLetterCount());
    }

    @Test
    void drawingRejectsNullPaint() {
        TerminalGrid grid = new TerminalGrid(8, 2);
        WrapLayout layout = layOut("hi", WrapRules.between(0, 5));

        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawText(grid, 0, 0, "hi", null));
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawTruncated(grid, 0, 0, 4, "hi", ELLIPSIS, null));
        assertThrows(NullPointerException.class, () -> TextDrawing.drawScrolling(grid, 0, 0, 4,
                "hi", TextScroll.backAndForth(1f, 0f), 0f, ELLIPSIS, null));
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawWrapped(grid, 0, layout, null));
    }

    @Test
    void siblingValidationStillApplies() {
        TerminalGrid grid = new TerminalGrid(8, 2);

        assertThrows(IllegalArgumentException.class,
                () -> TextDrawing.drawTruncated(grid, 0, 0, -1, "hi", ELLIPSIS, paint()));
        assertThrows(IllegalArgumentException.class,
                () -> TextDrawing.drawTruncated(grid, 0, 0, 3, "hi", -5, paint()));
    }

    @Test
    void plainPaintMatchesColorPairDrawing() {
        TerminalGrid withPaint = new TerminalGrid(12, 3);
        TerminalGrid withColors = new TerminalGrid(12, 3);
        String text = "ABCDEFGHIJKLMNOP";

        TextDrawing.drawTruncated(withPaint, 1, 0, 6, text, ELLIPSIS, paint());
        TextDrawing.drawTruncated(withColors, 1, 0, 6, text, ELLIPSIS, FOREGROUND, BACKGROUND);
        TextScroll scroll = TextScroll.loop(2f, 0f, 2);
        TextDrawing.drawScrolling(withPaint, -2, 1, 8, text, scroll, 1.5f, ELLIPSIS, paint());
        TextDrawing.drawScrolling(withColors, -2, 1, 8, text, scroll, 1.5f, ELLIPSIS, FOREGROUND,
                BACKGROUND);
        WrapLayout layout = layOut("hello brave new world", WrapRules.between(1, 7));
        TextDrawing.drawWrapped(withPaint, 2, layout, paint());
        TextDrawing.drawWrapped(withColors, 2, layout, FOREGROUND, BACKGROUND);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 12; column++) {
                assertEquals(withColors.getCodePoint(column, row),
                        withPaint.getCodePoint(column, row));
                assertEquals(withColors.getForeground(column, row),
                        withPaint.getForeground(column, row));
                assertEquals(withColors.getBackground(column, row),
                        withPaint.getBackground(column, row));
            }
        }
    }

    @Test
    void drawTextSkipsOffGridCharactersButConsumesCells() {
        TerminalGrid grid = sceneryGrid(4, 1);

        TextDrawing.drawText(grid, -2, 0, "ABCDEF", paint());

        assertEquals("CDEF", rowText(grid, 0));
        TerminalGrid right = sceneryGrid(4, 1);
        TextDrawing.drawText(right, 2, 0, "ABCDEF", paint());
        assertEquals("..AB", rowText(right, 0));
    }

    @Test
    void keepBackgroundNeverChangesBackgroundsIncludingPadding() {
        TerminalGrid grid = sceneryGrid(10, 1);
        TextPaint paint = paint();
        paint.setKeepBackground(true);

        TextDrawing.drawTruncated(grid, 0, 0, 8, "AB", ELLIPSIS, paint);

        assertEquals("AB      ..", rowText(grid, 0));
        for (int column = 0; column < 10; column++) {
            assertEquals(SCENERY, grid.getBackground(column, 0));
        }
        assertEquals(FOREGROUND, grid.getForeground(5, 0));
        assertEquals(0x01020304, grid.getForeground(9, 0));
    }

    @Test
    void keepBackgroundWorksForScrollingAndWrapped() {
        TerminalGrid grid = sceneryGrid(12, 3);
        TextPaint paint = paint();
        paint.setKeepBackground(true);

        TextDrawing.drawScrolling(grid, 0, 0, 5, "ABCDEFGHIJ", TextScroll.loop(1f, 0f, 2), 0f,
                ELLIPSIS, paint);
        TextDrawing.drawWrapped(grid, 1, layOut("hello world", WrapRules.between(0, 5)), paint);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 12; column++) {
                assertEquals(SCENERY, grid.getBackground(column, row));
            }
        }
    }

    @Test
    void withoutKeepBackgroundThePaintBackgroundIsWritten() {
        TerminalGrid grid = sceneryGrid(6, 1);

        TextDrawing.drawTruncated(grid, 0, 0, 4, "AB", ELLIPSIS, paint());

        assertEquals(BACKGROUND, grid.getBackground(3, 0));
        assertEquals(SCENERY, grid.getBackground(4, 0));
    }

    @Test
    void hiddenLettersAreBlanksWithPlainForeground() {
        TerminalGrid grid = sceneryGrid(8, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);
        paint.setVisibleLetterCount(2);

        TextDrawing.drawText(grid, 0, 0, "ABCD", paint);

        assertEquals("AB  ....", rowText(grid, 0));
        assertEquals(FOREGROUND, grid.getForeground(2, 0));
        assertEquals(BACKGROUND, grid.getBackground(2, 0));
    }

    @Test
    void zeroVisibleLettersShowsNothingButStillClearsTheArea() {
        TerminalGrid grid = sceneryGrid(6, 1);
        TextPaint paint = paint();
        paint.setVisibleLetterCount(0);

        TextDrawing.drawTruncated(grid, 0, 0, 4, "ABCDEFGH", ELLIPSIS, paint);

        assertEquals("    ..", rowText(grid, 0));
    }

    @Test
    void ellipsisAppearsOnlyWhenEveryLetterBeforeItIsVisible() {
        TextPaint paint = paint();
        paint.setVisibleLetterCount(2);
        TerminalGrid hidden = sceneryGrid(6, 1);
        TextDrawing.drawTruncated(hidden, 0, 0, 4, "ABCDEFGH", ELLIPSIS, paint);
        assertEquals("AB  ..", rowText(hidden, 0));

        paint.setVisibleLetterCount(3);
        TerminalGrid shown = sceneryGrid(6, 1);
        TextDrawing.drawTruncated(shown, 0, 0, 4, "ABCDEFGH", ELLIPSIS, paint);
        assertEquals("ABC" + (char) ELLIPSIS + "..", rowText(shown, 0));
    }

    @Test
    void wrappedEllipsisAppearsOnlyWhenEveryLetterBeforeItIsVisible() {
        WrapLayout layout = layOut("hello world", WrapRules.between(0, 5).withMaxLines(1));
        TextPaint paint = paint();
        paint.setVisibleLetterCount(4);
        TerminalGrid hidden = sceneryGrid(8, 1);
        TextDrawing.drawWrapped(hidden, 0, layout, paint);
        assertEquals("hell  ..", rowText(hidden, 0));

        paint.setVisibleLetterCount(5);
        TerminalGrid shown = sceneryGrid(8, 1);
        TextDrawing.drawWrapped(shown, 0, layout, paint);
        assertEquals("hello" + (char) ELLIPSIS + "..", rowText(shown, 0));
    }

    @Test
    void wholeTextGradientHasExactEndpoints() {
        TerminalGrid grid = new TerminalGrid(8, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);

        TextDrawing.drawText(grid, 0, 0, "ABCDE", paint);

        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(GRADIENT_END, grid.getForeground(4, 0));
        assertEquals(CellColor.mixOklab(FOREGROUND, GRADIENT_END, 0.5f),
                grid.getForeground(2, 0));
        assertNotEquals(FOREGROUND, grid.getForeground(2, 0));
    }

    @Test
    void oneLetterGradientUsesTheForeground() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);

        TextDrawing.drawText(grid, 0, 0, "A", paint);

        assertEquals(FOREGROUND, grid.getForeground(0, 0));
    }

    @Test
    void gradientDoesNotDependOnTheVisibleCount() {
        TerminalGrid all = new TerminalGrid(8, 1);
        TerminalGrid some = new TerminalGrid(8, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);
        TextDrawing.drawText(all, 0, 0, "ABCDE", paint);
        paint.setVisibleLetterCount(3);
        TextDrawing.drawText(some, 0, 0, "ABCDE", paint);

        for (int column = 0; column < 3; column++) {
            assertEquals(all.getForeground(column, 0), some.getForeground(column, 0));
        }
    }

    @Test
    void truncatedGradientRunsOverTheFullTextAndEllipsisTakesTheLastLetterColor() {
        TerminalGrid grid = new TerminalGrid(6, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);

        TextDrawing.drawTruncated(grid, 0, 0, 4, "ABCDEFGHIJ", ELLIPSIS, paint);

        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(CellColor.mixOklab(FOREGROUND, GRADIENT_END, 2f / 9f),
                grid.getForeground(2, 0));
        assertEquals(grid.getForeground(2, 0), grid.getForeground(3, 0));
    }

    @Test
    void scrollingGradientFollowsTheLetterNotTheScreenCell() {
        TextScroll scroll = TextScroll.backAndForth(1f, 0f);
        TerminalGrid start = new TerminalGrid(5, 1);
        TerminalGrid later = new TerminalGrid(5, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);
        String text = "ABCDEFGHIJ";

        TextDrawing.drawScrolling(start, 0, 0, 5, text, scroll, 0f, ELLIPSIS, paint);
        TextDrawing.drawScrolling(later, 0, 0, 5, text, scroll, 2f, ELLIPSIS, paint);

        int letterIndex = later.getCodePoint(2, 0) - 'A';
        assertNotEquals(2, letterIndex);
        assertEquals(CellColor.mixOklab(FOREGROUND, GRADIENT_END, letterIndex / 9f),
                later.getForeground(2, 0));
        assertEquals(CellColor.mixOklab(FOREGROUND, GRADIENT_END, 2f / 9f),
                start.getForeground(2, 0));
    }

    @Test
    void wrappedWholeTextGradientCoversAllLines() {
        WrapLayout layout = layOut("hello world", WrapRules.between(0, 5));
        TerminalGrid grid = new TerminalGrid(8, 2);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);

        TextDrawing.drawWrapped(grid, 0, layout, paint);

        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(GRADIENT_END, grid.getForeground(4, 1));
        assertNotEquals(FOREGROUND, grid.getForeground(0, 1));
    }

    @Test
    void wrappedEachLineGradientRestartsOnEveryLine() {
        WrapLayout layout = layOut("hello world", WrapRules.between(0, 5));
        TerminalGrid grid = new TerminalGrid(8, 2);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.EACH_LINE);

        TextDrawing.drawWrapped(grid, 0, layout, paint);

        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(GRADIENT_END, grid.getForeground(4, 0));
        assertEquals(FOREGROUND, grid.getForeground(0, 1));
        assertEquals(GRADIENT_END, grid.getForeground(4, 1));
    }

    @Test
    void eachLineEqualsWholeTextForSingleLineMethods() {
        TerminalGrid whole = new TerminalGrid(8, 1);
        TerminalGrid each = new TerminalGrid(8, 1);
        TextPaint paint = paint();
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);
        TextDrawing.drawTruncated(whole, 0, 0, 6, "ABCDEFGHIJ", ELLIPSIS, paint);
        paint.setGradient(GRADIENT_END, GradientSpan.EACH_LINE);
        TextDrawing.drawTruncated(each, 0, 0, 6, "ABCDEFGHIJ", ELLIPSIS, paint);

        for (int column = 0; column < 8; column++) {
            assertEquals(whole.getForeground(column, 0), each.getForeground(column, 0));
        }
    }

    @Test
    void keepTypewriterAndGradientCombine() {
        TerminalGrid grid = sceneryGrid(8, 1);
        TextPaint paint = paint();
        paint.setKeepBackground(true);
        paint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);
        paint.setVisibleLetterCount(2);

        TextDrawing.drawText(grid, 0, 0, "ABCD", paint);

        assertEquals("AB  ....", rowText(grid, 0));
        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(CellColor.mixOklab(FOREGROUND, GRADIENT_END, 1f / 3f),
                grid.getForeground(1, 0));
        assertEquals(FOREGROUND, grid.getForeground(2, 0));
        for (int column = 0; column < 8; column++) {
            assertEquals(SCENERY, grid.getBackground(column, 0));
        }
    }
}
