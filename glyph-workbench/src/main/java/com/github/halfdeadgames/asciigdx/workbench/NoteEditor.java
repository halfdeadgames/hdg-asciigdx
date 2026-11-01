package com.github.halfdeadgames.asciigdx.workbench;

/** The text being typed for a note, with the rules for which characters are accepted. */
final class NoteEditor {

    static final int MAX_LENGTH = 120;

    private final StringBuilder text = new StringBuilder();
    private boolean active;
    private boolean editingExisting;

    /** @param existing true when editing a saved answer, false when the answer is not saved yet */
    void begin(String initial, boolean existing) {
        text.setLength(0);
        text.append(initial, 0, Math.min(initial.length(), MAX_LENGTH));
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

    /** Appends a printable character while there is room; returns whether it was added. */
    boolean type(char c) {
        if (!active || !isPrintable(c) || text.length() >= MAX_LENGTH) {
            return false;
        }
        text.append(c);
        return true;
    }

    boolean backspace() {
        if (!active || text.length() == 0) {
            return false;
        }
        text.setLength(text.length() - 1);
        return true;
    }

    String value() {
        return text.toString();
    }
}
