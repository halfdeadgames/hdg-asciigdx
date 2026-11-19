package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

/**
 * Dev-only: once, after a delay in seconds, runs the same texture rebuild libGDX runs after a
 * real GPU context loss, so glyphs can be checked afterwards.
 */
final class TextureReloadHook {
    static final String KEY_RELOAD_TEXTURES = "reloadTextures";

    private final float delaySeconds;
    private float elapsedSeconds;
    private boolean done;

    private TextureReloadHook(float delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    /** Returns the hook, or null when the setting is missing, empty, or not a delay of 0 or more. */
    static TextureReloadHook fromSetting(String delaySecondsText) {
        if (delaySecondsText == null || delaySecondsText.isEmpty()) {
            return null;
        }
        float delaySeconds;
        try {
            delaySeconds = Float.parseFloat(delaySecondsText);
        } catch (NumberFormatException e) {
            delaySeconds = Float.NaN;
        }
        if (!(delaySeconds >= 0f) || Float.isInfinite(delaySeconds)) {
            Gdx.app.error("TextureReloadHook", "Ignoring bad delay: " + delaySecondsText);
            return null;
        }
        return new TextureReloadHook(delaySeconds);
    }

    void update(float deltaSeconds) {
        if (done) {
            return;
        }
        elapsedSeconds += deltaSeconds;
        if (elapsedSeconds < delaySeconds) {
            return;
        }
        done = true;
        Texture.invalidateAllTextures(Gdx.app);
        Gdx.app.log("TextureReloadHook", "Rebuilt all textures");
    }
}
