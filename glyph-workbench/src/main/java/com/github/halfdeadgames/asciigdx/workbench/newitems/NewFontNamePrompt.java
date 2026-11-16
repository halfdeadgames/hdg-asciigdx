package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.ui.DialogText;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** The name typed into the "new font" dialog, and the four lines the dialog shows for it. */
final class NewFontNamePrompt {

    /**
     * Typing stops here so the text stays bounded; it is past the name limit, so the error still
     * shows.
     */
    static final int MAX_TYPED = 40;
    static final int LINE_CELLS = DialogText.LINE_CELLS;
    private static final String HINT_LINE = "Creates the folder";

    private static final char CURSOR = '_';

    private final StringBuilder text = new StringBuilder(MAX_TYPED);
    private final Consumer<String> onNext;
    private final Runnable onCancel;
    private Collection<String> existingFolders = List.of();
    private boolean open;

    /**
     * @param onNext receives the name when Enter is pressed on a valid name
     * @param onCancel called when Esc is pressed
     */
    NewFontNamePrompt(Consumer<String> onNext, Runnable onCancel) {
        this.onNext = onNext;
        this.onCancel = onCancel;
    }

    boolean isOpen() {
        return open;
    }

    /**
     * Opens the prompt with {@code initialText} already typed, for coming back from a later page.
     */
    void open(Collection<String> existingFolders, String initialText) {
        text.setLength(0);
        this.existingFolders = List.copyOf(existingFolders);
        open = true;
        for (int i = 0; i < initialText.length(); i++) {
            type(initialText.charAt(i));
        }
    }

    void close() {
        text.setLength(0);
        open = false;
    }

    /**
     * Appends a non-control character while open, lowercasing A to Z; other characters are left for
     * the error.
     */
    void type(char character) {
        if (!open || Character.isISOControl(character) || text.length() >= MAX_TYPED) {
            return;
        }
        text.append(character >= 'A' && character <= 'Z' ? (char) (character + ('a' - 'A'))
                : character);
    }

    /** Removes the last code point, so a surrogate pair goes in one press. */
    void backspace() {
        if (!open || text.isEmpty()) {
            return;
        }
        text.setLength(text.offsetByCodePoints(text.length(), -1));
    }

    String name() {
        return text.toString();
    }

    /** Null when the typed name can be used, otherwise the message explaining why not. */
    String error() {
        return NewFontInput.nameError(text.toString(), existingFolders);
    }

    /**
     * Hands a valid name to the next-step callback; does nothing while the name is empty or
     * invalid.
     */
    void enter() {
        if (open && error() == null) {
            onNext.accept(text.toString());
        }
    }

    void cancel() {
        if (open) {
            onCancel.run();
        }
    }

    /**
     * Exactly four lines: the typed name with a cursor, a blank line, then the error wrapped over
     * two lines or the hint.
     */
    List<String> lines() {
        String error = error();
        if (error != null) {
            return wrapped(error);
        }
        return List.of(typedLine(), "", HINT_LINE, DialogText.cut(text.toString()));
    }

    private List<String> wrapped(String message) {
        List<String> messageLines = DialogText.wrapToTwoLines(message);
        return List.of(typedLine(), "", messageLines.get(0), messageLines.get(1));
    }

    private String typedLine() {
        return DialogText.typedLine(text, CURSOR, LINE_CELLS);
    }
}
