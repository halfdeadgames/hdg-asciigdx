package com.github.halfdeadgames.asciigdx.workbench.fontdata;

/** Shared cleanup of text read from font folder files. */
public final class TextFiles {

    private static final char BYTE_ORDER_MARK = 0xFEFF;

    private TextFiles() {
    }

    /**
     * Drops a leading byte-order mark, which some editors add to UTF-8 files.
     *
     * @param text the file text or one line of it, not null
     * @return the text without its first character if that was a byte-order mark, else the same
     *     text
     */
    public static String withoutByteOrderMark(String text) {
        return !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
    }
}
