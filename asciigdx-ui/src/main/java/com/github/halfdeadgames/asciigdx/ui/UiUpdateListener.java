package com.github.halfdeadgames.asciigdx.ui;

/**
 * Receives the per-frame time step of a {@link UiRoot}, added with
 * {@link UiRoot#addUpdateListener(UiUpdateListener)}. Use it for animation, such as a blinking
 * cursor or a tooltip delay.
 */
@FunctionalInterface
public interface UiUpdateListener {

    /**
     * Advances by one frame.
     *
     * @param deltaSeconds time since the previous update, in seconds; zero or greater
     */
    void update(float deltaSeconds);
}
