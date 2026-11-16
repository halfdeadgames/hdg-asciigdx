package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

/**
 * Resolves a layer's clip into a stack-space rectangle and applies it as a scissor through a
 * {@link ClipBackend}. Stack size and pixel snapping are passed per call, never kept.
 *
 * <p>Call order: {@link #beginFrame} once per frame, then for each layer {@link #resolve} before
 * {@link #isEmpty}, {@link #isInside}, or {@link #pushScissor}, which all read the last resolved
 * rectangle. {@link #pushScissor} needs {@link #queryViewportOnce} first in the same frame.
 */
final class LayerClipper {

    private final ClipBackend backend;
    private final Matrix4 clipMatrix = new Matrix4();
    private final Vector3 clipCorner = new Vector3();
    private final int[] viewport = new int[4];
    private final Rectangle stackRect = new Rectangle();
    private final Rectangle worldRect = new Rectangle();
    private final Rectangle windowRect = new Rectangle();
    private boolean viewportQueried;
    private boolean scissorActive;

    LayerClipper(ClipBackend backend) {
        this.backend = backend;
    }

    static boolean isClipped(Layer layer) {
        return layer.hasClip() || layer.getPanel() != null;
    }

    void beginFrame() {
        viewportQueried = false;
    }

    void queryViewportOnce(SpriteBatch batch) {
        if (viewportQueried) {
            return;
        }
        backend.getCombinedMatrix(batch, clipMatrix);
        backend.getViewport(viewport);
        viewportQueried = true;
    }

    /** The cached viewport; valid after {@link #queryViewportOnce}. */
    int[] getViewport() {
        return viewport;
    }

    /** Writes the layer's final clip in stack space; the layer must be clipped. */
    void resolve(Layer layer, float stackWidth, float stackHeight, boolean stackHasSize) {
        Panel panel = layer.getPanel();
        if (layer.isClippedToStack()) {
            resolveStackArea(stackWidth, stackHeight, stackHasSize);
        } else if (layer.hasClip()) {
            stackRect.set(layer.getClipX() + LayerPlacement.getPanelCornerX(panel),
                    layer.getClipY() + LayerPlacement.getPanelCornerY(panel),
                    layer.getClipWidth(), layer.getClipHeight());
        } else {
            stackRect.set(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight());
            return;
        }
        if (panel != null) {
            intersectWithPanel(panel);
        }
    }

    boolean isEmpty() {
        return stackRect.width <= 0f || stackRect.height <= 0f;
    }

    /** Flushes the batch and pushes the resolved rectangle as a scissor; see {@link #isScissorActive}. */
    void pushScissor(SpriteBatch batch, float stackX, float stackY, boolean pixelSnapping) {
        ClipMath.stackToWorld(stackX, stackY, stackRect.x, stackRect.y, stackRect.width, stackRect.height,
                pixelSnapping, worldRect);
        ClipMath.worldToWindow(worldRect, clipMatrix, viewport[0], viewport[1], viewport[2], viewport[3],
                clipCorner, windowRect);
        backend.flush(batch);
        scissorActive = backend.push(windowRect);
    }

    /** False if nothing was left of the last pushed rectangle. */
    boolean isScissorActive() {
        return scissorActive;
    }

    void popScissor(SpriteBatch batch) {
        backend.flush(batch);
        backend.pop();
        scissorActive = false;
    }

    /** Tells whether a point in batch world space is inside the resolved rectangle. */
    boolean isInside(float stackX, float stackY, float worldX, float worldY, boolean pixelSnapping) {
        ClipMath.stackToWorld(stackX, stackY, stackRect.x, stackRect.y, stackRect.width, stackRect.height,
                pixelSnapping, worldRect);
        return isInsideWindow(worldRect, worldX, worldY);
    }

    /**
     * Rectangle.contains includes every edge, but a clip window keeps its top and left edges
     * and drops its right and bottom ones. World y grows up, so the bottom edge is world.y.
     */
    private static boolean isInsideWindow(Rectangle world, float worldX, float worldY) {
        return worldX >= world.x && worldX < world.x + world.width
                && worldY > world.y && worldY <= world.y + world.height;
    }

    private void resolveStackArea(float stackWidth, float stackHeight, boolean stackHasSize) {
        if (!stackHasSize) {
            throw new IllegalStateException("Layer is clipped to the stack but the stack size is not set");
        }
        stackRect.set(0f, 0f, stackWidth, stackHeight);
    }

    /** Leaves a zero-size rectangle when nothing overlaps, never a negative one. */
    private void intersectWithPanel(Panel panel) {
        float left = Math.max(stackRect.x, panel.getX());
        float top = Math.max(stackRect.y, panel.getY());
        float right = Math.min(stackRect.x + stackRect.width, panel.getX() + panel.getWidth());
        float bottom = Math.min(stackRect.y + stackRect.height, panel.getY() + panel.getHeight());
        stackRect.set(left, top, Math.max(0f, right - left), Math.max(0f, bottom - top));
    }
}
