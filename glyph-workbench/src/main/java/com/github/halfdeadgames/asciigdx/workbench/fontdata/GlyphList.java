package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String BYTE_ORDER_MARK = "﻿";
    private static final String CP437_KEYWORD = "cp437";
    private static final Pattern ENTRY = Pattern.compile(
        "U\\+([0-9a-f]{1,8})(?:\\.\\.U\\+([0-9a-f]{1,8}))?", Pattern.CASE_INSENSITIVE);

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
        String withoutMark = text.startsWith(BYTE_ORDER_MARK) ? text.substring(1) : text;
        String[] lines = withoutMark.split("\\R", -1);
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
        Matcher matcher = ENTRY.matcher(line);
        if (!matcher.matches()) {
            throw new FontDataException(lineNumber, "expected cp437, a code point like U+2588, or a range like "
                + "U+2190..U+2193 but found '" + line + "'");
        }
        long first = Long.parseLong(matcher.group(1), 16);
        long last = matcher.group(2) == null ? first : Long.parseLong(matcher.group(2), 16);
        requireInUnicode(first, lineNumber);
        requireInUnicode(last, lineNumber);
        if (last < first) {
            throw new FontDataException(lineNumber, "range runs backwards; write the lower code point first, "
                + "like U+2190..U+2193");
        }
        if (last - first + 1 > MAX_PLANNED) {
            throw tooMany(lineNumber);
        }
        for (int codePoint = (int) first; codePoint <= last; codePoint++) {
            requireUsable(codePoint, lineNumber);
            planned.add(codePoint);
            if (planned.size() > MAX_PLANNED) {
                throw tooMany(lineNumber);
            }
        }
    }

    private static void requireInUnicode(long codePoint, int lineNumber) {
        if (codePoint > Character.MAX_CODE_POINT) {
            throw new FontDataException(lineNumber, String.format("U+%X is not a valid code point", codePoint));
        }
    }

    private static void requireUsable(int codePoint, int lineNumber) {
        boolean unusable = Character.getType(codePoint) == Character.SURROGATE || codePoint == SheetOrder.FILLER;
        if (unusable) {
            throw new FontDataException(lineNumber, String.format("U+%04X cannot be used as a glyph", codePoint));
        }
    }

    private static FontDataException tooMany(int lineNumber) {
        return new FontDataException(lineNumber, "the list names more than " + MAX_PLANNED + " code points");
    }
}
