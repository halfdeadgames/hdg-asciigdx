package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogText;
import java.util.List;
import java.util.function.Consumer;

/**
 * The size typed into the size page of the "new font" dialog, and the lines the dialog shows for
 * it.
 */
final class NewFontSizePrompt {

    static final int LINE_CELLS = DialogText.LINE_CELLS;
    /** Index of the typed-size line among {@link #lines()}. */
    static final int INPUT_LINE = 2;
    static final int MAX_TYPED = 9;
    static final String HINT = "e.g. 16x16";

    private static final char CURSOR = '_';

    private final StringBuilder text = new StringBuilder(MAX_TYPED);
    private final Consumer<CellSize> onConfirm;
    private final Runnable onBack;
    private String name = "";
    private String problem = "";
    private boolean open;

    /**
     * @param onConfirm receives the typed cell size when Enter is pressed on a valid size
     * @param onBack called when Esc is pressed, to return to the name page
     */
    NewFontSizePrompt(Consumer<CellSize> onConfirm, Runnable onBack) {
        this.onConfirm = onConfirm;
        this.onBack = onBack;
    }

    boolean isOpen() {
        return open;
    }

    /** Opens the page for the given font name with nothing typed. */
    void open(String fontName) {
        open(fontName, "");
    }

    /**
     * Opens the page for the given font name with {@code initialText} already typed, for coming
     * back from a later page.
     */
    void open(String fontName, String initialText) {
        name = fontName;
        text.setLength(0);
        problem = "";
        open = true;
        for (int i = 0; i < initialText.length(); i++) {
            type(initialText.charAt(i));
        }
    }

    void close() {
        name = "";
        text.setLength(0);
        problem = "";
        open = false;
    }

    /** The name the page was opened with, so Back can hand it to the name page. */
    String name() {
        return name;
    }

    /** The text typed so far, lowercase. */
    String typed() {
        return text.toString();
    }

    /**
     * Appends a digit or an x (a capital X is stored as x) while open; anything else, or a full
     * field, is ignored.
     */
    void type(char character) {
        if (!open || text.length() >= MAX_TYPED || !isSizeCharacter(character)) {
            return;
        }
        problem = "";
        text.append(Character.toLowerCase(character));
    }

    void backspace() {
        if (!open || text.isEmpty()) {
            return;
        }
        problem = "";
        text.setLength(text.length() - 1);
    }

    /** Shows why creating failed under the typed size. */
    void showProblem(String message) {
        problem = message;
    }

    /** The problem message, or an empty string. */
    String problem() {
        return problem;
    }

    /** Whether the gray example should be drawn in place of the (empty) typed text. */
    boolean showsHint() {
        return open && text.isEmpty();
    }

    /** Hands a valid size to the create callback; otherwise shows what is wrong and stays. */
    void enter() {
        if (!open) {
            return;
        }
        String sizeError = NewFontInput.typedSizeError(text.toString());
        if (sizeError != null) {
            problem = sizeError;
            return;
        }
        onConfirm.accept(NewFontInput.typedSize(text.toString()));
    }

    void back() {
        if (open) {
            onBack.run();
        }
    }

    /**
     * Exactly five lines: the "Size for" line, a blank line (the input box is drawn a little above
     * its line), the typed text with a cursor, then the problem over two lines.
     */
    List<String> lines() {
        List<String> problemLines = DialogText.wrapToTwoLines(problem());
        return List.of(sizeForLine(), "", typedLine(), problemLines.get(0), problemLines.get(1));
    }

    private String sizeForLine() {
        return DialogText.ellipsize("Size for " + name);
    }

    private String typedLine() {
        StringBuilder shown = new StringBuilder(LINE_CELLS).append(text).append(CURSOR);
        while (shown.length() < LINE_CELLS) {
            shown.append(' ');
        }
        return shown.toString();
    }

    private static boolean isSizeCharacter(char character) {
        return (character >= '0' && character <= '9') || character == 'x' || character == 'X';
    }
}
