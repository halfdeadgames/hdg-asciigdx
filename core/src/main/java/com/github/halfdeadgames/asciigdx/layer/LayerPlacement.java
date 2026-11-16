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

    static void compose(Layer layer, float stackX, float stackY, boolean pixelSnapping, Affine2 out) {
        Panel panel = layer.getPanel();
        // Zero for plain layers; for map windows it cancels the whole cells the content already scrolled.
        float mapOriginX = layer.getMapOriginPixelsX();
        float mapOriginY = layer.getMapOriginPixelsY();
        float effectiveOffsetX = getPanelCornerX(panel) + layer.getOffsetX()
                - getActiveCameraX(layer) * layer.getParallaxX() + mapOriginX;
        float effectiveOffsetY = getPanelCornerY(panel) + layer.getOffsetY()
                - getActiveCameraY(layer) * layer.getParallaxY() + mapOriginY;
        float screenX = stackX + effectiveOffsetX;
        float screenY = stackY - effectiveOffsetY;
        LayerTransform.compose(screenX, screenY, layer.getPivotX() - mapOriginX, layer.getPivotY() - mapOriginY,
                layer.getEffectiveZoom(), layer.getRotation(), isCornerSnapped(layer, pixelSnapping), out);
    }
}
