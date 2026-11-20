package com.github.halfdeadgames.asciigdx.ui;

/**
 * Hears the choice a user makes in a {@link Dialog}. Register it with
 * {@link Dialog#addChoiceListener}.
 */
@FunctionalInterface
public interface ChoiceListener {

    /**
     * Called after a choice was made. When the dialog closes on a choice (see
     * {@link Dialog#setCloseOnChoice(boolean)}), it has already closed and its modal state is
     * gone, so the listener may show another dialog. Runs on the render thread, during input
     * handling, so it must not call the input methods of the {@link UiRoot}.
     *
     * @param dialog the dialog that was answered; never {@code null}
     * @param choice the choice that was made; never {@code null}
     */
    void chosen(Dialog dialog, DialogChoice choice);
}
