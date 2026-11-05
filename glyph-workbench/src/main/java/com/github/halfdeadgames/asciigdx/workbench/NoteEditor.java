package com.github.halfdeadgames.asciigdx.workbench;

import java.util.List;

/**
 * The text being typed for a note, with the rules for which characters are accepted, the caret, and
 * the scroll position of the rows shown. Notes have no length limit.
 */
public final class NoteEditor {

    private final StringBuilder text = new StringBuilder();
    private int caret;
    private boolean active;
    private boolean editingExisting;
    private int viewTop;
    private boolean followCaret = true;

    /** Starts editing; the caret goes to the end of {@code initial}. */
    public void begin(String initial, boolean editingExistingNote) {
        text.setLength(0);
        text.append(initial);
        caret = text.length();
        editingExisting = editingExistingNote;
        viewTop = 0;
        followCaret = true;
        active = true;
    }

    public void end() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    boolean isEditingExisting() {
        return editingExisting;
    }

    public String value() {
        return text.toString();
    }

    /** Inserts a printable character at the caret; control characters are ignored. */
    public void type(char character) {
        if (!active || Character.isISOControl(character)) {
            return;
        }
        text.insert(caret, character);
        caret++;
        followCaret = true;
    }

    public void backspace() {
        if (active && caret > 0) {
            text.deleteCharAt(caret - 1);
            caret--;
            followCaret = true;
        }
    }

    public void deleteForward() {
        if (active && caret < text.length()) {
            text.deleteCharAt(caret);
            followCaret = true;
        }
    }

    public void caretLeft() {
        caret = Math.max(0, caret - 1);
        followCaret = true;
    }

    public void caretRight() {
        caret = Math.min(text.length(), caret + 1);
        followCaret = true;
    }

    public void caretToStart() {
        caret = 0;
        followCaret = true;
    }

    public void caretToEnd() {
        caret = text.length();
        followCaret = true;
    }

    /** Moves the caret by whole rows at a wrap width, keeping its column where the row is long enough. */
    public void caretRows(int rows, int width) {
        List<String> lines = NoteText.wrap(text.toString(), width);
        int[] starts = lineStarts(lines);
        int[] location = locateCaret(lines, starts, width);
        int target = Math.max(0, Math.min(Math.max(lines.size(), location[0] + 1) - 1, location[0] + rows));
        if (target >= lines.size()) {
            caret = text.length();
        } else {
            int limit = target == lines.size() - 1 ? text.length() : starts[target + 1] - 1;
            caret = Math.min(starts[target] + location[1], limit);
        }
        followCaret = true;
    }

    /** Scrolls the shown rows without moving the caret; typing or moving the caret brings it back into view. */
    public void scrollView(int rows) {
        viewTop += rows;
        followCaret = false;
    }

    /** Number of rows the text takes at a width, counting the row of a caret that wrapped past a full last line. */
    int rowCount(int width) {
        List<String> lines = NoteText.wrap(text.toString(), width);
        return Math.max(lines.size(), locateCaret(lines, lineStarts(lines), width)[0] + 1);
    }

    String row(int index, int width) {
        List<String> lines = NoteText.wrap(text.toString(), width);
        return index < lines.size() ? lines.get(index) : "";
    }

    int caretRow(int width) {
        return locate(width)[0];
    }

    int caretColumn(int width) {
        return locate(width)[1];
    }

    /**
     * First row to show in a window of {@code visibleRows} rows. It stays where it is while the caret
     * is inside the window, and never leaves the text.
     */
    int viewTop(int width, int visibleRows) {
        int rows = rowCount(width);
        if (followCaret) {
            int caretRow = caretRow(width);
            if (caretRow < viewTop) {
                viewTop = caretRow;
            } else if (caretRow >= viewTop + visibleRows) {
                viewTop = caretRow - visibleRows + 1;
            }
        }
        viewTop = Math.max(0, Math.min(Math.max(0, rows - visibleRows), viewTop));
        return viewTop;
    }

    private int[] locate(int width) {
        List<String> lines = NoteText.wrap(text.toString(), width);
        return locateCaret(lines, lineStarts(lines), width);
    }

    // The wrap drops the space it breaks at, so the offset where each row starts is walked row by row.
    private int[] lineStarts(List<String> lines) {
        int[] starts = new int[lines.size()];
        for (int i = 1; i < starts.length; i++) {
            int end = starts[i - 1] + lines.get(i - 1).length();
            starts[i] = end + (text.charAt(end) == ' ' ? 1 : 0);
        }
        return starts;
    }

    private int[] locateCaret(List<String> lines, int[] starts, int width) {
        int last = lines.size() - 1;
        for (int i = 0; i < last; i++) {
            if (caret < starts[i + 1]) {
                return new int[] {i, caret - starts[i]};
            }
        }
        int length = lines.get(last).length();
        boolean pastFullLine = length >= width && caret - starts[last] >= length;
        return pastFullLine ? new int[] {last + 1, 0} : new int[] {last, caret - starts[last]};
    }
}
