package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Optional;

/** One test scene in the sandbox menu. Scenes own their grids and renderers. */
interface SandboxScene {
    /** File-safe identifier, used for capture file names. */
    String id();

    String title();

    /** Menu group this scene is listed under; OTHER until a scene picks one. */
    default SceneGroup group() {
        return SceneGroup.OTHER;
    }

    /**
     * Window size the app switches to while this scene is open; empty keeps the current window.
     */
    default Optional<WindowSize> preferredWindowSize() {
        return Optional.empty();
    }

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

    /**
     * Whether the status changes without input, so the app re-reads {@link #statusText()} every
     * frame.
     */
    default boolean hasLiveStatus() {
        return false;
    }

    /** Key press forwarded by the app while this scene is open; return true if handled. */
    default boolean keyDown(int keycode) {
        return false;
    }

    /**
     * Touch forwarded by the app while this scene is open, in screen pixels; return true if
     * handled.
     */
    default boolean touchDown(int screenX, int screenY) {
        return false;
    }

    /** Finger or button moved while down, in screen pixels; return true if handled. */
    default boolean touchDragged(int screenX, int screenY) {
        return false;
    }

    /** Mouse moved with no button down, in screen pixels; return true if handled. */
    default boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    /** Finger or button released, in screen pixels; return true if handled. */
    default boolean touchUp(int screenX, int screenY) {
        return false;
    }

    /** Mouse wheel turned; positive is down. Return true if handled. */
    default boolean scrolled(float amountY) {
        return false;
    }

    /** Whether {@link #setDirtyPercent(int)} does anything, so the capture script can sweep it. */
    default boolean supportsDirtyPercent() {
        return false;
    }

    /**
     * Share of cells (0..100) rewritten per frame; only called when {@link #supportsDirtyPercent()}
     * is true.
     */
    default void setDirtyPercent(int percent) {
    }

    /** Whether {@link #setLayerCount(int)} does anything, so the capture script can sweep it. */
    default boolean supportsLayerCount() {
        return false;
    }

    /**
     * Number of stacked layers (1 or more); only called when {@link #supportsLayerCount()} is true.
     */
    default void setLayerCount(int count) {
    }

    /**
     * Ids of the views the capture script photographs separately, in order; empty when the scene
     * has one view. Each id must be file-safe.
     */
    default List<String> captureVariants() {
        return List.of();
    }

    /** Switches to one of the {@link #captureVariants()} before its capture. */
    default void showCaptureVariant(String variant) {
    }

    /** Whether the capture script records a GIF named after {@link #id()} for this scene. */
    default boolean recordsAnimation() {
        return false;
    }
}
