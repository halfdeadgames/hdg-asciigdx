package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Answers by code point, with the tab-separated results file format. */
final class AnswerStore {

    /** Fields before the note column; files from before notes existed have exactly this many. */
    private static final int FIELD_COUNT = 5;

    private final Map<Integer, GlyphAnswer> answers = new LinkedHashMap<>();

    GlyphAnswer get(int codePoint) {
        return answers.get(codePoint);
    }

    boolean has(int codePoint) {
        return answers.containsKey(codePoint);
    }

    void put(GlyphAnswer answer) {
        answers.put(answer.codePoint(), answer);
    }

    void remove(int codePoint) {
        answers.remove(codePoint);
    }

    int size() {
        return answers.size();
    }

    /** Returns the next non-blank slot after {@code from}, wrapping, whose glyph has no answer, or -1. */
    int nextUnanswered(int[] codePoints, boolean[] blank, int from) {
        int count = codePoints.length;
        for (int step = 1; step <= count; step++) {
            int slot = Math.floorMod(from + step, count);
            if (!blank[slot] && !has(codePoints[slot])) {
                return slot;
            }
        }
        return -1;
    }

    /** One line per answer in sheet order; answers for code points not on the sheet come last. */
    String format(int[] sheetOrder) {
        StringBuilder out = new StringBuilder();
        Set<Integer> written = new HashSet<>();
        for (int codePoint : sheetOrder) {
            GlyphAnswer answer = answers.get(codePoint);
            if (answer != null && written.add(codePoint)) {
                appendLine(out, answer);
            }
        }
        for (GlyphAnswer answer : answers.values()) {
            if (!written.contains(answer.codePoint())) {
                appendLine(out, answer);
            }
        }
        return out.toString();
    }

    private static void appendLine(StringBuilder out, GlyphAnswer answer) {
        out.append(String.format("U+%04X", answer.codePoint()))
            .append('\t').append(GlyphOrderText.escapeCodePoint(answer.codePoint()))
            .append('\t').append(answer.pick())
            .append('\t').append(answer.verdict())
            .append('\t').append(answer.reason())
            .append('\t').append(escapeNote(answer.note()))
            .append('\n');
    }

    static String escapeNote(String note) {
        StringBuilder out = new StringBuilder(note.length());
        for (int i = 0; i < note.length(); i++) {
            char c = note.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    static String unescapeNote(String escaped) {
        StringBuilder out = new StringBuilder(escaped.length());
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if (c != '\\' || i + 1 >= escaped.length()) {
                out.append(c);
                continue;
            }
            char next = escaped.charAt(i + 1);
            if (next == '\\') {
                out.append('\\');
                i++;
            } else if (next == 't') {
                out.append('\t');
                i++;
            } else if (next == 'n') {
                out.append('\n');
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** @throws IllegalArgumentException naming the first malformed line */
    static AnswerStore parse(String text) {
        AnswerStore store = new AnswerStore();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            if (line.isEmpty()) {
                continue;
            }
            GlyphAnswer answer = parseLine(line, i + 1);
            if (store.has(answer.codePoint())) {
                throw new IllegalArgumentException("line " + (i + 1) + ": duplicate glyph");
            }
            store.put(answer);
        }
        return store;
    }

    private static GlyphAnswer parseLine(String line, int number) {
        String[] fields = line.split("\t", -1);
        if (fields.length != FIELD_COUNT && fields.length != FIELD_COUNT + 1) {
            throw new IllegalArgumentException(
                "line " + number + ": expected " + FIELD_COUNT + " or " + (FIELD_COUNT + 1) + " fields");
        }
        if (!fields[0].startsWith("U+")) {
            throw new IllegalArgumentException("line " + number + ": bad code point");
        }
        int codePoint;
        try {
            codePoint = Integer.parseInt(fields[0].substring(2), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("line " + number + ": bad code point");
        }
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("line " + number + ": bad code point");
        }
        String pick = fields[2];
        String verdict = fields[3];
        int legacyJoin = pick.indexOf('=');
        if (legacyJoin > 0) {
            pick = pick.substring(0, legacyJoin);
        }
        if (pick.isEmpty() || !GlyphAnswer.isKnownVerdict(verdict)) {
            throw new IllegalArgumentException("line " + number + ": bad pick or verdict");
        }
        if (GlyphAnswer.LEGACY_NONE_VERDICT.equals(verdict)) {
            verdict = GlyphAnswer.NEEDS_HAND_DRAWN;
        }
        if (GlyphAnswer.NONE_PICK.equals(pick) != GlyphAnswer.NEEDS_HAND_DRAWN.equals(verdict)) {
            throw new IllegalArgumentException("line " + number + ": pick and verdict disagree about none");
        }
        String note = fields.length > FIELD_COUNT ? unescapeNote(fields[FIELD_COUNT]) : "";
        return new GlyphAnswer(codePoint, pick, verdict, fields[4], note);
    }
}
