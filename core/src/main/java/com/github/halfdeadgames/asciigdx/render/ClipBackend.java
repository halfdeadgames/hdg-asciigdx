package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;

/** Test seam for {@link LayerStack}: everything clipping needs from the GL side. */
interface ClipBackend {

    /** Submits the batch's pending geometry so it is drawn with the current scissor. */
    void flush(SpriteBatch batch);

    /** Writes the batch's projection times transform into {@code out}. */
    void getCombinedMatrix(SpriteBatch batch, Matrix4 out);

    /** Writes the current GL viewport (x, y, width, height) in backbuffer pixels. */
    void getViewport(int[] out);

    /**
     * Pushes a scissor given in backbuffer pixels, y-up, intersected with any scissor
     * already active.
     *
     * @return false if the resulting area is empty; nothing was pushed then
     */
    boolean push(Rectangle windowRect);

    /** Pops the scissor pushed by the last successful {@link #push}. */
    void pop();
}
