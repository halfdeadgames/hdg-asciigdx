package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
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
    private final Rectangle worldRect = new Rectangle();
    private final Rectangle windowRect = new Rectangle();
    private final int[] viewport = new int[4];
    private float width;
    private float height;
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
     * <p>With pixel snapping on, each layer's final screen position is rounded with
     * {@code floor(v + 0.5)}.
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
            float screenX = x + layer.getOffsetX();
            float screenY = y - layer.getOffsetY();
            if (pixelSnapping) {
                screenX = ClipMath.snap(screenX);
                screenY = ClipMath.snap(screenY);
            }
            if (!layer.hasClip()) {
                drawLayer(layer, batch, screenX, screenY);
                continue;
            }
            queryViewportOnce(batch);
            float clipX = layer.getClipX();
            float clipY = layer.getClipY();
            float clipWidth = layer.getClipWidth();
            float clipHeight = layer.getClipHeight();
            if (layer.isClippedToStack()) {
                if (!hasSize()) {
                    throw new IllegalStateException("Layer is clipped to the stack but the stack size is not set");
                }
                clipX = 0f;
                clipY = 0f;
                clipWidth = width;
                clipHeight = height;
            }
            if (!pushScissor(batch, x, y, clipX, clipY, clipWidth, clipHeight)) {
                continue;
            }
            drawLayer(layer, batch, screenX, screenY);
            clipBackend.flush(batch);
            clipBackend.pop();
        }
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
    private boolean pushScissor(SpriteBatch batch, float stackX, float stackY,
            float rectX, float rectY, float rectWidth, float rectHeight) {
        ClipMath.stackToWorld(stackX, stackY, rectX, rectY, rectWidth, rectHeight, pixelSnapping, worldRect);
        ClipMath.worldToWindow(worldRect, clipMatrix, viewport[0], viewport[1], viewport[2], viewport[3],
                clipCorner, windowRect);
        clipBackend.flush(batch);
        return clipBackend.push(windowRect);
    }

    private void drawLayer(Layer layer, SpriteBatch batch, float screenX, float screenY) {
        layer.syncCellTransforms();
        LayerEffects effects = layer.getEffects();
        if (OffscreenFade.isNeeded(effects.getTintA(), layer.getSprites(), layer.getCellTransforms())) {
            drawFadedLayer(layer, batch, screenX, screenY);
            return;
        }
        layer.getRenderer().draw(batch, screenX, screenY, effects, layer.getSprites(), pixelSnapping);
    }

    /**
     * Draws the layer at full alpha into the shared offscreen target, then fades that image onto
     * the screen in one go, so overlapping cells and sprites do not show through each other.
     */
    // The pass is deliberately not scissored to the layer's bounds: measured on a phone GPU, that
    // scissor added about 2 ms per frame and saved nothing (2026-11-13). Only the layer's own clip
    // applies.
    private void drawFadedLayer(Layer layer, SpriteBatch batch, float screenX, float screenY) {
        queryViewportOnce(batch);
        LayerEffects effects = layer.getEffects();
        opaqueEffects.copyFrom(effects);
        opaqueEffects.setTintAlpha(1f);
        offscreenBackend.begin(batch, viewport);
        layer.getRenderer().drawPremultiplied(batch, screenX, screenY, opaqueEffects, layer.getSprites(),
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
