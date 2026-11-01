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

    /** Short live status shown under the perf line; empty for none. */
    default String statusText() {
        return "";
    }

    /** Key press forwarded by the app while this scene is open; return true if handled. */
    default boolean keyDown(int keycode) {
        return false;
    }

    /** Touch forwarded by the app while this scene is open, in screen pixels; return true if handled. */
    default boolean touchDown(int screenX, int screenY) {
        return false;
    }

    /** Whether {@link #setDirtyPercent(int)} does anything, so the capture script can sweep it. */
    default boolean supportsDirtyPercent() {
        return false;
    }

    /** Share of cells (0..100) rewritten per frame; only called when {@link #supportsDirtyPercent()} is true. */
    default void setDirtyPercent(int percent) {
    }

    /** Whether {@link #setLayerCount(int)} does anything, so the capture script can sweep it. */
    default boolean supportsLayerCount() {
        return false;
    }

    /** Number of stacked layers (1 or more); only called when {@link #supportsLayerCount()} is true. */
    default void setLayerCount(int count) {
    }

    /** Whether the capture script records a GIF named after {@link #id()} for this scene. */
    default boolean recordsAnimation() {
        return false;
    }
}
