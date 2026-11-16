package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.GlyphName;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogText;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The text typed into the "new glyph" dialog, and the four or five lines the dialog shows for it.
 */
final class NewGlyphPrompt {

    private static final int MAX_CHARS = 10;
    static final int LINE_CELLS = DialogText.LINE_CELLS;
    private static final String EMPTY_HINT = "TYPE A HEX CODE, E.G. 263A OR U+1F5E1";

    private static final char CURSOR = '_';
    private static final String EXISTS_SUFFIX = " ALREADY IN FONT";

    private final StringBuilder text = new StringBuilder(MAX_CHARS);
    private boolean open;
    private int cachedNameCodePoint = -1;
    private String cachedName = "";

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

    /**
     * Appends a hex digit, {@code U}, {@code u}, or {@code +} while open, up to {@link #MAX_CHARS}.
     */
    void type(char character) {
        if (!open || text.length() >= MAX_CHARS || !isHexEntryCharacter(character)) {
            return;
        }
        text.append(character);
    }

    private static boolean isHexEntryCharacter(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F')
                || character == 'U' || character == 'u' || character == '+';
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
     * Four lines, or five when the glyph name wraps: the typed text with a cursor, a blank line,
     * then either the hint or error (wrapped over two lines) or the code point and glyph name.
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
        String code = CodePoints.label(codePoint);
        refreshNameCache(codePoint);
        String name = cachedName;
        String status = hasSlot.test(codePoint) ? code + EXISTS_SUFFIX : code;
        List<String> nameLines = DialogText.wrapToTwoLines(name);
        if (nameLines.get(1).isEmpty()) {
            return List.of(typedLine(), "", status, nameLines.get(0));
        }
        return List.of(typedLine(), "", status, nameLines.get(0), nameLines.get(1));
    }

    private void refreshNameCache(int codePoint) {
        if (codePoint == cachedNameCodePoint) {
            return;
        }
        cachedNameCodePoint = codePoint;
        cachedName = GlyphName.fullName(codePoint);
    }

    private List<String> wrapped(String message) {
        List<String> messageLines = DialogText.wrapToTwoLines(message);
        return List.of(typedLine(), "", messageLines.get(0), messageLines.get(1));
    }

    private String typedLine() {
        return DialogText.typedLine(text, CURSOR, LINE_CELLS);
    }
}
