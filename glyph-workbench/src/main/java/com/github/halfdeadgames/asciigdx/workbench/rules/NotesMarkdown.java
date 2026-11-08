package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.ArrayList;
import java.util.List;

/** Sorts the lines of a notes file into headings, bullets, text, and blanks; other markdown stays literal. */
public final class NotesMarkdown {

    /** What a line of notes is. */
    public enum Kind { HEADING, BULLET, TEXT, BLANK }

    /**
     * One line of notes.
     *
     * @param kind what the line is
     * @param text the line without its heading or bullet marker, empty for a blank
     */
    public record NoteLine(Kind kind, String text) {}

    private NotesMarkdown() {
    }

    /**
     * Splits markdown into lines. Runs of blank lines become one, and blanks at the start or end are dropped.
     *
     * @param markdown the notes text, with any line endings
     * @return the lines, empty for empty input
     */
    public static List<NoteLine> parse(String markdown) {
        var lines = new ArrayList<NoteLine>();
        for (String rawLine : markdown.split("\r?\n", -1)) {
            NoteLine line = parseLine(rawLine);
            if (line.kind() == Kind.BLANK && !canAddBlank(lines)) {
                continue;
            }
            lines.add(line);
        }
        dropTrailingBlank(lines);
        return lines;
    }

    private static boolean canAddBlank(List<NoteLine> lines) {
        return !lines.isEmpty() && lines.get(lines.size() - 1).kind() != Kind.BLANK;
    }

    private static void dropTrailingBlank(List<NoteLine> lines) {
        if (!lines.isEmpty() && lines.get(lines.size() - 1).kind() == Kind.BLANK) {
            lines.remove(lines.size() - 1);
        }
    }

    private static NoteLine parseLine(String rawLine) {
        String trimmedEnd = rawLine.stripTrailing();
        if (trimmedEnd.isEmpty()) {
            return new NoteLine(Kind.BLANK, "");
        }
        int markerCount = leadingHashCount(trimmedEnd);
        if (markerCount > 0 && trimmedEnd.startsWith(" ", markerCount)) {
            return new NoteLine(Kind.HEADING, trimmedEnd.substring(markerCount + 1));
        }
        String withoutIndent = trimmedEnd.stripLeading();
        if (withoutIndent.startsWith("- ") || withoutIndent.startsWith("* ")) {
            return new NoteLine(Kind.BULLET, withoutIndent.substring(2));
        }
        return new NoteLine(Kind.TEXT, trimmedEnd);
    }

    private static int leadingHashCount(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == '#') {
            count++;
        }
        return count;
    }
}
