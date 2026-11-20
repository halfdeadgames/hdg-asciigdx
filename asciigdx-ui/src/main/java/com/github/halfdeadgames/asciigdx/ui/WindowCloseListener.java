package com.github.halfdeadgames.asciigdx.ui;

/** Hears that a {@link Window} was closed. */
@FunctionalInterface
public interface WindowCloseListener {

    /**
     * Called by {@link Window#close()} after the window was removed from its parent. Runs on the
     * render thread, possibly while the {@link UiRoot} delivers an input event, so it must not
     * call input methods of the root or move keyboard focus. To reopen the window, defer the work
     * to the next frame with a {@link UiUpdateListener}.
     *
     * @param window the window that closed; never {@code null}
     */
    void windowClosed(Window window);
}
