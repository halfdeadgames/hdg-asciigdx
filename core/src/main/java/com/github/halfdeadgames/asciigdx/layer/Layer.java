package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * One grid in a {@link LayerStack}, with an offset, a parallax factor, a zoom, a rotation, a visibility flag,
 * color effects (tint, mix, saturation), a clip, and a switchable glyph source. Created only by the stack.
 *
 * <p>The offset is in virtual pixels, measured from the stack's top-left corner, or from the
 * top-left corner of the layer's {@link Panel} if it has one (see {@link #setPanel}),
 * with y growing downward like grid rows. Once the layer is removed from its
 * stack, getters keep working but every method that changes or uses the layer
 * throws {@link IllegalStateException}.
 */
public final class Layer {

    /** The most free sprites one layer can hold: 16383. */
    public static final int MAX_SPRITES = CellVertexLayout.MAX_QUADS_PER_MESH;

    private final TerminalGrid grid;
    private GlyphSource glyphSource;
    private final LayerRenderer renderer;
    private LayerStack owner;
    private float offsetX;
    private float offsetY;
    private float zoom = 1f;
    private float rotation;
    private float pivotX;
    private float pivotY;
    private ZoomMode zoomMode = ZoomMode.SMOOTH;
    private float parallaxX = 1f;
    private float parallaxY = 1f;
    private boolean visible = true;
    private boolean pickable = true;
    private final ColorEffects effects = new ColorEffects();
    private final Array<LayerGroup> groups = new Array<>(true, 2);
    private Panel panel;
    private MapWindow mapWindow;
    private final LayerClip clip = new LayerClip();
    private final Array<GlyphSprite> sprites = new Array<>();
    private final CellHandleSet cellHandleSet;
    private float frameReleaseSeconds = 1f;

    Layer(LayerStack owner, TerminalGrid grid, GlyphSource glyphSource, LayerRenderer renderer) {
        this.owner = owner;
        this.grid = grid;
        this.glyphSource = glyphSource;
        this.renderer = renderer;
        cellHandleSet = new CellHandleSet(this, grid, renderer);
    }

    /**
     * Returns the grid this layer draws, as a read-only view. The caller that passed the grid to
     * {@link LayerStack#add} keeps the writable {@link TerminalGrid}; the layer only scrolls it
     * (see {@link #scroll} and {@link #enableMapWindow()}).
     *
     * @return the same grid instance given when the layer was created; never null
     */
    public TerminalView getGrid() {
        return grid;
    }

    /** Returns the glyph source this layer draws with. */
    public GlyphSource getGlyphSource() {
        return glyphSource;
    }

    /**
     * Switches the layer to another glyph source, for example a different font size. Setting
     * the current source does nothing.
     *
     * <p>The grid keeps its contents. Its cell count is not changed; resizing the grid to the
     * new cell size (for example from a
     * {@link com.github.halfdeadgames.asciigdx.fit.FontFit}) is up to the caller. Glyphs
     * missing from the new source draw as its fallback glyph. The layer does not own the source and
     * never disposes it. The offset is not changed.
     *
     * <p>On a {@linkplain #enableMapWindow() map window}, changing the source changes the cell size
     * and with it the origin, so every cell is flagged as needing a refill.
     *
     * @throws IllegalArgumentException if the source is null
     * @throws IllegalStateException if the layer was removed
     */
    public void setGlyphSource(GlyphSource glyphSource) {
        requireAttached();
        if (glyphSource == null) {
            throw new IllegalArgumentException("glyphSource must not be null");
        }
        if (glyphSource == this.glyphSource) {
            return;
        }
        renderer.setGlyphSource(glyphSource);
        this.glyphSource = glyphSource;
        cellHandleSet.forgetHiddenFlags();
        followCamera();
        if (mapWindow != null) {
            // The origin changed unit, so surviving cells hold unrelated map cells.
            mapWindow.markAllNeedFill();
        }
    }

    /** Returns the horizontal offset in virtual pixels, growing to the right. */
    public float getOffsetX() {
        return offsetX;
    }

    /** Returns the vertical offset in virtual pixels, growing downward. */
    public float getOffsetY() {
        return offsetY;
    }

    /** Returns whether the layer is shown. A shown layer with tint alpha 0 is still skipped when drawing. */
    public boolean isVisible() {
        return visible;
    }

    /** Returns whether the stack's pick test can hit the layer. */
    public boolean isPickable() {
        return pickable;
    }

    /** Returns the panel this layer is placed in, or null if it is measured from the stack's corner. */
    public Panel getPanel() {
        return panel;
    }

    /**
     * Places the layer in a panel, or takes it out with null. A member layer's offset is measured
     * from the panel's top-left corner, and the layer is clipped to the panel's rectangle; see
     * {@link #setClip} and {@link #clipToStack} for how its own clip combines with that.
     *
     * <p>The offset value is not changed, so the layer moves on screen by the panel's corner (or
     * back by it when leaving a panel). Setting the current panel does nothing.
     *
     * <p>A member layer follows the panel's camera instead of the stack camera, scaled by its
     * parallax; see {@link Panel#setCamera}.
     *
     * @param panel a panel of this layer's stack, or null to leave the panel
     * @throws IllegalArgumentException if the panel belongs to another stack
     * @throws IllegalStateException if the layer or the panel was removed
     */
    public void setPanel(Panel panel) {
        LayerStack stack = requireAttached();
        if (panel != null) {
            Guards.requireOwnedBy(panel.getOwner(), stack, "Panel", "stack");
        }
        this.panel = panel;
        followCamera();
    }

    /**
     * Sets the offset from the stack's top-left corner, or from the panel's top-left corner for a
     * member of a {@link Panel}. The stack camera, or the panel's camera for a member, is applied on
     * top; see {@link LayerStack#setCamera} and {@link Panel#setCamera}.
     *
     * @param x virtual pixels to the right
     * @param y virtual pixels downward
     * @throws IllegalStateException if the layer was removed
     */
    public void setOffset(float x, float y) {
        requireAttached();
        offsetX = x;
        offsetY = y;
    }

    /** Returns the horizontal parallax factor, 1 by default. */
    public float getParallaxX() {
        return parallaxX;
    }

    /** Returns the vertical parallax factor, 1 by default. */
    public float getParallaxY() {
        return parallaxY;
    }

    /**
     * Sets how much the camera moves this layer per axis: the stack camera, or the panel's camera
     * for a panel member. 1 moves fully with the camera
     * (the default), 0 stays fixed on screen (a HUD), values between 0 and 1 suit distant
     * backgrounds, and values above 1 suit near foregrounds. Negative values are allowed.
     *
     * @throws IllegalArgumentException if a factor is not finite
     * @throws IllegalStateException if the layer was removed
     */
    public void setParallax(float factorX, float factorY) {
        requireAttached();
        if (!Float.isFinite(factorX) || !Float.isFinite(factorY)) {
            throw new IllegalArgumentException("parallax must be finite: " + factorX + ", " + factorY);
        }
        parallaxX = factorX;
        parallaxY = factorY;
        followCamera();
    }

    /**
     * Sets the same parallax factor on both axes.
     *
     * @see #setParallax(float, float)
     */
    public void setParallax(float factor) {
        setParallax(factor, factor);
    }

    /**
     * Sets the offset in whole cells of this layer's glyph source, equal to
     * {@code setOffset(column * cellWidth, row * cellHeight)}.
     *
     * @param column cells to the right
     * @param row cells downward
     * @throws IllegalStateException if the layer was removed
     */
    public void setCellOffset(int column, int row) {
        setOffset(column * glyphSource.getCellWidth(), row * glyphSource.getCellHeight());
    }

    /**
     * Shifts the layer relative to its current offset.
     *
     * @param dx virtual pixels to the right
     * @param dy virtual pixels downward
     * @throws IllegalStateException if the layer was removed
     */
    public void offsetBy(float dx, float dy) {
        setOffset(offsetX + dx, offsetY + dy);
    }

    /**
     * Shifts the layer by whole cells of this layer's glyph source, equal to
     * {@code offsetBy(columns * cellWidth, rows * cellHeight)}.
     *
     * @param columns cells to the right
     * @param rows cells downward
     * @throws IllegalStateException if the layer was removed
     */
    public void cellOffsetBy(int columns, int rows) {
        offsetBy(columns * glyphSource.getCellWidth(), rows * glyphSource.getCellHeight());
    }

    /** Returns the requested zoom, 1 by default. See {@link #getEffectiveZoom()} for the applied one. */
    public float getZoom() {
        return zoom;
    }

    /** Returns the zoom and rotation pivot's x in layer pixels from the layer's left edge; 0 by default. */
    public float getPivotX() {
        return pivotX;
    }

    /** Returns the zoom and rotation pivot's y in layer pixels from the layer's top edge, growing down; 0 by default. */
    public float getPivotY() {
        return pivotY;
    }

    /** Returns how the zoom is applied, {@link ZoomMode#SMOOTH} by default. */
    public ZoomMode getZoomMode() {
        return zoomMode;
    }

    /** Returns the zoom actually drawn: the requested zoom for SMOOTH, or a whole number of at least 1 for WHOLE. */
    public float getEffectiveZoom() {
        return LayerTransform.effectiveZoom(zoom, zoomMode);
    }

    /**
     * Scales the layer about its pivot. 1 is normal (the default).
     *
     * <p>The offset and the clip are not zoomed: moving the offset by 10 pixels moves the layer
     * 10 screen pixels at any zoom, and the clip stays in stack space.
     *
     * <p>With {@link ZoomMode#SMOOTH} and nearest filtering, a fractional zoom gives uneven pixel
     * widths. With {@link ZoomMode#WHOLE} the zoom is rounded to the nearest whole number, at least
     * 1, and the zoomed layer's top-left corner always snaps to a whole pixel.
     *
     * @param zoom the scale factor; must be above 0 and finite
     * @throws IllegalArgumentException if the zoom is not above 0 or not finite
     * @throws IllegalStateException if the layer was removed
     */
    public void setZoom(float zoom) {
        requireAttached();
        if (!(zoom > 0f) || Float.isInfinite(zoom)) {
            throw new IllegalArgumentException("zoom must be above 0 and finite: " + zoom);
        }
        this.zoom = zoom;
    }

    /** Returns the rotation in degrees, 0 by default; positive is clockwise on screen. */
    public float getRotation() {
        return rotation;
    }

    /**
     * Rotates the layer clockwise on screen about its pivot (see {@link #setPivot}), which stays
     * fixed on screen. 0 is normal (the default).
     *
     * <p>The pivot is shared with zoom. The offset and the clip do not rotate: the clip stays an
     * axis-aligned rectangle in stack space.
     *
     * <p>With nearest filtering, angles other than multiples of 90 look jagged. Pixel snapping and
     * {@link ZoomMode#WHOLE} only round the layer's corner, so pixels are crisp only at multiples
     * of 90. At 90 and 270 degrees, cells of tall fonts lie sideways.
     *
     * @param degrees the angle in degrees, positive is clockwise on screen; must be finite
     * @throws IllegalArgumentException if the angle is not finite
     * @throws IllegalStateException if the layer was removed
     */
    public void setRotation(float degrees) {
        requireAttached();
        if (Float.isNaN(degrees) || Float.isInfinite(degrees)) {
            throw new IllegalArgumentException("rotation must be finite: " + degrees);
        }
        this.rotation = degrees;
    }

    /**
     * Sets the point the layer zooms and rotates about, which stays fixed on screen. It moves with
     * the offset. In {@linkplain #enableMapWindow() map window} mode the pivot is measured in map
     * pixels, so it does not jump when the window scrolls. The default pivot (0, 0) is then the
     * map's top-left corner, so a game that zooms or rotates a map window should set the pivot to
     * the map point it wants to stay fixed, for example the player.
     *
     * @param x layer pixels from the layer's left edge
     * @param y layer pixels from the layer's top edge, growing down
     * @throws IllegalStateException if the layer was removed
     */
    public void setPivot(float x, float y) {
        requireAttached();
        pivotX = x;
        pivotY = y;
    }

    /**
     * Moves the pivot and adjusts the offset so the picture stays where it is drawn now. Placement
     * is kept for the zoom and rotation set at the time of the call. In map window mode the pivot
     * is in map pixels, as for {@link #setPivot}. Later changes to zoom or rotation
     * happen about the new pivot. {@link #setPivot} alone moves the picture when the layer is
     * zoomed or rotated. With pixel snapping or {@link ZoomMode#WHOLE}, the snapped corner can
     * still shift the picture by up to 1 pixel.
     *
     * @param x layer pixels from the layer's left edge
     * @param y layer pixels from the layer's top edge, growing down
     * @throws IllegalStateException if the layer was removed
     */
    public void setPivotKeepingPlacement(float x, float y) {
        requireAttached();
        float shiftX = x - pivotX;
        float shiftY = y - pivotY;
        float zoomApplied = getEffectiveZoom();
        float newOffsetX = LayerTransform.offsetXKeepingPlacement(offsetX, shiftX, shiftY, zoomApplied, rotation);
        float newOffsetY = LayerTransform.offsetYKeepingPlacement(offsetY, shiftX, shiftY, zoomApplied, rotation);
        setPivot(x, y);
        setOffset(newOffsetX, newOffsetY);
    }

    /**
     * Sets the pivot to the center of a cell of this layer's glyph source, equal to
     * {@code setPivot((column + 0.5f) * cellWidth, (row + 0.5f) * cellHeight)}. The pivot is
     * stored in pixels: after switching to a glyph source with another cell size, call this again.
     * The cell does not have to be inside the grid. In map window mode the cell is a map cell,
     * because the pivot is in map pixels; the default pivot is the map's top-left corner.
     *
     * @param column cells from the layer's left edge (map cells in map window mode)
     * @param row cells from the layer's top edge, growing down (map cells in map window mode)
     * @throws IllegalStateException if the layer was removed
     */
    public void setPivotCell(int column, int row) {
        setPivot((column + 0.5f) * glyphSource.getCellWidth(), (row + 0.5f) * glyphSource.getCellHeight());
    }

    /**
     * Moves the pivot to the center of a cell as {@link #setPivotCell} does, keeping the picture
     * in place as {@link #setPivotKeepingPlacement} does. In map window mode the cell is a map cell.
     *
     * @param column cells from the layer's left edge
     * @param row cells from the layer's top edge, growing down
     * @throws IllegalStateException if the layer was removed
     */
    public void setPivotCellKeepingPlacement(int column, int row) {
        setPivotKeepingPlacement((column + 0.5f) * glyphSource.getCellWidth(),
                (row + 0.5f) * glyphSource.getCellHeight());
    }

    /**
     * Chooses how the zoom is applied. Default {@link ZoomMode#SMOOTH}.
     *
     * @throws IllegalArgumentException if the mode is null
     * @throws IllegalStateException if the layer was removed
     */
    public void setZoomMode(ZoomMode mode) {
        requireAttached();
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        zoomMode = mode;
    }

    /**
     * Shows or hides the layer. A hidden layer is skipped when drawing but keeps
     * its place in the stack.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setVisible(boolean visible) {
        requireAttached();
        this.visible = visible;
    }

    /**
     * Sets whether the stack's pick test, {@link LayerStack#isPickableAt}, can hit this layer. A
     * non-pickable layer still draws; it is only ignored by the pick test.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setPickable(boolean pickable) {
        requireAttached();
        this.pickable = pickable;
    }

    /**
     * Returns this layer's own color effects: tint, alpha, mix, saturation, and invert. The object
     * is live and owned by the layer; every call returns the same instance and allocates nothing.
     * Changes apply on the next draw to this layer's cells and sprites. If the layer is in a
     * {@link LayerGroup}, the group's effects run after these.
     *
     * <p>The object stays readable after the layer is removed from its stack, but its setters then
     * throw {@link IllegalStateException}.
     *
     * <p>Example, a white hit flash on a sepia layer:
     * <pre>{@code
     * ColorEffects effects = layer.getColorEffects();
     * effects.setSaturation(0f);
     * effects.setTint(0.9f, 0.7f, 0.4f, 1f);
     * effects.setMix(1f, 1f, 1f, flashStrength);
     * }</pre>
     *
     * @return the live color effects of this layer; never null
     */
    public ColorEffects getColorEffects() {
        return effects;
    }

    /**
     * Returns whether the layer has its own clip, by a rectangle or to the stack's size. A member
     * of a {@link Panel} is clipped to the panel even when this is false.
     */
    public boolean hasClip() {
        return clip.isSet();
    }

    /** Returns whether the clip follows the stack's size instead of a fixed rectangle. */
    public boolean isClippedToStack() {
        return clip.isStackArea();
    }

    /**
     * Returns the clip rectangle's left edge as passed to {@link #setClip}, 0 unless set. For a
     * panel member this is measured from the panel's corner.
     */
    public float getClipX() {
        return clip.getX();
    }

    /**
     * Returns the clip rectangle's top edge as passed to {@link #setClip}, 0 unless set. For a
     * panel member this is measured from the panel's corner.
     */
    public float getClipY() {
        return clip.getY();
    }

    /** Returns the clip rectangle's width; 0 unless set by {@link #setClip}. */
    public float getClipWidth() {
        return clip.getWidth();
    }

    /** Returns the clip rectangle's height; 0 unless set by {@link #setClip}. */
    public float getClipHeight() {
        return clip.getHeight();
    }

    /**
     * Clips the layer to a rectangle. By default layers are not clipped and may
     * overhang the stack.
     *
     * <p>The rectangle is in the same space as layer offsets: virtual pixels from the stack's
     * top-left corner, y growing downward. It does not move with this layer's own offset. For a
     * member of a {@link Panel} it is measured from the panel's top-left corner instead, and the
     * clip is also intersected with the panel's rectangle.
     *
     * <p>Clipping is an axis-aligned screen rectangle (a GL scissor). If the batch
     * transform rotates, the clip becomes the bounding box of the rotated rectangle,
     * not an exact clip. It is also intersected with any scissor already active.
     *
     * <p>The clip assumes libGDX's default {@code HdpiMode.Logical}. While
     * {@code HdpiMode.Pixels} is set, the clip is the wrong size on HDPI screens.
     *
     * @throws IllegalArgumentException if width or height is negative
     * @throws IllegalStateException if the layer was removed
     */
    public void setClip(float x, float y, float width, float height) {
        requireAttached();
        if (width < 0f || height < 0f) {
            throw new IllegalArgumentException("clip width and height must not be negative");
        }
        clip.setRect(x, y, width, height);
    }

    /**
     * Clips the layer to the stack's area, from its top-left corner with the size set
     * by {@link LayerStack#setSize}. The size is read at draw time, so later size
     * changes apply. For a member of a {@link Panel} the clip is the stack's area intersected with
     * the panel's rectangle.
     *
     * @throws IllegalStateException if the layer was removed, or if the stack's size
     *     is not set yet. Drawing also throws if the size has been reset to 0 since.
     */
    public void clipToStack() {
        LayerStack stack = requireAttached();
        if (!stack.hasSize()) {
            throw new IllegalStateException("Set the stack size before clipping a layer to it");
        }
        clip.setToStack();
    }

    /**
     * Removes any clip so the layer may overhang again.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void clearClip() {
        requireAttached();
        clip.clear();
    }

    /**
     * Adds a sprite glyph on top of the layer's existing sprites; sprites draw in the order they
     * were added, after the grid. Sprites use the layer's glyph source, effects, offset, clip,
     * and visibility. A layer holds at most {@link #MAX_SPRITES} (16383) sprites.
     *
     * <p>Both {@code codePoint} and {@code rgba} are ints, so swapping them compiles; check the
     * argument order.
     *
     * @param codePoint the glyph's Unicode code point
     * @param rgba the glyph color as RGBA8888, the same format as {@code TerminalGrid} colors
     * @param x left edge of the cell box, in virtual pixels from the layer's top-left corner
     * @param y top edge of the cell box, growing downward
     * @return the new sprite
     * @throws IllegalStateException if the layer was removed or already holds {@link #MAX_SPRITES}
     *     sprites
     */
    public GlyphSprite addSprite(int codePoint, int rgba, float x, float y) {
        requireAttached();
        if (sprites.size >= MAX_SPRITES) {
            throw new IllegalStateException("A layer holds at most " + MAX_SPRITES + " sprites");
        }
        GlyphSprite sprite = new GlyphSprite(this, codePoint, rgba);
        sprite.setPosition(x, y);
        sprites.add(sprite);
        return sprite;
    }

    /**
     * Removes a sprite from this layer. The sprite stops drawing and can no longer be changed.
     *
     * <p>Later sprites shift down one index; when removing in a loop, iterate backward.
     *
     * @param sprite the sprite to remove; must not be null
     * @throws IllegalArgumentException if the sprite is null or belongs to another layer
     * @throws IllegalStateException if the layer or the sprite was already removed
     */
    public void removeSprite(GlyphSprite sprite) {
        requireAttached();
        requireOwnedSprite(sprite);
        sprites.removeValue(sprite, true);
        sprite.detach();
    }

    /** Returns the number of sprites. Indices shift when a sprite is removed. */
    public int getSpriteCount() {
        return sprites.size;
    }

    /**
     * Returns the sprite at a position, in the order the sprites were added. Indices shift when a
     * sprite is removed; iterate backward when removing in a loop.
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public GlyphSprite getSprite(int index) {
        Guards.requireIndexInRange(index, sprites.size);
        return sprites.get(index);
    }

    /**
     * Returns the handle that lifts one cell's glyph off the grid, creating it on the first
     * request (the only time this allocates). The same handle is returned every time, until it is
     * detached or released by {@link #releaseIdleCellHandles}. See {@link CellHandle} for what
     * lifting does.
     *
     * <p>If the grid shrinks so the cell no longer exists, the handle is detached at the next
     * draw and dropped; request a new one after the grid grows back.
     *
     * <p>In a {@linkplain #enableMapWindow() map window} layer, a lasting handle is dropped when
     * its cell leaves the window. Lasting handles are meant for display-owned animation such as
     * tweens, flashes, and bumps. Game state that must survive scrolling belongs in the grid, set
     * through the map window's filler; per-frame effects belong in {@link #frameCell frame handles}.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the grid's current size
     * @throws IllegalStateException if the layer was removed
     */
    public CellHandle cell(int column, int row) {
        requireAttached();
        requireCellInsideGrid(column, row);
        return cellHandleSet.getOrCreateLasting(column, row);
    }

    /**
     * Returns the frame handle for one cell, creating it on the first request (the only time this
     * allocates). It is meant for effects applied again every frame: {@link LayerStack#beginCellEffects}
     * resets it, and releases it once it stays unused for {@link #getFrameReleaseSeconds()}. The
     * same handle is returned until it is released or detached, so request it again after a release.
     *
     * <p>A cell can have both a lasting handle ({@link #cell}) and a frame handle. They are separate
     * objects and never change each other's values; the cell is drawn with both effects combined, as
     * described in {@link CellHandle}. A frame handle has no pivot of its own, and its pivot setters
     * throw; the effective pivot is the lasting handle's.
     *
     * <p>Frame handles move with {@link #scroll} and are detached by a grid shrink or layer removal,
     * like lasting handles. {@link #resetCellHandles} and {@link #releaseIdleCellHandles} do not
     * touch them; {@link LayerStack#beginCellEffects} manages them.
     *
     * <p>To drive a frame handle with a timeline, use
     * {@link com.github.halfdeadgames.asciigdx.tween.Timeline#writingEveryApply()} with a setter
     * that calls this method on every write. A held handle can be released or detached between
     * frames, and then its setters throw.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the grid's current size
     * @throws IllegalStateException if the layer was removed
     */
    public CellHandle frameCell(int column, int row) {
        requireAttached();
        requireCellInsideGrid(column, row);
        return cellHandleSet.getOrCreateFrame(column, row);
    }

    /**
     * Resets everything on every lasting cell handle: lifted glyphs settle back into the grid, and
     * the temporary colors and glyphs on them are cleared. The handles stay in the layer; to also
     * drop them, follow with {@link #releaseIdleCellHandles}. Frame handles are not touched.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void resetCellHandles() {
        requireAttached();
        cellHandleSet.resetLasting();
    }

    /**
     * Drops every idle cell handle, so handles requested by sweeping effects do not pile up. A
     * handle is idle when it is not lifted, has no temporary glyph, and both of its mix amounts
     * are 0. The cells of released handles draw as plain grid cells.
     *
     * <p>A released handle is detached: its getters keep working and its setters throw
     * {@link IllegalStateException}. Call {@link #cell} again for a fresh handle. Call this when an
     * effect ends or moves on, not every frame, because each re-request allocates a new handle.
     *
     * <p>Settings that do not lift the cell, such as a custom pivot, do not keep a handle from
     * being released, and they are lost with it.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void releaseIdleCellHandles() {
        requireAttached();
        cellHandleSet.releaseIdleLasting();
    }

    /**
     * Sets how many seconds a frame handle must stay idle before {@link LayerStack#beginCellEffects}
     * releases it. Lasting handles are not affected. Default 1.
     *
     * @throws IllegalArgumentException if {@code seconds} is not a finite number above 0
     */
    public void setFrameReleaseSeconds(float seconds) {
        if (Float.isNaN(seconds) || Float.isInfinite(seconds) || seconds <= 0f) {
            throw new IllegalArgumentException("Release seconds must be finite and above 0: " + seconds);
        }
        frameReleaseSeconds = seconds;
    }

    /** Returns the idle time before a frame handle is released, in seconds. */
    public float getFrameReleaseSeconds() {
        return frameReleaseSeconds;
    }

    /** Sweeps the frame handles; see {@link LayerStack#beginCellEffects}. */
    void sweepCellHandles(float deltaSeconds) {
        FrameHandleSweep.runOnFrameHandles(cellHandleSet, frameReleaseSeconds, deltaSeconds);
        cellHandleSet.rebindRendererIfChanged();
    }

    /**
     * Moves the grid content and the cell handles together, so a handle keeps following the same
     * content: every cell {@code (x, y)} and its handle move to {@code (x + columns, y + rows)}.
     * Exposed cells are empty, as after {@link TerminalGrid#scroll}.
     *
     * <p>Unlike {@link TerminalGrid#scroll}, which moves content only and leaves handles keyed to
     * their positions, this keeps each handle's motion and overrides with its content. Handles
     * pushed out of the grid are detached as after a shrink: getters work, setters throw, and
     * {@link #cell} gives a new handle. Free sprites are pixel-positioned and stay where they are.
     *
     * <p>Nothing is allocated except a possible rehash of the position index. A zero shift does
     * nothing.
     *
     * <p>Not available in {@linkplain #enableMapWindow() map window} mode, where the camera alone
     * moves the content so it cannot drift away from the map origin.
     *
     * @param columns columns to move to the right (negative moves left)
     * @param rows rows to move down (negative moves up)
     * @throws IllegalStateException if the layer was removed or is a map window
     */
    public void scroll(int columns, int rows) {
        requireAttached();
        if (mapWindow != null) {
            throw new IllegalStateException("A map window scrolls with the camera, not by scroll()");
        }
        scrollContent(columns, rows);
    }

    private void scrollContent(int columns, int rows) {
        if (columns == 0 && rows == 0) {
            return;
        }
        // Order matters: unhide at old positions first, then move content, then handles.
        cellHandleSet.showGlyphsAtOldPositions();
        grid.scroll(columns, rows);
        cellHandleSet.moveWithContent(columns, rows);
        if (mapWindow != null) {
            mapWindow.shiftFillFlags(columns, rows);
        }
    }

    /**
     * Turns on map window mode with no margins.
     *
     * @see #enableMapWindow(int, int)
     */
    public void enableMapWindow() {
        enableMapWindow(0, 0);
    }

    /**
     * Makes the grid a window onto a larger map. The map origin is the whole map cell shown at grid
     * cell (0,0). It follows the active camera (the stack camera, or the panel's camera for a panel
     * member) times the parallax factor, in cells of this layer's glyph source, so layers with
     * different cell sizes follow one camera with different origin numbers.
     *
     * <p>Enabling only places the window: the grid content is taken to already show the map at the
     * current origin, and nothing scrolls. Calling this again replaces the margins the same way.
     * Afterward, a change of camera, panel camera, parallax, panel, or glyph source that moves the
     * origin scrolls the content and the cell handles as {@link #scroll} does: exposed cells become
     * empty, handles pushed out are detached, and free sprites do not move. The drawing position
     * compensates, so only the sub-cell remainder of the camera moves on screen. The shift happens
     * inside the setter, so the game can refill the exposed cells before drawing.
     *
     * <p>Make the grid at least the window size plus one column and one row, so the sub-cell
     * offset never exposes an edge, plus the margins for zoom or rotation.
     *
     * <p>In this mode the pivot (see {@link #setPivot}) is measured in map pixels, and the default
     * pivot (0, 0) is the map's top-left corner. A game that zooms or rotates a map window should set
     * the pivot to the map point it wants to stay fixed, for example the player. Cell queries
     * still return grid cells.
     *
     * <p>Changing the glyph source of a map window changes the cell size, so it asks for a full
     * refill: every cell is flagged by {@link MapWindow#forEachCellToFill}.
     *
     * @param marginColumns extra cells kept before the visible left edge; not negative
     * @param marginRows extra cells kept before the visible top edge; not negative
     * @throws IllegalArgumentException if a margin is negative
     * @throws IllegalStateException if the layer was removed
     */
    public void enableMapWindow(int marginColumns, int marginRows) {
        requireAttached();
        var newWindow = new MapWindow(this, grid, new MapWindowOrigin(marginColumns, marginRows));
        detachMapWindow();
        mapWindow = newWindow;
        updateMapWindowOrigin();
    }

    /**
     * Turns off map window mode and detaches the {@link MapWindow}. The content is not touched.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void disableMapWindow() {
        requireAttached();
        detachMapWindow();
    }

    /**
     * Returns the {@link MapWindow} created by the latest {@link #enableMapWindow()}.
     *
     * @throws IllegalStateException if the layer was removed or is not a map window
     */
    public MapWindow getMapWindow() {
        requireAttached();
        if (mapWindow == null) {
            throw new IllegalStateException("Layer is not a map window");
        }
        return mapWindow;
    }

    private void detachMapWindow() {
        if (mapWindow == null) {
            return;
        }
        mapWindow.detach();
        mapWindow = null;
    }

    /**
     * Returns whether the layer is in map window mode.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public boolean isMapWindow() {
        requireAttached();
        return mapWindow != null;
    }

    /** Re-reads the camera and scrolls the content by any origin change; does nothing outside map window mode. */
    void followCamera() {
        if (mapWindow == null) {
            return;
        }
        updateMapWindowOrigin();
        if (mapWindow.hasShifted()) {
            // Origins are bounded by MapWindowOrigin.MAX_ORIGIN, so negating the shift cannot overflow.
            scrollContent(-mapWindow.getShiftColumns(), -mapWindow.getShiftRows());
        }
    }

    /** Pixels the map origin column sits from the map's left edge, or 0 outside map window mode. */
    float getMapOriginPixelsX() {
        return mapWindow == null ? 0f : mapWindow.getOriginPixelsX(glyphSource.getCellWidth());
    }

    /** Pixels the map origin row sits from the map's top edge, or 0 outside map window mode. */
    float getMapOriginPixelsY() {
        return mapWindow == null ? 0f : mapWindow.getOriginPixelsY(glyphSource.getCellHeight());
    }

    private void updateMapWindowOrigin() {
        requireAttached();
        mapWindow.followCamera(LayerPlacement.getActiveCameraX(this) * parallaxX,
                LayerPlacement.getActiveCameraY(this) * parallaxY,
                glyphSource.getCellWidth(), glyphSource.getCellHeight());
    }

    /** Returns how many lasting cell handles the layer currently holds. */
    public int getCellHandleCount() {
        return cellHandleSet.lastingCount();
    }

    /** Returns how many frame cell handles the layer currently holds. */
    public int getFrameCellHandleCount() {
        return cellHandleSet.frameCount();
    }

    /**
     * Removes every free sprite (those added with {@link #addSprite}), like calling
     * {@link #removeSprite} on each. Cell handles are not touched; see {@link #resetCellHandles}.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void removeFreeSprites() {
        requireAttached();
        detachAllSprites();
    }

    private void detachAllSprites() {
        for (int i = sprites.size - 1; i >= 0; i--) {
            sprites.get(i).detach();
        }
        sprites.clear();
    }

    /**
     * Resets every cell handle ({@link #resetCellHandles}), which also clears their temporary
     * colors and glyphs, and removes every free sprite ({@link #removeFreeSprites}).
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void clearAllSprites() {
        resetCellHandles();
        removeFreeSprites();
    }

    private void requireCellInsideGrid(int column, int row) {
        Guards.requireCellInside(column, row, grid.getColumns(), grid.getRows());
    }

    /** Brings the renderer in line with the cell handles before drawing; see {@link CellHandleSet#syncBeforeDraw}. */
    void syncCellHandles() {
        cellHandleSet.syncBeforeDraw();
    }

    /**
     * Moves the layer to a position in its stack.
     *
     * @see LayerStack#moveTo(Layer, int)
     */
    public void moveTo(int index) {
        requireAttached().moveTo(this, index);
    }

    /**
     * Removes the layer from its stack and disposes its renderer.
     *
     * @throws IllegalStateException if the layer was already removed
     */
    public void remove() {
        requireAttached().remove(this);
    }

    LayerStack getOwner() {
        return owner;
    }

    /** The groups this layer belongs to, in join order. */
    Array<LayerGroup> getGroups() {
        return groups;
    }

    void joinGroup(LayerGroup group) {
        groups.add(group);
    }

    void leaveGroup(LayerGroup group) {
        groups.removeValue(group, true);
    }

    LayerRenderer getRenderer() {
        return renderer;
    }

    Array<GlyphSprite> getSprites() {
        return sprites;
    }

    /** The handles the renderer draws: one per cell that has a lasting or a frame handle. */
    Array<CellHandle> getCellHandles() {
        return cellHandleSet.drawnHandles();
    }

    void detach() {
        owner = null;
        effects.markDetached();
        detachMapWindow();
        leaveAllGroups();
        detachAllSprites();
        cellHandleSet.detachAll();
    }

    private void leaveAllGroups() {
        for (int i = groups.size - 1; i >= 0; i--) {
            groups.get(i).forgetMember(this);
        }
        groups.clear();
    }

    private void requireOwnedSprite(GlyphSprite sprite) {
        Guards.requireNotNull(sprite, "sprite");
        Guards.requireOwnedBy(sprite.getOwner(), this, "Sprite", "layer");
    }

    private LayerStack requireAttached() {
        if (owner == null) {
            throw new IllegalStateException("Layer was removed from its stack");
        }
        return owner;
    }
}
