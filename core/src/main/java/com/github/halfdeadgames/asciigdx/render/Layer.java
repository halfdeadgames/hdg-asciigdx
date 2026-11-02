package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * One grid in a {@link LayerStack}, with an offset and a visibility flag. Created
 * only by the stack.
 *
 * <p>The offset is in virtual pixels, measured from the stack's top-left corner,
 * with y growing downward like grid rows. Once the layer is removed from its
 * stack, getters keep working but every method that changes or uses the layer
 * throws {@link IllegalStateException}.
 */
public final class Layer {

    private final TerminalView grid;
    private GlyphSource glyphSource;
    private final LayerRenderer renderer;
    private LayerStack owner;
    private float offsetX;
    private float offsetY;
    private boolean visible = true;
    private float tintR = 1f;
    private float tintG = 1f;
    private float tintB = 1f;
    private float tintA = 1f;
    private ClipMode clipMode = ClipMode.NONE;
    private float clipX;
    private float clipY;
    private float clipWidth;
    private float clipHeight;

    private enum ClipMode { NONE, RECT, STACK }

    Layer(LayerStack owner, TerminalView grid, GlyphSource glyphSource, LayerRenderer renderer) {
        this.owner = owner;
        this.grid = grid;
        this.glyphSource = glyphSource;
        this.renderer = renderer;
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
    }

    /** Returns the horizontal offset in virtual pixels, growing to the right. */
    public float getOffsetX() {
        return offsetX;
    }

    /** Returns the vertical offset in virtual pixels, growing downward. */
    public float getOffsetY() {
        return offsetY;
    }

    /** Returns whether the layer is drawn. */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the offset from the stack's top-left corner.
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
     * Shows or hides the layer. A hidden layer is skipped when drawing but keeps
     * its place in the stack.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setVisible(boolean visible) {
        requireAttached();
        this.visible = visible;
    }

    /** Returns the tint's red component. */
    public float getTintR() {
        return tintR;
    }

    /** Returns the tint's green component. */
    public float getTintG() {
        return tintG;
    }

    /** Returns the tint's blue component. */
    public float getTintB() {
        return tintB;
    }

    /** Returns the tint's alpha component. */
    public float getTintA() {
        return tintA;
    }

    /** Copies the tint into {@code out} and returns it. */
    public Color getTint(Color out) {
        return out.set(tintR, tintG, tintB, tintA);
    }

    /**
     * Sets the tint, a color multiplied into every cell's background and glyph
     * colors on the GPU each frame. Changing it rewrites and uploads nothing.
     * Default is white (1, 1, 1, 1), which leaves colors unchanged. Values are not
     * clamped.
     *
     * <p>Alpha applies per cell, not to the layer as one flat image: a glyph's own
     * background shows through a semi-transparent glyph, and the layers below show
     * through both.
     *
     * @throws IllegalStateException if the layer was removed
     */
    public void setTint(float r, float g, float b, float a) {
        requireAttached();
        tintR = r;
        tintG = g;
        tintB = b;
        tintA = a;
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
        tintA = alpha;
    }

    /** Returns whether the layer is clipped, by a rectangle or to the stack's size. */
    public boolean hasClip() {
        return clipMode != ClipMode.NONE;
    }

    /** Returns whether the clip follows the stack's size instead of a fixed rectangle. */
    public boolean isClippedToStack() {
        return clipMode == ClipMode.STACK;
    }

    /** Returns the clip rectangle's left edge in stack space; 0 unless set by {@link #setClip}. */
    public float getClipX() {
        return clipX;
    }

    /** Returns the clip rectangle's top edge in stack space; 0 unless set by {@link #setClip}. */
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
     * <p>The rectangle is in stack space, the same space as layer offsets: virtual
     * pixels from the stack's top-left corner, y growing downward. It does not move
     * with this layer's own offset.
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
     * changes apply.
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

    LayerRenderer getRenderer() {
        return renderer;
    }

    void detach() {
        owner = null;
    }

    private LayerStack requireAttached() {
        if (owner == null) {
            throw new IllegalStateException("Layer was removed from its stack");
        }
        return owner;
    }
}
