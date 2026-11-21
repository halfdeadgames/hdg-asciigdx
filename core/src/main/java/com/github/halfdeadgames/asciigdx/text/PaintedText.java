package com.github.halfdeadgames.asciigdx.text;

import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Objects;

/**
 * Implements the {@link TextPaint} variants of the {@link TextDrawing} methods. Letter indexes
 * are code point indexes in the text; the gradient index and count are given separately because
 * a gradient may cover a different range than the typewriter.
 *
 * <p>Every method also takes the {@link StyledText} that supplies per-letter colors; plain text
 * passes {@link StyledText#UNSTYLED_EMPTY}, which styles no letter. A letter's own color replaces
 * the paint's color (and the gradient). A marker (ellipsis or hyphen) takes the style of the
 * letter before it, or of the first letter when it has none before it.
 */
final class PaintedText {

    private static final int BLANK_CODE_POINT = ' ';

    private PaintedText() {
    }

    static void drawText(TerminalGrid grid, int column, int row, CharSequence text,
            TextPaint paint) {
        drawText(grid, column, row, text, paint, StyledText.UNSTYLED_EMPTY);
    }

    static void drawText(TerminalGrid grid, int column, int row, CharSequence text,
            TextPaint paint, StyledText styles) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(paint, "paint");
        Objects.requireNonNull(styles, "styles");
        if (isRowOutside(grid, row) || column >= grid.getColumns()) {
            return;
        }
        int letterCount = paint.hasGradient()
                ? Character.codePointCount(text, 0, text.length()) : 0;
        long cell = column;
        int index = 0;
        int letterIndex = 0;
        while (index < text.length() && cell < grid.getColumns()) {
            int codePoint = Character.codePointAt(text, index);
            putTextLetter(grid, cell, row, codePoint, letterIndex, letterIndex, letterCount,
                    paint, styles);
            index += Character.charCount(codePoint);
            cell++;
            letterIndex++;
        }
    }

    static void drawTruncated(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, int ellipsisCodePoint, TextPaint paint) {
        drawTruncated(grid, column, row, widthCells, text, ellipsisCodePoint, paint,
                StyledText.UNSTYLED_EMPTY);
    }

    static void drawTruncated(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, int ellipsisCodePoint, TextPaint paint, StyledText styles) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(paint, "paint");
        Objects.requireNonNull(styles, "styles");
        TextDrawing.requireNonNegativeWidth(widthCells);
        TextDrawing.requireValidEllipsis(ellipsisCodePoint);
        if (widthCells == 0 || isRowOutside(grid, row)) {
            return;
        }
        long firstCell = Math.max(0L, -(long) column);
        long endCell = Math.min(widthCells, (long) grid.getColumns() - column);
        if (firstCell >= endCell) {
            return;
        }
        int textCells = Character.codePointCount(text, 0, text.length());
        boolean truncated = textCells > widthCells;
        int index = Character.offsetByCodePoints(text, 0, (int) Math.min(firstCell, textCells));
        for (int offset = (int) firstCell; offset < endCell; offset++) {
            long cell = (long) column + offset;
            if (truncated && offset == widthCells - 1) {
                putMarker(grid, cell, row, ellipsisCodePoint, offset, offset - 1, textCells,
                        paint, styles);
            } else if (offset < textCells) {
                int codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
                putTextLetter(grid, cell, row, codePoint, offset, offset, textCells, paint,
                        styles);
            } else {
                putBlank(grid, cell, row, paint);
            }
        }
    }

    static void drawScrolling(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, TextScroll scroll, float secondsSinceStart, int ellipsisCodePoint,
            TextPaint paint) {
        drawScrolling(grid, column, row, widthCells, text, scroll, secondsSinceStart,
                ellipsisCodePoint, paint, StyledText.UNSTYLED_EMPTY);
    }

    static void drawScrolling(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, TextScroll scroll, float secondsSinceStart, int ellipsisCodePoint,
            TextPaint paint, StyledText styles) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(scroll, "scroll");
        Objects.requireNonNull(paint, "paint");
        Objects.requireNonNull(styles, "styles");
        TextDrawing.requireNonNegativeWidth(widthCells);
        TextDrawing.requireValidEllipsis(ellipsisCodePoint);
        if (widthCells == 0 || isRowOutside(grid, row)) {
            return;
        }
        int textCells = Character.codePointCount(text, 0, text.length());
        if (textCells <= widthCells) {
            drawTruncated(grid, column, row, widthCells, text, ellipsisCodePoint, paint, styles);
            return;
        }
        long firstCell = Math.max(0L, -(long) column);
        long endCell = Math.min(widthCells, (long) grid.getColumns() - column);
        if (firstCell >= endCell) {
            return;
        }
        int offset = scroll.getOffsetCells(textCells, widthCells, secondsSinceStart);
        long stripCells = scroll.isLooping() ? (long) textCells + scroll.getGapCells() : textCells;
        boolean hiddenBefore = TextDrawing.hasTextBefore(scroll, offset, textCells, stripCells);
        boolean hiddenAfter = TextDrawing.hasTextAfter(scroll, offset, widthCells, textCells,
                stripCells);
        long stripCell = ((long) offset + firstCell) % stripCells;
        int index = stripCell < textCells
                ? Character.offsetByCodePoints(text, 0, (int) stripCell) : 0;
        for (int windowCell = (int) firstCell; windowCell < endCell; windowCell++) {
            long cell = (long) column + windowCell;
            boolean isText = stripCell < textCells;
            int codePoint = BLANK_CODE_POINT;
            if (isText) {
                codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
            }
            boolean ellipsisHere = (windowCell == 0 && hiddenBefore)
                    || (windowCell == widthCells - 1 && hiddenAfter);
            int lettersBefore = (int) Math.min(stripCell, textCells);
            if (ellipsisHere) {
                putMarker(grid, cell, row, ellipsisCodePoint, lettersBefore, lettersBefore - 1,
                        textCells, paint, styles);
            } else if (isText) {
                putTextLetter(grid, cell, row, codePoint, lettersBefore, lettersBefore,
                        textCells, paint, styles);
            } else {
                putBlank(grid, cell, row, paint);
            }
            stripCell++;
            if (stripCell == stripCells) {
                stripCell = 0;
                index = 0;
            }
        }
    }

    static void drawWrapped(TerminalGrid grid, int topRow, WrapLayout layout, TextPaint paint) {
        drawWrapped(grid, topRow, layout, paint, StyledText.UNSTYLED_EMPTY);
    }

    static void drawWrapped(TerminalGrid grid, int topRow, WrapLayout layout, TextPaint paint,
            StyledText styles) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(paint, "paint");
        Objects.requireNonNull(styles, "styles");
        int lastLine = layout.getLineCount() - 1;
        if (lastLine < 0) {
            return;
        }
        CharSequence text = layout.getText();
        int firstStart = layout.getLineStart(0);
        int totalLetters = Character.codePointCount(text, firstStart, layout.getLineEnd(lastLine));
        boolean restartPerLine = paint.hasGradient()
                && paint.getGradientSpan() == GradientSpan.EACH_LINE;
        int previousStart = firstStart;
        int lettersBefore = 0;
        for (int line = 0; line <= lastLine; line++) {
            int lineStart = layout.getLineStart(line);
            lettersBefore += Character.codePointCount(text, previousStart, lineStart);
            previousStart = lineStart;
            long row = (long) topRow + line;
            if (row < 0 || row >= grid.getRows()) {
                continue;
            }
            int lineLetters = Character.codePointCount(text, lineStart, layout.getLineEnd(line));
            int gradientBase = restartPerLine ? 0 : lettersBefore;
            int gradientCount = restartPerLine ? lineLetters : totalLetters;
            boolean endsWithEllipsis = layout.isTruncated() && line == lastLine;
            drawWrappedLine(grid, (int) row, layout, line, endsWithEllipsis, paint,
                    lettersBefore, gradientBase, gradientCount, styles);
        }
    }

    private static void drawWrappedLine(TerminalGrid grid, int row, WrapLayout layout, int line,
            boolean endsWithEllipsis, TextPaint paint, int lettersBefore, int gradientBase,
            int gradientCount, StyledText styles) {
        CharSequence text = layout.getText();
        long column = layout.getLineStartColumn(line);
        int index = layout.getLineStart(line);
        int end = layout.getLineEnd(line);
        int lineLetterIndex = 0;
        while (index < end) {
            int codePoint = Character.codePointAt(text, index);
            putTextLetter(grid, column, row, codePoint, lettersBefore + lineLetterIndex,
                    gradientBase + lineLetterIndex, gradientCount, paint, styles);
            index += Character.charCount(codePoint);
            column++;
            lineLetterIndex++;
        }
        int lettersThroughLine = lettersBefore + lineLetterIndex;
        int lastLetterGradientIndex = gradientBase + lineLetterIndex - 1;
        if (layout.isLineHyphenated(line)) {
            putMarker(grid, column, row, layout.getRules().getHyphenCodePoint(),
                    lettersThroughLine, lastLetterGradientIndex, gradientCount, paint, styles);
            column++;
        }
        if (endsWithEllipsis) {
            putMarker(grid, column, row, layout.getRules().getEllipsisCodePoint(),
                    lettersThroughLine, lastLetterGradientIndex, gradientCount, paint, styles);
            column++;
        }
        long lastColumn = Math.min(layout.getLineEndColumn(line), grid.getColumns() - 1);
        for (long fillColumn = Math.max(column, 0); fillColumn <= lastColumn; fillColumn++) {
            putBlank(grid, fillColumn, row, paint);
        }
    }

    private static boolean isRowOutside(TerminalGrid grid, int row) {
        return row < 0 || row >= grid.getRows();
    }

    private static void putTextLetter(TerminalGrid grid, long column, int row, int codePoint,
            int letterIndex, int gradientIndex, int gradientCount, TextPaint paint,
            StyledText styles) {
        if (letterIndex >= paint.getVisibleLetterCount()) {
            putBlank(grid, column, row, paint);
            return;
        }
        putStyledCell(grid, column, row, codePoint, letterIndex, gradientIndex, gradientCount,
                paint, styles);
    }

    /**
     * Writes an ellipsis or hyphen, or a blank when a letter before it is still hidden.
     *
     * @param lettersBefore number of text letters before the marker; all must be visible
     * @param followedLetterGradientIndex gradient index of the letter the marker follows; the
     *         marker takes that letter's color, and -1 (no letter) counts as the first letter
     */
    private static void putMarker(TerminalGrid grid, long column, int row, int codePoint,
            int lettersBefore, int followedLetterGradientIndex, int gradientCount,
            TextPaint paint, StyledText styles) {
        if (paint.getVisibleLetterCount() < lettersBefore) {
            putBlank(grid, column, row, paint);
            return;
        }
        int gradientIndex = Math.max(0, followedLetterGradientIndex);
        int followedLetterIndex = Math.max(0, lettersBefore - 1);
        putStyledCell(grid, column, row, codePoint, followedLetterIndex, gradientIndex,
                gradientCount, paint, styles);
    }

    private static void putStyledCell(TerminalGrid grid, long column, int row, int codePoint,
            int styleLetterIndex, int gradientIndex, int gradientCount, TextPaint paint,
            StyledText styles) {
        int foreground = styles.foregroundOr(styleLetterIndex,
                gradientColor(paint, gradientIndex, gradientCount));
        int background = styles.backgroundOr(styleLetterIndex, paint.getBackground());
        putCell(grid, column, row, codePoint, foreground, background, paint);
    }

    private static void putBlank(TerminalGrid grid, long column, int row, TextPaint paint) {
        putCell(grid, column, row, BLANK_CODE_POINT, paint.getForeground(), paint.getBackground(),
                paint);
    }

    private static void putCell(TerminalGrid grid, long column, int row, int codePoint,
            int foreground, int background, TextPaint paint) {
        if (column < 0 || column >= grid.getColumns()) {
            return;
        }
        int cellColumn = (int) column;
        if (paint.isKeepBackground()) {
            grid.setCodePoint(cellColumn, row, codePoint);
            grid.setForeground(cellColumn, row, foreground);
            return;
        }
        grid.set(cellColumn, row, codePoint, foreground, background);
    }

    private static int gradientColor(TextPaint paint, int gradientIndex, int gradientCount) {
        if (!paint.hasGradient() || gradientCount <= 1) {
            return paint.getForeground();
        }
        float amount = (float) gradientIndex / (gradientCount - 1);
        return CellColor.mixOklab(paint.getForeground(), paint.getGradientEnd(), amount);
    }
}
