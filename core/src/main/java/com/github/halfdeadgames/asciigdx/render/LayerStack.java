package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;

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
 *
 * <p>A layer can be placed in a {@link Panel} made by {@link #addPanel}: its offset and clip are then
 * measured from the panel's top-left corner, and it is clipped to the panel's rectangle.
 */
public final class LayerStack implements Disposable {

    private final LayerRendererFactory rendererFactory;
    private final LayerClipper clipper;
    private final OffscreenBackend offscreenBackend;
    private final ResolvedEffects resolvedEffects = new ResolvedEffects();
    private final Array<Layer> layers = new Array<>();
    private final Array<Panel> panels = new Array<>();
    private final Array<LayerGroup> groups = new Array<>();
    private final Affine2 layerTransform = new Affine2();
    private final Affine2 inverseTransform = new Affine2();
    private final Affine2 pointTransform = new Affine2();
    private final Vector2 layerPixel = new Vector2();
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

    LayerStack(LayerRendererFactory rendererFactory, ClipBackend clipBackend, OffscreenBackend offscreenBackend) {
        this.rendererFactory = rendererFactory;
        this.clipper = new LayerClipper(clipBackend);
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
     * @param grid the grid to draw; not null. The caller writes its cells, and the layer may
     *     scroll it (see {@link Layer#scroll} and {@link Layer#enableMapWindow()}). The layer does
     *     not own or dispose it
     * @param glyphSource supplies glyphs and the cell size; not null. The layer does not own or
     *     dispose it
     * @return the new layer
     * @throws IllegalArgumentException if the grid or glyph source is null
     * @throws IllegalStateException if the stack was disposed
     */
    public Layer add(TerminalGrid grid, GlyphSource glyphSource) {
        return insert(layers.size, grid, glyphSource);
    }

    /**
     * Inserts a layer at a position; layers at and above it move up by one.
     *
     * @param index position from 0 (bottom) to {@link #size()} (top)
     * @param grid the grid to draw; not null. The caller writes its cells, and the layer may
     *     scroll it (see {@link Layer#scroll} and {@link Layer#enableMapWindow()}). The layer does
     *     not own or dispose it
     * @param glyphSource supplies glyphs and the cell size; not null. The layer does not own or
     *     dispose it
     * @return the new layer
     * @throws IllegalArgumentException if the grid or glyph source is null
     * @throws IndexOutOfBoundsException if the index is outside 0 to size
     * @throws IllegalStateException if the stack was disposed
     */
    public Layer insert(int index, TerminalGrid grid, GlyphSource glyphSource) {
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
        Guards.requireIndexInRange(index, layers.size);
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
        Guards.requireIndexInRange(index, layers.size);
        return layers.get(index);
    }

    /**
     * Creates a panel, a rectangle layers can be placed in with {@link Layer#setPanel}.
     *
     * @param x left edge in virtual pixels from the stack's left edge
     * @param y top edge in virtual pixels from the stack's top edge, growing downward
     * @param width width in virtual pixels
     * @param height height in virtual pixels
     * @return the new panel
     * @throws IllegalArgumentException if a value is not finite, or width or height is negative
     * @throws IllegalStateException if the stack was disposed
     */
    public Panel addPanel(float x, float y, float width, float height) {
        requireNotDisposed();
        Panel panel = new Panel(this, x, y, width, height);
        panels.add(panel);
        return panel;
    }

    /**
     * Creates a panel measured in whole cells of a glyph source, stored in pixels as the cell counts
     * times the source's cell size. The source is not kept, so a later change of font does not
     * resize the panel.
     *
     * @param column cells from the stack's left edge; may be negative
     * @param row cells from the stack's top edge, growing downward; may be negative
     * @param widthInCells width in cells
     * @param heightInCells height in cells
     * @param cellSource supplies the cell size
     * @return the new panel
     * @throws IllegalArgumentException if the source is null, or widthInCells or heightInCells is negative
     * @throws IllegalStateException if the stack was disposed
     */
    public Panel addPanelInCells(int column, int row, int widthInCells, int heightInCells,
            GlyphSource cellSource) {
        if (cellSource == null) {
            throw new IllegalArgumentException("cellSource must not be null");
        }
        float cellWidth = cellSource.getCellWidth();
        float cellHeight = cellSource.getCellHeight();
        return addPanel(column * cellWidth, row * cellHeight, widthInCells * cellWidth,
                heightInCells * cellHeight);
    }

    /**
     * Removes a panel. The panel stays readable but can no longer be changed.
     *
     * @throws IllegalArgumentException if the panel is null or belongs to another stack
     * @throws IllegalStateException if the panel was already removed, or a layer of this stack is
     *     still placed in it
     */
    public void removePanel(Panel panel) {
        requirePanelOwned(panel);
        for (int i = 0; i < layers.size; i++) {
            if (layers.get(i).getPanel() == panel) {
                throw new IllegalStateException("Panel still has layers; move them out with setPanel(null) first");
            }
        }
        panels.removeValue(panel, true);
        panel.detach();
    }

    /**
     * Creates a group, a set of layers that share color effects on top of their own; see
     * {@link LayerGroup}.
     *
     * @return the new, empty group
     * @throws IllegalStateException if the stack was disposed
     */
    public LayerGroup addGroup() {
        requireNotDisposed();
        LayerGroup group = new LayerGroup(this);
        groups.add(group);
        return group;
    }

    /**
     * Removes a group. Its member layers leave it and keep their own effects. The group stays
     * readable but can no longer be changed.
     *
     * @throws IllegalArgumentException if the group is null or belongs to another stack
     * @throws IllegalStateException if the group was already removed
     */
    public void removeGroup(LayerGroup group) {
        requireGroupOwned(group);
        groups.removeValue(group, true);
        orphanChildrenOf(group);
        group.detach();
    }

    /**
     * Leaves the removed group's children with no parent rather than moving them up to its parent,
     * so no group picks up an ancestor it was never given.
     */
    private void orphanChildrenOf(LayerGroup removed) {
        for (int i = 0; i < groups.size; i++) {
            LayerGroup candidate = groups.get(i);
            if (candidate.getParent() == removed) {
                candidate.clearParent();
            }
        }
    }

    private void requireGroupOwned(LayerGroup group) {
        Guards.requireNotNull(group, "group");
        Guards.requireOwnedBy(group.getOwner(), this, "LayerGroup", "stack");
    }

    private void requirePanelOwned(Panel panel) {
        Guards.requireNotNull(panel, "panel");
        Guards.requireOwnedBy(panel.getOwner(), this, "Panel", "stack");
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
     * <p>Layers in a {@link Panel} ignore this camera and follow the panel's own camera instead; see
     * {@link Panel#setCamera}.
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
        followCameraOfLayersIn(null);
    }

    /** Re-follows the camera for map window layers that are in {@code panel} (null: in no panel). */
    void followCameraOfLayersIn(Panel panel) {
        for (int i = 0; i < layers.size; i++) {
            Layer layer = layers.get(i);
            if (layer.getPanel() == panel) {
                layer.followCamera();
            }
        }
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
     * {@link Layer#setPivot}. The camera (the panel's camera for a panel member) and each
     * layer's parallax shift the offset first; see {@link #setCamera}.
     *
     * <p>A clipped layer (see {@link Layer#setClip}), or a member of a {@link Panel}, flushes the
     * batch, draws inside a scissor, and flushes again, so each such layer costs an extra draw
     * call. A panel member is clipped to the panel's rectangle intersected with its own clip, which
     * is measured from the panel's corner; if that is empty, the layer is skipped without a flush.
     * The scissor is intersected with any {@code ScissorStack} scissor the caller has
     * pushed; if nothing is left, the layer is skipped. Scissor edges are rounded
     * outward to whole backbuffer pixels, then to whole logical pixels on HDPI
     * displays.
     *
     * <p>A layer's effective alpha is its tint alpha times the alpha of every {@link LayerGroup}
     * it belongs to, and of every ancestor group of those (the parents, grandparents, and so on); a
     * group reached more than once counts once. A group with alpha 0 anywhere in a layer's chain
     * makes the effective alpha 0. A hidden layer, or a layer whose effective alpha is 0, is
     * skipped entirely: no draw, no clip work, and no offscreen pass.
     *
     * <p>Offscreen fade: a layer whose effective alpha is below 1 and that has at least one drawn
     * free sprite (visible, color alpha above 0) or lifted cell is drawn at full alpha into a
     * shared offscreen target, then composited with its alpha, so the grid, sprites, and lifted cells fade as one image. This
     * costs one offscreen target switch per such layer per frame. The layer's own clip still
     * applies. The batch stays begun with its shader bound. The composite does not use the
     * batch's blend functions; it always blends premultiplied. Do not draw while another
     * framebuffer is bound. To skip the offscreen pass, keep the effective alpha at 1. Other layers draw
     * directly.
     *
     * @param batch the active batch
     * @param x screen x of the stack's top-left corner
     * @param y screen y of the stack's top-left corner (y grows upward)
     */
    public void draw(SpriteBatch batch, float x, float y) {
        clipper.beginFrame();
        for (int i = 0; i < layers.size; i++) {
            Layer layer = layers.get(i);
            if (!layer.isVisible()) {
                continue;
            }
            resolvedEffects.resolve(layer.getEffects(), layer.getGroups());
            if (resolvedEffects.getAlpha() <= 0f) {
                continue;
            }
            LayerPlacement.compose(layer, x, y, pixelSnapping, layerTransform);
            if (!LayerClipper.isClipped(layer)) {
                drawLayer(layer, batch);
                continue;
            }
            clipper.queryViewportOnce(batch);
            clipper.resolve(layer, width, height, hasSize());
            if (layer.getPanel() != null && clipper.isEmpty()) {
                continue;
            }
            clipper.pushScissor(batch, x, y, pixelSnapping);
            if (!clipper.isScissorActive()) {
                continue;
            }
            drawLayer(layer, batch);
            clipper.popScissor(batch);
        }
    }

    /**
     * Finds which cell of a layer's grid lies under a point, using the same transform
     * {@link #draw} uses: offset (plus the panel's corner for a panel member) shifted by camera
     * and parallax, zoom, rotation about the pivot, and corner snapping.
     *
     * <p>This takes two steps. The caller first turns a window pixel into batch world space
     * with its viewport, for example {@code viewport.unproject}, then passes that point here
     * with the same stack position it gives to {@code draw}.
     *
     * <p>The result is always written to {@code out}, even outside the grid, so the column and
     * row can be negative or at or past the grid size. Check {@code grid.contains(column, row)}
     * before using them. A point exactly on a cell border belongs to the cell to its right or
     * below it. Visibility, the pickable flag, and clip are ignored; use {@link #isPickableAt}
     * for those.
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
        layerPointAt(layer, stackX, stackY, worldX, worldY, layerPixel);
        GlyphSource source = layer.getGlyphSource();
        out.x = (int) Math.floor(layerPixel.x / source.getCellWidth());
        out.y = (int) Math.floor(layerPixel.y / source.getCellHeight());
    }

    /**
     * Maps a point in batch world space to layer pixels, using the same transform
     * {@link #draw} uses: offset shifted by camera and parallax, zoom, rotation about the
     * pivot, and corner snapping. The result is measured from the top-left corner of the layer's
     * grid, x right and y down, before zoom and rotation. It uses the same units as sprite
     * positions and {@link Layer#setPivot}.
     *
     * <p>This takes two steps. The caller first turns a window pixel into batch world space
     * with its viewport, for example {@code viewport.unproject}, then passes that point here
     * with the same stack position it gives to {@code draw}.
     *
     * <p>The result is always written to {@code out}, even outside the grid. It matches what is
     * drawn, so with corner snapping on it can step by up to half a pixel. It describes the
     * layer's current state only.
     *
     * @param layer a layer of this stack
     * @param stackX the x passed to {@link #draw}
     * @param stackY the y passed to {@link #draw} (y grows upward)
     * @param worldX x of the point in batch world space
     * @param worldY y of the point in batch world space
     * @param out receives the point in layer pixels
     * @throws IllegalArgumentException if the layer belongs to another stack or out is null
     * @throws IllegalStateException if the layer was removed
     */
    public void layerPointAt(Layer layer, float stackX, float stackY, float worldX, float worldY, Vector2 out) {
        requireOutAndOwned(layer, out);
        LayerPlacement.compose(layer, stackX, stackY, pixelSnapping, inverseTransform);
        inverseTransform.inv();
        LayerTransform.worldToLayerPixels(inverseTransform, worldX, worldY, out);
    }

    /**
     * Maps a point in layer pixels to batch world space, the inverse of {@link #layerPointAt}.
     * It uses the same transform {@link #draw} uses: offset shifted by camera and parallax,
     * zoom, rotation about the pivot, and corner snapping. The layer point is measured from the
     * top-left corner of the layer's grid, x right and y down, before zoom and rotation.
     *
     * <p>The caller passes the same stack position it gives to {@code draw}. The result is in
     * batch world space with y up, so the caller can pass it to {@code viewport.project} to get
     * a window pixel.
     *
     * <p>The result is always written to {@code out}, even outside the grid. It matches what is
     * drawn, so with corner snapping on it can step by up to half a pixel. It describes the
     * layer's current state only.
     *
     * @param layer a layer of this stack
     * @param stackX the x passed to {@link #draw}
     * @param stackY the y passed to {@link #draw} (y grows upward)
     * @param layerX x of the point in layer pixels
     * @param layerY y of the point in layer pixels (y grows downward)
     * @param out receives the point in batch world space
     * @throws IllegalArgumentException if the layer belongs to another stack or out is null
     * @throws IllegalStateException if the layer was removed
     */
    public void worldPointOf(Layer layer, float stackX, float stackY, float layerX, float layerY, Vector2 out) {
        requireOutAndOwned(layer, out);
        LayerPlacement.compose(layer, stackX, stackY, pixelSnapping, pointTransform);
        LayerTransform.layerPixelsToWorld(pointTransform, layerX, layerY, out);
    }

    private void requireOutAndOwned(Layer layer, Vector2 out) {
        requireOwned(layer);
        if (out == null) {
            throw new IllegalArgumentException("out must not be null");
        }
    }

    /**
     * Tells whether a layer is visible, pickable, and a point is inside its clip window. A layer
     * without a clip and without a panel counts as inside everywhere. The window is in stack space,
     * from the stack's top-left corner with y down, and is not zoomed or rotated. For a panel
     * member it is the panel's rectangle intersected with the layer's clip, and an empty window
     * never matches. The right and bottom edges are outside.
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
    public boolean isPickableAt(Layer layer, float stackX, float stackY, float worldX, float worldY) {
        requireOwned(layer);
        if (!layer.isVisible()) {
            return false;
        }
        if (!layer.isPickable()) {
            return false;
        }
        if (!LayerClipper.isClipped(layer)) {
            return true;
        }
        clipper.resolve(layer, width, height, hasSize());
        if (clipper.isEmpty()) {
            return false;
        }
        return clipper.isInside(stackX, stackY, worldX, worldY, pixelSnapping);
    }

    private void drawLayer(Layer layer, SpriteBatch batch) {
        layer.syncCellHandles();
        if (OffscreenFade.isNeeded(resolvedEffects.getAlpha(), layer.getSprites(), layer.getCellHandles())) {
            drawFadedLayer(layer, batch);
            return;
        }
        layer.getRenderer().draw(batch, layerTransform, resolvedEffects, layer.getSprites(), pixelSnapping);
    }

    /**
     * Draws the layer at full alpha into the shared offscreen target, then fades that image onto
     * the screen in one go, so overlapping cells and sprites do not show through each other.
     */
    // The pass is deliberately not scissored to the layer's bounds: measured on a phone GPU, that
    // scissor added about 2 ms per frame and saved nothing (2026-11-13). Only the layer's own clip
    // applies.
    private void drawFadedLayer(Layer layer, SpriteBatch batch) {
        clipper.queryViewportOnce(batch);
        int[] viewport = clipper.getViewport();
        float fadeAlpha = resolvedEffects.getAlpha();
        resolvedEffects.setAlpha(1f);
        offscreenBackend.begin(batch, viewport);
        layer.getRenderer().drawPremultiplied(batch, layerTransform, resolvedEffects, layer.getSprites(),
                pixelSnapping);
        offscreenBackend.end(viewport);
        offscreenBackend.composite(batch, fadeAlpha, viewport);
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
        for (int i = panels.size - 1; i >= 0; i--) {
            panels.get(i).detach();
        }
        panels.clear();
        for (int i = groups.size - 1; i >= 0; i--) {
            groups.get(i).detach();
        }
        groups.clear();
        offscreenBackend.dispose();
        if (sharedResources != null) {
            sharedResources.dispose();
            sharedResources = null;
        }
    }

    /**
     * Call once at the start of each update, before applying cell effects. On every layer, the
     * frame handles ({@link Layer#frameCell}) are swept: handles that stayed idle since the last
     * call add the delta to their unused time, and handles that were used start over at zero.
     * Handles unused for at least {@link Layer#getFrameReleaseSeconds()} are released, and the
     * rest are reset. Lasting handles ({@link Layer#cell}) are never touched. A zero, negative,
     * NaN, or infinite delta adds no unused time, but the reset still happens.
     *
     * @throws IllegalStateException if the stack was disposed
     */
    public void beginCellEffects(float deltaSeconds) {
        requireNotDisposed();
        for (int i = 0; i < layers.size; i++) {
            layers.get(i).sweepCellHandles(deltaSeconds);
        }
    }

    private void requireNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("LayerStack was disposed");
        }
    }

    void requireOwned(Layer layer) {
        Guards.requireNotNull(layer, "layer");
        Guards.requireOwnedBy(layer.getOwner(), this, "Layer", "stack");
    }
}
