package com.github.halfdeadgames.asciigdx.layer;

/**
 * The rules that fold a cell's lasting handle and frame handle into the one handle the renderer
 * draws. Offsets and rotations add, scales and alpha multiply, the pivot is the lasting handle's,
 * a frame glyph wins over a lasting glyph, and the frame mix is applied on top of the lasting mix.
 * Nothing here allocates.
 */
final class CellHandleCombine {

    /** Stands in for a cell that has no lasting handle: every value at its default. Never modified. */
    static final CellHandle NO_EFFECT = new CellHandle(0, 0);

    private static final int CHANNEL_MASK = 0xFF;
    private static final int OPAQUE = 0xFF;
    private static final float MAX_CHANNEL = 255f;

    private CellHandleCombine() {
    }

    /**
     * Writes the combined values into {@code target}. Colors and glyph are written through the
     * handle's setters, so its overrides count as changed only when the combined value differs from
     * what it held.
     *
     * @param lasting the cell's lasting handle, or {@link #NO_EFFECT}
     * @param frame the cell's frame handle, or {@link #NO_EFFECT}
     */
    static void combine(CellHandle target, CellHandle lasting, CellHandle frame) {
        combineMotion(target, lasting, frame);
        combineMixes(target, lasting, frame);
        target.setCodePoint(codePoint(lasting.getCodePoint(), frame.getCodePoint()));
    }

    private static void combineMotion(CellHandle target, CellHandle lasting, CellHandle frame) {
        target.setOffset(lasting.getOffsetX() + frame.getOffsetX(), lasting.getOffsetY() + frame.getOffsetY());
        target.setRotation(lasting.getRotation() + frame.getRotation());
        target.setScale(lasting.getScaleX() * frame.getScaleX(), lasting.getScaleY() * frame.getScaleY());
        target.setPivot(lasting.getPivotX(), lasting.getPivotY());
        target.setAlphaMultiplier(lasting.getAlphaMultiplier() * frame.getAlphaMultiplier());
    }

    private static void combineMixes(CellHandle target, CellHandle lasting, CellHandle frame) {
        target.setForegroundMix(
                mixColor(lasting.getForegroundMixColor(), lasting.getForegroundMixAmount(),
                        frame.getForegroundMixColor(), frame.getForegroundMixAmount()),
                mixAmount(lasting.getForegroundMixAmount(), frame.getForegroundMixAmount()));
        target.setBackgroundMix(
                mixColor(lasting.getBackgroundMixColor(), lasting.getBackgroundMixAmount(),
                        frame.getBackgroundMixColor(), frame.getBackgroundMixAmount()),
                mixAmount(lasting.getBackgroundMixAmount(), frame.getBackgroundMixAmount()));
    }

    /** The frame glyph if one is set, otherwise the lasting glyph; either may be {@link CellHandle#NO_CODE_POINT}. */
    static int codePoint(int lastingCodePoint, int frameCodePoint) {
        if (frameCodePoint != CellHandle.NO_CODE_POINT) {
            return frameCodePoint;
        }
        return lastingCodePoint;
    }

    /**
     * The amount of the one mix equal to mixing by {@code lastingAmount} and then by
     * {@code frameAmount}: {@code a1 + a2 - a1 * a2}. If only one is above 0 it is returned as is.
     */
    static float mixAmount(float lastingAmount, float frameAmount) {
        if (!(lastingAmount > 0f)) {
            return frameAmount;
        }
        if (!(frameAmount > 0f)) {
            return lastingAmount;
        }
        return Math.min(1f, lastingAmount + frameAmount - lastingAmount * frameAmount);
    }

    /**
     * The target color of the one mix equal to mixing toward {@code lastingRgba} by
     * {@code lastingAmount} and then toward {@code frameRgba} by {@code frameAmount}. If only one
     * amount is above 0 its color is returned as is; if neither is, 0, so an unused mix looks the
     * same whatever color was stored. The result's alpha byte is opaque; mixes ignore it.
     *
     * <p>Composing is exact in real numbers. The renderer rounds each color channel to a byte once
     * per mix, so the result can differ from applying the two mixes one after the other by 1 per
     * channel.
     */
    static int mixColor(int lastingRgba, float lastingAmount, int frameRgba, float frameAmount) {
        if (!(lastingAmount > 0f)) {
            return frameAmount > 0f ? frameRgba : 0;
        }
        if (!(frameAmount > 0f)) {
            return lastingRgba;
        }
        float composedAmount = mixAmount(lastingAmount, frameAmount);
        float lastingWeight = lastingAmount * (1f - frameAmount);
        int red = composedChannel(lastingRgba, frameRgba, 24, lastingWeight, frameAmount, composedAmount);
        int green = composedChannel(lastingRgba, frameRgba, 16, lastingWeight, frameAmount, composedAmount);
        int blue = composedChannel(lastingRgba, frameRgba, 8, lastingWeight, frameAmount, composedAmount);
        return (red << 24) | (green << 16) | (blue << 8) | OPAQUE;
    }

    private static int composedChannel(int lastingRgba, int frameRgba, int shift, float lastingWeight,
            float frameWeight, float composedAmount) {
        int lasting = (lastingRgba >>> shift) & CHANNEL_MASK;
        int frame = (frameRgba >>> shift) & CHANNEL_MASK;
        float blended = (lasting * lastingWeight + frame * frameWeight) / composedAmount;
        return Math.round(Math.max(0f, Math.min(MAX_CHANNEL, blended)));
    }
}
