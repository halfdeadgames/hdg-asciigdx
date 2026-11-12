package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.util.ArrayList;
import java.util.List;

/** Reads the data lines of a Unicode Character Database file: "HEX[..HEX] ; value # comment". */
final class UcdEntries {

    record Entry(int first, int last, String value) {
    }

    private static final String COMMENT_MARK = "#";
    private static final char FIELD_SEPARATOR = ';';

    private UcdEntries() {
    }

    /** Returns the entries in file order. Throws IllegalArgumentException naming the first bad line. */
    static List<Entry> parse(String text) {
        List<Entry> entries = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            Entry entry = parseLine(lines[index], index + 1);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Entry parseLine(String rawLine, int lineNumber) {
        String line = withoutComment(rawLine).strip();
        if (line.isEmpty()) {
            return null;
        }
        int separator = line.indexOf(FIELD_SEPARATOR);
        if (separator < 0) {
            throw malformed(lineNumber, "expected '<range> ; <value>'");
        }
        int[] bounds = HexCodePoints.readRange(line.substring(0, separator).strip());
        if (bounds == null) {
            throw malformed(lineNumber, "bad code point range");
        }
        String value = line.substring(separator + 1).strip();
        if (value.isEmpty()) {
            throw malformed(lineNumber, "empty value");
        }
        return new Entry(bounds[0], bounds[1], value);
    }

    private static String withoutComment(String line) {
        int mark = line.indexOf(COMMENT_MARK);
        return mark < 0 ? line : line.substring(0, mark);
    }

    private static IllegalArgumentException malformed(int lineNumber, String problem) {
        return new IllegalArgumentException("UCD file line " + lineNumber + ": " + problem);
    }
}
