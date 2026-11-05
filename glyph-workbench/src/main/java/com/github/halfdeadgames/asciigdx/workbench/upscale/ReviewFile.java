package com.github.halfdeadgames.asciigdx.workbench.upscale;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * The answers of an upscale review, one line per glyph: {@code U+XXXX<TAB>pick<TAB>note}. The pick is
 * a method id such as {@code nearest}, with {@code @size} when several source sizes were offered
 * (for example {@code mmpx@16}), or {@code none}. In the note, backslash, tab, and newline are
 * written as {@code \\}, {@code \t}, and {@code \n}. Lines are sorted by code point.
 */
public final class ReviewFile {

    static final String NONE = "none";

    /** One answer; the note may be empty. */
    public record Answer(int codePoint, String pick, String note) {

        public Answer {
            if (pick.isEmpty() || pick.indexOf('\t') >= 0 || pick.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Bad pick: '" + pick + "'");
            }
        }

        Answer withNote(String newNote) {
            return new Answer(codePoint, pick, newNote);
        }
    }

    private final Map<Integer, Answer> answers = new TreeMap<>();

    /** Reads the file text; blank lines are skipped. Throws {@link IllegalArgumentException} on a bad line. */
    public static ReviewFile parse(String text) {
        ReviewFile file = new ReviewFile();
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            try {
                file.put(parseLine(lines[i]));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return file;
    }

    private static Answer parseLine(String line) {
        int firstTab = line.indexOf('\t');
        int secondTab = firstTab < 0 ? -1 : line.indexOf('\t', firstTab + 1);
        if (secondTab < 0) {
            throw new IllegalArgumentException("expected 'U+XXXX<TAB>pick<TAB>note'");
        }
        String code = line.substring(0, firstTab);
        if (!code.startsWith("U+")) {
            throw new IllegalArgumentException("expected a code point like U+0041 but found '" + code + "'");
        }
        int codePoint;
        try {
            codePoint = Integer.parseInt(code.substring(2), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad code point '" + code + "'", e);
        }
        return new Answer(codePoint, line.substring(firstTab + 1, secondTab), unescape(line.substring(secondTab + 1)));
    }

    /** Text with LF line endings and a final newline; empty for no answers. */
    String format() {
        StringBuilder text = new StringBuilder();
        for (Answer answer : answers.values()) {
            text.append(String.format("U+%04X", answer.codePoint())).append('\t')
                .append(answer.pick()).append('\t')
                .append(escape(answer.note())).append('\n');
        }
        return text.toString();
    }

    public Answer get(int codePoint) {
        return answers.get(codePoint);
    }

    boolean has(int codePoint) {
        return answers.containsKey(codePoint);
    }

    void put(Answer answer) {
        answers.put(answer.codePoint(), answer);
    }

    /**
     * The next slot after {@code from} (wrapping around) that is not blank and has no answer yet, or -1.
     *
     * @param codePoints the code point in each slot
     * @param blank true for slots to skip: padding, or glyphs that do not exist
     */
    int nextUnanswered(int[] codePoints, boolean[] blank, int from) {
        for (int step = 1; step <= codePoints.length; step++) {
            int slot = (from + step) % codePoints.length;
            if (!blank[slot] && !answers.containsKey(codePoints[slot])) {
                return slot;
            }
        }
        return -1;
    }

    Collection<Answer> all() {
        return answers.values();
    }

    boolean isEmpty() {
        return answers.isEmpty();
    }

    static String escape(String note) {
        StringBuilder out = new StringBuilder(note.length());
        for (int i = 0; i < note.length(); i++) {
            char c = note.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    static String unescape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 't' -> out.append('\t');
                case 'n' -> out.append('\n');
                case '\\' -> out.append('\\');
                default -> out.append('\\').append(next);
            }
        }
        return out.toString();
    }
}
