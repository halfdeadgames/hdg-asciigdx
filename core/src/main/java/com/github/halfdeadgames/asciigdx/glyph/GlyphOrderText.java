package com.github.halfdeadgames.asciigdx.glyph;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Reads and writes a {@link GlyphOrder} as plain text: one line per sheet row, one character
 * per slot. This is the format of the {@code .txt} file that sits next to a glyph sheet image.
 *
 * <p>Characters that are hard to see or keep in a text file are written as escapes: a
 * backslash followed by {@code u{E01F}} for any code point (hex, 1 to 6 digits), and two
 * backslashes for a backslash. The writer escapes control characters, NBSP, private-use code
 * points, lone surrogates, variation selectors, the byte order mark, the line separator
 * (U+2028), the paragraph separator (U+2029), and a space at the end of a line. Everything else
 * is written as itself. Reading accepts escapes anywhere.
 *
 * <p>Every row must hold exactly {@code columns} slots, except the last, which may be shorter.
 * Both {@code \n} and {@code \r\n} line endings are accepted, and a leading byte order mark is
 * ignored. Empty lines at the end of the text are ignored; an empty line anywhere else is
 * rejected by {@link #read(String, int)} and skipped by {@link #readAnyRowLength(String)}.
 *
 * <p>All methods are static, keep no state, and are safe to call from any thread.
 */
public final class GlyphOrderText {

    private static final char ESCAPE = '\\';
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    private GlyphOrderText() {
    }

    /**
     * Writes an order as text.
     *
     * @param order the order to write; must not be null
     * @param columns slots per line, must be positive; the last line holds the remainder
     * @return the text, with {@code \n} line endings and a trailing line break
     * @throws NullPointerException if {@code order} is null
     * @throws IllegalArgumentException if {@code columns} is not positive
     */
    public static String write(GlyphOrder order, int columns) {
        Objects.requireNonNull(order, "order");
        requirePositiveColumns(columns);
        StringBuilder text = new StringBuilder();
        for (int slot = 0; slot < order.getGlyphCount(); slot++) {
            int codePoint = order.getCodePoint(slot);
            boolean lastInLine = slot % columns == columns - 1 || slot == order.getGlyphCount() - 1;
            if (lastInLine && codePoint == ' ') {
                appendEscape(text, codePoint);
            } else {
                appendCodePointOrEscape(text, codePoint);
            }
            if (lastInLine) {
                text.append('\n');
            }
        }
        return text.toString();
    }

    /**
     * Writes one code point the way {@link #write(GlyphOrder, int)} would, escaping it when it is
     * hard to see or keep in a text file. A space is returned as a plain space, because the
     * end-of-line rule needs the line context.
     *
     * @param codePoint the code point to write; must be a valid Unicode code point
     * @return the code point as text, or its escape
     * @throws IllegalArgumentException if {@code codePoint} is not a valid Unicode code point
     */
    public static String escapeCodePoint(int codePoint) {
        StringBuilder text = new StringBuilder();
        appendCodePointOrEscape(text, codePoint);
        return text.toString();
    }

    private static void appendCodePointOrEscape(StringBuilder text, int codePoint) {
        if (needsEscape(codePoint)) {
            appendEscape(text, codePoint);
        } else {
            text.appendCodePoint(codePoint);
        }
    }

    /**
     * Reads an order from text.
     *
     * @param text the text to read; must not be null
     * @param columns the number of slots every row must hold, must be positive
     * @return the order, with the slots of all rows in reading order
     * @throws NullPointerException if {@code text} is null
     * @throws IllegalArgumentException if {@code columns} is not positive, an escape is malformed,
     *         a row other than the last does not hold exactly {@code columns} slots, a row is
     *         longer than {@code columns}, or the text holds no slots
     */
    public static GlyphOrder read(String text, int columns) {
        Objects.requireNonNull(text, "text");
        requirePositiveColumns(columns);
        return readRows(text, columns);
    }

    /**
     * Reads an order from text, taking every row in order whatever its length.
     *
     * @param text the text to read; must not be null
     * @return the order, with the slots of all rows in reading order
     * @throws NullPointerException if {@code text} is null
     * @throws IllegalArgumentException if an escape is malformed or the text holds no slots
     */
    public static GlyphOrder readAnyRowLength(String text) {
        Objects.requireNonNull(text, "text");
        return readRows(text, Integer.MAX_VALUE);
    }

    private static GlyphOrder readRows(String text, int columns) {
        String[] lines = stripByteOrderMark(text).split("\r?\n", -1);
        int lastLine = lines.length - 1;
        while (lastLine >= 0 && lines[lastLine].isEmpty()) {
            lastLine--;
        }
        boolean anyLength = columns == Integer.MAX_VALUE;
        IntStream.Builder slots = IntStream.builder();
        for (int line = 0; line <= lastLine; line++) {
            int[] row = parseLine(lines[line], line + 1);
            boolean tooShort = row.length < columns && line != lastLine;
            if (!anyLength && (tooShort || row.length > columns)) {
                throw new IllegalArgumentException(
                        "Line " + (line + 1) + " has " + row.length + " glyphs but every row needs "
                                + columns + (line == lastLine ? " or fewer" : ""));
            }
            for (int codePoint : row) {
                slots.add(codePoint);
            }
        }
        int[] codePoints = slots.build().toArray();
        if (codePoints.length == 0) {
            throw new IllegalArgumentException("Text holds no glyphs");
        }
        return GlyphOrder.ofCodePoints(codePoints);
    }

    private static int[] parseLine(String line, int lineNumber) {
        IntStream.Builder row = IntStream.builder();
        int index = 0;
        while (index < line.length()) {
            int codePoint = line.codePointAt(index);
            if (codePoint != ESCAPE) {
                row.add(codePoint);
                index += Character.charCount(codePoint);
                continue;
            }
            if (line.startsWith("\\\\", index)) {
                row.add(ESCAPE);
                index += 2;
                continue;
            }
            int close = line.indexOf('}', index);
            if (!line.startsWith("\\u{", index) || close < 0) {
                throw malformedEscape(lineNumber, index);
            }
            String hex = line.substring(index + 3, close);
            if (hex.isEmpty() || hex.length() > 6 || !containsOnlyHexDigits(hex)) {
                throw malformedEscape(lineNumber, index);
            }
            int escaped = Integer.parseInt(hex, 16);
            if (!Character.isValidCodePoint(escaped)) {
                throw malformedEscape(lineNumber, index);
            }
            row.add(escaped);
            index = close + 1;
        }
        return row.build().toArray();
    }

    private static boolean needsEscape(int codePoint) {
        return codePoint == ESCAPE
                || Character.isISOControl(codePoint)
                || codePoint == 0x00A0
                || codePoint == BYTE_ORDER_MARK
                || Character.getType(codePoint) == Character.PRIVATE_USE
                || Character.getType(codePoint) == Character.SURROGATE
                || GlyphOrder.isVariationSelector(codePoint)
                || codePoint == 0x2028
                || codePoint == 0x2029;
    }

    private static void appendEscape(StringBuilder text, int codePoint) {
        if (codePoint == ESCAPE) {
            text.append("\\\\");
        } else {
            text.append("\\u{").append(Integer.toHexString(codePoint).toUpperCase(Locale.ROOT))
                    .append('}');
        }
    }

    private static String stripByteOrderMark(String text) {
        return !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
    }

    private static boolean containsOnlyHexDigits(String hex) {
        for (int charIndex = 0; charIndex < hex.length(); charIndex++) {
            if (!isHexDigit(hex.charAt(charIndex))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(int c) {
        return Character.digit(c, 16) >= 0 && c < 128;
    }

    private static IllegalArgumentException malformedEscape(int lineNumber, int index) {
        return new IllegalArgumentException(
                "Malformed escape on line " + lineNumber + " at column " + (index + 1)
                        + "; use \\u{HEX} or \\\\");
    }

    private static void requirePositiveColumns(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("Columns must be positive, was " + columns);
        }
    }
}
