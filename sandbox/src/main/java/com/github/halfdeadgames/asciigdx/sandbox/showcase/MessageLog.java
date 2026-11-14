package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** Typed lines in start order; the newest started lines are shown and older ones scroll off. */
public final class MessageLog {

    private final TypedLine[] lines;

    public MessageLog(TypedLine... lines) {
        if (lines == null || lines.length == 0) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        for (int i = 0; i < lines.length; i++) {
            requireOrderedAt(lines, i);
        }
        this.lines = lines.clone();
    }

    public int lineCount() {
        return lines.length;
    }

    public TypedLine line(int index) {
        return lines[index];
    }

    public int startedCount(float timeSeconds) {
        int started = 0;
        while (started < lines.length && lines[started].hasStarted(timeSeconds)) {
            started++;
        }
        return started;
    }

    public int firstShownLine(float timeSeconds, int rows) {
        return Math.max(0, startedCount(timeSeconds) - rows);
    }

    private static void requireOrderedAt(TypedLine[] lines, int index) {
        if (lines[index] == null) {
            throw new IllegalArgumentException("line " + index + " must not be null");
        }
        if (index > 0 && lines[index].startSeconds() < lines[index - 1].startSeconds()) {
            throw new IllegalArgumentException("line " + index + " starts before line " + (index - 1));
        }
    }
}
