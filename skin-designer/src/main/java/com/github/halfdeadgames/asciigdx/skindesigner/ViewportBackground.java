package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Paints the screen area of a viewport in the designer's background color. */
final class ViewportBackground {
    private static final float RED = 0.15f;
    private static final float GREEN = 0.15f;
    private static final float BLUE = 0.2f;
    private static final float ALPHA = 1f;

    private ViewportBackground() {
    }

    /** Clears only the screen area covered by the viewport, leaving the rest alone. */
    static void clearArea(Viewport viewport) {
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        ScreenUtils.clear(RED, GREEN, BLUE, ALPHA);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }
}
