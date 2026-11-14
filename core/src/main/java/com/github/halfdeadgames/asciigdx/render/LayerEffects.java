package com.github.halfdeadgames.asciigdx.render;

/**
 * The per-layer color effects the cell shader applies each frame: saturation, then tint, then
 * mix, with alpha from the tint. Each layer owns one and reuses it every frame.
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

    void reset() {
        setTint(1f, 1f, 1f, 1f);
        setMix(1f, 1f, 1f, 0f);
        saturation = 1f;
    }
}
