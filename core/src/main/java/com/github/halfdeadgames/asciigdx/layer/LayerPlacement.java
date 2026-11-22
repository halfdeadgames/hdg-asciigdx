package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.math.Affine2;

/**
 * Works out where a layer sits in stack space: which camera moves it, where its panel corner is,
 * and the transform that draws and picks it. The layer must still belong to a stack.
 */
final class LayerPlacement {

    private LayerPlacement() {
    }

    static float getActiveCameraX(Layer layer) {
        Panel panel = layer.getPanel();
        return panel == null ? layer.getOwner().getCameraX() : panel.getCameraX();
    }

    static float getActiveCameraY(Layer layer) {
        Panel panel = layer.getPanel();
        return panel == null ? layer.getOwner().getCameraY() : panel.getCameraY();
    }

    static float getPanelCornerX(Panel panel) {
        return panel == null ? 0f : panel.getX();
    }

    static float getPanelCornerY(Panel panel) {
        return panel == null ? 0f : panel.getY();
    }

    static boolean isCornerSnapped(Layer layer, boolean pixelSnapping) {
        return pixelSnapping || layer.getZoomMode() == ZoomMode.WHOLE;
    }

    static void compose(Layer layer, float stackX, float stackY, boolean pixelSnapping,
            Affine2 out) {
        Panel panel = layer.getPanel();
        // Zero for plain layers; for map windows it cancels the whole cells the content already
        // scrolled.
        float mapOriginX = layer.getMapOriginPixelsX();
        float mapOriginY = layer.getMapOriginPixelsY();
        float effectiveOffsetX = getPanelCornerX(panel) + layer.getOffsetX()
                - getActiveCameraX(layer) * layer.getParallaxX() + mapOriginX;
        float effectiveOffsetY = getPanelCornerY(panel) + layer.getOffsetY()
                - getActiveCameraY(layer) * layer.getParallaxY() + mapOriginY;
        float screenX = stackX + effectiveOffsetX;
        float screenY = stackY - effectiveOffsetY;
        boolean snapCorner = isCornerSnapped(layer, pixelSnapping);
        float cameraZoomScale = getCameraZoomScale(layer);
        if (cameraZoomScale == 1f) {
            LayerTransform.compose(screenX, screenY, layer.getPivotX() - mapOriginX,
                    layer.getPivotY() - mapOriginY, layer.getEffectiveZoom(), layer.getRotation(),
                    snapCorner, out);
            return;
        }
        LayerTransform.compose(screenX, screenY, layer.getPivotX() - mapOriginX,
                layer.getPivotY() - mapOriginY, layer.getEffectiveZoom(), layer.getRotation(),
                false, out);
        LayerStack stack = layer.getOwner();
        LayerTransform.scaleAboutPoint(out, cameraZoomScale,
                stackX + stack.getCameraZoomFocusX(), stackY - stack.getCameraZoomFocusY());
        if (snapCorner) {
            LayerTransform.snapTranslation(out);
        }
    }

    /**
     * Returns the scale the stack's camera zoom gives a layer: {@code cameraZoom ^ zoomRate}, or 1
     * for a layer in a panel, which the stack's camera zoom does not reach.
     */
    static float getCameraZoomScale(Layer layer) {
        if (layer.getPanel() != null) {
            return 1f;
        }
        return (float) Math.pow(layer.getOwner().getCameraZoom(), layer.getZoomRate());
    }
}
