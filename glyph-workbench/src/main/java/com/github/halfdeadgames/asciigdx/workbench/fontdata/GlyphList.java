package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The slots a font plans to have, read from its optional {@code glyphs.txt} file.
 *
 * <p>Each line is {@code cp437}, a code point like {@code U+2588}, or an inclusive range like
 * {@code U+2190..U+2193}. Blank lines are skipped.
 */
public final class GlyphList {

    /** Most code points one list may name, so a huge range fails instead of hanging the editor. */
    static final int MAX_PLANNED = 65536;

    /** No preset and nothing planned; stands in for a missing file. */
    public static final GlyphList NONE = new GlyphList(false, new TreeSet<>());

    private static final String CP437_KEYWORD = "cp437";
    private static final String RANGE_MARKER = "..";

    private final boolean cp437;
    private final Set<Integer> planned;

    private GlyphList(boolean cp437, TreeSet<Integer> planned) {
        this.cp437 = cp437;
        this.planned = Collections.unmodifiableSet(planned);
    }

    /**
     * Reads the text of a {@code glyphs.txt} file.
     *
     * @param text the file content
     * @return the parsed list
     * @throws FontDataException if a line is not valid, naming the line
     */
    public static GlyphList parse(String text) {
        Objects.requireNonNull(text, "text");
        boolean cp437 = false;
        TreeSet<Integer> planned = new TreeSet<>();
        String[] lines = TextFiles.withoutByteOrderMark(text).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase(CP437_KEYWORD)) {
                cp437 = true;
                continue;
            }
            addEntry(line, i + 1, planned);
        }
        return new GlyphList(cp437, planned);
    }

    /** Returns whether the Code Page 437 preset is listed. */
    public boolean usesCp437() {
        return cp437;
    }

    /** Returns the explicitly listed code points, sorted and unmodifiable; the cp437 preset is not expanded. */
    public Set<Integer> plannedCodePoints() {
        return planned;
    }

    private static void addEntry(String line, int lineNumber, TreeSet<Integer> planned) {
        int rangeMarker = line.indexOf(RANGE_MARKER);
        int first = CodePoints.readUPlus(rangeMarker < 0 ? line : line.substring(0, rangeMarker));
        int last = rangeMarker < 0 ? first : CodePoints.readUPlus(line.substring(rangeMarker + RANGE_MARKER.length()));
        if (first == CodePoints.NOT_A_CODE || last == CodePoints.NOT_A_CODE) {
            throw new FontDataException(lineNumber, "expected cp437, a code point like U+2588, or a range like "
                + "U+2190..U+2193 but found '" + line + "'");
        }
        requireInUnicode(first, line, lineNumber);
        requireInUnicode(last, line, lineNumber);
        if (last < first) {
            throw new FontDataException(lineNumber, "range runs backwards; write the lower code point first, "
                + "like U+2190..U+2193");
        }
        if (last - first + 1 > MAX_PLANNED) {
            throw tooMany(lineNumber);
        }
        for (int codePoint = first; codePoint <= last; codePoint++) {
            requireUsable(codePoint, lineNumber);
            planned.add(codePoint);
            if (planned.size() > MAX_PLANNED) {
                throw tooMany(lineNumber);
            }
        }
    }

    private static void requireInUnicode(int codePoint, String line, int lineNumber) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new FontDataException(lineNumber, "'" + line + "' goes past U+10FFFF, the last code point");
        }
    }

    private static void requireUsable(int codePoint, int lineNumber) {
        if (!CodePoints.isUsableAsGlyph(codePoint)) {
            throw new FontDataException(lineNumber, CodePoints.label(codePoint) + " cannot be used as a glyph");
        }
    }

    private static FontDataException tooMany(int lineNumber) {
        return new FontDataException(lineNumber, "the list names more than " + MAX_PLANNED + " code points");
    }
}
