package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads font data text: a {@code cell WxH} line, then glyphs separated by blank lines. See
 * {@code fonts/} for examples. Every problem is reported as a {@link FontDataException} with the
 * line it was found on.
 */
public final class FontDataParser {

    private static final Pattern CELL = Pattern.compile("cell (\\d+)x(\\d+)");
    private static final Pattern HEX = Pattern.compile("[0-9A-Fa-f]{1,6}");
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    private final String[] lines;
    private final Map<Integer, GlyphEntry> entries = new TreeMap<>();
    private final Map<Integer, Integer> linesByCodePoint = new HashMap<>();
    private int cellWidth;
    private int cellHeight;
    private int bodyWidth;
    private int next;

    private FontDataParser(String text) {
        String withoutMark = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
        lines = withoutMark.split("\r?\n", -1);
    }

    /**
     * Parses font data text; LF and CRLF line endings are both accepted.
     *
     * @param text the file contents
     * @return the font
     * @throws FontDataException if the text is invalid
     */
    public static FontData parse(String text) {
        return new FontDataParser(text).parseAll();
    }

    private FontData parseAll() {
        readCell();
        while (skipToContent()) {
            readEntry();
        }
        FontData data = new FontData(cellWidth, cellHeight, entries);
        GlyphResolver.resolveAll(data, codePoint -> linesByCodePoint.getOrDefault(codePoint, 0));
        return data;
    }

    private void readCell() {
        if (!skipToContent()) {
            throw new FontDataException(0, "the file has no 'cell WxH' line");
        }
        Matcher matcher = CELL.matcher(lines[next].strip());
        if (!matcher.matches()) {
            throw error(next, "the first line must be 'cell WxH', for example 'cell 8x8'");
        }
        try {
            cellWidth = Integer.parseInt(matcher.group(1));
            cellHeight = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            throw error(next, "cell size is too large");
        }
        if (cellWidth < 8 || cellHeight < 1) {
            throw error(next, "cell must be at least 8 wide and 1 high");
        }
        bodyWidth = cellWidth - cellWidth / 8;
        next++;
    }

    // Moves to the next line that is neither blank nor a comment.
    private boolean skipToContent() {
        while (next < lines.length) {
            String line = lines[next];
            if (!line.isBlank() && !line.equals("#") && !line.startsWith("# ")) {
                return true;
            }
            next++;
        }
        return false;
    }

    private void readEntry() {
        int headerIndex = next;
        String line = lines[headerIndex];
        int codePointEnd = line.indexOf(' ');
        if (!line.startsWith("U+") || codePointEnd < 0) {
            throw error(headerIndex, "expected a glyph header like 'U+0041 A' but found: " + line);
        }
        int codePoint = parseCodePoint(line.substring(0, codePointEnd), headerIndex);
        int characterEnd = readCharacter(line, codePointEnd + 1, codePoint, headerIndex);
        if (entries.containsKey(codePoint)) {
            throw error(headerIndex, label(codePoint) + " is defined twice (first on line "
                + linesByCodePoint.get(codePoint) + ")");
        }
        String rest = line.substring(characterEnd).strip();
        GlyphEntry entry;
        if (rest.isEmpty() || rest.equals("full")) {
            entry = readPixelGlyph(codePoint, rest.equals("full"), headerIndex);
        } else if (rest.startsWith("=")) {
            entry = new DerivedGlyph(codePoint, parseOp(rest.substring(1).strip(), headerIndex));
            next++;
        } else {
            throw error(headerIndex, "after the character expected 'full' or '= <operation>' but found: " + rest);
        }
        entries.put(codePoint, entry);
        linesByCodePoint.put(codePoint, headerIndex + 1);
    }

    // Returns the index just past the character, after checking it matches the code point.
    private int readCharacter(String line, int start, int codePoint, int lineIndex) {
        if (start >= line.length()) {
            throw error(lineIndex, "missing character after " + label(codePoint));
        }
        int end;
        if (line.startsWith("\\u{", start)) {
            int close = line.indexOf('}', start);
            if (close < 0) {
                throw error(lineIndex, "unterminated escape in the character");
            }
            end = close + 1;
        } else if (line.startsWith("\\\\", start)) {
            end = start + 2;
        } else {
            end = start + Character.charCount(line.codePointAt(start));
        }
        String token = line.substring(start, end);
        int actual;
        try {
            actual = token.startsWith("\\") && token.length() > 1
                ? decodeEscape(token)
                : token.codePointAt(0);
        } catch (IllegalArgumentException e) {
            throw error(lineIndex, "bad character escape: " + e.getMessage());
        }
        if (actual != codePoint) {
            throw error(lineIndex, "the character is " + label(actual) + " but the header says " + label(codePoint));
        }
        return end;
    }

    private static int decodeEscape(String token) {
        GlyphOrder decoded = GlyphOrderText.readAnyRowLength(token);
        if (decoded.size() != 1) {
            throw new IllegalArgumentException("expected a single character");
        }
        return decoded.codePointAt(0);
    }

    private GlyphEntry readPixelGlyph(int codePoint, boolean full, int headerIndex) {
        int width = full ? cellWidth : bodyWidth;
        List<String> rows = new ArrayList<>();
        for (int row = 1; row <= cellHeight; row++) {
            int rowIndex = headerIndex + row;
            if (rowIndex >= lines.length || lines[rowIndex].isBlank()) {
                throw error(Math.min(rowIndex, lines.length - 1), label(codePoint) + " needs " + cellHeight
                    + " rows but has " + (row - 1));
            }
            rows.add(checkedRow(lines[rowIndex], width, rowIndex));
        }
        next = headerIndex + cellHeight + 1;
        if (next < lines.length && !lines[next].isBlank()) {
            throw error(next, label(codePoint) + " has more than " + cellHeight
                + " rows (separate glyphs with a blank line)");
        }
        return new PixelGlyph(codePoint, full, PixelGrid.ofRows(rows));
    }

    private String checkedRow(String row, int width, int rowIndex) {
        if (row.length() != width) {
            throw error(rowIndex, "row is " + row.length() + " wide but must be " + width
                + (width == bodyWidth ? " (cell width minus the letter gap)" : " (full cell width)"));
        }
        for (int column = 0; column < row.length(); column++) {
            char pixel = row.charAt(column);
            if (pixel != '#' && pixel != '.') {
                throw error(rowIndex, "row has '" + pixel + "' at column " + (column + 1) + "; use '#' and '.'");
            }
        }
        return row;
    }

    private GlyphOp parseOp(String text, int lineIndex) {
        String[] words = text.split("\\s+");
        GlyphOp.Kind kind = null;
        for (GlyphOp.Kind candidate : GlyphOp.Kind.values()) {
            if (candidate.keyword().equals(words[0])) {
                kind = candidate;
            }
        }
        if (kind == null) {
            throw error(lineIndex, "unknown operation '" + words[0] + "'");
        }
        if (kind == GlyphOp.Kind.OVERLAY) {
            return new GlyphOp(kind, parseOverlaySources(text.substring(words[0].length()), lineIndex));
        }
        if (words.length != 2) {
            throw error(lineIndex, kind.keyword() + " takes one code point, like '" + kind.keyword() + " U+0041'");
        }
        return new GlyphOp(kind, List.of(new GlyphOp.Source(parseCodePoint(words[1], lineIndex), 0)));
    }

    private List<GlyphOp.Source> parseOverlaySources(String text, int lineIndex) {
        List<GlyphOp.Source> sources = new ArrayList<>();
        for (String part : text.split(",", -1)) {
            String[] words = part.strip().split("\\s+");
            if (words.length != 3 || !words[1].equals("shift")) {
                throw error(lineIndex, "overlay parts look like 'U+0041 shift 1' but found: '" + part.strip() + "'");
            }
            try {
                sources.add(new GlyphOp.Source(parseCodePoint(words[0], lineIndex), Integer.parseInt(words[2])));
            } catch (NumberFormatException e) {
                throw error(lineIndex, "shift must be a whole number but was '" + words[2] + "'");
            }
        }
        return sources;
    }

    private int parseCodePoint(String text, int lineIndex) {
        if (!text.startsWith("U+") || !HEX.matcher(text.substring(2)).matches()) {
            throw error(lineIndex, "expected a code point like U+0041 but found '" + text + "'");
        }
        int codePoint = Integer.parseInt(text.substring(2), 16);
        boolean unusable = !Character.isValidCodePoint(codePoint)
            || Character.getType(codePoint) == Character.SURROGATE
            || codePoint == SheetOrder.FILLER;
        if (unusable) {
            throw error(lineIndex, label(codePoint) + " cannot be used as a glyph");
        }
        return codePoint;
    }

    private static String label(int codePoint) {
        return String.format("U+%04X", codePoint);
    }

    private static FontDataException error(int lineIndex, String problem) {
        return new FontDataException(lineIndex + 1, problem);
    }
}
