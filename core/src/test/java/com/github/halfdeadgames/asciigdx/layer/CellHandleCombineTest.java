package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.color.CellColor;
import org.junit.jupiter.api.Test;

class CellHandleCombineTest {

    private static final float DELTA = 0.0001f;
    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;
    private static final int STEEL = 0x6E8AFFFF;
    private static final int CHANNEL_MASK = 0xFF;

    private final CellHandle lasting = new CellHandle(1, 1);
    private final CellHandle frame = new CellHandle(null, 1, 1, true);
    private final CellHandle target = new CellHandle(1, 1);

    private void combine() {
        CellHandleCombine.combine(target, lasting, frame);
    }

    @Test
    void offsetsAndRotationsAdd() {
        lasting.setOffset(3f, -2f);
        frame.setOffset(1f, 5f);
        lasting.setRotation(30f);
        frame.setRotation(-10f);

        combine();

        assertEquals(4f, target.getOffsetX(), DELTA);
        assertEquals(3f, target.getOffsetY(), DELTA);
        assertEquals(20f, target.getRotation(), DELTA);
    }

    @Test
    void scalesAndAlphaMultiply() {
        lasting.setScale(2f, 3f);
        frame.setScale(0.5f, -1f);
        lasting.setAlphaMultiplier(0.5f);
        frame.setAlphaMultiplier(0.5f);

        combine();

        assertEquals(1f, target.getScaleX(), DELTA);
        assertEquals(-3f, target.getScaleY(), DELTA);
        assertEquals(0.25f, target.getAlphaMultiplier(), DELTA);
    }

    @Test
    void pivotComesFromTheLastingHandle() {
        lasting.setPivot(0.2f, 0.8f);

        combine();

        assertEquals(0.2f, target.getPivotX(), DELTA);
        assertEquals(0.8f, target.getPivotY(), DELTA);
    }

    @Test
    void pivotIsTheCenterWithoutALastingHandle() {
        target.setPivot(0f, 1f);
        frame.setRotation(15f);

        CellHandleCombine.combine(target, CellHandleCombine.NO_EFFECT, frame);

        assertEquals(0.5f, target.getPivotX(), DELTA);
        assertEquals(0.5f, target.getPivotY(), DELTA);
        assertEquals(15f, target.getRotation(), DELTA);
    }

    @Test
    void frameGlyphWinsOverLastingGlyph() {
        assertEquals('f', CellHandleCombine.codePoint('l', 'f'));
        assertEquals('l', CellHandleCombine.codePoint('l', CellHandle.NO_CODE_POINT));
        assertEquals('f', CellHandleCombine.codePoint(CellHandle.NO_CODE_POINT, 'f'));
        assertEquals(CellHandle.NO_CODE_POINT,
                CellHandleCombine.codePoint(CellHandle.NO_CODE_POINT, CellHandle.NO_CODE_POINT));
    }

    @Test
    void combinedHandleShowsTheFrameGlyphOnlyWhileItIsSet() {
        lasting.setCodePoint('l');
        frame.setCodePoint('f');
        combine();
        assertEquals('f', target.getCodePoint());

        frame.clearCodePoint();
        combine();
        assertEquals('l', target.getCodePoint());

        lasting.clearCodePoint();
        combine();
        assertFalse(target.hasCodePoint());
    }

    @Test
    void aMixOnOneHandleOnlyIsCopiedExactly() {
        lasting.setForegroundMix(STEEL, 0.3f);
        frame.setBackgroundMix(RED, 0.6f);

        combine();

        assertEquals(STEEL, target.getForegroundMixColor());
        assertEquals(0.3f, target.getForegroundMixAmount());
        assertEquals(RED, target.getBackgroundMixColor());
        assertEquals(0.6f, target.getBackgroundMixAmount());
    }

    @Test
    void aStoredColorWithAmountZeroIsDropped() {
        lasting.setForegroundMix(STEEL, 0f);

        combine();

        assertEquals(0, target.getForegroundMixColor());
        assertEquals(0f, target.getForegroundMixAmount());
    }

    @Test
    void composedAmountIsOneMinusTheProductOfWhatIsLeft() {
        assertEquals(0.75f, CellHandleCombine.mixAmount(0.5f, 0.5f), DELTA);
        assertEquals(1f, CellHandleCombine.mixAmount(1f, 0.3f), DELTA);
        assertEquals(1f, CellHandleCombine.mixAmount(0.3f, 1f), DELTA);
        assertEquals(0.4f, CellHandleCombine.mixAmount(0f, 0.4f), DELTA);
        assertEquals(0.4f, CellHandleCombine.mixAmount(0.4f, 0f), DELTA);
        assertEquals(0f, CellHandleCombine.mixAmount(0f, 0f), DELTA);
    }

    @Test
    void aFullFrameMixCoversTheLastingMix() {
        assertEquals(BLUE & ~CHANNEL_MASK,
                CellHandleCombine.mixColor(RED, 0.5f, BLUE, 1f) & ~CHANNEL_MASK);
    }

    @Test
    void twoMixesMatchApplyingThemOneAfterTheOtherWithinOnePerChannel() {
        int[] cellColors = {0x102030FF, 0xFFFFFF80, 0x000000FF, 0x8040C0FF};
        int[] lastingColors = {RED, 0x00FF80FF, STEEL};
        int[] frameColors = {BLUE, 0xFFFF00FF, 0x808080FF};
        float[] amounts = {0.25f, 0.5f, 0.75f, 1f};

        for (int cell : cellColors) {
            for (int lastingColor : lastingColors) {
                for (int frameColor : frameColors) {
                    for (float lastingAmount : amounts) {
                        for (float frameAmount : amounts) {
                            assertComposedMatchesSequence(cell, lastingColor, lastingAmount,
                                    frameColor,
                                    frameAmount);
                        }
                    }
                }
            }
        }
    }

    private static void assertComposedMatchesSequence(int cell, int lastingColor,
            float lastingAmount,
            int frameColor, float frameAmount) {
        int sequential = CellColor.mixRgbKeepingAlpha(
                CellColor.mixRgbKeepingAlpha(cell, lastingColor, lastingAmount), frameColor,
                frameAmount);
        int composedColor =
                CellHandleCombine.mixColor(lastingColor, lastingAmount, frameColor, frameAmount);
        float composedAmount = CellHandleCombine.mixAmount(lastingAmount, frameAmount);
        int composed = CellColor.mixRgbKeepingAlpha(cell, composedColor, composedAmount);

        assertEquals(sequential & CHANNEL_MASK, composed & CHANNEL_MASK);
        for (int shift = 8; shift <= 24; shift += 8) {
            int sequentialChannel = (sequential >>> shift) & CHANNEL_MASK;
            int composedChannel = (composed >>> shift) & CHANNEL_MASK;
            assertTrue(Math.abs(sequentialChannel - composedChannel) <= 1,
                    "cell " + Integer.toHexString(cell) + " mixes " + lastingAmount + ", "
                            + frameAmount
                            + " shift " + shift + ": " + sequentialChannel + " vs "
                            + composedChannel);
        }
    }

    @Test
    void twoMixesOnTheHandlesCombineIntoOne() {
        lasting.setForegroundMix(RED, 0.5f);
        frame.setForegroundMix(BLUE, 0.5f);

        combine();

        assertEquals(0.75f, target.getForegroundMixAmount(), DELTA);
        assertEquals(0x55, (target.getForegroundMixColor() >>> 24) & CHANNEL_MASK, 1);
        assertEquals(0xAA, (target.getForegroundMixColor() >>> 8) & CHANNEL_MASK, 1);
    }

    @Test
    void overridesCountAsChangedOnlyWhenTheCombinedValueDiffers() {
        frame.setForegroundMix(RED, 0.5f);
        frame.setCodePoint('f');
        combine();
        assertTrue(target.areOverridesChanged());

        target.clearOverridesChanged();
        combine();
        assertFalse(target.areOverridesChanged());

        frame.setForegroundMix(RED, 0.25f);
        combine();
        assertTrue(target.areOverridesChanged());
    }

    @Test
    void noEffectRejectsChanges() {
        assertThrows(IllegalStateException.class,
                () -> CellHandleCombine.NO_EFFECT.setOffset(1f, 1f));
        assertThrows(IllegalStateException.class, CellHandleCombine.NO_EFFECT::reset);
    }

    @Test
    void combiningWithNoEffectGivesDefaults() {
        CellHandleCombine.combine(target, CellHandleCombine.NO_EFFECT,
                CellHandleCombine.NO_EFFECT);

        assertFalse(target.isLifted());
        assertFalse(target.hasOverrides());
    }

    @Test
    void combiningWithNothingToAddLeavesDefaults() {
        combine();

        assertFalse(target.isLifted());
        assertFalse(target.hasOverrides());
        assertFalse(target.areOverridesChanged());
    }
}
