package com.github.halfdeadgames.asciigdx.fit;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;

/**
 * A libGDX {@link Scaling} that enlarges the source by the largest whole-number factor that fits
 * the target, never below 1x. A target that is too small gets the source size and overflows. For
 * smaller output, use a smaller font. If any source or target size is zero or negative, the
 * result is 0 by 0.
 *
 * <p>Like the built-in scalings, the returned vector is shared and overwritten by the next call,
 * so copy it if you need to keep it. Use it from one thread at a time, normally the render
 * thread.
 *
 * <pre>{@code
 * Viewport viewport = new ScalingViewport(IntegerScaling.INSTANCE, worldWidth, worldHeight);
 * }</pre>
 */
public final class IntegerScaling extends Scaling {

    /**
     * The shared instance. It has no state of its own beyond libGDX's shared temporary vector
     * that every {@code Scaling} returns, so one instance can serve any number of viewports.
     */
    public static final IntegerScaling INSTANCE = new IntegerScaling();

    private IntegerScaling() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>All sizes are in pixels. The result is the source size times the largest whole number
     * that fits inside the target on both axes, or times 1 if the source is larger than the target
     * on either axis. If any argument is 0 or less, the result is 0 by 0.
     *
     * @return a vector shared with later calls; never null
     */
    @Override
    public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth,
            float targetHeight) {
        if (sourceWidth <= 0f || sourceHeight <= 0f || targetWidth <= 0f || targetHeight <= 0f) {
            temp.set(0f, 0f);
            return temp;
        }
        float fit = Math.min(targetWidth / sourceWidth, targetHeight / sourceHeight);
        float scale = Math.max(1f, (float) Math.floor(fit));
        temp.x = sourceWidth * scale;
        temp.y = sourceHeight * scale;
        return temp;
    }
}
