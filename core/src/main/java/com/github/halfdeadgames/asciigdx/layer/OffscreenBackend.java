package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Test seam for {@link LayerStack}: everything fading a layer through an offscreen target needs
 * from GL.
 */
interface OffscreenBackend {

    /**
     * Flushes the batch, binds the shared target (creating or resizing it if needed), sets the GL
     * viewport to the given rectangle, and clears the target to transparent.
     *
     * @param viewport x, y, width, height in backbuffer pixels
     */
    void begin(SpriteBatch batch, int[] viewport);

    /** Unbinds the target and restores the GL viewport to the given rectangle. */
    void end(int[] viewport);

    /**
     * Draws the target's content for the viewport rectangle onto the bound screen, multiplied by
     * {@code alpha}, as premultiplied color. Rebinds the batch's shader afterward.
     */
    void composite(SpriteBatch batch, float alpha, int[] viewport);

    /** Frees the target and its helpers, if any were created. */
    void dispose();
}
