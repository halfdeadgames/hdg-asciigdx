package com.github.halfdeadgames.asciigdx.text;

import java.util.Arrays;
import java.util.Objects;

/**
 * The result of wrapping one text into lines, kept between calls so that drawing it every frame
 * allocates nothing. Create one instance, call {@link #layOut(CharSequence, WrapRules)} when the text or the
 * rules change, and draw it with {@code TextDrawing.drawWrapped}.
 *
 * <p>Wrapping rules:
 * <ul>
 *   <li>Only the very first line of the whole text uses the first-line columns of the
 *       {@link WrapRules}. Every other line, including a line that starts after a newline, uses
 *       the wrapped-line columns.</li>
 *   <li>{@code '\n'} forces a new line and is not drawn; {@code "\r\n"} counts as one newline.
 *       Leading spaces after a newline are kept, so paragraph indents survive. Consecutive
 *       newlines give empty lines. A newline at the very end of the text adds no extra empty
 *       line. Empty text gives no lines.</li>
 *   <li>Each code point takes one cell; a lone surrogate char takes one cell too. Only the space
 *       {@code ' '} (U+0020) is a breaking space. Every other character is a glyph.</li>
 *   <li>With {@link BreakMode#WORD}, a line takes as many whole words as fit, and a word longer
 *       than the line is split at a character, filling the line. With
 *       {@link BreakMode#CHARACTER}, a line takes as many code points as fit. In both modes the
 *       spaces at a wrap point are dropped: the trailing spaces of the wrapped line and the
 *       leading spaces of the next line.</li>
 *   <li>With {@link BreakMode#HYPHENATE}, words wrap as in {@code WORD}, but a word that does not
 *       fit the room left is split at the last allowed point where the piece plus a hyphen fits;
 *       see {@link #isLineHyphenated(int)}. If no point fits, or the line is narrower than 3
 *       cells, the word is handled as in {@code WORD}.</li>
 *   <li>If text remains after line {@link WrapRules#getMaxLines()}, ignoring remaining spaces and
 *       newlines, the layout is truncated: the last line is shortened to at most its width minus
 *       one cell, its trailing spaces are trimmed, and drawing puts the ellipsis in the cell after
 *       the content. On a line of width 1, only the ellipsis is shown. On a hyphenated last
 *       line, the ellipsis replaces the hyphen.</li>
 * </ul>
 *
 * <p>Before the first {@code layOut}, {@link #getLineCount()} is 0, {@link #isTruncated()} is
 * {@code false}, {@link #getRules()} returns {@code null} (the project uses no nullability
 * annotations), and the line getters throw {@link IndexOutOfBoundsException} because there are no
 * lines. Drawing such a layout writes nothing.
 *
 * <p>Not thread-safe; use it from the render thread. The instance is owned by the caller and
 * reusable. Its internal arrays grow only when a text needs more lines, or more break points in
 * one word, than any earlier one, so {@code layOut} otherwise allocates nothing.
 *
 * <p>Example:
 * <pre>{@code
 * WrapLayout layout = new WrapLayout();
 * layout.layOut(message, WrapRules.between(2, 31).withMaxLines(4));
 * // every frame:
 * TextDrawing.drawWrapped(grid, 1, layout, 0xFFFFFFFF, 0x000000FF);
 * }</pre>
 */
public final class WrapLayout {

    private static final int INITIAL_LINE_CAPACITY = 16;
    private static final int INITIAL_BREAK_CAPACITY = 16;
    private static final int NO_BREAK = -1;
    /** Two letters plus the hyphen: the narrowest line that can hold a hyphenated piece. */
    private static final int MIN_HYPHENATED_WIDTH = 3;
    private static final char EXISTING_HYPHEN = '-';

    private int[] lineStarts = new int[INITIAL_LINE_CAPACITY];
    private int[] lineEnds = new int[INITIAL_LINE_CAPACITY];
    private int[] lineStartColumns = new int[INITIAL_LINE_CAPACITY];
    private int[] lineEndColumns = new int[INITIAL_LINE_CAPACITY];
    private boolean[] lineHyphenated = new boolean[INITIAL_LINE_CAPACITY];
    private int[] breakOffsets = new int[INITIAL_BREAK_CAPACITY];
    private int lineCount;
    private int textLength;
    private CharSequence text;
    private boolean truncated;
    private WrapRules rules;

    /** Creates an empty layout with no lines. Call {@link #layOut} before drawing. */
    public WrapLayout() {
    }

    /**
     * Recomputes the lines of {@code text} under {@code rules}, replacing any earlier result.
     * Call it only when the text or the rules change; drawing reuses the result every frame. The
     * layout keeps a reference to {@code text} and does not copy it. If a mutable
     * {@code CharSequence} such as a {@code StringBuilder} changes, call this method again before
     * drawing, or the drawing is undefined. Allocates only when a text needs more lines, or more
     * break points in one word, than any earlier one; the first hyphenating call may load the
     * bundled English patterns.
     *
     * @param text text to wrap; must not be {@code null}; may be empty
     * @param rules columns, line limit, break mode, and ellipsis; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code rules} is {@code null}
     * @throws IllegalStateException if the break mode is {@code HYPHENATE}, no hyphenator is set,
     *         and the bundled English patterns cannot be loaded
     */
    public void layOut(CharSequence text, WrapRules rules) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(rules, "rules");
        this.rules = rules;
        this.text = text;
        this.textLength = text.length();
        this.lineCount = 0;
        this.truncated = false;
        int position = 0;
        while (position < textLength) {
            if (lineCount == rules.getMaxLines()) {
                truncateWhenTextRemains(text, position);
                return;
            }
            position = layOutLine(text, rules, position);
        }
    }

    /**
     * Returns the number of laid-out lines.
     *
     * @return line count; 0 before the first {@code layOut} and for empty text
     */
    public int getLineCount() {
        return lineCount;
    }

    /**
     * Returns where a line's content starts in the text.
     *
     * @param line line index, from 0 to {@code getLineCount() - 1}
     * @return index of the first char of the line, in chars
     * @throws IndexOutOfBoundsException if {@code line} is outside that range
     */
    public int getLineStart(int line) {
        checkLine(line);
        return lineStarts[line];
    }

    /**
     * Returns where a line's content ends in the text. Dropped spaces and the newline are not part
     * of the content.
     *
     * @param line line index, from 0 to {@code getLineCount() - 1}
     * @return index after the last char of the line, in chars (end exclusive)
     * @throws IndexOutOfBoundsException if {@code line} is outside that range
     */
    public int getLineEnd(int line) {
        checkLine(line);
        return lineEnds[line];
    }

    /**
     * Returns the first column of a line's area.
     *
     * @param line line index, from 0 to {@code getLineCount() - 1}
     * @return grid column
     * @throws IndexOutOfBoundsException if {@code line} is outside that range
     */
    public int getLineStartColumn(int line) {
        checkLine(line);
        return lineStartColumns[line];
    }

    /**
     * Returns the last column of a line's area, included.
     *
     * @param line line index, from 0 to {@code getLineCount() - 1}
     * @return grid column
     * @throws IndexOutOfBoundsException if {@code line} is outside that range
     */
    public int getLineEndColumn(int line) {
        checkLine(line);
        return lineEndColumns[line];
    }

    /**
     * Returns whether a hyphen is drawn after the line's last character. Drawing puts the hyphen
     * code point of the rules in the cell right after the line's content; that cell always lies
     * inside the line's columns, never past {@link #getLineEndColumn(int)}. A line is hyphenated
     * when {@link BreakMode#HYPHENATE} split a word. A split right after an existing
     * {@code '-'} is not hyphenated, because the text already shows a hyphen. A truncated last
     * line is not hyphenated, because its ellipsis takes that cell.
     *
     * @param line line index, from 0 to {@code getLineCount() - 1}
     * @return {@code true} if the hyphen must be drawn after the line
     * @throws IndexOutOfBoundsException if {@code line} is outside that range
     */
    public boolean isLineHyphenated(int line) {
        checkLine(line);
        return lineHyphenated[line];
    }

    /**
     * Returns whether the line limit cut off text. If so, the last line ends with the ellipsis.
     *
     * @return {@code true} if text remained after the last allowed line
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Returns the rules of the last {@code layOut} call.
     *
     * @return the rules, or {@code null} before the first {@code layOut}
     */
    public WrapRules getRules() {
        return rules;
    }

    /** Returns the text of the last {@code layOut} call, or {@code null} before the first. */
    CharSequence getText() {
        return text;
    }

    private void checkLine(int line) {
        if (line < 0 || line >= lineCount) {
            throw new IndexOutOfBoundsException(
                    "Line " + line + " is outside 0.." + (lineCount - 1));
        }
    }

    /** Appends the line that starts at {@code start} and returns where the next line starts. */
    private int layOutLine(CharSequence text, WrapRules rules, int start) {
        boolean isFirstLine = lineCount == 0;
        int startColumn = isFirstLine
                ? rules.getFirstLineStartColumn() : rules.getWrappedLineStartColumn();
        int endColumn = isFirstLine
                ? rules.getFirstLineEndColumn() : rules.getWrappedLineEndColumn();
        int width = widthOf(startColumn, endColumn);
        int index = start;
        int cells = 0;
        int lastSpaceBreak = -1;
        boolean seenGlyph = false;
        while (index < textLength) {
            int newlineLength = newlineLengthAt(text, index);
            if (newlineLength > 0) {
                appendLine(start, index, startColumn, endColumn);
                return index + newlineLength;
            }
            if (cells == width) {
                int hyphenBreak = findHyphenBreak(text, rules, start, index, width);
                if (hyphenBreak != NO_BREAK) {
                    boolean addsHyphen = text.charAt(hyphenBreak - 1) != EXISTING_HYPHEN;
                    appendLine(start, hyphenBreak, startColumn, endColumn, addsHyphen);
                    return hyphenBreak;
                }
                int breakIndex = chooseBreakIndex(
                        rules.getBreakMode(), text, index, lastSpaceBreak);
                appendLine(start, trimTrailingSpaces(text, start, breakIndex), startColumn,
                        endColumn);
                return skipDroppedSpaces(text, breakIndex);
            }
            if (text.charAt(index) != ' ') {
                seenGlyph = true;
            } else if (seenGlyph && text.charAt(index - 1) != ' ') {
                lastSpaceBreak = index;
            }
            index += Character.charCount(Character.codePointAt(text, index));
            cells++;
        }
        appendLine(start, textLength, startColumn, endColumn);
        return textLength;
    }

    /**
     * Picks the char index where an overflowing line ends. A new break mode adds one case here.
     */
    private static int chooseBreakIndex(BreakMode mode, CharSequence text, int overflowIndex,
            int lastSpaceBreak) {
        return switch (mode) {
            case WORD, HYPHENATE -> text.charAt(overflowIndex) == ' ' || lastSpaceBreak < 0
                    ? overflowIndex : lastSpaceBreak;
            case CHARACTER -> overflowIndex;
        };
    }

    /**
     * Returns the char index where the overflowing word is split, or {@link #NO_BREAK} when the
     * mode is not hyphenating or no allowed point fits the line.
     */
    private int findHyphenBreak(CharSequence text, WrapRules rules, int lineStart,
            int overflowIndex, int width) {
        if (rules.getBreakMode() != BreakMode.HYPHENATE || width < MIN_HYPHENATED_WIDTH
                || text.charAt(overflowIndex) == ' ') {
            return NO_BREAK;
        }
        int wordStart = findWordStart(text, overflowIndex);
        int wordEnd = findWordEnd(text, overflowIndex);
        Hyphenator hyphenator = rules.getHyphenator() != null
                ? rules.getHyphenator() : Hyphenator.englishUs();
        int bestBreak = NO_BREAK;
        int runStart = wordStart;
        while (runStart < wordEnd) {
            int runEnd = findRunEnd(text, runStart, wordEnd);
            bestBreak = lastFittingHyphenatorBreak(
                    text, hyphenator, runStart, runEnd, lineStart, width, bestBreak);
            boolean hyphenInsideWord = runEnd > runStart && runEnd + 1 < wordEnd;
            if (hyphenInsideWord && fits(text, lineStart, runEnd + 1, 0, width)) {
                bestBreak = runEnd + 1;
            }
            runStart = runEnd + 1;
        }
        return bestBreak;
    }

    /** Returns the start of the word that contains {@code index}, ignoring the line start. */
    private static int findWordStart(CharSequence text, int index) {
        int wordStart = index;
        while (wordStart > 0 && !isWordBoundary(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        return wordStart;
    }

    private int findWordEnd(CharSequence text, int index) {
        int wordEnd = index;
        while (wordEnd < textLength && !isWordBoundary(text.charAt(wordEnd))
                && newlineLengthAt(text, wordEnd) == 0) {
            wordEnd++;
        }
        return wordEnd;
    }

    private static boolean isWordBoundary(char character) {
        return character == ' ' || character == '\n';
    }

    private static int findRunEnd(CharSequence text, int runStart, int wordEnd) {
        int runEnd = runStart;
        while (runEnd < wordEnd && text.charAt(runEnd) != EXISTING_HYPHEN) {
            runEnd++;
        }
        return runEnd;
    }

    /**
     * Returns the last hyphenator break of the letters inside the run that fits the line, or
     * {@code best} when none does. Punctuation around the letters stays out of the request.
     */
    private int lastFittingHyphenatorBreak(CharSequence text, Hyphenator hyphenator,
            int runStart, int runEnd, int lineStart, int width, int best) {
        int coreStart = skipNonLettersForward(text, runStart, runEnd);
        int coreEnd = skipNonLettersBackward(text, coreStart, runEnd);
        if (coreStart >= coreEnd) {
            return best;
        }
        int count = hyphenator.findBreaks(text, coreStart, coreEnd, breakOffsets);
        if (count > breakOffsets.length) {
            breakOffsets = new int[count];
            hyphenator.findBreaks(text, coreStart, coreEnd, breakOffsets);
        }
        int lastFitting = best;
        for (int i = 0; i < count; i++) {
            if (fits(text, lineStart, breakOffsets[i], 1, width)) {
                lastFitting = breakOffsets[i];
            }
        }
        return lastFitting;
    }

    private static int skipNonLettersForward(CharSequence text, int from, int end) {
        int index = from;
        while (index < end && !Character.isLetter(Character.codePointAt(text, index))) {
            index += Character.charCount(Character.codePointAt(text, index));
        }
        return index;
    }

    private static int skipNonLettersBackward(CharSequence text, int start, int from) {
        int index = from;
        while (index > start && !Character.isLetter(Character.codePointBefore(text, index))) {
            index -= Character.charCount(Character.codePointBefore(text, index));
        }
        return index;
    }

    /** Tells whether the line content up to {@code breakIndex} plus the hyphen cells fits. */
    private static boolean fits(CharSequence text, int lineStart, int breakIndex, int hyphenCells,
            int width) {
        return breakIndex > lineStart
                && Character.codePointCount(text, lineStart, breakIndex) + hyphenCells <= width;
    }

    private int newlineLengthAt(CharSequence text, int index) {
        char current = text.charAt(index);
        if (current == '\n') {
            return 1;
        }
        boolean isCarriageReturnLineFeed = current == '\r' && index + 1 < textLength
                && text.charAt(index + 1) == '\n';
        return isCarriageReturnLineFeed ? 2 : 0;
    }

    private int skipDroppedSpaces(CharSequence text, int from) {
        int index = from;
        while (index < textLength && text.charAt(index) == ' ') {
            index++;
        }
        return index < textLength ? index + newlineLengthAt(text, index) : index;
    }

    private static int trimTrailingSpaces(CharSequence text, int start, int end) {
        int trimmedEnd = end;
        while (trimmedEnd > start && text.charAt(trimmedEnd - 1) == ' ') {
            trimmedEnd--;
        }
        return trimmedEnd;
    }

    private void truncateWhenTextRemains(CharSequence text, int position) {
        if (!hasGlyphFrom(text, position)) {
            return;
        }
        int last = lineCount - 1;
        int maxCells = widthOf(lineStartColumns[last], lineEndColumns[last]) - 1;
        int index = lineStarts[last];
        int cells = 0;
        while (index < lineEnds[last] && cells < maxCells) {
            index += Character.charCount(Character.codePointAt(text, index));
            cells++;
        }
        lineEnds[last] = trimTrailingSpaces(text, lineStarts[last], index);
        lineHyphenated[last] = false;
        truncated = true;
    }

    private boolean hasGlyphFrom(CharSequence text, int from) {
        for (int index = from; index < textLength; index++) {
            if (text.charAt(index) != ' ' && newlineLengthAt(text, index) == 0) {
                return true;
            }
        }
        return false;
    }

    private static int widthOf(int startColumn, int endColumn) {
        return (int) ((long) endColumn - startColumn + 1);
    }

    private void appendLine(int start, int end, int startColumn, int endColumn) {
        appendLine(start, end, startColumn, endColumn, false);
    }

    private void appendLine(int start, int end, int startColumn, int endColumn,
            boolean hyphenated) {
        if (lineCount == lineStarts.length) {
            int newCapacity = lineCount * 2;
            lineStarts = Arrays.copyOf(lineStarts, newCapacity);
            lineEnds = Arrays.copyOf(lineEnds, newCapacity);
            lineStartColumns = Arrays.copyOf(lineStartColumns, newCapacity);
            lineEndColumns = Arrays.copyOf(lineEndColumns, newCapacity);
            lineHyphenated = Arrays.copyOf(lineHyphenated, newCapacity);
        }
        lineHyphenated[lineCount] = hyphenated;
        lineStarts[lineCount] = start;
        lineEnds[lineCount] = end;
        lineStartColumns[lineCount] = startColumn;
        lineEndColumns[lineCount] = endColumn;
        lineCount++;
    }
}
