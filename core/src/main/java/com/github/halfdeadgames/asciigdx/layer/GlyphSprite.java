package com.github.halfdeadgames.asciigdx.layer;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/**
 * A glyph placed off the grid: a position in virtual pixels, plus rotation, scale, color, and
 * visibility. Position is the top-left corner of the sprite's unscaled cell box, measured from
 * the layer's top-left corner with y growing downward like grid rows.
 *
 * <p>Rotation and scale happen about a pivot, given as a fraction of the cell box. Rotation is in
 * degrees, positive clockwise on screen. Values are not clamped, except alpha.
 *
 * <p>Sprites are created by {@link Layer#addSprite} and drawn with the layer's effects, offset,
 * clip, and visibility. A sprite holds no GPU resources and needs no disposing; the layer's
 * renderer owns the GPU data. Once the sprite is removed from its layer (by
 * {@link Layer#removeSprite}, or because the layer was removed or its stack disposed), getters
 * keep working but every setter and mutator throws {@link IllegalStateException}.
 *
 * <p>Change a sprite from the game thread, for example once per frame. Changes show at the next
 * draw. Example, a glyph that drifts right and spins:
 * <pre>{@code
 * sprite.moveBy(speed * deltaSeconds, 0f);
 * sprite.rotateBy(90f * deltaSeconds);
 * }</pre>
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

    /**
     * The glyph's code point.
     *
     * @return the Unicode code point drawn for this sprite
     */
    public int getCodePoint() {
        return codePoint;
    }

    /**
     * Sets the glyph's code point. Any int is accepted, like a grid write; a value the font lacks
     * shows the glyph source's fallback glyph.
     *
     * @param codePoint glyph as a Unicode code point
     * @throws IllegalStateException if the sprite was removed
     */
    public void setCodePoint(int codePoint) {
        requireNotRemoved();
        this.codePoint = codePoint;
    }

    /**
     * The glyph color as RGBA8888, the same format as the grid colors.
     *
     * @return the color including its alpha byte, for example {@code 0xFF6E8AFF}
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets the glyph color as RGBA8888, including its alpha byte.
     *
     * @param color the color as an RGBA8888 int, for example {@code 0xFF6E8AFF}
     * @throws IllegalStateException if the sprite was removed
     */
    public void setColor(int color) {
        requireNotRemoved();
        this.color = color;
    }

    /**
     * The alpha of the glyph color, from 0 to 1.
     *
     * @return the alpha, 0 for transparent and 1 for opaque, in steps of 1/255
     */
    public float getAlpha() {
        return (color & ALPHA_MASK) / MAX_ALPHA_BYTE;
    }

    /**
     * Sets only the alpha of the glyph color, clamped to 0..1 and rounded to a byte. The red,
     * green, and blue bytes are kept.
     *
     * @param alpha the new alpha, 0 for transparent and 1 for opaque; values outside 0..1 are clamped
     * @throws IllegalStateException if the sprite was removed
     */
    public void setAlpha(float alpha) {
        requireNotRemoved();
        float clamped = Math.max(0f, Math.min(1f, alpha));
        int alphaByte = Math.round(clamped * MAX_ALPHA_BYTE);
        color = (color & ~ALPHA_MASK) | alphaByte;
    }

    /**
     * Horizontal position of the cell box's left edge, in virtual pixels from the layer's left edge.
     *
     * @return the x position in virtual pixels
     */
    public float getX() {
        return x;
    }

    /**
     * Vertical position of the cell box's top edge, in virtual pixels from the layer's top edge,
     * growing downward.
     *
     * @return the y position in virtual pixels
     */
    public float getY() {
        return y;
    }

    /**
     * Places the top-left corner of the unscaled cell box, in virtual pixels from the layer's
     * top-left corner. Positive y is down. Values are not clamped, so a sprite can sit outside the
     * layer.
     *
     * @param x horizontal position in virtual pixels
     * @param y vertical position in virtual pixels, growing downward
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
     * @param dx horizontal distance in virtual pixels; positive moves right
     * @param dy vertical distance in virtual pixels; positive moves down
     * @throws IllegalStateException if the sprite was removed
     */
    public void moveBy(float dx, float dy) {
        requireNotRemoved();
        x += dx;
        y += dy;
    }

    /**
     * Rotation in degrees; positive is clockwise on screen.
     *
     * @return the rotation in degrees, not wrapped to a single turn
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation in degrees; positive is clockwise on screen. The rotation is about the
     * pivot and is not wrapped.
     *
     * @param degrees the rotation in degrees
     * @throws IllegalStateException if the sprite was removed
     */
    public void setRotation(float degrees) {
        requireNotRemoved();
        rotation = degrees;
    }

    /**
     * Adds to the rotation, in degrees; positive turns clockwise on screen.
     *
     * @param degrees the angle to add in degrees; negative turns counterclockwise
     * @throws IllegalStateException if the sprite was removed
     */
    public void rotateBy(float degrees) {
        requireNotRemoved();
        rotation += degrees;
    }

    /**
     * Horizontal scale; 1 is unscaled, negative mirrors.
     *
     * @return the horizontal scale factor
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * Vertical scale; 1 is unscaled, negative mirrors.
     *
     * @return the vertical scale factor
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the same scale on both axes, about the pivot.
     *
     * @param scale the scale factor for both axes; 1 is unscaled, negative mirrors
     * @throws IllegalStateException if the sprite was removed
     */
    public void setScale(float scale) {
        setScale(scale, scale);
    }

    /**
     * Sets the scale per axis, about the pivot; negative values mirror.
     *
     * @param scaleX the horizontal scale factor; 1 is unscaled
     * @param scaleY the vertical scale factor; 1 is unscaled
     * @throws IllegalStateException if the sprite was removed
     */
    public void setScale(float scaleX, float scaleY) {
        requireNotRemoved();
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Horizontal pivot as a fraction of the cell box: 0 is left, 1 is right.
     *
     * @return the horizontal pivot, 0.5 by default
     */
    public float getPivotX() {
        return pivotX;
    }

    /**
     * Vertical pivot as a fraction of the cell box: 0 is top, 1 is bottom.
     *
     * @return the vertical pivot, 0.5 by default
     */
    public float getPivotY() {
        return pivotY;
    }

    /**
     * Sets the pivot for rotation and scale, as fractions of the cell box. Defaults to the center.
     * Moving the pivot of a rotated or scaled sprite moves the glyph on screen; use
     * {@link #setPivotKeepingPlacement} to avoid that.
     *
     * @param pivotX horizontal pivot as a fraction of the cell box: 0 is left, 1 is right
     * @param pivotY vertical pivot as a fraction of the cell box: 0 is top, 1 is bottom
     * @throws IllegalStateException if the sprite was removed
     */
    public void setPivot(float pivotX, float pivotY) {
        requireNotRemoved();
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    /**
     * Moves the pivot and adjusts the position so the glyph stays where it is drawn now. Placement
     * is kept for the rotation and scale set at the time of the call; later changes to either
     * happen about the new pivot. {@link #setPivot} alone moves the glyph when the sprite is
     * rotated or scaled.
     *
     * @param pivotX horizontal pivot as a fraction of the cell box
     * @param pivotY vertical pivot as a fraction of the cell box
     * @throws IllegalStateException if the sprite was removed
     */
    public void setPivotKeepingPlacement(float pivotX, float pivotY) {
        requireNotRemoved();
        GlyphSource glyphSource = owner.getGlyphSource();
        float shiftX = (pivotX - this.pivotX) * glyphSource.getCellWidth();
        float shiftY = (pivotY - this.pivotY) * glyphSource.getCellHeight();
        float newX = SpritePlacement.xKeepingPlacement(x, shiftX, shiftY, scaleX, scaleY, rotation);
        float newY = SpritePlacement.yKeepingPlacement(y, shiftX, shiftY, scaleX, scaleY, rotation);
        setPivot(pivotX, pivotY);
        setPosition(newX, newY);
    }

    /**
     * Whether the sprite is shown. A visible sprite whose color alpha is 0 is skipped too.
     *
     * @return true if the sprite is shown, false if it was hidden with {@link #setVisible}
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Shows or hides the sprite. A hidden sprite keeps its state and stays in its layer.
     *
     * @param visible true to show the sprite, false to hide it
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
     *
     * @return true if the sprite was removed and every setter now throws
     */
    public boolean isRemoved() {
        return removed;
    }
}
