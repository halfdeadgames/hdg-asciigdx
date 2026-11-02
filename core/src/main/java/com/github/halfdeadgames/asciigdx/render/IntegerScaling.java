package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;

/**
 * Scales by the largest whole number that fits, never below 1x. A target that is too small gets
 * the source size and overflows. For smaller output, use a smaller font. If any size is zero or
 * negative, the result is 0 by 0.
 *
 * <p>Like the built-in scalings, the returned vector is shared and overwritten by the next call.
 */
public final class IntegerScaling extends Scaling {

    public static final IntegerScaling INSTANCE = new IntegerScaling();

    private IntegerScaling() {
    }

    @Override
    public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
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
