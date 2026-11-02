package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;

/**
 * Scales the source by the largest whole-number factor that fits the target in both axes,
 * keeping glyph pixels uniform. {@link #INSTANCE} never goes below 1x, so a target that is too
 * small gets the source size and it overflows. {@link #SHRINK_BELOW_ONE} shrinks fractionally
 * below 1x like {@link Scaling#fit}, which leaves glyph pixels uneven. If any size is zero or
 * negative, the result is 0 by 0.
 *
 * <p>Like the built-in scalings, the returned vector is shared and overwritten by the next call.
 */
public final class IntegerScaling extends Scaling {

    public static final IntegerScaling INSTANCE = new IntegerScaling(false);
    public static final IntegerScaling SHRINK_BELOW_ONE = new IntegerScaling(true);

    private final boolean shrinksBelowOneTimes;

    private IntegerScaling(boolean shrinksBelowOneTimes) {
        this.shrinksBelowOneTimes = shrinksBelowOneTimes;
    }

    @Override
    public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
        if (sourceWidth <= 0f || sourceHeight <= 0f || targetWidth <= 0f || targetHeight <= 0f) {
            temp.set(0f, 0f);
            return temp;
        }
        float fit = Math.min(targetWidth / sourceWidth, targetHeight / sourceHeight);
        float scale;
        if (fit >= 1f) {
            scale = (float) Math.floor(fit);
        } else {
            scale = shrinksBelowOneTimes ? fit : 1f;
        }
        temp.x = sourceWidth * scale;
        temp.y = sourceHeight * scale;
        return temp;
    }
}
