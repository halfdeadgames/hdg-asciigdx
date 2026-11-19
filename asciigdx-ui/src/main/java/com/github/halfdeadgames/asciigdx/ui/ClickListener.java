package com.github.halfdeadgames.asciigdx.ui;

/**
 * Receives the clicks of a {@link TextButton}, added with {@link TextButton#addClickListener}.
 * A click happens when the primary button is pressed and released inside the button, or when
 * Enter, numpad Enter, or Space is pressed while the button has keyboard focus. A disabled button
 * never clicks. See {@link TextButton} for the full rules.
 */
@FunctionalInterface
public interface ClickListener {

    /**
     * Called when the button is clicked, by the pointer or the keyboard.
     *
     * @param button the button that was clicked; never {@code null}
     */
    void clicked(TextButton button);
}
