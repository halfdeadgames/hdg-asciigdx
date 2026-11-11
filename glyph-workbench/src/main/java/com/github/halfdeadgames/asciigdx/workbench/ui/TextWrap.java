package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.ArrayList;
import java.util.List;

/** Splits text into lines of at most a given number of characters. */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * Breaks at the last space that fits and drops that space; a word longer than a line is cut at
     * the line width. Always returns at least one line, and the last line is empty when the text
     * ends right after a break. A width below 1 is treated as 1.
     */
    public static List<String> wrap(String text, int width) {
        int limit = Math.max(1, width);
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (true) {
            if (text.length() - start <= limit) {
                lines.add(text.substring(start));
                return lines;
            }
            int space = text.lastIndexOf(' ', start + limit);
            if (space > start) {
                lines.add(text.substring(start, space));
                start = space + 1;
            } else {
                lines.add(text.substring(start, start + limit));
                start += limit;
            }
        }
    }
}
