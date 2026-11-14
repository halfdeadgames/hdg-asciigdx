package com.github.halfdeadgames.asciigdx.render;

/**
 * Lifts one cell's glyph off the grid so it can move, rotate, scale, and fade on its own, for
 * bump attacks, wiggles, and hit pops. Handles come from {@link Layer#cell(int, int)}.
 *
 * <p>A handle with default values does nothing: the glyph stays in the grid. As soon as one value
 * differs (see {@link #isLifted()}), the glyph is drawn as a sprite instead. The cell's background
 * always stays in the grid. The lifted glyph shows the cell's current glyph and foreground color
 * every frame, so game changes to the cell follow it. The grid data is never modified.
 *
 * <p>Handles are keyed by cell position. If {@link com.github.halfdeadgames.asciigdx.grid.TerminalGrid}
 * is resized with a content shift, handles do not move: the transform applies to whatever is now at
 * that position. Lifting and settling take effect at the next draw. Lifted cells draw before the
 * layer's free sprites, in the order their handles were created, and are positioned relative to the
 * layer like sprites.
 *
 * <p>Offsets are in virtual pixels with y growing downward, like layer offsets. Rotation is in
 * degrees, counterclockwise on screen. Values are not clamped, except alpha. Once the handle is
 * detached (the layer was removed, or the grid shrank so the cell no longer exists), getters keep
 * working but every mutator throws {@link IllegalStateException}; ask the layer for a new handle.
 */
public final class CellHandle implements SpriteTransform {

    private static final float FULL_TURN_DEGREES = 360f;
    private static final int ALPHA_MASK = 0xFF;

    private final int column;
    private final int row;
    private boolean detached;
    private boolean glyphHidden;
    private float offsetX;
    private float offsetY;
    private float rotation;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;
    private float alpha = 1f;

    CellHandle(int column, int row) {
        this.column = column;
        this.row = row;
    }

    void detach() {
        detached = true;
    }

    /** Whether the renderer currently hides this cell's glyph; owned by the layer's sync. */
    boolean isGlyphHidden() {
        return glyphHidden;
    }

    void setGlyphHidden(boolean glyphHidden) {
        this.glyphHidden = glyphHidden;
    }

    /** Returns {@code rgba} with its alpha byte multiplied by {@code factor}, rounded. */
    static int withAlphaScaled(int rgba, float factor) {
        int alphaByte = Math.round((rgba & ALPHA_MASK) * factor);
        return (rgba & ~ALPHA_MASK) | alphaByte;
    }

    private void requireAttached() {
        if (detached) {
            throw new IllegalStateException("Cell handle was detached from its layer");
        }
    }

    /** The cell's column. */
    public int getColumn() {
        return column;
    }

    /** The cell's row. */
    public int getRow() {
        return row;
    }

    /** Horizontal offset from the cell's own position, in virtual pixels. */
    public float getOffsetX() {
        return offsetX;
    }

    /** Vertical offset from the cell's own position, in virtual pixels, growing downward. */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the offset from the cell's own position.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setOffset(float x, float y) {
        requireAttached();
        offsetX = x;
        offsetY = y;
    }

    /**
     * Moves the glyph relative to its current offset.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void offsetBy(float dx, float dy) {
        setOffset(offsetX + dx, offsetY + dy);
    }

    /** Rotation in degrees; positive is counterclockwise on screen. */
    @Override
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation in degrees; positive is counterclockwise on screen.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setRotation(float degrees) {
        requireAttached();
        rotation = degrees;
    }

    /**
     * Adds to the rotation, in degrees.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void rotateBy(float degrees) {
        setRotation(rotation + degrees);
    }

    /** Horizontal scale; 1 is unscaled, negative mirrors. */
    @Override
    public float getScaleX() {
        return scaleX;
    }

    /** Vertical scale; 1 is unscaled, negative mirrors. */
    @Override
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the same scale on both axes.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setScale(float scale) {
        setScale(scale, scale);
    }

    /**
     * Sets the scale per axis; negative values mirror.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setScale(float scaleX, float scaleY) {
        requireAttached();
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /** Horizontal pivot as a fraction of the cell box: 0 is left, 1 is right. */
    @Override
    public float getPivotX() {
        return pivotX;
    }

    /** Vertical pivot as a fraction of the cell box: 0 is top, 1 is bottom. */
    @Override
    public float getPivotY() {
        return pivotY;
    }

    /**
     * Sets the pivot for rotation and scale, as fractions of the cell box. Defaults to the center.
     * The pivot alone does not lift the glyph.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setPivot(float pivotX, float pivotY) {
        requireAttached();
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    /** Multiplier on the glyph's alpha, from 0 to 1. */
    public float getAlphaMultiplier() {
        return alpha;
    }

    /**
     * Sets the multiplier applied to the glyph's own alpha, clamped to 0..1. Default 1.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setAlphaMultiplier(float alpha) {
        requireAttached();
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }

    /**
     * Sets every value back to its default: no offset, no rotation, scale 1, pivot at the center,
     * alpha 1. The glyph settles back into the grid.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void reset() {
        requireAttached();
        offsetX = 0f;
        offsetY = 0f;
        rotation = 0f;
        scaleX = 1f;
        scaleY = 1f;
        pivotX = 0.5f;
        pivotY = 0.5f;
        alpha = 1f;
    }

    /**
     * Returns whether the glyph is drawn off the grid: any of offset, rotation (other than a
     * whole number of turns), scale, or alpha differs from its default. A detached handle keeps
     * its values but is never drawn.
     */
    public boolean isLifted() {
        return offsetX != 0f || offsetY != 0f
                || rotation % FULL_TURN_DEGREES != 0f
                || scaleX != 1f || scaleY != 1f
                || alpha != 1f;
    }

    /** Returns whether the handle was detached from its layer. */
    public boolean isDetached() {
        return detached;
    }
}
