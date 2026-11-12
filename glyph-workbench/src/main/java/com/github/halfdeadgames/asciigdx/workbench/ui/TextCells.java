package com.github.halfdeadgames.asciigdx.workbench.ui;

/** Measures and cuts text in display cells, where one code point is one cell and a surrogate pair is never split. */
public final class TextCells {

    private TextCells() {
    }

    /** The number of cells the text takes: its code point count. */
    public static int cellCount(CharSequence text) {
        return Character.codePointCount(text, 0, text.length());
    }

    /**
     * The char index where {@code cells} cells end, counting from {@code fromIndex}; the text length when it has
     * fewer cells left. Never lands between the two halves of a surrogate pair.
     */
    public static int indexAfterCells(CharSequence text, int fromIndex, int cells) {
        int index = fromIndex;
        int remaining = cells;
        while (remaining > 0 && index < text.length()) {
            index += Character.charCount(Character.codePointAt(text, index));
            remaining--;
        }
        return index;
    }

    /** The first {@code cells} cells of the text, or all of it when it is not longer. */
    public static String prefix(String text, int cells) {
        return text.substring(0, indexAfterCells(text, 0, cells));
    }

    /** The text without its first {@code cells} cells. */
    public static String dropCells(String text, int cells) {
        return text.substring(indexAfterCells(text, 0, cells));
    }
}
