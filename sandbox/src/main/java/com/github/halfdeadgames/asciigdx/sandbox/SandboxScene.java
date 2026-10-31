package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** One test scene in the sandbox menu. Scenes own their grids and renderers. */
interface SandboxScene {
    /** File-safe identifier, used for capture file names. */
    String id();

    String title();

    void show();

    void resize(int width, int height);

    void render(SpriteBatch batch, float delta);

    void hide();

    void dispose();

    int cellCount();

    /** Whether the capture script records a GIF named after {@link #id()} for this scene. */
    default boolean recordsAnimation() {
        return false;
    }
}
