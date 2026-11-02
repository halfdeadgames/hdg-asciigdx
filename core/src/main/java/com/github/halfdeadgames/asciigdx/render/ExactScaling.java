package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;

/**
 * Multiplies the source by a fixed whole-number factor and ignores the target size.
 *
 * <p>Like the built-in scalings, the returned vector is shared and overwritten by the next call.
 */
final class ExactScaling extends Scaling {

    private final int scale;

    ExactScaling(int scale) {
        this.scale = scale;
    }

    @Override
    public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
        temp.set(sourceWidth * scale, sourceHeight * scale);
        return temp;
    }
}
