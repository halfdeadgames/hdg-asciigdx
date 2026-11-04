package com.github.halfdeadgames.asciigdx.workbench;

import java.util.List;
import java.util.function.IntPredicate;

/** The text typed into the "new glyph" dialog, and the four lines the dialog shows for it. */
final class NewGlyphPrompt {

    static final int MAX_CHARS = 10;
    static final int LINE_CELLS = 26;
    static final String EMPTY_HINT = "Type a character or hex code";

    private static final char CURSOR = '_';
    private static final char UNDRAWABLE = '?';
    private static final String EXISTS_SUFFIX = " ALREADY IN FONT";

    private final StringBuilder text = new StringBuilder(MAX_CHARS);
    private boolean open;

    boolean isOpen() {
        return open;
    }

    /** Opens the prompt with nothing typed. */
    void open() {
        text.setLength(0);
        open = true;
    }

    void close() {
        text.setLength(0);
        open = false;
    }

    /** Appends any character that is not an ISO control while open, up to {@link #MAX_CHARS}. */
    void type(char character) {
        if (!open || Character.isISOControl(character) || text.length() >= MAX_CHARS) {
            return;
        }
        text.append(character);
    }

    /** Removes the last code point, so a surrogate pair goes in one press. */
    void backspace() {
        if (!open || text.isEmpty()) {
            return;
        }
        text.setLength(text.offsetByCodePoints(text.length(), -1));
    }

    NewGlyphInput.Result result() {
        return NewGlyphInput.parse(text.toString());
    }

    /** Tells whether Enter would jump to a slot the sheet already has instead of adding one. */
    boolean goesToExisting(IntPredicate hasSlot) {
        NewGlyphInput.Result result = result();
        return result.isValid() && hasSlot.test(result.codePoint());
    }

    /**
     * Exactly four lines: the typed text with a cursor, a blank line, then either the hint or error wrapped
     * over two lines, or the code point and the glyph name.
     *
     * @param hasSlot tells whether the sheet already has a slot for a code point
     */
    List<String> lines(IntPredicate hasSlot) {
        if (text.isEmpty()) {
            return wrapped(EMPTY_HINT);
        }
        NewGlyphInput.Result result = result();
        if (!result.isValid()) {
            return wrapped(result.error());
        }
        int codePoint = result.codePoint();
        String code = String.format("U+%04X", codePoint);
        String name = GlyphName.fullName(codePoint);
        String status = hasSlot.test(codePoint) ? code + EXISTS_SUFFIX : code;
        List<String> nameLines = twoLines(name);
        if (nameLines.get(1).isEmpty()) {
            return List.of(typedLine(), "", status, nameLines.get(0));
        }
        return List.of(typedLine(), "", status, nameLines.get(0), nameLines.get(1));
    }

    private List<String> wrapped(String message) {
        List<String> messageLines = twoLines(message);
        return List.of(typedLine(), "", messageLines.get(0), messageLines.get(1));
    }

    // Splits at the last space that fits; the second line is cut if it is still too long.
    private static List<String> twoLines(String message) {
        if (message.length() <= LINE_CELLS) {
            return List.of(message, "");
        }
        int split = message.lastIndexOf(' ', LINE_CELLS);
        if (split <= 0) {
            split = LINE_CELLS;
        }
        return List.of(message.substring(0, split).stripTrailing(), cut(message.substring(split).strip()));
    }

    private static String cut(String value) {
        return value.length() > LINE_CELLS ? value.substring(0, LINE_CELLS) : value;
    }

    // The UI font may lack the typed character, so only plain BMP characters are drawn; the rest show as '?'.
    private String typedLine() {
        StringBuilder line = new StringBuilder(LINE_CELLS);
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            boolean drawable = Character.isBmpCodePoint(codePoint) && !Character.isSurrogate((char) codePoint);
            line.append(drawable ? (char) codePoint : UNDRAWABLE);
        }
        line.append(CURSOR);
        while (line.length() < LINE_CELLS) {
            line.append(' ');
        }
        return line.toString();
    }
}
