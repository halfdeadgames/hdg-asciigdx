package com.github.halfdeadgames.asciigdx.workbench;

import java.util.ArrayList;
import java.util.List;

/** Splits note text into lines of at most a given number of characters. */
final class NoteText {

    private NoteText() {
    }

    /**
     * Breaks at the last space that fits and drops that space; a word longer than a line is cut at
     * the line width. Always returns at least one line, and the last line is empty when the text
     * ends right after a break.
     */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (true) {
            if (text.length() - start <= width) {
                lines.add(text.substring(start));
                return lines;
            }
            int space = text.lastIndexOf(' ', start + width);
            if (space > start) {
                lines.add(text.substring(start, space));
                start = space + 1;
            } else {
                lines.add(text.substring(start, start + width));
                start += width;
            }
        }
    }

    /** Row of the typing cursor, which sits after the last character; a full last line moves it down. */
    static int cursorRow(List<String> lines, int width) {
        int last = lines.size() - 1;
        return lines.get(last).length() >= width ? last + 1 : last;
    }

    static int cursorColumn(List<String> lines, int width) {
        int length = lines.get(lines.size() - 1).length();
        return length >= width ? 0 : length;
    }
}
