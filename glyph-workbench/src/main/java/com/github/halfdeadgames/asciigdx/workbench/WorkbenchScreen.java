package com.github.halfdeadgames.asciigdx.workbench;

/**
 * One screen of the workbench. A fresh instance is created each time the screen is opened:
 * {@link #show()} creates its textures and batches and sets the input processor, and
 * {@link #hide()} disposes them, so switching screens back and forth leaks nothing.
 */
interface WorkbenchScreen {

    void show();

    void resize(int width, int height);

    /** Handles input and draws one frame. */
    void render();

    void hide();
}
