package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;

/**
 * Math for a layer's zoom and rotation.
 *
 * <p>Layer-local space has its origin at the layer's top-left corner, x growing right and y
 * growing up, so rows sit at negative y. Batch world space is the screen with y up. Pivots are
 * given in layer pixels from the top-left corner with y growing down, so pivot (px, py) is the
 * local point (px, -py).
 */
final class LayerTransform {

    private LayerTransform() {
    }

    /**
     * Resolves the zoom actually applied for the given mode.
     *
     * @param zoom the requested zoom
     * @param mode how the zoom is applied
     * @return the zoom as given for {@link ZoomMode#SMOOTH}, or the nearest whole number of at
     *     least 1 for {@link ZoomMode#WHOLE}
     */
    static float effectiveZoom(float zoom, ZoomMode mode) {
        if (mode == ZoomMode.SMOOTH) {
            return zoom;
        }
        return Math.max(1f, ClipMath.snap(zoom));
    }

    /**
     * Writes the layer-local to batch-world transform into {@code out}: translate to the screen
     * corner, then rotate counterclockwise and scale about the pivot.
     *
     * @param screenX the layer's top-left corner on screen, x
     * @param screenY the layer's top-left corner on screen, y (up)
     * @param pivotX pivot in layer pixels from the left edge
     * @param pivotY pivot in layer pixels from the top edge (down)
     * @param zoom the effective zoom
     * @param rotationDegrees counterclockwise rotation in degrees
     * @param snapCorner whether to round the image of the local origin to whole pixels
     * @param out receives the transform
     */
    static void compose(float screenX, float screenY, float pivotX, float pivotY, float zoom,
            float rotationDegrees, boolean snapCorner, Affine2 out) {
        setLinearPart(zoom, rotationDegrees, out);
        setTranslationAboutPivot(screenX, screenY, pivotX, -pivotY, out);
        if (snapCorner) {
            snapTranslation(out);
        }
    }

    /**
     * Maps a batch-world point back into the layer's pixels, measured from its top-left corner
     * with y growing down.
     *
     * @param worldToLayer the inverse of a transform written by {@link #compose}
     * @param out receives the point in layer pixels
     */
    static void worldToLayerPixels(Affine2 worldToLayer, float worldX, float worldY, Vector2 out) {
        out.set(worldX, worldY);
        worldToLayer.applyTo(out);
        out.y = -out.y;
    }

    /**
     * Maps a point in the layer's pixels, measured from its top-left corner with y growing
     * down, into batch-world space.
     *
     * @param layerToWorld a transform written by {@link #compose}
     * @param out receives the point in batch-world space
     */
    static void layerPixelsToWorld(Affine2 layerToWorld, float layerX, float layerY, Vector2 out) {
        out.set(layerX, -layerY);
        layerToWorld.applyTo(out);
    }

    private static void setLinearPart(float zoom, float rotationDegrees, Affine2 out) {
        if (rotationDegrees == 0f) {
            out.setToScaling(zoom, zoom);
            return;
        }
        out.setToRotation(rotationDegrees);
        out.scale(zoom, zoom);
    }

    /**
     * Places the transform so the local pivot stays fixed on screen. The pivot's own image is
     * subtracted before adding the screen corner, so zoom 1 without rotation gives exactly
     * (screenX, screenY), with no float drift that could change how the corner snaps.
     */
    private static void setTranslationAboutPivot(float screenX, float screenY,
            float localPivotX, float localPivotY, Affine2 out) {
        float movedPivotX = out.m00 * localPivotX + out.m01 * localPivotY;
        float movedPivotY = out.m10 * localPivotX + out.m11 * localPivotY;
        out.m02 = screenX + (localPivotX - movedPivotX);
        out.m12 = screenY + (localPivotY - movedPivotY);
    }

    private static void snapTranslation(Affine2 transform) {
        transform.m02 = ClipMath.snap(transform.m02);
        transform.m12 = ClipMath.snap(transform.m12);
    }
}
