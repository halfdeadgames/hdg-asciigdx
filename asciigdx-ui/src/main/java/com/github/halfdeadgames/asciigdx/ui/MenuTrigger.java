package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;

/** Helpers for the trigger letters of menu titles and items. */
final class MenuTrigger {

    static final char NONE = '\0';
    static final int NO_CELL = -1;

    private MenuTrigger() {
    }

    /**
     * Checks a trigger against its text and returns the cell of its first case-insensitive match,
     * or {@link #NO_CELL} when the trigger is {@link #NONE}.
     */
    static int requireCellOf(String text, char trigger) {
        if (trigger == NONE) {
            return NO_CELL;
        }
        if (!isAsciiLetterOrDigit(trigger)) {
            throw new IllegalArgumentException("Trigger must be an ASCII letter or digit, was U+"
                    + Integer.toHexString(trigger));
        }
        int cell = findCell(text, trigger);
        if (cell == NO_CELL) {
            throw new IllegalArgumentException(
                    "Trigger '" + trigger + "' does not appear in \"" + text + "\"");
        }
        return cell;
    }

    private static boolean isAsciiLetterOrDigit(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9');
    }

    static boolean matches(char trigger, char key) {
        return trigger != NONE && Character.toLowerCase(trigger) == Character.toLowerCase(key);
    }

    /** Returns the character a key types on a letter, digit, or numpad digit key, else NONE. */
    static char keyToCharacter(int keycode) {
        if (keycode >= Input.Keys.A && keycode <= Input.Keys.Z) {
            return (char) ('a' + keycode - Input.Keys.A);
        }
        if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
            return (char) ('0' + keycode - Input.Keys.NUM_0);
        }
        if (keycode >= Input.Keys.NUMPAD_0 && keycode <= Input.Keys.NUMPAD_9) {
            return (char) ('0' + keycode - Input.Keys.NUMPAD_0);
        }
        return NONE;
    }

    private static int findCell(String text, char trigger) {
        char wanted = Character.toLowerCase(trigger);
        int cell = 0;
        for (int index = 0; index < text.length(); cell++) {
            int codePoint = text.codePointAt(index);
            if (Character.toLowerCase(codePoint) == wanted) {
                return cell;
            }
            index += Character.charCount(codePoint);
        }
        return NO_CELL;
    }
}
