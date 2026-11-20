package com.github.halfdeadgames.asciigdx.text;

import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Objects;

/**
 * Implements the {@link TextPaint} variants of the {@link TextDrawing} methods. Letter indexes
 * are code point indexes in the text; the gradient index and count are given separately because
 * a gradient may cover a different range than the typewriter.
 */
final class PaintedText {

    private static final int BLANK_CODE_POINT = ' ';

    private PaintedText() {
    }

    static void drawText(TerminalGrid grid, int column, int row, CharSequence text,
            TextPaint paint) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(paint, "paint");
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
                    paint);
            index += Character.charCount(codePoint);
            cell++;
            letterIndex++;
        }
    }

    static void drawTruncated(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, int ellipsisCodePoint, TextPaint paint) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(paint, "paint");
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
                        paint);
            } else if (offset < textCells) {
                int codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
                putTextLetter(grid, cell, row, codePoint, offset, offset, textCells, paint);
            } else {
                putBlank(grid, cell, row, paint);
            }
        }
    }

    static void drawScrolling(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, TextScroll scroll, float secondsSinceStart, int ellipsisCodePoint,
            TextPaint paint) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(scroll, "scroll");
        Objects.requireNonNull(paint, "paint");
        TextDrawing.requireNonNegativeWidth(widthCells);
        TextDrawing.requireValidEllipsis(ellipsisCodePoint);
        if (widthCells == 0 || isRowOutside(grid, row)) {
            return;
        }
        int textCells = Character.codePointCount(text, 0, text.length());
        if (textCells <= widthCells) {
            drawTruncated(grid, column, row, widthCells, text, ellipsisCodePoint, paint);
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
                        textCells, paint);
            } else if (isText) {
                putTextLetter(grid, cell, row, codePoint, lettersBefore, lettersBefore,
                        textCells, paint);
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
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(paint, "paint");
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
                    lettersBefore, gradientBase, gradientCount);
        }
    }

    private static void drawWrappedLine(TerminalGrid grid, int row, WrapLayout layout, int line,
            boolean endsWithEllipsis, TextPaint paint, int lettersBefore, int gradientBase,
            int gradientCount) {
        CharSequence text = layout.getText();
        long column = layout.getLineStartColumn(line);
        int index = layout.getLineStart(line);
        int end = layout.getLineEnd(line);
        int lineLetterIndex = 0;
        while (index < end) {
            int codePoint = Character.codePointAt(text, index);
            putTextLetter(grid, column, row, codePoint, lettersBefore + lineLetterIndex,
                    gradientBase + lineLetterIndex, gradientCount, paint);
            index += Character.charCount(codePoint);
            column++;
            lineLetterIndex++;
        }
        int lettersThroughLine = lettersBefore + lineLetterIndex;
        int lastLetterGradientIndex = gradientBase + lineLetterIndex - 1;
        if (layout.isLineHyphenated(line)) {
            putMarker(grid, column, row, layout.getRules().getHyphenCodePoint(),
                    lettersThroughLine, lastLetterGradientIndex, gradientCount, paint);
            column++;
        }
        if (endsWithEllipsis) {
            putMarker(grid, column, row, layout.getRules().getEllipsisCodePoint(),
                    lettersThroughLine, lastLetterGradientIndex, gradientCount, paint);
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
            int letterIndex, int gradientIndex, int gradientCount, TextPaint paint) {
        if (letterIndex >= paint.getVisibleLetterCount()) {
            putBlank(grid, column, row, paint);
            return;
        }
        putCell(grid, column, row, codePoint, gradientColor(paint, gradientIndex, gradientCount),
                paint);
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
            TextPaint paint) {
        if (paint.getVisibleLetterCount() < lettersBefore) {
            putBlank(grid, column, row, paint);
            return;
        }
        int gradientIndex = Math.max(0, followedLetterGradientIndex);
        putCell(grid, column, row, codePoint, gradientColor(paint, gradientIndex, gradientCount),
                paint);
    }

    private static void putBlank(TerminalGrid grid, long column, int row, TextPaint paint) {
        putCell(grid, column, row, BLANK_CODE_POINT, paint.getForeground(), paint);
    }

    private static void putCell(TerminalGrid grid, long column, int row, int codePoint,
            int foreground, TextPaint paint) {
        if (column < 0 || column >= grid.getColumns()) {
            return;
        }
        int cellColumn = (int) column;
        if (paint.isKeepBackground()) {
            grid.setCodePoint(cellColumn, row, codePoint);
            grid.setForeground(cellColumn, row, foreground);
            return;
        }
        grid.set(cellColumn, row, codePoint, foreground, paint.getBackground());
    }

    private static int gradientColor(TextPaint paint, int gradientIndex, int gradientCount) {
        if (!paint.hasGradient() || gradientCount <= 1) {
            return paint.getForeground();
        }
        float amount = (float) gradientIndex / (gradientCount - 1);
        return CellColor.mixOklab(paint.getForeground(), paint.getGradientEnd(), amount);
    }
}
