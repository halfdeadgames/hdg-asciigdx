package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * An ordered stack of {@link Layer}s drawn into one {@link SpriteBatch}. Index 0
 * is the bottom layer and the last index is the top. Grids of different sizes
 * and glyph sources can be mixed.
 *
 * <p>The stack creates one {@link GridRenderer} per layer and disposes it when the
 * layer is removed or the stack is disposed. Grids and glyph sources are not
 * owned and are not disposed.
 *
 * <p>Layer offsets are measured from the stack's top-left corner with y growing
 * downward, like grid rows. The stack position passed to {@link #draw} is a screen
 * position with y growing upward.
 */
public final class LayerStack implements Disposable {

    private final LayerRendererFactory rendererFactory;
    private final ClipBackend clipBackend;
    private final OffscreenBackend offscreenBackend;
    private final LayerEffects opaqueEffects = new LayerEffects();
    private boolean viewportQueried;
    private final Array<Layer> layers = new Array<>();
    private final Matrix4 clipMatrix = new Matrix4();
    private final Vector3 clipCorner = new Vector3();
    private final Affine2 layerTransform = new Affine2();
    private final Affine2 inverseTransform = new Affine2();
    private final Vector2 layerPixel = new Vector2();
    private final Rectangle clipRect = new Rectangle();
    private final Rectangle worldRect = new Rectangle();
    private final Rectangle windowRect = new Rectangle();
    private final int[] viewport = new int[4];
    private float width;
    private float height;
    private float cameraX;
    private float cameraY;
    private boolean pixelSnapping = true;
    private boolean disposed;
    private GridRenderResources sharedResources;

    /** Creates an empty stack that draws layers with {@link GridRenderer}. */
    public LayerStack() {
        this(LayerRendererFactory.SHARED_GRID_RENDERERS, new GlClipBackend(), new GlOffscreenBackend());
    }

    LayerStack(LayerRendererFactory rendererFactory) {
        this(rendererFactory, new GlClipBackend(), new GlOffscreenBackend());
    }

    LayerStack(LayerRendererFactory rendererFactory, ClipBackend clipBackend) {
        this(rendererFactory, clipBackend, new GlOffscreenBackend());
    }

    LayerStack(LayerRendererFactory rendererFactory, ClipBackend clipBackend, OffscreenBackend offscreenBackend) {
        this.rendererFactory = rendererFactory;
        this.clipBackend = clipBackend;
        this.offscreenBackend = offscreenBackend;
    }

    /**
     * Sets the stack's area in virtual pixels, used by {@link Layer#clipToStack()}.
     * 0 means unset. The area starts at the stack's top-left corner and extends right
     * and down.
     *
     * @throws IllegalArgumentException if width or height is negative
     */
    public void setSize(float width, float height) {
        if (width < 0f || height < 0f) {
            throw new IllegalArgumentException("size must not be negative");
        }
        this.width = width;
        this.height = height;
    }

    /** Returns the stack's width in virtual pixels, 0 if unset. */
    public float getWidth() {
        return width;
    }

    /** Returns the stack's height in virtual pixels, 0 if unset. */
    public float getHeight() {
        return height;
    }

    boolean hasSize() {
        return width > 0f && height > 0f;
    }

    /**
     * Appends a layer on top of the stack.
     *
     * @param grid the grid to draw
     * @param glyphSource supplies glyphs and the cell size
     * @return the new layer
     */
    public Layer add(TerminalView grid, GlyphSource glyphSource) {
        return insert(layers.size, grid, glyphSource);
    }

    /**
     * Inserts a layer at a position; layers at and above it move up by one.
     *
     * @param index position from 0 (bottom) to {@link #size()} (top)
     * @return the new layer
     * @throws IndexOutOfBoundsException if the index is outside 0 to size
     * @throws IllegalStateException if the stack was disposed
     */
    public Layer insert(int index, TerminalView grid, GlyphSource glyphSource) {
        requireNotDisposed();
        if (grid == null || glyphSource == null) {
            throw new IllegalArgumentException("grid and glyphSource must not be null");
        }
        if (index < 0 || index > layers.size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + layers.size);
        }
        LayerRenderer renderer = rendererFactory.create(grid, glyphSource, this::sharedResources);
        Layer layer = new Layer(this, grid, glyphSource, renderer);
        layers.insert(index, layer);
        return layer;
    }

    private GridRenderResources sharedResources() {
        if (sharedResources == null) {
            sharedResources = new GridRenderResources();
        }
        return sharedResources;
    }

    /**
     * Removes a layer and disposes its renderer.
     *
     * @throws IllegalStateException if the layer was already removed
     * @throws IllegalArgumentException if the layer belongs to another stack
     */
    public void remove(Layer layer) {
        requireOwned(layer);
        layers.removeValue(layer, true);
        layer.detach();
        layer.getRenderer().dispose();
    }

    /**
     * Moves a layer to a position; the other layers keep their relative order.
     *
     * @param index new position from 0 (bottom) to {@link #size()} - 1 (top)
     * @throws IndexOutOfBoundsException if the index is outside 0 to size - 1
     * @throws IllegalStateException if the layer was removed
     * @throws IllegalArgumentException if the layer belongs to another stack
     */
    public void moveTo(Layer layer, int index) {
        requireOwned(layer);
        if (index < 0 || index >= layers.size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + layers.size);
        }
        layers.removeValue(layer, true);
        layers.insert(index, layer);
    }

    /** Returns the number of layers. */
    public int size() {
        return layers.size;
    }

    /**
     * Returns the layer at a position.
     *
     * @param index from 0 (bottom) to {@link #size()} - 1 (top)
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Layer get(int index) {
        if (index < 0 || index >= layers.size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + layers.size);
        }
        return layers.get(index);
    }

    /** Returns the position of a layer, or -1 if it is not in this stack. */
    public int indexOf(Layer layer) {
        return layers.indexOf(layer, true);
    }

    /**
     * Sets the camera position in virtual pixels, in the same space as layer offsets (y grows
     * downward). Each layer shifts by minus the camera times its parallax factor (see
     * {@link Layer#setParallax(float, float)}). Default (0, 0).
     *
     * <p>The clip does not move with the camera: it stays in stack space.
     *
     * <p>Pixel snapping rounds each layer's corner on its own, so layers with different parallax
     * factors can be 1 pixel apart during slow pans. Turn snapping off for smooth motion.
     *
     * @throws IllegalArgumentException if x or y is not finite
     */
    public void setCamera(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("camera must be finite: " + x + ", " + y);
        }
        cameraX = x;
        cameraY = y;
    }

    /** Returns the camera's x in virtual pixels, 0 by default. */
    public float getCameraX() {
        return cameraX;
    }

    /** Returns the camera's y in virtual pixels, growing downward, 0 by default. */
    public float getCameraY() {
        return cameraY;
    }

    /**
     * Sets whether final layer positions are rounded to whole pixels. Default true.
     */
    public void setPixelSnapping(boolean pixelSnapping) {
        this.pixelSnapping = pixelSnapping;
    }

    /** Returns whether final layer positions are rounded to whole pixels. */
    public boolean isPixelSnapping() {
        return pixelSnapping;
    }

    /**
     * Draws every visible layer from bottom to top. The batch must already be
     * begun; this method never begins or ends it.
     *
     * <p>Every visible layer is drawn on every call, in stack order, whether or not
     * its grid changed. Dirty tracking only decides whether a layer's GPU data is
     * re-uploaded inside its renderer.
     *
     * <p>With pixel snapping on, or when a layer uses {@link ZoomMode#WHOLE}, the screen
     * position of each layer's top-left corner, after zoom and rotation, is rounded with
     * {@code floor(v + 0.5)}. Zoom and rotation are applied about the layer's pivot; see
     * {@link Layer#setPivot}. The camera and each layer's parallax shift the offset first; see
     * {@link #setCamera}.
     *
     * <p>A clipped layer (see {@link Layer#setClip}) flushes the batch, draws inside a
     * scissor, and flushes again, so each clipped layer costs an extra draw call. The
     * scissor is intersected with any {@code ScissorStack} scissor the caller has
     * pushed; if nothing is left, the layer is skipped. Scissor edges are rounded
     * outward to whole backbuffer pixels, then to whole logical pixels on HDPI
     * displays.
     *
     * <p>Offscreen fade: a layer whose tint alpha is below 1 and that has at least one visible
     * free sprite or lifted cell is drawn at full alpha into a shared offscreen target, then
     * composited with its alpha, so the grid, sprites, and lifted cells fade as one image. This
     * costs one offscreen target switch per such layer per frame. The layer's own clip still
     * applies. The batch stays begun with its shader bound. The composite does not use the
     * batch's blend functions; it always blends premultiplied. Do not draw while another
     * framebuffer is bound. To skip the offscreen pass, keep the tint alpha at 1. Other layers draw
     * directly.
     *
     * @param batch the active batch
     * @param x screen x of the stack's top-left corner
     * @param y screen y of the stack's top-left corner (y grows upward)
     */
    public void draw(SpriteBatch batch, float x, float y) {
        viewportQueried = false;
        for (int i = 0; i < layers.size; i++) {
            Layer layer = layers.get(i);
            if (!layer.isVisible()) {
                continue;
            }
            composeLayerTransform(layer, x, y, layerTransform);
            if (!layer.hasClip()) {
                drawLayer(layer, batch);
                continue;
            }
            queryViewportOnce(batch);
            resolveClipRect(layer);
            if (!pushScissor(batch, x, y, clipRect)) {
                continue;
            }
            drawLayer(layer, batch);
            clipBackend.flush(batch);
            clipBackend.pop();
        }
    }

    /**
     * Finds which cell of a layer's grid lies under a point, using the same transform
     * {@link #draw} uses: offset shifted by camera and parallax, zoom, rotation about the
     * pivot, and corner snapping.
     *
     * <p>This takes two steps. The caller first turns a window pixel into batch world space
     * with its viewport, for example {@code viewport.unproject}, then passes that point here
     * with the same stack position it gives to {@code draw}.
     *
     * <p>The result is always written to {@code out}, even outside the grid, so the column and
     * row can be negative or at or past the grid size. Check {@code grid.contains(column, row)}
     * before using them. A point exactly on a cell border belongs to the cell to its right or
     * below it. Visibility and clip are ignored; use {@link #isShownAt} for those.
     *
     * @param layer a layer of this stack
     * @param stackX the x passed to {@link #draw}
     * @param stackY the y passed to {@link #draw} (y grows upward)
     * @param worldX x of the point in batch world space
     * @param worldY y of the point in batch world space
     * @param out receives the column in x and the row in y, row 0 at the top
     * @throws IllegalArgumentException if the layer belongs to another stack or out is null
     * @throws IllegalStateException if the layer was removed
     */
    public void cellAt(Layer layer, float stackX, float stackY, float worldX, float worldY, GridPoint2 out) {
        requireOwned(layer);
        if (out == null) {
            throw new IllegalArgumentException("out must not be null");
        }
        composeLayerTransform(layer, stackX, stackY, inverseTransform);
        inverseTransform.inv();
        LayerTransform.worldToLayerPixels(inverseTransform, worldX, worldY, layerPixel);
        GlyphSource source = layer.getGlyphSource();
        out.x = (int) Math.floor(layerPixel.x / source.getCellWidth());
        out.y = (int) Math.floor(layerPixel.y / source.getCellHeight());
    }

    /**
     * Tells whether a layer is visible and a point is inside its clip window. A layer without a
     * clip counts as inside everywhere. The clip is in stack space, from the stack's top-left
     * corner with y down, and is not zoomed or rotated. The right and bottom edges are outside.
     * With pixel snapping on, the edges are rounded to whole pixels, as in {@link #draw}.
     *
     * <p>Tint alpha and other layers covering this one are ignored; the game decides those.
     *
     * @param layer a layer of this stack
     * @param stackX the x passed to {@link #draw}
     * @param stackY the y passed to {@link #draw} (y grows upward)
     * @param worldX x of the point in batch world space
     * @param worldY y of the point in batch world space
     * @throws IllegalArgumentException if the layer belongs to another stack
     * @throws IllegalStateException if the layer was removed, or is clipped to the stack while
     *     the stack size is unset
     */
    public boolean isShownAt(Layer layer, float stackX, float stackY, float worldX, float worldY) {
        requireOwned(layer);
        if (!layer.isVisible()) {
            return false;
        }
        if (!layer.hasClip()) {
            return true;
        }
        resolveClipRect(layer);
        ClipMath.stackToWorld(stackX, stackY, clipRect.x, clipRect.y, clipRect.width, clipRect.height,
                pixelSnapping, worldRect);
        return isInsideClipWindow(worldRect, worldX, worldY);
    }

    /**
     * Rectangle.contains includes every edge, but a clip window keeps its top and left edges
     * and drops its right and bottom ones. World y grows up, so the bottom edge is rect.y.
     */
    private static boolean isInsideClipWindow(Rectangle world, float worldX, float worldY) {
        return worldX >= world.x && worldX < world.x + world.width
                && worldY > world.y && worldY <= world.y + world.height;
    }

    private void resolveClipRect(Layer layer) {
        if (!layer.isClippedToStack()) {
            clipRect.set(layer.getClipX(), layer.getClipY(), layer.getClipWidth(), layer.getClipHeight());
            return;
        }
        if (!hasSize()) {
            throw new IllegalStateException("Layer is clipped to the stack but the stack size is not set");
        }
        clipRect.set(0f, 0f, width, height);
    }

    private void composeLayerTransform(Layer layer, float stackX, float stackY, Affine2 out) {
        float effectiveOffsetX = layer.getOffsetX() - cameraX * layer.getParallaxX();
        float effectiveOffsetY = layer.getOffsetY() - cameraY * layer.getParallaxY();
        float screenX = stackX + effectiveOffsetX;
        float screenY = stackY - effectiveOffsetY;
        boolean snapCorner = pixelSnapping || layer.getZoomMode() == ZoomMode.WHOLE;
        LayerTransform.compose(screenX, screenY, layer.getPivotX(), layer.getPivotY(),
                layer.getEffectiveZoom(), layer.getRotation(), snapCorner, out);
    }

    private void queryViewportOnce(SpriteBatch batch) {
        if (viewportQueried) {
            return;
        }
        clipBackend.getCombinedMatrix(batch, clipMatrix);
        clipBackend.getViewport(viewport);
        viewportQueried = true;
    }

    /** Flushes the batch and pushes a stack-space rectangle as a scissor; false if nothing is left of it. */
    private boolean pushScissor(SpriteBatch batch, float stackX, float stackY, Rectangle stackRect) {
        ClipMath.stackToWorld(stackX, stackY, stackRect.x, stackRect.y, stackRect.width, stackRect.height,
                pixelSnapping, worldRect);
        ClipMath.worldToWindow(worldRect, clipMatrix, viewport[0], viewport[1], viewport[2], viewport[3],
                clipCorner, windowRect);
        clipBackend.flush(batch);
        return clipBackend.push(windowRect);
    }

    private void drawLayer(Layer layer, SpriteBatch batch) {
        layer.syncCellTransforms();
        LayerEffects effects = layer.getEffects();
        if (OffscreenFade.isNeeded(effects.getTintA(), layer.getSprites(), layer.getCellTransforms())) {
            drawFadedLayer(layer, batch);
            return;
        }
        layer.getRenderer().draw(batch, layerTransform, effects, layer.getSprites(), pixelSnapping);
    }

    /**
     * Draws the layer at full alpha into the shared offscreen target, then fades that image onto
     * the screen in one go, so overlapping cells and sprites do not show through each other.
     */
    // The pass is deliberately not scissored to the layer's bounds: measured on a phone GPU, that
    // scissor added about 2 ms per frame and saved nothing (2026-11-13). Only the layer's own clip
    // applies.
    private void drawFadedLayer(Layer layer, SpriteBatch batch) {
        queryViewportOnce(batch);
        LayerEffects effects = layer.getEffects();
        opaqueEffects.copyFrom(effects);
        opaqueEffects.setTintAlpha(1f);
        offscreenBackend.begin(batch, viewport);
        layer.getRenderer().drawPremultiplied(batch, layerTransform, opaqueEffects, layer.getSprites(),
                pixelSnapping);
        offscreenBackend.end(viewport);
        offscreenBackend.composite(batch, effects.getTintA(), viewport);
    }

    /**
     * Disposes every layer's renderer and empties the stack. Removed layers can no
     * longer be used. Calling it again does nothing.
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int i = layers.size - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            layer.detach();
            layer.getRenderer().dispose();
        }
        layers.clear();
        offscreenBackend.dispose();
        if (sharedResources != null) {
            sharedResources.dispose();
            sharedResources = null;
        }
    }

    private void requireNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("LayerStack was disposed");
        }
    }

    private void requireOwned(Layer layer) {
        if (layer == null) {
            throw new IllegalArgumentException("layer must not be null");
        }
        if (layer.getOwner() == null) {
            throw new IllegalStateException("Layer was removed from its stack");
        }
        if (layer.getOwner() != this) {
            throw new IllegalArgumentException("Layer belongs to another stack");
        }
    }
}
