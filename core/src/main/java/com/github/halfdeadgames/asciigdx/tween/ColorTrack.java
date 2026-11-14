package com.github.halfdeadgames.asciigdx.tween;

import com.github.halfdeadgames.asciigdx.color.CellColor;

/**
 * A color over time, mixed in Oklab between two RGBA8888 colors by an amount track. Amount 0 is
 * {@code fromRgba}, amount 1 is {@code toRgba}. Amounts outside 0..1 (overshoot easings) are
 * clamped, so the color stops at the end colors.
 */
public final class ColorTrack {

    private final FloatTrack amount;
    private final int fromRgba;
    private final int toRgba;

    public ColorTrack(FloatTrack amount, int fromRgba, int toRgba) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        this.amount = amount;
        this.fromRgba = fromRgba;
        this.toRgba = toRgba;
    }

    public int colorAt(float timeSeconds) {
        return CellColor.mixOklab(fromRgba, toRgba, amount.valueAt(timeSeconds));
    }

    public float start() {
        return amount.start();
    }

    public float end() {
        return amount.end();
    }

    public int fromRgba() {
        return fromRgba;
    }

    public int toRgba() {
        return toRgba;
    }
}
