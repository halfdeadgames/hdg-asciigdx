package com.github.halfdeadgames.asciigdx.sandbox;

/**
 * Changes the app window's size for scenes that ask for one. Platforms without a resizable
 * window use {@link #NONE}.
 */
public interface WindowResizer {
    /** Does nothing and always reports settled; used on Android and when no resizer is set. */
    WindowResizer NONE = new WindowResizer() {
        @Override
        public void resizeTo(WindowSize size) {
        }

        @Override
        public void restorePreviousSize() {
        }

        @Override
        public boolean isSettled() {
            return true;
        }
    };

    /** Remembers the current window and switches to the given drawable size. */
    void resizeTo(WindowSize size);

    /** Puts back the window remembered by the last {@link #resizeTo}; no-op if none. */
    void restorePreviousSize();

    /** Whether the drawable size now matches the last request. */
    boolean isSettled();
}
