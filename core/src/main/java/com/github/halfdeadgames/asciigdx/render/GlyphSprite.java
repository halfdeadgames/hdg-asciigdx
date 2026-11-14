package com.github.halfdeadgames.asciigdx.render;

/**
 * A glyph placed off the grid: a position in virtual pixels, plus rotation, scale, color, and
 * visibility. Position is the top-left corner of the sprite's unscaled cell box, measured from
 * the layer's top-left corner with y growing downward like grid rows.
 *
 * <p>Rotation and scale happen about a pivot, given as a fraction of the cell box. Values are
 * not clamped, except alpha.
 *
 * <p>Sprites are created by {@link Layer#addSprite} and drawn with the layer's effects, offset,
 * clip, and visibility. A sprite holds no GPU resources and needs no disposing; the layer's
 * renderer owns the GPU data. Once the sprite is removed from its layer (by
 * {@link Layer#removeSprite}, or because the layer was removed or its stack disposed), getters
 * keep working but every setter and mutator throws {@link IllegalStateException}.
 */
public final class GlyphSprite implements SpriteTransform {

    private static final int ALPHA_MASK = 0xFF;
    private static final float MAX_ALPHA_BYTE = 255f;

    private Layer owner;
    private boolean removed;
    private int codePoint;
    private int color;
    private float x;
    private float y;
    private float rotation;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;
    private boolean visible = true;

    GlyphSprite(int codePoint, int color) {
        this.codePoint = codePoint;
        this.color = color;
    }

    GlyphSprite(Layer owner, int codePoint, int color) {
        this(codePoint, color);
        this.owner = owner;
    }

    Layer getOwner() {
        return owner;
    }

    void detach() {
        owner = null;
        removed = true;
    }

    private void requireNotRemoved() {
        if (removed) {
            throw new IllegalStateException("Sprite was removed from its layer");
        }
    }

    /** The glyph's code point. */
    public int getCodePoint() {
        return codePoint;
    }

    /**
     * Sets the glyph's code point.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setCodePoint(int codePoint) {
        requireNotRemoved();
        this.codePoint = codePoint;
    }

    /** The glyph color as RGBA8888, the same format as the grid colors. */
    public int getColor() {
        return color;
    }

    /**
     * Sets the glyph color as RGBA8888.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setColor(int color) {
        requireNotRemoved();
        this.color = color;
    }

    /** The alpha of the glyph color, from 0 to 1. */
    public float getAlpha() {
        return (color & ALPHA_MASK) / MAX_ALPHA_BYTE;
    }

    /**
     * Sets only the alpha of the glyph color, clamped to 0..1 and rounded to a byte.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setAlpha(float alpha) {
        requireNotRemoved();
        float clamped = Math.max(0f, Math.min(1f, alpha));
        int alphaByte = Math.round(clamped * MAX_ALPHA_BYTE);
        color = (color & ~ALPHA_MASK) | alphaByte;
    }

    /** Horizontal position of the cell box's left edge, in virtual pixels. */
    public float getX() {
        return x;
    }

    /** Vertical position of the cell box's top edge, in virtual pixels, growing downward. */
    public float getY() {
        return y;
    }

    /**
     * Places the top-left corner of the unscaled cell box, in virtual pixels.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setPosition(float x, float y) {
        requireNotRemoved();
        this.x = x;
        this.y = y;
    }

    /**
     * Moves the sprite by the given virtual pixels; positive y moves down.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void moveBy(float dx, float dy) {
        requireNotRemoved();
        x += dx;
        y += dy;
    }

    /** Rotation in degrees; positive is counterclockwise on screen. */
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation in degrees; positive is counterclockwise on screen.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setRotation(float degrees) {
        requireNotRemoved();
        rotation = degrees;
    }

    /**
     * Adds to the rotation, in degrees.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void rotateBy(float degrees) {
        requireNotRemoved();
        rotation += degrees;
    }

    /** Horizontal scale; 1 is unscaled, negative mirrors. */
    public float getScaleX() {
        return scaleX;
    }

    /** Vertical scale; 1 is unscaled, negative mirrors. */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the same scale on both axes.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setScale(float scale) {
        setScale(scale, scale);
    }

    /**
     * Sets the scale per axis; negative values mirror.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setScale(float scaleX, float scaleY) {
        requireNotRemoved();
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /** Horizontal pivot as a fraction of the cell box: 0 is left, 1 is right. */
    public float getPivotX() {
        return pivotX;
    }

    /** Vertical pivot as a fraction of the cell box: 0 is top, 1 is bottom. */
    public float getPivotY() {
        return pivotY;
    }

    /**
     * Sets the pivot for rotation and scale, as fractions of the cell box. Defaults to the center.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setPivot(float pivotX, float pivotY) {
        requireNotRemoved();
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    /** Whether the sprite is drawn. */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Shows or hides the sprite.
     *
     * @throws IllegalStateException if the sprite was removed
     */
    public void setVisible(boolean visible) {
        requireNotRemoved();
        this.visible = visible;
    }

    /**
     * Whether the sprite was removed from its layer: by {@link Layer#removeSprite},
     * {@link Layer#removeFreeSprites}, {@link Layer#clearAllSprites}, or because the layer was
     * removed or its stack disposed. A removed sprite cannot be changed.
     */
    public boolean isRemoved() {
        return removed;
    }
}
