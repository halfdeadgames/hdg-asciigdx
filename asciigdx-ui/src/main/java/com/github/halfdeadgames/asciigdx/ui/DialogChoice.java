package com.github.halfdeadgames.asciigdx.ui;

/**
 * The answer a user gives to a {@link Dialog}: the button that was chosen, or the dialog's
 * back-out choice when the user pressed Escape or used the window's close button.
 *
 * <p>Later versions may add constants, so a {@code switch} over a choice should keep a
 * {@code default} branch. For answers of your own, add a button with
 * {@link Dialog#addCustomButton(TextButton)} and handle its click yourself.
 */
public enum DialogChoice {

    /** Confirms; also the only answer of an information dialog. */
    OK,

    /** Agrees to a question. */
    YES,

    /** Declines a question. */
    NO,

    /** Abandons the action the dialog asked about. */
    CANCEL
}
