package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;

/**
 * The color effects the GPU applies to everything a {@link Layer} or a {@link LayerGroup} draws:
 * saturation, then tint, then mix, then invert, with the alpha taken from the tint. Each layer and
 * each group owns exactly one instance and reuses it every frame; there is no public constructor.
 *
 * <p>Every change takes effect on the GPU in the next frame. Changing an effect rewrites and uploads
 * nothing, so it is cheap to change every frame, for example to fade a layer or flash it white.
 * Values are never clamped, and the defaults leave every color unchanged: tint white (1, 1, 1, 1),
 * mix amount 0, saturation 1, invert 0.
 *
 * <p>A layer's effects apply to that layer's cells and sprites. A group's effects apply to every
 * member layer after the member's own effects: the color transforms run in sequence and the alphas
 * multiply.
 *
 * <p>Once the owning layer or group is removed from its stack, the getters keep working but every
 * setter throws {@link IllegalStateException}.
 *
 * <p>Example, a white hit flash that fades out over time:
 * <pre>{@code
 * effects.setMix(1f, 1f, 1f, flashStrength);
 * effects.setSaturation(0f);
 * effects.setTint(0.9f, 0.7f, 0.4f, 1f);
 * }</pre>
 */
public final class ColorEffects {

    private float tintRed = 1f;
    private float tintGreen = 1f;
    private float tintBlue = 1f;
    private float tintAlpha = 1f;
    private float mixRed = 1f;
    private float mixGreen = 1f;
    private float mixBlue = 1f;
    private float mixAmount;
    private float saturation = 1f;
    private float invert;
    private boolean detached;

    ColorEffects() {
    }

    /** Returns the tint's red component, 1 by default. */
    public float getTintRed() {
        return tintRed;
    }

    /** Returns the tint's green component, 1 by default. */
    public float getTintGreen() {
        return tintGreen;
    }

    /** Returns the tint's blue component, 1 by default. */
    public float getTintBlue() {
        return tintBlue;
    }

    /** Returns the tint's alpha component, 1 by default. */
    public float getTintAlpha() {
        return tintAlpha;
    }

    /**
     * Copies the tint into a color.
     *
     * @param out the color to overwrite; must not be null
     * @return {@code out}
     */
    public Color getTint(Color out) {
        return out.set(tintRed, tintGreen, tintBlue, tintAlpha);
    }

    /** Returns the mix color's red component, 1 by default. */
    public float getMixRed() {
        return mixRed;
    }

    /** Returns the mix color's green component, 1 by default. */
    public float getMixGreen() {
        return mixGreen;
    }

    /** Returns the mix color's blue component, 1 by default. */
    public float getMixBlue() {
        return mixBlue;
    }

    /** Returns how far colors are mixed toward the mix color, 0 for not at all (the default). */
    public float getMixAmount() {
        return mixAmount;
    }

    /** Returns the saturation, 1 for normal (the default). */
    public float getSaturation() {
        return saturation;
    }

    /** Returns the invert amount, 0 for normal (the default). */
    public float getInvert() {
        return invert;
    }

    /**
     * Sets the tint, a color multiplied into every drawn background and glyph color. The default is
     * white (1, 1, 1, 1), which leaves colors unchanged. Values are not clamped.
     *
     * <p>The tint is applied after saturation and before the mix and the invert (see
     * {@link #setSaturation}, {@link #setMix(float, float, float, float)}, and {@link #setInvert}).
     *
     * <p>Alpha fades each cell as one flat color, so a glyph's own background does not show through
     * it; whatever is drawn below shows through the whole faded cell. Inside a cell, glyph and
     * background always combine with normal "over" blending. The batch's blend functions decide only
     * how the finished cell combines with what is below. A group's alpha multiplies with the alpha of
     * each member layer's own tint.
     *
     * @param red the red multiplier, 1 for unchanged
     * @param green the green multiplier, 1 for unchanged
     * @param blue the blue multiplier, 1 for unchanged
     * @param alpha the opacity multiplier, 1 for opaque and 0 for invisible
     * @throws IllegalStateException if the owning layer or group was removed
     */
    public void setTint(float red, float green, float blue, float alpha) {
        requireAttached();
        assignTint(red, green, blue, alpha);
    }

    /**
     * Sets the tint from a color. The values are copied; no reference is kept.
     *
     * @param color the tint, including its alpha; must not be null
     * @throws IllegalStateException if the owning layer or group was removed
     * @see #setTint(float, float, float, float)
     */
    public void setTint(Color color) {
        setTint(color.r, color.g, color.b, color.a);
    }

    /**
     * Sets only the tint's alpha, keeping red, green, and blue.
     *
     * @param alpha the opacity multiplier, 1 for opaque and 0 for invisible; not clamped
     * @throws IllegalStateException if the owning layer or group was removed
     * @see #setTint(float, float, float, float)
     */
    public void setAlpha(float alpha) {
        requireAttached();
        tintAlpha = alpha;
    }

    /**
     * Mixes every drawn color toward the given color, for hit flashes (white, amount fading to 0)
     * and fog washes. Amount 0 leaves colors unchanged (the default, with white as the color); 1
     * turns everything drawn into the color. Only colors change: alpha is untouched, so transparent
     * parts stay transparent. Values are not clamped.
     *
     * <p>The mix is applied after saturation and the tint, so a flash can reach its pure color even
     * on a darkly tinted layer. Only the invert follows it (see {@link #setInvert}).
     *
     * @param red the mix color's red component
     * @param green the mix color's green component
     * @param blue the mix color's blue component
     * @param amount how far to mix toward the color, 0 for not at all and 1 for fully
     * @throws IllegalStateException if the owning layer or group was removed
     * @see CellHandle#setMix(int, float) the cell-level mix, which takes an RGBA8888 int and
     *     changes one cell's own colors before these effects run
     */
    public void setMix(float red, float green, float blue, float amount) {
        requireAttached();
        assignMix(red, green, blue, amount);
    }

    /**
     * Sets the mix from a color's red, green, and blue; its alpha is ignored. The values are
     * copied; no reference is kept.
     *
     * @param color the mix color; must not be null
     * @param amount how far to mix toward the color, 0 for not at all and 1 for fully
     * @throws IllegalStateException if the owning layer or group was removed
     * @see #setMix(float, float, float, float)
     */
    public void setMix(Color color, float amount) {
        setMix(color.r, color.g, color.b, amount);
    }

    /**
     * Sets the saturation: 1 is normal (the default), 0 is grayscale, and values above 1
     * oversaturate. Values are not clamped.
     *
     * <p>Saturation is applied first, before the tint, the mix, and the invert. Saturation 0 plus a
     * tint gives a one-hue look, such as sepia or night vision.
     *
     * @param saturation the saturation, 1 for unchanged
     * @throws IllegalStateException if the owning layer or group was removed
     */
    public void setSaturation(float saturation) {
        requireAttached();
        this.saturation = saturation;
    }

    /**
     * Sets how far every drawn color is turned into its negative: 0 leaves colors unchanged (the
     * default) and 1 is a full negative. Alpha is untouched. Values are not clamped.
     *
     * <p>The invert is applied last, after saturation, the tint, and the mix. Inverting every layer
     * of a stack equals inverting the whole screen as long as the bottom layer is opaque;
     * transparent parts stay transparent, so they are not inverted.
     *
     * @param invert the invert amount, 0 for unchanged and 1 for a full negative
     * @throws IllegalStateException if the owning layer or group was removed
     */
    public void setInvert(float invert) {
        requireAttached();
        this.invert = invert;
    }

    /** Overwrites {@code out} with saturation, then tint, mix, and invert folded into one transform. */
    void writeColorMatrix(ColorMatrix out) {
        out.setSaturation(saturation);
        out.thenScale(tintRed, tintGreen, tintBlue);
        out.thenMixToward(mixRed, mixGreen, mixBlue, mixAmount);
        out.thenInvert(invert);
    }

    void copyFrom(ColorEffects other) {
        assignTint(other.tintRed, other.tintGreen, other.tintBlue, other.tintAlpha);
        assignMix(other.mixRed, other.mixGreen, other.mixBlue, other.mixAmount);
        saturation = other.saturation;
        invert = other.invert;
    }

    void reset() {
        assignTint(1f, 1f, 1f, 1f);
        assignMix(1f, 1f, 1f, 0f);
        saturation = 1f;
        invert = 0f;
    }

    /** Makes every public setter throw from now on; called when the owner is removed. */
    void markDetached() {
        detached = true;
    }

    private void requireAttached() {
        if (detached) {
            throw new IllegalStateException(
                    "The layer or group that owns these color effects was removed from its stack");
        }
    }

    private void assignTint(float red, float green, float blue, float alpha) {
        tintRed = red;
        tintGreen = green;
        tintBlue = blue;
        tintAlpha = alpha;
    }

    private void assignMix(float red, float green, float blue, float amount) {
        mixRed = red;
        mixGreen = green;
        mixBlue = blue;
        mixAmount = amount;
    }
}
