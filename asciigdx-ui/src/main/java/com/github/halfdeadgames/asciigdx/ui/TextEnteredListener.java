package com.github.halfdeadgames.asciigdx.ui;

/**
 * Receives the text a user confirmed in a dialog made by
 * {@link Dialogs#textInput(DialogStyle, String, String, String, int, TextEnteredListener)}.
 */
@FunctionalInterface
public interface TextEnteredListener {

    /**
     * Called when the user confirmed the text with the OK button or the Enter key. It is not
     * called when the user cancels. The dialog has already closed. Runs on the render thread,
     * during input handling, so it must not call the input methods of the {@link UiRoot}.
     *
     * @param text the text of the field; never {@code null}; may be empty
     */
    void entered(String text);
}
