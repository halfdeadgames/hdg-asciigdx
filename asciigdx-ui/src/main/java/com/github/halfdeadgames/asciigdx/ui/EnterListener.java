package com.github.halfdeadgames.asciigdx.ui;

/**
 * Called when the user presses Enter (or numpad Enter) in a {@link TextField}, added with
 * {@link TextField#addEnterListener(EnterListener)}. Listeners run in the order they were added
 * and stop at the first one that returns {@code true}. If none returns {@code true}, the key is
 * not handled by the field and continues to the unhandled-key path of the {@link UiRoot}.
 *
 * <p>Example:
 * <pre>{@code
 * field.addEnterListener(source -> {
 *     submit(source.getText());
 *     return true;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface EnterListener {

    /**
     * Returns {@code true} if the listener handled Enter. Called when Enter is pressed in the
     * field; a {@code true} result consumes the key and stops later listeners, and a
     * {@code false} result lets the next listener run.
     *
     * @param field the field that has focus; never {@code null}
     * @return {@code true} if the listener handled the key, {@code false} to pass it on
     */
    boolean enterPressed(TextField field);
}
