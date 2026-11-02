package com.github.halfdeadgames.asciigdx.workbench;

/** The text being typed for a note, with the rules for which characters are accepted. */
final class NoteEditor {

    static final int MAX_LENGTH = 120;

    private final StringBuilder text = new StringBuilder();
    private int caret;
    private int scrollTop;
    private boolean active;
    private boolean editingExisting;

    /** @param existing true when editing a saved answer, false when the answer is not saved yet */
    void begin(String initial, boolean existing) {
        text.setLength(0);
        text.append(initial, 0, Math.min(initial.length(), MAX_LENGTH));
        caret = text.length();
        scrollTop = 0;
        active = true;
        editingExisting = existing;
    }

    void end() {
        active = false;
    }

    boolean isActive() {
        return active;
    }

    boolean isEditingExisting() {
        return editingExisting;
    }

    static boolean isPrintable(char c) {
        return c >= ' ' && c <= '~';
    }

    /** Inserts a printable character at the caret while there is room; returns whether it was added. */
    boolean type(char c) {
        if (!active || !isPrintable(c) || text.length() >= MAX_LENGTH) {
            return false;
        }
        text.insert(caret, c);
        caret++;
        return true;
    }

    /** Deletes the code point before the caret. */
    boolean backspace() {
        if (!active || caret == 0) {
            return false;
        }
        int start = text.offsetByCodePoints(caret, -1);
        text.delete(start, caret);
        caret = start;
        return true;
    }

    /** Deletes the code point after the caret. */
    boolean deleteForward() {
        if (!active || caret >= text.length()) {
            return false;
        }
        text.delete(caret, text.offsetByCodePoints(caret, 1));
        return true;
    }

    /** Caret position as a char index into {@link #value()}. */
    int caret() {
        return caret;
    }

    boolean caretLeft() {
        if (!active || caret == 0) {
            return false;
        }
        caret = text.offsetByCodePoints(caret, -1);
        return true;
    }

    boolean caretRight() {
        if (!active || caret >= text.length()) {
            return false;
        }
        caret = text.offsetByCodePoints(caret, 1);
        return true;
    }

    void caretToStart() {
        caret = 0;
    }

    void caretToEnd() {
        caret = text.length();
    }

    /** The text is cut into fixed-width rows, so the caret maps to a row and column without word-wrap rules. */
    int rowCount(int width) {
        return text.length() / width + 1;
    }

    String row(int index, int width) {
        int start = Math.min(text.length(), index * width);
        return text.substring(start, Math.min(text.length(), start + width));
    }

    int caretRow(int width) {
        return caret / width;
    }

    int caretColumn(int width) {
        return caret % width;
    }

    /** First row to draw: the previous window moved just far enough that the caret row is inside it. */
    int firstVisibleRow(int width, int visibleRows) {
        int caretRow = caretRow(width);
        if (caretRow < scrollTop) {
            scrollTop = caretRow;
        } else if (caretRow >= scrollTop + visibleRows) {
            scrollTop = caretRow - visibleRows + 1;
        }
        scrollTop = Math.max(0, Math.min(scrollTop, Math.max(0, rowCount(width) - visibleRows)));
        return scrollTop;
    }

    String value() {
        return text.toString();
    }
}
