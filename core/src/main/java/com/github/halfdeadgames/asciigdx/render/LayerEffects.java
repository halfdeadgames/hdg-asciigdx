package com.github.halfdeadgames.asciigdx.render;

/**
 * The per-layer color effects the cell shader applies each frame: saturation, then tint, then
 * mix, then invert, with alpha from the tint. Each layer owns one and reuses it every frame.
 * Values are not clamped.
 */
final class LayerEffects {

    private float tintR = 1f;
    private float tintG = 1f;
    private float tintB = 1f;
    private float tintA = 1f;
    private float mixR = 1f;
    private float mixG = 1f;
    private float mixB = 1f;
    private float mixAmount;
    private float saturation = 1f;
    private float invert;

    float getTintR() {
        return tintR;
    }

    float getTintG() {
        return tintG;
    }

    float getTintB() {
        return tintB;
    }

    float getTintA() {
        return tintA;
    }

    float getMixR() {
        return mixR;
    }

    float getMixG() {
        return mixG;
    }

    float getMixB() {
        return mixB;
    }

    float getMixAmount() {
        return mixAmount;
    }

    float getSaturation() {
        return saturation;
    }

    void setTint(float r, float g, float b, float a) {
        tintR = r;
        tintG = g;
        tintB = b;
        tintA = a;
    }

    void setTintAlpha(float a) {
        tintA = a;
    }

    void setMix(float r, float g, float b, float amount) {
        mixR = r;
        mixG = g;
        mixB = b;
        mixAmount = amount;
    }

    void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    float getInvert() {
        return invert;
    }

    void setInvert(float invert) {
        this.invert = invert;
    }

    /** Overwrites {@code out} with saturation, then tint, mix, and invert folded into one transform. */
    void writeColorMatrix(ColorMatrix out) {
        out.setSaturation(saturation);
        out.thenScale(tintR, tintG, tintB);
        out.thenMixToward(mixR, mixG, mixB, mixAmount);
        out.thenInvert(invert);
    }

    void copyFrom(LayerEffects other) {
        setTint(other.tintR, other.tintG, other.tintB, other.tintA);
        setMix(other.mixR, other.mixG, other.mixB, other.mixAmount);
        saturation = other.saturation;
        invert = other.invert;
    }

    void reset() {
        setTint(1f, 1f, 1f, 1f);
        setMix(1f, 1f, 1f, 0f);
        saturation = 1f;
        invert = 0f;
    }
}
