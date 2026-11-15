package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** Game state of one lily pad; the display reads it every frame. */
public final class Lily {

    private static final int BULLET_CODE_POINT = 0x2022;

    /** The color of a lily nobody recolored, as RGBA. */
    public static final int BASE_COLOR = LilyPalette.BASE_GREEN;

    private float offsetX;
    private float offsetY;
    private float scale = 1f;
    private int color = BASE_COLOR;

    /** Returns the lily's color as RGBA. */
    public int getColor() {
        return color;
    }

    /** Sets the lily's color as RGBA. */
    public void setColor(int color) {
        this.color = color;
    }

    /** Returns the glyph code point a lily is drawn with. */
    public int codePoint() {
        return BULLET_CODE_POINT;
    }

    /** Returns the horizontal offset in lily cells; 0 is centered on the slot. */
    public float getOffsetX() {
        return offsetX;
    }

    /** Sets the horizontal offset in lily cells. */
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    /** Returns the vertical offset in lily cells; 0 is centered on the slot. */
    public float getOffsetY() {
        return offsetY;
    }

    /** Sets the vertical offset in lily cells. */
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    /** Returns the draw scale; 1 is the normal size. */
    public float getScale() {
        return scale;
    }

    /** Sets the draw scale. */
    public void setScale(float scale) {
        this.scale = scale;
    }
}
