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
 * backslashes for a backslash. (The escape is spelled out because Java reads a backslash-u
 * sequence as a Unicode escape even inside comments.) The writer escapes control characters,
 * NBSP, private-use code points, variation selectors, the byte order mark, and a space at the
 * end of a line. Everything else is written as itself. Reading accepts escapes anywhere.
 *
 * <p>Every row must hold exactly {@code columns} slots, except the last, which may be shorter.
 * Both {@code \n} and {@code \r\n} line endings are accepted, and a leading byte order mark is
 * ignored.
 */
public final class GlyphOrderText {

    private static final char ESCAPE = '\\';
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    private GlyphOrderText() {
    }

    /**
     * Writes an order as text.
     *
     * @param order the order to write
     * @param columns slots per line, must be positive
     * @return the text, with {@code \n} line endings and a trailing line break
     * @throws IllegalArgumentException if {@code columns} is not positive
     */
    public static String write(GlyphOrder order, int columns) {
        Objects.requireNonNull(order, "order must not be null");
        requirePositiveColumns(columns);
        StringBuilder text = new StringBuilder();
        for (int slot = 0; slot < order.size(); slot++) {
            int codePoint = order.codePointAt(slot);
            boolean lastInLine = slot % columns == columns - 1 || slot == order.size() - 1;
            if (needsEscape(codePoint) || (lastInLine && codePoint == ' ')) {
                appendEscape(text, codePoint);
            } else {
                text.appendCodePoint(codePoint);
            }
            if (lastInLine) {
                text.append('\n');
            }
        }
        return text.toString();
    }

    /**
     * Reads an order from text.
     *
     * @param text the text to read
     * @param columns the number of slots every row must hold, must be positive
     * @return the order
     * @throws IllegalArgumentException if {@code columns} is not positive, an escape is malformed,
     *         a row other than the last does not hold exactly {@code columns} slots, a row is
     *         longer than {@code columns}, or the text holds no slots
     */
    public static GlyphOrder read(String text, int columns) {
        Objects.requireNonNull(text, "text must not be null");
        requirePositiveColumns(columns);
        String[] lines = stripByteOrderMark(text).split("\r?\n", -1);
        int lastLine = lines.length - 1;
        while (lastLine >= 0 && lines[lastLine].isEmpty()) {
            lastLine--;
        }
        IntStream.Builder slots = IntStream.builder();
        for (int line = 0; line <= lastLine; line++) {
            int[] row = parseLine(lines[line], line + 1);
            boolean tooShort = row.length < columns && line != lastLine;
            if (tooShort || row.length > columns) {
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
            if (hex.isEmpty() || hex.length() > 6 || !hex.chars().allMatch(GlyphOrderText::isHexDigit)) {
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
            || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
            || (codePoint >= 0xE0100 && codePoint <= 0xE01EF)
            || codePoint == 0x2028
            || codePoint == 0x2029;
    }

    private static void appendEscape(StringBuilder text, int codePoint) {
        if (codePoint == ESCAPE) {
            text.append("\\\\");
        } else {
            text.append("\\u{").append(Integer.toHexString(codePoint).toUpperCase(Locale.ROOT)).append('}');
        }
    }

    private static String stripByteOrderMark(String text) {
        return !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
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
