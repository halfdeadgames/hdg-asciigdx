package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.List;

/** Text helpers shared by the dialog prompts: lines are {@link #LINE_CELLS} cells wide. */
public final class DialogText {

    /** Width in cells of every dialog text line. */
    public static final int LINE_CELLS = 26;

    private static final String ELLIPSIS = "..";
    private static final char UNDRAWABLE = '?';

    private DialogText() {
    }

    /**
     * Splits a message over two lines of {@link #LINE_CELLS}, at a space when there is one; the
     * rest is cut.
     */
    public static List<String> wrapToTwoLines(String message) {
        if (TextCells.cellCount(message) <= LINE_CELLS) {
            return List.of(message, "");
        }
        int lineEnd = TextCells.indexAfterCells(message, 0, LINE_CELLS);
        int split = message.lastIndexOf(' ', lineEnd);
        if (split <= 0) {
            split = lineEnd;
        }
        return List.of(message.substring(0, split).stripTrailing(),
                cut(message.substring(split).strip()));
    }

    /** Cuts a value to at most {@link #LINE_CELLS} cells. */
    public static String cut(String value) {
        return TextCells.prefix(value, LINE_CELLS);
    }

    /** Cuts a line longer than {@link #LINE_CELLS} cells and ends it with "..". */
    public static String ellipsize(String line) {
        if (TextCells.cellCount(line) <= LINE_CELLS) {
            return line;
        }
        return TextCells.prefix(line, LINE_CELLS - ELLIPSIS.length()) + ELLIPSIS;
    }

    /**
     * The typed text followed by a cursor, padded with spaces to {@code width}. Long text scrolls
     * so the cursor stays visible (the last {@code width - 1} cells are kept). The UI font may lack
     * a typed character, so only plain BMP characters are drawn; the rest show as '?'.
     */
    public static String typedLine(CharSequence text, char cursor, int width) {
        StringBuilder shown = new StringBuilder(text.length() + 1);
        String typed = text.toString();
        for (int i = 0; i < typed.length(); ) {
            int codePoint = typed.codePointAt(i);
            i += Character.charCount(codePoint);
            boolean drawable =
                    Character.isBmpCodePoint(codePoint) && !Character.isSurrogate((char) codePoint);
            shown.append(drawable ? (char) codePoint : UNDRAWABLE);
        }
        if (shown.length() > width - 1) {
            shown.delete(0, shown.length() - (width - 1));
        }
        shown.append(cursor);
        while (shown.length() < width) {
            shown.append(' ');
        }
        return shown.toString();
    }
}
