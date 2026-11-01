package com.github.halfdeadgames.asciigdx.sandbox.review;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks a fail, needs-work, or perfect review of an ordered list of glyphs and reads and writes
 * the results as tab-separated text. Holds no rendering or file types, so it can be tested
 * headlessly.
 *
 * <p>Each result line is {@code U+XXXX}, the escaped character, {@code fail},
 * {@code needs-work}, or {@code perfect}, and for a fail or needs-work with a reason, the reason
 * number.
 */
public final class ReviewSession {

    /** The outcome of reviewing one glyph. */
    public enum Verdict {
        FAIL("fail"),
        NEEDS_WORK("needs-work"),
        PERFECT("perfect");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    /** Why a glyph failed or needs work. */
    public enum FailReason {
        UNREADABLE_AT_1X(1),
        OFF_CENTER(2),
        TOO_SIMILAR(3),
        UGLY_OR_WRONG_SHAPE(4),
        WRONG_ORIENTATION(5);

        private final int number;

        FailReason(int number) {
            this.number = number;
        }

        /**
         * Returns the number used in the results text and on the keyboard.
         *
         * @return the reason number
         */
        public int number() {
            return number;
        }

        /**
         * Finds a reason by number.
         *
         * @param number the reason number
         * @return the reason, or empty when the number is unknown
         */
        public static Optional<FailReason> fromNumber(int number) {
            for (FailReason reason : values()) {
                if (reason.number == number) {
                    return Optional.of(reason);
                }
            }
            return Optional.empty();
        }
    }

    private record Answer(int codePoint, Verdict verdict, FailReason reason) {
    }

    private static final String LINE_SPLIT = "\r?\n";
    private static final String CODE_POINT_PREFIX = "U+";

    private final List<Integer> codePoints;
    private final List<Answer> answers = new ArrayList<>();
    private final Set<Integer> answered = new HashSet<>();

    /**
     * Starts a review. A code point listed more than once is reviewed once, at its first position.
     *
     * @param codePoints the glyphs to review, in order
     */
    public ReviewSession(int[] codePoints) {
        this(codePoints, "");
    }

    /**
     * Starts a review and applies earlier results. Result lines for code points that are not in
     * the list are ignored, and so are repeated lines for the same code point after the first.
     *
     * @param codePoints the glyphs to review, in order
     * @param resultsText earlier results text
     * @throws IllegalArgumentException if a results line is malformed
     */
    public ReviewSession(int[] codePoints, String resultsText) {
        Objects.requireNonNull(codePoints, "codePoints must not be null");
        Objects.requireNonNull(resultsText, "resultsText must not be null");
        Set<Integer> unique = new LinkedHashSet<>();
        for (int codePoint : codePoints) {
            unique.add(codePoint);
        }
        this.codePoints = new ArrayList<>(unique);
        applyResults(resultsText);
    }

    /**
     * Returns the first glyph without an answer.
     *
     * @return the code point, or empty when the review is finished
     */
    public OptionalInt current() {
        for (int codePoint : codePoints) {
            if (!answered.contains(codePoint)) {
                return OptionalInt.of(codePoint);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Returns how many glyphs have an answer.
     *
     * @return the answer count
     */
    public int answeredCount() {
        return answers.size();
    }

    /**
     * Returns how many glyphs are under review.
     *
     * @return the glyph count, without duplicates
     */
    public int totalCount() {
        return codePoints.size();
    }

    /**
     * Tells whether every glyph has an answer.
     *
     * @return true when finished
     */
    public boolean isFinished() {
        return answers.size() == codePoints.size();
    }

    /**
     * Marks the current glyph as failed, with no reason. Does nothing when finished.
     */
    public void fail() {
        record(Verdict.FAIL);
    }

    /**
     * Marks the current glyph as needing work, with no reason. Does nothing when finished.
     */
    public void needsWork() {
        record(Verdict.NEEDS_WORK);
    }

    /**
     * Marks the current glyph as perfect. Does nothing when finished.
     */
    public void perfect() {
        record(Verdict.PERFECT);
    }

    /**
     * Attaches a reason to the latest answer when that answer is a fail or needs-work.
     *
     * @param reason the reason
     */
    public void setReasonForLast(FailReason reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        if (answers.isEmpty()) {
            return;
        }
        int last = answers.size() - 1;
        Answer latest = answers.get(last);
        if (latest.verdict() != Verdict.PERFECT) {
            answers.set(last, new Answer(latest.codePoint(), latest.verdict(), reason));
        }
    }

    /**
     * Returns the verdict of the latest answer.
     *
     * @return the verdict, or empty when there are no answers
     */
    public Optional<Verdict> lastVerdict() {
        return answers.isEmpty()
            ? Optional.empty()
            : Optional.of(answers.get(answers.size() - 1).verdict());
    }

    /**
     * Returns the code point of the latest answer.
     *
     * @return the code point, or empty when there are no answers
     */
    public OptionalInt lastCodePoint() {
        return answers.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(answers.get(answers.size() - 1).codePoint());
    }

    /**
     * Returns the reason of the latest answer.
     *
     * @return the reason, or empty when there are no answers or the latest has no reason
     */
    public Optional<FailReason> lastReason() {
        return answers.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(answers.get(answers.size() - 1).reason());
    }

    /**
     * Counts the answers with a verdict.
     *
     * @param verdict the verdict to count
     * @return the number of answers with that verdict
     */
    public int count(Verdict verdict) {
        int total = 0;
        for (Answer answer : answers) {
            if (answer.verdict() == verdict) {
                total++;
            }
        }
        return total;
    }

    /**
     * Removes the latest answer. Does nothing when there are no answers.
     */
    public void undo() {
        if (answers.isEmpty()) {
            return;
        }
        Answer removed = answers.remove(answers.size() - 1);
        answered.remove(removed.codePoint());
    }

    /**
     * Writes the answers in answer order.
     *
     * @return the results text, one line per answer, each ending in a line break
     */
    public String toResultsText() {
        StringBuilder text = new StringBuilder();
        for (Answer answer : answers) {
            text.append(String.format(Locale.ROOT, "U+%04X", answer.codePoint()))
                .append('\t').append(GlyphOrderText.escapeCodePoint(answer.codePoint()))
                .append('\t').append(answer.verdict().label());
            if (answer.reason() != null) {
                text.append('\t').append(answer.reason().number());
            }
            text.append('\n');
        }
        return text.toString();
    }

    private void record(Verdict verdict) {
        OptionalInt target = current();
        if (target.isPresent()) {
            answers.add(new Answer(target.getAsInt(), verdict, null));
            answered.add(target.getAsInt());
        }
    }

    private void applyResults(String resultsText) {
        String[] lines = resultsText.split(LINE_SPLIT, -1);
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            Answer answer = parseLine(lines[index], index + 1);
            if (codePoints.contains(answer.codePoint()) && answered.add(answer.codePoint())) {
                answers.add(answer);
            }
        }
    }

    private static Answer parseLine(String line, int lineNumber) {
        String[] fields = line.split("\t", -1);
        if (fields.length < 3 || fields.length > 4 || !fields[0].startsWith(CODE_POINT_PREFIX)) {
            throw malformed(lineNumber, "expected U+XXXX, character, fail or needs-work or perfect, optional reason");
        }
        int codePoint = parseCodePoint(fields[0].substring(CODE_POINT_PREFIX.length()), lineNumber);
        Verdict verdict = parseVerdict(fields[2], lineNumber);
        FailReason reason = fields.length == 4 ? parseReason(fields[3], verdict, lineNumber) : null;
        return new Answer(codePoint, verdict, reason);
    }

    private static int parseCodePoint(String hex, int lineNumber) {
        try {
            int codePoint = Integer.parseInt(hex, 16);
            if (hex.startsWith("+") || hex.startsWith("-") || !Character.isValidCodePoint(codePoint)) {
                throw malformed(lineNumber, "invalid code point U+" + hex);
            }
            return codePoint;
        } catch (NumberFormatException e) {
            throw malformed(lineNumber, "invalid code point U+" + hex);
        }
    }

    private static Verdict parseVerdict(String field, int lineNumber) {
        for (Verdict verdict : Verdict.values()) {
            if (verdict.label().equals(field)) {
                return verdict;
            }
        }
        throw malformed(lineNumber, "unknown verdict '" + field + "'");
    }

    private static FailReason parseReason(String field, Verdict verdict, int lineNumber) {
        if (verdict == Verdict.PERFECT) {
            throw malformed(lineNumber, "a perfect line cannot have a reason");
        }
        try {
            return FailReason.fromNumber(Integer.parseInt(field))
                .orElseThrow(() -> malformed(lineNumber, "unknown reason number " + field));
        } catch (NumberFormatException e) {
            throw malformed(lineNumber, "invalid reason number '" + field + "'");
        }
    }

    private static IllegalArgumentException malformed(int lineNumber, String problem) {
        return new IllegalArgumentException("Results line " + lineNumber + ": " + problem);
    }
}
