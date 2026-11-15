package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
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

    private final TerminalView grid;
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
    private final LayerEffects effects = new LayerEffects();
    private final Array<LayerGroup> groups = new Array<>(true, 2);
    private Panel panel;
    private ClipMode clipMode = ClipMode.NONE;
    private float clipX;
    private float clipY;
    private float clipWidth;
    private float clipHeight;
    private final Array<GlyphSprite> sprites = new Array<>();
    private final Array<CellHandle> cellHandles = new Array<>();
    private final LongMap<CellHandle> cellsByPosition = new LongMap<>();
    private int syncedColumns;
    private int syncedRows;

    private enum ClipMode { NONE, RECT, STACK }

    Layer(LayerStack owner, TerminalView grid, GlyphSource glyphSource, LayerRenderer renderer) {
        this.owner = owner;
        this.grid = grid;
        this.glyphSource = glyphSource;
        this.renderer = renderer;
        syncedColumns = grid.getWidth();
        syncedRows = grid.getHeight();
    }

    /** Returns the grid this layer draws. */
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
     * new cell size (for example from a {@link FontFit}) is up to the caller. Glyphs missing
     * from the new source draw as its fallback glyph. The layer does not own the source and
     * never disposes it. The offset is not changed.
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
        forgetHiddenFlags();
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
            requireAttachedPanelOf(panel, stack);
        }
        this.panel = panel;
    }

    private static void requireAttachedPanelOf(Panel panel, LayerStack stack) {
        if (panel.getOwner() == null) {
            throw new IllegalStateException("Panel was removed from its stack");
        }
        if (panel.getOwner() != stack) {
            throw new IllegalArgumentException("Panel belongs to another stack");
        }
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
     * the offset.
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
     * is kept for the zoom and rotation set at the time of the call; later changes to either
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
     * The cell does not have to be inside the grid.
     *
     * @param column cells from the layer's left edge
     * @param row cells from the layer's top edge, growing down
     * @throws IllegalStateException if the layer was removed
     */
    public void setPivotCell(int column, int row) {
        setPivot((column + 0.5f) * glyphSource.getCellWidth(), (row + 0.5f) * glyphSource.getCellHeight());
    }

    /**
     * Moves the pivot to the center of a cell as {@link #setPivotCell} does, keeping the picture
     * in place as {@link #setPivotKeepingPlacement} does.
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

    /** Returns the tint's red component. */
    public float getTintR() {
        return effects.getTintR();
    }

    /** Returns the tint's green component. */
    public float getTintG() {
        return effects.getTintG();
    }

    /** Returns the tint's blue component. */
    public float getTintB() {
        return effects.getTintB();
    }

    /** Returns the tint's alpha component. */
    public float getTintA() {
        return effects.getTintA();
    }

    /** Copies the tint into {@code out} and returns it. */
    public Color getTint(Color out) {
        return out.set(effects.getTintR(), effects.getTintG(), effects.getTintB(), effects.getTintA());
    }

    /**
     * Sets the tint, a color multiplied into every cell's background and glyph
     * colors on the GPU each frame. Changing it rewrites and uploads nothing.
     * Default is white (1, 1, 1, 1), which leaves colors unchanged. Values are not
     * clamped.
     *
     * <p>The tint is applied after saturation and before the mix and the invert (see
     * {@link #setSaturation}, {@link #setMix}, and {@link #setInvert}).
     *
     * <p>Alpha fades each cell as one flat color, so a glyph's own background does
     * not show through it; the layers below show through the whole faded layer.
     * Inside a cell, glyph and background always combine with normal "over"
     * blending. The batch's blend functions decide only how the finished cell
     * combines with what is below.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setTint(float r, float g, float b, float a) {
        requireAttached();
        effects.setTint(r, g, b, a);
    }

    /**
     * Sets the tint from a color. The values are copied; no reference is kept.
     *
     * @see #setTint(float, float, float, float)
     * @throws IllegalStateException if the layer was removed
     */
    public void setTint(Color color) {
        setTint(color.r, color.g, color.b, color.a);
    }

    /**
     * Sets only the tint's alpha, keeping red, green, and blue.
     *
     * @see #setTint(float, float, float, float)
     * @throws IllegalStateException if the layer was removed
     */
    public void setAlpha(float alpha) {
        requireAttached();
        effects.setTintAlpha(alpha);
    }

    /** Returns the mix color's red component. */
    public float getMixR() {
        return effects.getMixR();
    }

    /** Returns the mix color's green component. */
    public float getMixG() {
        return effects.getMixG();
    }

    /** Returns the mix color's blue component. */
    public float getMixB() {
        return effects.getMixB();
    }

    /** Returns how far the layer is mixed toward the mix color, 0 for not at all. */
    public float getMixAmount() {
        return effects.getMixAmount();
    }

    /**
     * Mixes every drawn color toward the given color on the GPU each frame, for hit flashes
     * (white, amount fading to 0) and fog washes. Changing it rewrites and uploads nothing.
     * Amount 0 leaves colors unchanged (the default, with white as the color); 1 turns
     * everything drawn into the color. Only colors change: alpha is untouched, so transparent
     * parts stay transparent. Values are not clamped.
     *
     * <p>The mix is applied after saturation and the tint, so a flash can reach its pure
     * color even on a darkly tinted layer. Only the invert follows it (see {@link #setInvert}).
     *
     * @throws IllegalStateException if the layer was removed
     * @see CellHandle#setMix(int, float) the cell-level mix, which takes an RGBA8888 int and
     *     changes one cell's own colors before the layer effects run
     */
    public void setMix(float r, float g, float b, float amount) {
        requireAttached();
        effects.setMix(r, g, b, amount);
    }

    /**
     * Sets the mix from a color's red, green, and blue; its alpha is ignored. The values are
     * copied; no reference is kept.
     *
     * @see #setMix(float, float, float, float)
     * @throws IllegalStateException if the layer was removed
     */
    public void setMix(Color color, float amount) {
        setMix(color.r, color.g, color.b, amount);
    }

    /** Returns the saturation, 1 for normal. */
    public float getSaturation() {
        return effects.getSaturation();
    }

    /**
     * Sets the saturation applied on the GPU each frame: 1 is normal (the default), 0 is
     * grayscale, and values above 1 oversaturate. Changing it rewrites and uploads nothing.
     * Values are not clamped.
     *
     * <p>Saturation is applied first, before the tint, the mix, and the invert. Saturation 0
     * plus a tint gives a one-hue look, such as sepia or night vision.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setSaturation(float saturation) {
        requireAttached();
        effects.setSaturation(saturation);
    }

    /** Returns the invert amount, 0 for normal. */
    public float getInvert() {
        return effects.getInvert();
    }

    /**
     * Sets how far every drawn color is turned into its negative on the GPU each frame: 0 leaves
     * colors unchanged (the default) and 1 is a full negative. Changing it rewrites and uploads
     * nothing. Alpha is untouched. Values are not clamped.
     *
     * <p>The invert is applied last, after saturation, the tint, and the mix. Inverting every
     * layer of a stack equals inverting the whole screen as long as the bottom layer is opaque;
     * transparent parts stay transparent, so they are not inverted.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setInvert(float amount) {
        requireAttached();
        effects.setInvert(amount);
    }

    /**
     * Returns whether the layer has its own clip, by a rectangle or to the stack's size. A member
     * of a {@link Panel} is clipped to the panel even when this is false.
     */
    public boolean hasClip() {
        return clipMode != ClipMode.NONE;
    }

    /** Returns whether the clip follows the stack's size instead of a fixed rectangle. */
    public boolean isClippedToStack() {
        return clipMode == ClipMode.STACK;
    }

    /**
     * Returns the clip rectangle's left edge as passed to {@link #setClip}, 0 unless set. For a
     * panel member this is measured from the panel's corner.
     */
    public float getClipX() {
        return clipX;
    }

    /**
     * Returns the clip rectangle's top edge as passed to {@link #setClip}, 0 unless set. For a
     * panel member this is measured from the panel's corner.
     */
    public float getClipY() {
        return clipY;
    }

    /** Returns the clip rectangle's width; 0 unless set by {@link #setClip}. */
    public float getClipWidth() {
        return clipWidth;
    }

    /** Returns the clip rectangle's height; 0 unless set by {@link #setClip}. */
    public float getClipHeight() {
        return clipHeight;
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
        clipMode = ClipMode.RECT;
        clipX = x;
        clipY = y;
        clipWidth = width;
        clipHeight = height;
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
        clipMode = ClipMode.STACK;
        clipX = 0f;
        clipY = 0f;
        clipWidth = 0f;
        clipHeight = 0f;
    }

    /**
     * Removes any clip so the layer may overhang again.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void clearClip() {
        requireAttached();
        clipMode = ClipMode.NONE;
        clipX = 0f;
        clipY = 0f;
        clipWidth = 0f;
        clipHeight = 0f;
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
        if (index < 0 || index >= sprites.size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + sprites.size);
        }
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
     * @throws IndexOutOfBoundsException if the cell is outside the grid's current size
     * @throws IllegalStateException if the layer was removed
     */
    public CellHandle cell(int column, int row) {
        requireAttached();
        if (column < 0 || column >= grid.getWidth() || row < 0 || row >= grid.getHeight()) {
            throw new IndexOutOfBoundsException("Cell (" + column + ", " + row + ") outside "
                    + grid.getWidth() + "x" + grid.getHeight());
        }
        CellHandle existing = cellsByPosition.get(cellKey(column, row));
        if (existing != null) {
            return existing;
        }
        return createCellHandle(column, row);
    }

    /**
     * Resets everything on every cell handle: lifted glyphs settle back into the grid, and the
     * temporary colors and glyphs on them are cleared. The handles stay in the layer; to also drop
     * them, follow with {@link #releaseIdleCellHandles}.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void resetCellHandles() {
        requireAttached();
        for (int i = 0; i < cellHandles.size; i++) {
            cellHandles.get(i).reset();
        }
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
        boolean released = false;
        for (int i = cellHandles.size - 1; i >= 0; i--) {
            CellHandle cell = cellHandles.get(i);
            if (!isIdle(cell)) {
                continue;
            }
            settleRendererForCell(cell);
            cellHandles.removeIndex(i);
            cellsByPosition.remove(cellKey(cell.getColumn(), cell.getRow()));
            cell.detach();
            released = true;
        }
        if (released) {
            renderer.setCellHandles(cellHandles);
        }
    }

    /** Returns how many cell handles the layer currently holds. */
    public int getCellHandleCount() {
        return cellHandles.size;
    }

    private static boolean isIdle(CellHandle cell) {
        return !cell.isLifted()
                && !cell.hasCodePoint()
                && cell.getForegroundMixAmount() == 0f
                && cell.getBackgroundMixAmount() == 0f;
    }

    /**
     * Sends the renderer any reset this handle has not synced yet, since a released handle gets
     * no later sync.
     */
    private void settleRendererForCell(CellHandle cell) {
        if (cell.getColumn() >= grid.getWidth() || cell.getRow() >= grid.getHeight()) {
            // The grid shrank since the last draw; the renderer's rebuild already forgets this cell.
            return;
        }
        if (cell.isGlyphHidden()) {
            renderer.setCellGlyphHidden(cell.getColumn(), cell.getRow(), false);
            cell.setGlyphHidden(false);
        }
        if (cell.areOverridesChanged()) {
            renderer.refreshCell(cell.getColumn(), cell.getRow());
            cell.clearOverridesChanged();
        }
    }

    /**
     * Removes every free sprite (those added with {@link #addSprite}), like calling
     * {@link #removeSprite} on each. Cell handles are not touched; see {@link #resetCellHandles}.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void removeFreeSprites() {
        requireAttached();
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

    private CellHandle createCellHandle(int column, int row) {
        if (cellHandles.size == 0) {
            renderer.setCellHandles(cellHandles);
        }
        CellHandle created = new CellHandle(this, column, row);
        cellsByPosition.put(cellKey(column, row), created);
        cellHandles.add(created);
        return created;
    }

    private static long cellKey(int column, int row) {
        return ((long) row << Integer.SIZE) | (column & 0xFFFFFFFFL);
    }

    /**
     * Brings the renderer in line with the cell handles before drawing: detaches handles whose
     * cell left the grid, then hides or shows the glyph of each handle whose lifted state changed.
     */
    void syncCellHandles() {
        if (cellHandles.size == 0) {
            syncedColumns = grid.getWidth();
            syncedRows = grid.getHeight();
            return;
        }
        if (grid.getWidth() != syncedColumns || grid.getHeight() != syncedRows) {
            syncedColumns = grid.getWidth();
            syncedRows = grid.getHeight();
            detachCellsOutsideGrid();
            forgetHiddenFlags();
        }
        applyLiftedFlags();
        refreshChangedOverrides();
    }

    private void refreshChangedOverrides() {
        for (int i = 0; i < cellHandles.size; i++) {
            CellHandle cell = cellHandles.get(i);
            if (!cell.areOverridesChanged()) {
                continue;
            }
            renderer.refreshCell(cell.getColumn(), cell.getRow());
            cell.clearOverridesChanged();
        }
    }

    /*
     * A handle is only detached when its cell left the grid, so there is never a surviving cell
     * to rewrite; the renderer's rebuild on the size change drops it from its index.
     */
    private void detachCellsOutsideGrid() {
        for (int i = cellHandles.size - 1; i >= 0; i--) {
            CellHandle cell = cellHandles.get(i);
            if (cell.getColumn() < syncedColumns && cell.getRow() < syncedRows) {
                continue;
            }
            cellHandles.removeIndex(i);
            cellsByPosition.remove(cellKey(cell.getColumn(), cell.getRow()));
            cell.detach();
        }
    }

    /** The renderer clears its hidden flags when it rebuilds, so every handle starts shown again. */
    private void forgetHiddenFlags() {
        for (int i = 0; i < cellHandles.size; i++) {
            cellHandles.get(i).setGlyphHidden(false);
        }
    }

    private void applyLiftedFlags() {
        for (int i = 0; i < cellHandles.size; i++) {
            CellHandle cell = cellHandles.get(i);
            boolean lifted = cell.isLifted();
            if (lifted == cell.isGlyphHidden()) {
                continue;
            }
            renderer.setCellGlyphHidden(cell.getColumn(), cell.getRow(), lifted);
            cell.setGlyphHidden(lifted);
        }
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

    LayerEffects getEffects() {
        return effects;
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

    Array<CellHandle> getCellHandles() {
        return cellHandles;
    }

    void detach() {
        owner = null;
        leaveAllGroups();
        for (int i = sprites.size - 1; i >= 0; i--) {
            sprites.get(i).detach();
        }
        sprites.clear();
        for (int i = cellHandles.size - 1; i >= 0; i--) {
            cellHandles.get(i).detach();
        }
        cellHandles.clear();
        cellsByPosition.clear();
    }

    private void leaveAllGroups() {
        for (int i = groups.size - 1; i >= 0; i--) {
            groups.get(i).forgetMember(this);
        }
        groups.clear();
    }

    private void requireOwnedSprite(GlyphSprite sprite) {
        if (sprite == null) {
            throw new IllegalArgumentException("sprite must not be null");
        }
        if (sprite.getOwner() == null) {
            throw new IllegalStateException("Sprite was removed from its layer");
        }
        if (sprite.getOwner() != this) {
            throw new IllegalArgumentException("Sprite belongs to another layer");
        }
    }

    private LayerStack requireAttached() {
        if (owner == null) {
            throw new IllegalStateException("Layer was removed from its stack");
        }
        return owner;
    }
}
