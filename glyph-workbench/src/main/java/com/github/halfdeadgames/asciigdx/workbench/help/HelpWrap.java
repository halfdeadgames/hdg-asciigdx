package com.github.halfdeadgames.asciigdx.workbench.help;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns help rows into screen lines that all fit a character width: long actions wrap under the action
 * column and long keys wrap inside the key column. It holds no drawing or input code.
 */
final class HelpWrap {

    /** One screen line; the key sits at column 0 and the action at {@code actionColumn}. */
    record Line(PaintHelp.Kind kind, String key, String action, int actionColumn) {
    }

    private static final int MIN_ACTION_CHARS = 3;
    private static final int STACK_INDENT = 2;

    private HelpWrap() {
    }

    /** The help lines for a state at {@code chars} characters per line, narrowed by one when they overflow {@code visibleRows}. */
    static List<Line> lines(PaintHelp.State state, int chars, int visibleRows) {
        List<Line> full = layout(PaintHelp.rows(state, chars), chars);
        if (full.size() <= visibleRows || chars <= 1) {
            return full;
        }
        int narrowed = chars - 1;
        return layout(PaintHelp.rows(state, narrowed), narrowed);
    }

    static List<Line> layout(List<PaintHelp.Row> rows, int chars) {
        List<Line> out = new ArrayList<>();
        int i = 0;
        while (i < rows.size()) {
            PaintHelp.Row row = rows.get(i);
            if (!row.isTable()) {
                out.add(new Line(row.kind(), "", clip(row.action(), chars), 0));
                i++;
                continue;
            }
            int end = i;
            while (end < rows.size() && rows.get(end).isTable()) {
                end++;
            }
            layoutTable(rows.subList(i, end), chars, out);
            i = end;
        }
        return out;
    }

    // A run of table rows is laid out side by side or stacked as a whole, never mixed.
    private static void layoutTable(List<PaintHelp.Row> table, int chars, List<Line> out) {
        int tableColumn = 0;
        int longestWord = 0;
        for (PaintHelp.Row row : table) {
            tableColumn = Math.max(tableColumn, row.actionColumn());
            longestWord = Math.max(longestWord, longestWord(row.action()));
        }
        int actionColumn = Math.max(2, Math.min(tableColumn, chars - MIN_ACTION_CHARS));
        boolean stacked = chars - actionColumn < longestWord;
        for (PaintHelp.Row row : table) {
            if (stacked) {
                for (String key : breakText(row.key(), chars)) {
                    out.add(new Line(row.kind(), key, "", 0));
                }
                for (String action : breakText(row.action(), chars - STACK_INDENT)) {
                    out.add(new Line(row.kind(), "", action, STACK_INDENT));
                }
                continue;
            }
            List<String> keys = breakText(row.key(), actionColumn - 1);
            List<String> actions = breakText(row.action(), chars - actionColumn);
            int count = Math.max(keys.size(), actions.size());
            for (int i = 0; i < count; i++) {
                out.add(new Line(row.kind(), i < keys.size() ? keys.get(i) : "",
                    i < actions.size() ? actions.get(i) : "", actionColumn));
            }
        }
    }

    private static int longestWord(String text) {
        int longest = 0;
        for (String word : text.split(" ")) {
            longest = Math.max(longest, word.length());
        }
        return longest;
    }

    /** Breaks at spaces; a word longer than the width is cut. An empty text gives no lines. */
    static List<String> breakText(String text, int width) {
        int limit = Math.max(1, width);
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            if (text.length() - start <= limit) {
                out.add(text.substring(start));
                break;
            }
            int space = text.lastIndexOf(' ', start + limit);
            if (space > start) {
                out.add(text.substring(start, space));
                start = space + 1;
            } else {
                out.add(text.substring(start, start + limit));
                start += limit;
            }
        }
        return out;
    }

    private static String clip(String text, int chars) {
        return text.length() > chars ? text.substring(0, Math.max(0, chars)) : text;
    }
}
