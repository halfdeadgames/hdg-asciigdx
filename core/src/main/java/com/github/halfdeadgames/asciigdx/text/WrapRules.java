package com.github.halfdeadgames.asciigdx.text;

import java.util.Objects;

/**
 * Immutable settings that tell a {@link WrapLayout} how to wrap text: the columns of the first
 * line, the columns of every other line, the line limit, the break mode, the ellipsis, and, for
 * {@link BreakMode#HYPHENATE}, the hyphenator and the hyphen.
 *
 * <p>Columns are grid columns and the end column is included: start 4 and end 20 mean columns 4
 * through 20, a width of 17 cells. Columns may lie outside the grid; drawing clips them.
 *
 * <p>Wrapped text takes inclusive start and end columns, while the one-line methods in
 * {@code TextDrawing} take a start column plus a width in cells. Columns 4 through 20 cover the
 * same cells as column 4 with a width of 17.
 *
 * <p>Defaults: wrapped lines use the same columns as the first line, there is no line limit,
 * the break mode is {@link BreakMode#WORD}, the ellipsis is
 * {@link TextDrawing#DEFAULT_ELLIPSIS}, no hyphenator is set (English is used when hyphenating),
 * and the hyphen is {@link TextDrawing#DEFAULT_HYPHEN}.
 *
 * <p>Example:
 * <pre>{@code
 * WrapRules rules = WrapRules.between(2, 31).withMaxLines(4);
 * WrapRules hyphenating = rules.withBreakMode(BreakMode.HYPHENATE);
 * }</pre>
 */
public final class WrapRules {

    /** Value of {@link #getMaxLines()} when the number of lines is not limited. */
    public static final int NO_LINE_LIMIT = Integer.MAX_VALUE;

    private final int firstLineStartColumn;
    private final int firstLineEndColumn;
    private final int wrappedLineStartColumn;
    private final int wrappedLineEndColumn;
    private final int maxLines;
    private final BreakMode breakMode;
    private final int ellipsisCodePoint;
    private final Hyphenator hyphenator;
    private final int hyphenCodePoint;

    private WrapRules(int firstLineStartColumn, int firstLineEndColumn,
            int wrappedLineStartColumn, int wrappedLineEndColumn, int maxLines,
            BreakMode breakMode, int ellipsisCodePoint, Hyphenator hyphenator,
            int hyphenCodePoint) {
        this.firstLineStartColumn = firstLineStartColumn;
        this.firstLineEndColumn = firstLineEndColumn;
        this.wrappedLineStartColumn = wrappedLineStartColumn;
        this.wrappedLineEndColumn = wrappedLineEndColumn;
        this.maxLines = maxLines;
        this.breakMode = breakMode;
        this.ellipsisCodePoint = ellipsisCodePoint;
        this.hyphenator = hyphenator;
        this.hyphenCodePoint = hyphenCodePoint;
    }

    /**
     * Creates rules with the given columns for the first line. Wrapped lines use the same columns.
     *
     * @param startColumn first column of the line area, in grid columns; any value is accepted
     * @param endColumn last column of the line area, included; must not be less than
     *         {@code startColumn}
     * @return new rules with the defaults described in the class documentation
     * @throws IllegalArgumentException if {@code startColumn > endColumn} or the width does not
     *         fit in an {@code int}
     */
    public static WrapRules between(int startColumn, int endColumn) {
        requireValidColumns(startColumn, endColumn);
        return new WrapRules(startColumn, endColumn, startColumn, endColumn, NO_LINE_LIMIT,
                BreakMode.WORD, TextDrawing.DEFAULT_ELLIPSIS, null, TextDrawing.DEFAULT_HYPHEN);
    }

    /**
     * Returns a copy that gives every line except the very first the given columns. This includes
     * the lines that start after a newline.
     *
     * @param startColumn first column of the area, in grid columns; any value is accepted
     * @param endColumn last column of the area, included; must not be less than
     *         {@code startColumn}
     * @return changed copy; this object is not modified
     * @throws IllegalArgumentException if {@code startColumn > endColumn} or the width does not
     *         fit in an {@code int}
     */
    public WrapRules withWrappedLineColumns(int startColumn, int endColumn) {
        requireValidColumns(startColumn, endColumn);
        return new WrapRules(firstLineStartColumn, firstLineEndColumn, startColumn, endColumn,
                maxLines, breakMode, ellipsisCodePoint, hyphenator, hyphenCodePoint);
    }

    /**
     * Returns a copy that limits the number of lines. When more text remains, the last line ends
     * with the ellipsis.
     *
     * @param maxLines largest number of lines, at least 1; {@link #NO_LINE_LIMIT} removes the
     *         limit
     * @return changed copy; this object is not modified
     * @throws IllegalArgumentException if {@code maxLines} is less than 1
     */
    public WrapRules withMaxLines(int maxLines) {
        if (maxLines < 1) {
            throw new IllegalArgumentException("maxLines must be at least 1: " + maxLines);
        }
        return new WrapRules(firstLineStartColumn, firstLineEndColumn, wrappedLineStartColumn,
                wrappedLineEndColumn, maxLines, breakMode, ellipsisCodePoint, hyphenator,
                hyphenCodePoint);
    }

    /**
     * Returns a copy that uses the given break mode. The default is {@link BreakMode#WORD}.
     *
     * @param mode where lines may break; must not be {@code null}
     * @return changed copy; this object is not modified
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public WrapRules withBreakMode(BreakMode mode) {
        Objects.requireNonNull(mode, "mode");
        return new WrapRules(firstLineStartColumn, firstLineEndColumn, wrappedLineStartColumn,
                wrappedLineEndColumn, maxLines, mode, ellipsisCodePoint, hyphenator,
                hyphenCodePoint);
    }

    /**
     * Returns a copy that shows the given code point where text is cut off by the line limit. It
     * takes one cell. The default is {@link TextDrawing#DEFAULT_ELLIPSIS}.
     *
     * @param codePoint glyph to draw, a valid Unicode code point; a font without U+2026 should
     *         pass another single code point, such as {@code '~'}
     * @return changed copy; this object is not modified
     * @throws IllegalArgumentException if {@code codePoint} is not a valid code point
     */
    public WrapRules withEllipsis(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid ellipsis code point: " + codePoint);
        }
        return new WrapRules(firstLineStartColumn, firstLineEndColumn, wrappedLineStartColumn,
                wrappedLineEndColumn, maxLines, breakMode, codePoint, hyphenator,
                hyphenCodePoint);
    }

    /**
     * Returns a copy that finds word break points with the given hyphenator. It has no effect
     * unless the break mode is {@link BreakMode#HYPHENATE}; the value is kept if the mode changes
     * later. All copies share the instance, and {@code layOut} reads it. A {@code Hyphenator} is
     * immutable and thread-safe, so sharing is safe. Without one, the layout uses
     * {@link Hyphenator#englishUs()}. The first layout that needs it loads the bundled patterns
     * once, which takes a few milliseconds, so call {@code Hyphenator.englishUs()} while loading
     * to avoid a stall during play.
     *
     * @param hyphenator source of break points; must not be {@code null}
     * @return changed copy; this object is not modified
     * @throws NullPointerException if {@code hyphenator} is {@code null}
     */
    public WrapRules withHyphenator(Hyphenator hyphenator) {
        Objects.requireNonNull(hyphenator, "hyphenator");
        return new WrapRules(firstLineStartColumn, firstLineEndColumn, wrappedLineStartColumn,
                wrappedLineEndColumn, maxLines, breakMode, ellipsisCodePoint, hyphenator,
                hyphenCodePoint);
    }

    /**
     * Returns a copy that draws the given code point at the end of a line that was split inside a
     * word. It takes one cell. The default is {@link TextDrawing#DEFAULT_HYPHEN}.
     *
     * @param codePoint glyph to draw, a valid Unicode code point that the font must contain; used
     *         only with {@link BreakMode#HYPHENATE} and ignored in other modes
     * @return changed copy; this object is not modified
     * @throws IllegalArgumentException if {@code codePoint} is not a valid code point
     */
    public WrapRules withHyphen(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid hyphen code point: " + codePoint);
        }
        return new WrapRules(firstLineStartColumn, firstLineEndColumn, wrappedLineStartColumn,
                wrappedLineEndColumn, maxLines, breakMode, ellipsisCodePoint, hyphenator,
                codePoint);
    }

    /**
     * Returns the first column of the very first line.
     *
     * @return grid column, any int
     */
    public int getFirstLineStartColumn() {
        return firstLineStartColumn;
    }

    /**
     * Returns the last column of the very first line, included.
     *
     * @return grid column, not less than {@link #getFirstLineStartColumn()}
     */
    public int getFirstLineEndColumn() {
        return firstLineEndColumn;
    }

    /**
     * Returns the first column of every line except the very first.
     *
     * @return grid column, any int
     */
    public int getWrappedLineStartColumn() {
        return wrappedLineStartColumn;
    }

    /**
     * Returns the last column of every line except the very first, included.
     *
     * @return grid column, not less than {@link #getWrappedLineStartColumn()}
     */
    public int getWrappedLineEndColumn() {
        return wrappedLineEndColumn;
    }

    /**
     * Returns the largest number of lines.
     *
     * @return at least 1; {@link #NO_LINE_LIMIT} when unlimited
     */
    public int getMaxLines() {
        return maxLines;
    }

    /**
     * Returns where lines may break.
     *
     * @return the break mode, never {@code null}
     */
    public BreakMode getBreakMode() {
        return breakMode;
    }

    /**
     * Returns the code point drawn where text is cut off by the line limit.
     *
     * @return a valid Unicode code point
     */
    public int getEllipsisCodePoint() {
        return ellipsisCodePoint;
    }

    /**
     * Returns the hyphenator set with {@link #withHyphenator(Hyphenator)}.
     *
     * @return the hyphenator, or {@code null} when none was set (the project uses no nullability
     *         annotations; Kotlin callers should treat the result as nullable); the layout then
     *         uses {@link Hyphenator#englishUs()}
     */
    public Hyphenator getHyphenator() {
        return hyphenator;
    }

    /**
     * Returns the code point drawn at the end of a line that was split inside a word.
     *
     * @return a valid Unicode code point; {@link TextDrawing#DEFAULT_HYPHEN} by default
     */
    public int getHyphenCodePoint() {
        return hyphenCodePoint;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WrapRules)) {
            return false;
        }
        WrapRules rules = (WrapRules) other;
        return firstLineStartColumn == rules.firstLineStartColumn
                && firstLineEndColumn == rules.firstLineEndColumn
                && wrappedLineStartColumn == rules.wrappedLineStartColumn
                && wrappedLineEndColumn == rules.wrappedLineEndColumn
                && maxLines == rules.maxLines
                && breakMode == rules.breakMode
                && ellipsisCodePoint == rules.ellipsisCodePoint
                && Objects.equals(hyphenator, rules.hyphenator)
                && hyphenCodePoint == rules.hyphenCodePoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstLineStartColumn, firstLineEndColumn, wrappedLineStartColumn,
                wrappedLineEndColumn, maxLines, breakMode, ellipsisCodePoint, hyphenator,
                hyphenCodePoint);
    }

    @Override
    public String toString() {
        return "WrapRules[firstLine=" + firstLineStartColumn + ".." + firstLineEndColumn
                + ", wrappedLines=" + wrappedLineStartColumn + ".." + wrappedLineEndColumn
                + ", maxLines=" + maxLines + ", breakMode=" + breakMode
                + ", ellipsisCodePoint=" + ellipsisCodePoint
                + ", hyphenator=" + hyphenator
                + ", hyphenCodePoint=" + hyphenCodePoint + "]";
    }

    private static void requireValidColumns(int startColumn, int endColumn) {
        if (startColumn > endColumn) {
            throw new IllegalArgumentException(
                    "startColumn " + startColumn + " is greater than endColumn " + endColumn);
        }
        long width = (long) endColumn - startColumn + 1;
        if (width > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Width does not fit in an int: " + startColumn + ".." + endColumn);
        }
    }
}
