package com.github.halfdeadgames.asciigdx.text;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Objects;

/**
 * Draws one line of text into a fixed width, either cut short with an ellipsis or scrolling.
 *
 * <p>Common rules for every method:
 * <ul>
 *   <li>Text length is counted in cells, one cell per code point, as in
 *       {@code GridDrawing.drawText}. A lone (unpaired) surrogate char counts as one cell.</li>
 *   <li>The ellipsis is always exactly one code point and one cell.</li>
 *   <li>Every call writes every cell of {@code [column, column + widthCells)} on the row. Cells
 *       without text get a space with the given colors, so text from an earlier frame never
 *       stays behind.</li>
 *   <li>Cells outside the grid are skipped but still consume a cell, and a row outside the grid
 *       writes nothing. Positions never cause an exception or overflow, and only the cells
 *       inside the grid are visited.</li>
 *   <li>{@code '\n'} gets no special handling: it is written like any other character.</li>
 *   <li>Call on the render thread. Drawing does not allocate.</li>
 * </ul>
 *
 * <p>Area convention: the one-line methods in this class describe their area as a start column
 * plus a width in cells. Wrapped text uses an inclusive start and end column instead. For
 * example, column 4 with width 17 covers the same cells as columns 4 through 20.
 *
 * <p>Example:
 * <pre>{@code
 * TextDrawing.drawTruncated(grid, 2, 1, 10, "A long title", foreground, background);
 * }</pre>
 */
public final class TextDrawing {

    /**
     * The horizontal ellipsis, U+2026. It is one cell wide and present in all built-in Saiksistt
     * sizes. A font without it should pass another single code point.
     */
    public static final int DEFAULT_ELLIPSIS = 0x2026;

    private static final int BLANK_CODE_POINT = ' ';

    private TextDrawing() {
    }

    /**
     * Writes text into a fixed width and replaces the end with {@link #DEFAULT_ELLIPSIS} when it
     * does not fit. Same as the full form with {@link #DEFAULT_ELLIPSIS}.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param column column of the first cell, in cells; any value is accepted
     * @param row row of the text, in cells; any value is accepted, rows outside the grid write
     *         nothing
     * @param widthCells width of the area in cells; at least 0
     * @param text text to write; must not be {@code null}; may be empty
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} or {@code text} is {@code null}
     * @throws IllegalArgumentException if {@code widthCells} is negative
     */
    public static void drawTruncated(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, int foreground, int background) {
        drawTruncated(grid, column, row, widthCells, text, DEFAULT_ELLIPSIS, foreground,
                background);
    }

    /**
     * Writes text into a fixed width and replaces the end with an ellipsis when it does not fit.
     *
     * <p>If the text fits ({@code cells <= widthCells}) it is written and the remaining cells are
     * filled with spaces. Otherwise the first {@code widthCells - 1} code points are written,
     * followed by the ellipsis in the last cell. With a width of 1 only the ellipsis is written.
     * With a width of 0 nothing is written.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param column column of the first cell, in cells; any value is accepted
     * @param row row of the text, in cells; any value is accepted, rows outside the grid write
     *         nothing
     * @param widthCells width of the area in cells; at least 0
     * @param text text to write; must not be {@code null}; may be empty
     * @param ellipsisCodePoint single valid Unicode code point drawn as the ellipsis, for
     *         example {@link #DEFAULT_ELLIPSIS}
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid} or {@code text} is {@code null}
     * @throws IllegalArgumentException if {@code widthCells} is negative or
     *         {@code ellipsisCodePoint} is not a valid code point
     */
    public static void drawTruncated(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, int ellipsisCodePoint, int foreground, int background) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        requireNonNegativeWidth(widthCells);
        requireValidEllipsis(ellipsisCodePoint);
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
            int codePoint = BLANK_CODE_POINT;
            if (truncated && offset == widthCells - 1) {
                codePoint = ellipsisCodePoint;
            } else if (offset < textCells) {
                codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
            }
            grid.set(column + offset, row, codePoint, foreground, background);
        }
    }

    /**
     * Writes text into a fixed width, scrolling it over time when it does not fit. Same as the
     * full form with {@link #DEFAULT_ELLIPSIS}.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param column column of the first cell, in cells; any value is accepted
     * @param row row of the text, in cells; any value is accepted, rows outside the grid write
     *         nothing
     * @param widthCells width of the area in cells; at least 0
     * @param text text to write; must not be {@code null}; may be empty
     * @param scroll motion of the text; must not be {@code null}
     * @param secondsSinceStart seconds since this text was first shown in this area; the caller
     *         owns that clock and resets it to 0 when the text or width changes. Very large
     *         values lose float precision, so passing total game uptime makes the steps uneven
     *         after long runs. NaN, infinite, and negative values show the start position
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid}, {@code text}, or {@code scroll} is
     *         {@code null}
     * @throws IllegalArgumentException if {@code widthCells} is negative
     */
    public static void drawScrolling(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, TextScroll scroll, float secondsSinceStart, int foreground,
            int background) {
        drawScrolling(grid, column, row, widthCells, text, scroll, secondsSinceStart,
                DEFAULT_ELLIPSIS, foreground, background);
    }

    /**
     * Writes text into a fixed width, scrolling it over time when it does not fit.
     *
     * <p>If the text fits it is written without an ellipsis and the remaining cells are filled with
     * spaces; the scroll is not consulted. Otherwise the window shows the part of the text chosen
     * by {@link TextScroll#getOffsetCells(int, int, float)}. Text moves one cell per step. Window
     * cell {@code k} shows text code point {@code offset + k} for a back-and-forth scroll. For a
     * loop scroll it shows strip cell {@code (offset + k) mod (cells + gap)}, where strip cells
     * past the text are blanks.
     *
     * <p>The ellipsis marks the sides where text is hidden. It replaces the leftmost window cell
     * when the strip cell just before the window holds text, and the rightmost window cell when the
     * strip cell just after the window holds text. With a width of 1 the ellipsis is shown if
     * either side applies. With a width of 0 nothing is written.
     *
     * @param grid grid to write to; must not be {@code null}
     * @param column column of the first cell, in cells; any value is accepted
     * @param row row of the text, in cells; any value is accepted, rows outside the grid write
     *         nothing
     * @param widthCells width of the area in cells; at least 0
     * @param text text to write; must not be {@code null}; may be empty
     * @param scroll motion of the text; must not be {@code null}
     * @param secondsSinceStart seconds since this text was first shown in this area; the caller
     *         owns that clock and resets it to 0 when the text or width changes. Very large
     *         values lose float precision, so passing total game uptime makes the steps uneven
     *         after long runs. NaN, infinite, and negative values show the start position
     * @param ellipsisCodePoint single valid Unicode code point drawn as the ellipsis, for
     *         example {@link #DEFAULT_ELLIPSIS}
     * @param foreground foreground color as RGBA8888
     * @param background background color as RGBA8888
     * @throws NullPointerException if {@code grid}, {@code text}, or {@code scroll} is
     *         {@code null}
     * @throws IllegalArgumentException if {@code widthCells} is negative or
     *         {@code ellipsisCodePoint} is not a valid code point
     */
    public static void drawScrolling(TerminalGrid grid, int column, int row, int widthCells,
            CharSequence text, TextScroll scroll, float secondsSinceStart, int ellipsisCodePoint,
            int foreground, int background) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(scroll, "scroll");
        requireNonNegativeWidth(widthCells);
        requireValidEllipsis(ellipsisCodePoint);
        if (widthCells == 0 || isRowOutside(grid, row)) {
            return;
        }
        int textCells = Character.codePointCount(text, 0, text.length());
        if (textCells <= widthCells) {
            drawTruncated(grid, column, row, widthCells, text, ellipsisCodePoint, foreground,
                    background);
            return;
        }
        long firstCell = Math.max(0L, -(long) column);
        long endCell = Math.min(widthCells, (long) grid.getColumns() - column);
        if (firstCell >= endCell) {
            return;
        }
        int offset = scroll.getOffsetCells(textCells, widthCells, secondsSinceStart);
        long stripCells = scroll.isLooping() ? (long) textCells + scroll.getGapCells() : textCells;
        boolean hiddenBefore = hasTextBefore(scroll, offset, textCells, stripCells);
        boolean hiddenAfter = hasTextAfter(scroll, offset, widthCells, textCells, stripCells);
        long stripCell = ((long) offset + firstCell) % stripCells;
        int index = stripCell < textCells
                ? Character.offsetByCodePoints(text, 0, (int) stripCell) : 0;
        for (int windowCell = (int) firstCell; windowCell < endCell; windowCell++) {
            int codePoint = BLANK_CODE_POINT;
            if (stripCell < textCells) {
                codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
            }
            boolean ellipsisHere = (windowCell == 0 && hiddenBefore)
                    || (windowCell == widthCells - 1 && hiddenAfter);
            grid.set(column + windowCell, row, ellipsisHere ? ellipsisCodePoint : codePoint,
                    foreground, background);
            stripCell++;
            if (stripCell == stripCells) {
                stripCell = 0;
                index = 0;
            }
        }
    }

    private static boolean hasTextBefore(TextScroll scroll, int offset, int textCells,
            long stripCells) {
        if (!scroll.isLooping()) {
            return offset > 0;
        }
        return (offset - 1L + stripCells) % stripCells < textCells;
    }

    private static boolean hasTextAfter(TextScroll scroll, int offset, int widthCells,
            int textCells, long stripCells) {
        if (!scroll.isLooping()) {
            return (long) offset + widthCells < textCells;
        }
        return ((long) offset + widthCells) % stripCells < textCells;
    }

    private static void requireNonNegativeWidth(int widthCells) {
        if (widthCells < 0) {
            throw new IllegalArgumentException("widthCells must be at least 0: " + widthCells);
        }
    }

    private static void requireValidEllipsis(int ellipsisCodePoint) {
        if (!Character.isValidCodePoint(ellipsisCodePoint)) {
            throw new IllegalArgumentException(
                    "ellipsisCodePoint is not a valid code point: " + ellipsisCodePoint);
        }
    }

    private static boolean isRowOutside(TerminalGrid grid, int row) {
        return row < 0 || row >= grid.getRows();
    }
}
