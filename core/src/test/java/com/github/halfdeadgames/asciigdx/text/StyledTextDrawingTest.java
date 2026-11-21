package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class StyledTextDrawingTest {

    private static final int FOREGROUND = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x000000FF;
    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;
    private static final int ELLIPSIS = TextDrawing.DEFAULT_ELLIPSIS;
    private static final ColorNames NAMES = name -> switch (name) {
        case "red" -> OptionalInt.of(RED);
        case "blue" -> OptionalInt.of(BLUE);
        default -> OptionalInt.empty();
    };

    private final TerminalGrid grid = new TerminalGrid(20, 4);
    private final TextPaint paint = TextPaint.of(FOREGROUND, BACKGROUND);

    private static StyledText parse(String source) {
        return StyledText.parse(source, NAMES);
    }

    private static String rowText(TerminalGrid target, int row, int count) {
        StringBuilder builder = new StringBuilder();
        for (int column = 0; column < count; column++) {
            builder.appendCodePoint(target.getCodePoint(column, row));
        }
        return builder.toString();
    }

    @Test
    void drawTextWritesVisibleLettersWithTheirOwnColors() {
        TextDrawing.drawText(grid, 2, 0, parse("a[color:red]b[/color]c"), paint);

        assertEquals("abc", rowText(grid, 0, 5).substring(2));
        assertEquals(FOREGROUND, grid.getForeground(2, 0));
        assertEquals(RED, grid.getForeground(3, 0));
        assertEquals(FOREGROUND, grid.getForeground(4, 0));
    }

    @Test
    void drawTextUsesPerLetterBackgroundAndThePaintBackgroundElsewhere() {
        TextDrawing.drawText(grid, 0, 0, parse("a[bg:blue]b[/bg]c"), paint);

        assertEquals(BACKGROUND, grid.getBackground(0, 0));
        assertEquals(BLUE, grid.getBackground(1, 0));
        assertEquals(BACKGROUND, grid.getBackground(2, 0));
    }

    @Test
    void keepBackgroundLeavesBackgroundsEvenForTaggedLetters() {
        grid.setBackground(1, 0, 0x12345678);
        paint.setKeepBackground(true);

        TextDrawing.drawText(grid, 0, 0, parse("a[bg:blue]b"), paint);

        assertEquals(0x12345678, grid.getBackground(1, 0));
        assertEquals('b', grid.getCodePoint(1, 0));
    }

    @Test
    void letterColorReplacesTheGradientColor() {
        paint.setGradient(0x00FF00FF, GradientSpan.WHOLE_TEXT);

        TextDrawing.drawText(grid, 0, 0, parse("a[color:red]b[/color]c"), paint);

        assertEquals(RED, grid.getForeground(1, 0));
        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(0x00FF00FF, grid.getForeground(2, 0));
    }

    @Test
    void typewriterCountsVisibleLettersOnly() {
        paint.setVisibleLetterCount(2);

        TextDrawing.drawText(grid, 0, 0, parse("[color:red]abcd"), paint);

        assertEquals("ab  ", rowText(grid, 0, 4));
    }

    @Test
    void truncationCountsVisibleLettersAndPutsTheEllipsisWhereThePlainTextDoes() {
        TerminalGrid plain = new TerminalGrid(20, 4);
        TextDrawing.drawTruncated(plain, 0, 0, 5, "hello world", ELLIPSIS, paint);

        TextDrawing.drawTruncated(grid, 0, 0, 5, parse("[color:red]hello[/color] world"),
                ELLIPSIS, paint);

        assertEquals(rowText(plain, 0, 6), rowText(grid, 0, 6));
        assertEquals("hell…", rowText(grid, 0, 5));
    }

    @Test
    void ellipsisTakesTheStyleOfTheLetterBeforeIt() {
        TextDrawing.drawTruncated(grid, 0, 0, 5, parse("hel[color:red]lo wor[/color]ld"),
                ELLIPSIS, paint);

        assertEquals(RED, grid.getForeground(3, 0));
        assertEquals(RED, grid.getForeground(4, 0));
    }

    @Test
    void ellipsisAfterAnUnstyledLetterUsesThePaint() {
        TextDrawing.drawTruncated(grid, 0, 0, 5, parse("hell[color:red]o world"), ELLIPSIS,
                paint);

        assertEquals(FOREGROUND, grid.getForeground(4, 0));
    }

    @Test
    void ellipsisAloneInAWidthOfOneTakesTheFirstLettersStyle() {
        TextDrawing.drawTruncated(grid, 0, 0, 1, parse("[color:red]abc"), ELLIPSIS, paint);

        assertEquals(ELLIPSIS, grid.getCodePoint(0, 0));
        assertEquals(RED, grid.getForeground(0, 0));
    }

    @Test
    void textThatFitsHasNoEllipsisAndPadsWithThePaint() {
        TextDrawing.drawTruncated(grid, 0, 0, 6, parse("[color:red]abc"), ELLIPSIS, paint);

        assertEquals("abc   ", rowText(grid, 0, 6));
        assertEquals(FOREGROUND, grid.getForeground(4, 0));
    }

    @Test
    void scrollingKeepsEachLettersColorWhileItMoves() {
        TextScroll scroll = TextScroll.backAndForth(1f, 0f);
        TerminalGrid plain = new TerminalGrid(20, 4);
        TextDrawing.drawScrolling(plain, 0, 0, 5, "hello world", scroll, 0f, ELLIPSIS, paint);

        TextDrawing.drawScrolling(grid, 0, 0, 5, parse("he[color:red]llo[/color] world"), scroll,
                0f, ELLIPSIS, paint);

        assertEquals(rowText(plain, 0, 5), rowText(grid, 0, 5));
        assertEquals(FOREGROUND, grid.getForeground(0, 0));
        assertEquals(RED, grid.getForeground(2, 0));
    }

    @Test
    void wrappingRunsOnTheVisibleTextSoTagsTakeNoWidth() {
        StyledText text = parse("[color:red]hello[/color] [color:blue]world[/color]");
        WrapLayout layout = new WrapLayout();
        layout.layOut(text.getVisibleText(), WrapRules.between(0, 5));

        TextDrawing.drawWrapped(grid, 0, layout, text, paint);

        assertEquals("hello", rowText(grid, 0, 5));
        assertEquals("world", rowText(grid, 1, 5));
        assertEquals(RED, grid.getForeground(0, 0));
        assertEquals(RED, grid.getForeground(4, 0));
        assertEquals(BLUE, grid.getForeground(0, 1));
        assertEquals(BLUE, grid.getForeground(4, 1));
    }

    @Test
    void wrappedEllipsisTakesTheStyleOfTheLastLetterOfItsLine() {
        StyledText text = parse("[color:red]hello[/color] world");
        WrapLayout layout = new WrapLayout();
        layout.layOut(text.getVisibleText(), WrapRules.between(0, 5).withMaxLines(1));

        TextDrawing.drawWrapped(grid, 0, layout, text, paint);

        assertEquals(ELLIPSIS, grid.getCodePoint(5, 0));
        assertEquals(RED, grid.getForeground(5, 0));
    }

    @Test
    void wrappedHyphenTakesTheStyleOfTheLastLetterOfItsLine() {
        StyledText text = parse("[color:blue]internationalization");
        WrapLayout layout = new WrapLayout();
        layout.layOut(text.getVisibleText(),
                WrapRules.between(0, 7).withBreakMode(BreakMode.HYPHENATE));
        assumeTrue(layout.isLineHyphenated(0));
        int hyphenColumn = layout.getLineEnd(0);

        TextDrawing.drawWrapped(grid, 0, layout, text, paint);

        assertEquals(TextDrawing.DEFAULT_HYPHEN, grid.getCodePoint(hyphenColumn, 0));
        assertEquals(BLUE, grid.getForeground(hyphenColumn, 0));
    }

    @Test
    void plainTextDrawsTheSameAsBefore() {
        TerminalGrid plain = new TerminalGrid(20, 4);
        TextDrawing.drawText(plain, 1, 0, "hello", paint);

        TextDrawing.drawText(grid, 1, 0, parse("hello"), paint);

        assertEquals(rowText(plain, 0, 8), rowText(grid, 0, 8));
        assertEquals(plain.getForeground(2, 0), grid.getForeground(2, 0));
        assertEquals(plain.getBackground(2, 0), grid.getBackground(2, 0));
    }

    @Test
    void wrappedDrawingRejectsALayoutOfAnotherText() {
        StyledText text = parse("[color:red]hello");
        WrapLayout rawLayout = new WrapLayout();
        rawLayout.layOut("[color:red]hello", WrapRules.between(0, 20));
        WrapLayout otherLayout = new WrapLayout();
        otherLayout.layOut("other", WrapRules.between(0, 20));

        assertThrows(IllegalArgumentException.class,
                () -> TextDrawing.drawWrapped(grid, 0, rawLayout, text, paint));
        assertThrows(IllegalArgumentException.class,
                () -> TextDrawing.drawWrapped(grid, 0, otherLayout, text, paint));
    }

    @Test
    void wrappedDrawingAcceptsALayoutThatWasNeverLaidOut() {
        TextDrawing.drawWrapped(grid, 0, new WrapLayout(), parse("a"), paint);
    }

    @Test
    void wrappedDrawingAcceptsAnEqualTextInAnotherCharSequence() {
        StyledText text = parse("[color:red]hello");
        WrapLayout layout = new WrapLayout();
        layout.layOut(new StringBuilder("hello"), WrapRules.between(0, 20));

        TextDrawing.drawWrapped(grid, 0, layout, text, paint);

        assertEquals(RED, grid.getForeground(0, 0));
    }

    @Test
    void nullArgumentsThrow() {
        StyledText text = parse("a");
        WrapLayout layout = new WrapLayout();
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawText(grid, 0, 0, (StyledText) null, paint));
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawText(grid, 0, 0, text, null));
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawTruncated(grid, 0, 0, 3, (StyledText) null, ELLIPSIS,
                        paint));
        assertThrows(NullPointerException.class,
                () -> TextDrawing.drawWrapped(grid, 0, layout, (StyledText) null, paint));
    }
}
