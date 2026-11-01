package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/** Pure rectangle math behind layer clipping, kept free of GL so tests can run headless. */
final class ClipMath {

    /** Tolerance so float noise on an exact pixel edge does not widen the scissor. */
    static final float EPSILON = 1e-3f;

    private ClipMath() {
    }

    /**
     * Converts a clip rectangle in stack space (y down from the stack's top-left) to
     * a world rectangle (y up). With snapping on, all four edges are rounded with
     * {@code floor(v + 0.5)}, like layer positions.
     */
    static Rectangle stackToWorld(float stackX, float stackY, float clipX, float clipY,
            float clipWidth, float clipHeight, boolean pixelSnapping, Rectangle out) {
        float left = stackX + clipX;
        float top = stackY - clipY;
        float right = left + clipWidth;
        float bottom = top - clipHeight;
        if (pixelSnapping) {
            left = snap(left);
            top = snap(top);
            right = snap(right);
            bottom = snap(bottom);
        }
        return out.set(left, bottom, right - left, top - bottom);
    }

    /**
     * Projects the four corners of a world rectangle with {@code combined} and maps
     * them to window pixels through the viewport. The result is the corners'
     * bounding box, rounded outward (floor for min, ceil for max) so the scissor
     * never cuts into the rectangle.
     */
    static Rectangle worldToWindow(Rectangle world, Matrix4 combined, int viewportX, int viewportY,
            int viewportWidth, int viewportHeight, Vector3 tmp, Rectangle out) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            float cx = (corner & 1) == 0 ? world.x : world.x + world.width;
            float cy = (corner & 2) == 0 ? world.y : world.y + world.height;
            tmp.set(cx, cy, 0f).prj(combined);
            float windowX = viewportX + (tmp.x + 1f) * 0.5f * viewportWidth;
            float windowY = viewportY + (tmp.y + 1f) * 0.5f * viewportHeight;
            minX = Math.min(minX, windowX);
            maxX = Math.max(maxX, windowX);
            minY = Math.min(minY, windowY);
            maxY = Math.max(maxY, windowY);
        }
        float x0 = (float) Math.floor(minX + EPSILON);
        float y0 = (float) Math.floor(minY + EPSILON);
        float x1 = (float) Math.ceil(maxX - EPSILON);
        float y1 = (float) Math.ceil(maxY - EPSILON);
        return out.set(x0, y0, x1 - x0, y1 - y0);
    }

    private static float snap(float value) {
        return (float) Math.floor(value + 0.5f);
    }
}
