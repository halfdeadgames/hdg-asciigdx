package com.github.halfdeadgames.asciigdx.workbench;

/**
 * The text being typed for a guide label. The guide file splits on whitespace, so only printable characters
 * without whitespace are accepted; letters are uppercased, {@code #} cannot start a label, and the length is capped.
 */
final class LabelField {

    static final int MAX_LENGTH = 8;

    private static final char COMMENT_MARK = '#';

    private final StringBuilder text = new StringBuilder();
    private boolean active;

    /** Starts editing from {@code initial}, which is kept as given even when it breaks the rules. */
    void begin(String initial) {
        text.setLength(0);
        text.append(initial);
        active = true;
    }

    void end() {
        active = false;
    }

    boolean isActive() {
        return active;
    }

    String value() {
        return text.toString();
    }

    boolean isEmpty() {
        return text.length() == 0;
    }

    /** Appends the character if the rules accept it; anything else, or any input while not editing, is ignored. */
    void type(char character) {
        if (!active || text.length() >= MAX_LENGTH || !isAccepted(character)) {
            return;
        }
        if (character == COMMENT_MARK && text.length() == 0) {
            return;
        }
        text.append(Character.toUpperCase(character));
    }

    void backspace() {
        if (active && text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
    }

    private static boolean isAccepted(char character) {
        return !Character.isISOControl(character) && !Character.isWhitespace(character)
            && !Character.isSpaceChar(character) && !Character.isSurrogate(character);
    }
}
