package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellHandleTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    private final CellHandle cell = new CellHandle(3, 2);

    @Test
    void startsAtDefaultsAndIsNotLifted() {
        assertEquals(3, cell.getColumn());
        assertEquals(2, cell.getRow());
        assertEquals(0f, cell.getOffsetX());
        assertEquals(0f, cell.getOffsetY());
        assertEquals(0f, cell.getRotation());
        assertEquals(1f, cell.getScaleX());
        assertEquals(1f, cell.getScaleY());
        assertEquals(0.5f, cell.getPivotX());
        assertEquals(0.5f, cell.getPivotY());
        assertEquals(1f, cell.getAlphaMultiplier());
        assertFalse(cell.isLifted());
        assertFalse(cell.isDetached());
    }

    @Test
    void eachValueLiftsTheGlyph() {
        cell.setOffset(1f, 0f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setOffset(0f, -1f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setRotation(10f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setScale(1f, 2f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setScale(-1f, 1f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setAlphaMultiplier(0.9f);
        assertTrue(cell.isLifted());
    }

    @Test
    void wholeTurnsAndPivotDoNotLift() {
        cell.setRotation(360f);
        assertFalse(cell.isLifted());
        cell.setRotation(-720f);
        assertFalse(cell.isLifted());
        cell.setRotation(0f);
        cell.setPivot(0f, 1f);
        assertFalse(cell.isLifted());
    }

    @Test
    void alphaIsClamped() {
        cell.setAlphaMultiplier(2f);
        assertEquals(1f, cell.getAlphaMultiplier());
        cell.setAlphaMultiplier(-1f);
        assertEquals(0f, cell.getAlphaMultiplier());
    }

    @Test
    void relativeMutatorsAdd() {
        cell.offsetBy(1f, 2f);
        cell.offsetBy(1f, 2f);
        cell.rotateBy(30f);
        cell.rotateBy(30f);
        assertEquals(2f, cell.getOffsetX());
        assertEquals(4f, cell.getOffsetY());
        assertEquals(60f, cell.getRotation());
    }

    @Test
    void resetRestoresEveryDefaultIncludingThePivot() {
        cell.setOffset(1f, 1f);
        cell.setRotation(5f);
        cell.setScale(2f);
        cell.setPivot(0f, 0f);
        cell.setAlphaMultiplier(0.2f);

        cell.reset();

        assertFalse(cell.isLifted());
        assertEquals(0.5f, cell.getPivotX());
        assertEquals(0.5f, cell.getPivotY());
        assertEquals(1f, cell.getScaleX());
        assertEquals(1f, cell.getAlphaMultiplier());
    }

    @Test
    void detachedHandlesKeepGettersAndRejectMutators() {
        cell.setOffset(4f, 5f);
        cell.detach();

        assertTrue(cell.isDetached());
        assertEquals(4f, cell.getOffsetX());
        assertEquals(5f, cell.getOffsetY());
        assertThrows(IllegalStateException.class, () -> cell.setOffset(0f, 0f));
        assertThrows(IllegalStateException.class, () -> cell.offsetBy(1f, 1f));
        assertThrows(IllegalStateException.class, () -> cell.setRotation(1f));
        assertThrows(IllegalStateException.class, () -> cell.rotateBy(1f));
        assertThrows(IllegalStateException.class, () -> cell.setScale(1f));
        assertThrows(IllegalStateException.class, () -> cell.setScale(1f, 1f));
        assertThrows(IllegalStateException.class, () -> cell.setPivot(0f, 0f));
        assertThrows(IllegalStateException.class, () -> cell.setAlphaMultiplier(0.5f));
        assertThrows(IllegalStateException.class, cell::reset);
        assertThrows(IllegalStateException.class, cell::resetTransform);
        assertThrows(IllegalStateException.class, cell::clearOverrides);
        assertThrows(IllegalStateException.class, () -> cell.setForegroundMix(RED, 0.5f));
        assertThrows(IllegalStateException.class, () -> cell.setBackgroundMix(RED, 0.5f));
        assertThrows(IllegalStateException.class, () -> cell.setMix(RED, 0.5f));
        assertThrows(IllegalStateException.class, () -> cell.setMix(RED, 0.5f, BLUE, 0.5f));
        assertThrows(IllegalStateException.class, () -> cell.setCodePoint('a'));
        assertThrows(IllegalStateException.class, cell::clearCodePoint);
        assertThrows(IllegalStateException.class, cell::clearMix);
    }

    @Test
    void startsWithoutOverrides() {
        assertFalse(cell.hasOverrides());
        assertFalse(cell.hasCodePoint());
        assertEquals(0f, cell.getForegroundMixAmount());
        assertEquals(0f, cell.getBackgroundMixAmount());
        assertFalse(cell.areOverridesChanged());
    }

    @Test
    void mixSettersStoreAndClampAmounts() {
        cell.setForegroundMix(RED, 0.25f);
        cell.setBackgroundMix(BLUE, 2f);

        assertEquals(RED, cell.getForegroundMixColor());
        assertEquals(0.25f, cell.getForegroundMixAmount());
        assertEquals(BLUE, cell.getBackgroundMixColor());
        assertEquals(1f, cell.getBackgroundMixAmount());
        cell.setForegroundMix(RED, -1f);
        assertEquals(0f, cell.getForegroundMixAmount());
        cell.setForegroundMix(RED, Float.NaN);
        assertEquals(0f, cell.getForegroundMixAmount());
        assertTrue(cell.hasOverrides());
    }

    @Test
    void sharedMixSetsBothSides() {
        cell.setMix(RED, 0.5f);

        assertEquals(RED, cell.getForegroundMixColor());
        assertEquals(RED, cell.getBackgroundMixColor());
        assertEquals(0.5f, cell.getForegroundMixAmount());
        assertEquals(0.5f, cell.getBackgroundMixAmount());
    }

    @Test
    void fourArgumentMixSetsEachSideSeparately() {
        cell.setMix(RED, 0.1f, BLUE, 0.9f);

        assertEquals(RED, cell.getForegroundMixColor());
        assertEquals(0.1f, cell.getForegroundMixAmount());
        assertEquals(BLUE, cell.getBackgroundMixColor());
        assertEquals(0.9f, cell.getBackgroundMixAmount());
    }

    @Test
    void codePointStoresAndClears() {
        assertFalse(cell.hasCodePoint());

        cell.setCodePoint('x');
        assertTrue(cell.hasCodePoint());
        assertEquals('x', cell.getCodePoint());
        assertTrue(cell.hasOverrides());

        cell.clearCodePoint();
        assertFalse(cell.hasCodePoint());
        assertFalse(cell.hasOverrides());
    }

    @Test
    void codePointZeroIsAValidOverride() {
        cell.setCodePoint(0);

        assertTrue(cell.hasCodePoint());
        assertEquals(0, cell.getCodePoint());
    }

    @Test
    void getCodePointReturnsTheSentinelWhenNoneIsSet() {
        assertEquals(CellHandle.NO_CODE_POINT, cell.getCodePoint());
    }

    @Test
    void anyIntOtherThanTheSentinelIsAccepted() {
        cell.setCodePoint(-1);
        assertEquals(-1, cell.getCodePoint());
        cell.setCodePoint(0x110000);
        assertEquals(0x110000, cell.getCodePoint());
        assertTrue(cell.hasCodePoint());
    }

    @Test
    void settingTheSentinelClearsLikeClearCodePoint() {
        cell.setCodePoint('x');
        cell.clearOverridesChanged();

        cell.setCodePoint(CellHandle.NO_CODE_POINT);

        assertFalse(cell.hasCodePoint());
        assertEquals(CellHandle.NO_CODE_POINT, cell.getCodePoint());
        assertTrue(cell.areOverridesChanged());
        assertEquals('q', cell.codePointFor('q'));
    }

    @Test
    void clearMixZeroesBothAmounts() {
        cell.setMix(RED, 0.5f);

        cell.clearMix();

        assertEquals(0f, cell.getForegroundMixAmount());
        assertEquals(0f, cell.getBackgroundMixAmount());
        assertFalse(cell.hasOverrides());
    }

    @Test
    void overridesNeverLiftTheGlyph() {
        cell.setMix(RED, 1f);
        cell.setCodePoint('x');

        assertFalse(cell.isLifted());
    }

    @Test
    void changedFlagIsSetOnlyByActualChanges() {
        cell.setMix(RED, 0.5f);
        assertTrue(cell.areOverridesChanged());
        cell.clearOverridesChanged();
        assertFalse(cell.areOverridesChanged());

        cell.setMix(RED, 0.5f);
        cell.setCodePoint('x');
        cell.clearOverridesChanged();
        cell.setCodePoint('x');
        cell.clearMix();
        cell.clearOverridesChanged();
        cell.clearMix();
        cell.clearCodePoint();
        cell.clearCodePoint();

        assertTrue(cell.areOverridesChanged());
        cell.clearOverridesChanged();
        cell.clearCodePoint();
        assertFalse(cell.areOverridesChanged());
    }

    @Test
    void motionChangesDoNotSetTheOverridesFlag() {
        cell.setOffset(1f, 1f);

        assertFalse(cell.areOverridesChanged());
    }

    @Test
    void resetClearsMotionAndOverrides() {
        cell.setOffset(1f, 1f);
        cell.setMix(RED, 0.5f);
        cell.setCodePoint('x');

        cell.reset();

        assertFalse(cell.isLifted());
        assertFalse(cell.hasOverrides());
        assertEquals(0, cell.getForegroundMixColor());
    }

    @Test
    void resetTransformKeepsOverrides() {
        cell.setOffset(1f, 1f);
        cell.setMix(RED, 0.5f);
        cell.setCodePoint('x');

        cell.resetTransform();

        assertFalse(cell.isLifted());
        assertEquals(0.5f, cell.getForegroundMixAmount());
        assertEquals('x', cell.getCodePoint());
    }

    @Test
    void clearOverridesKeepsTransform() {
        cell.setOffset(1f, 1f);
        cell.setMix(RED, 0.5f);
        cell.setCodePoint('x');

        cell.clearOverrides();

        assertTrue(cell.isLifted());
        assertEquals(1f, cell.getOffsetX());
        assertFalse(cell.hasOverrides());
    }

    @Test
    void helpersLeaveValuesAloneWithoutOverrides() {
        assertEquals(0x11223344, cell.foregroundFor(0x11223344));
        assertEquals(0x55667788, cell.backgroundFor(0x55667788));
        assertEquals('q', cell.codePointFor('q'));
    }

    @Test
    void helpersApplyMixesKeepingCellAlpha() {
        cell.setMix(0xFF000000, 1f, BLUE, 0f);
        assertEquals(0xFF000044, cell.foregroundFor(0x11223344));
        assertEquals(0x55667788, cell.backgroundFor(0x55667788));

        cell.setBackgroundMix(0x00FF0000, 1f);
        assertEquals(0x00FF0088, cell.backgroundFor(0x55667788));

        cell.setForegroundMix(0x000000FF, 0.5f);
        assertEquals(0x09111A44, cell.foregroundFor(0x11223344));
    }

    @Test
    void codePointHelperPrefersTheOverride() {
        cell.setCodePoint('z');

        assertEquals('z', cell.codePointFor('q'));
    }

    @Test
    void scaleByMultipliesTheCurrentScale() {
        cell.setScale(2f, 3f);

        cell.scaleBy(2f);
        cell.scaleBy(0.5f, -1f);

        assertEquals(2f, cell.getScaleX());
        assertEquals(-6f, cell.getScaleY());
        assertTrue(cell.isLifted());
    }

    @Test
    void scaleByThrowsWhenDetached() {
        cell.detach();

        assertThrows(IllegalStateException.class, () -> cell.scaleBy(2f));
        assertThrows(IllegalStateException.class, () -> cell.scaleBy(2f, 1f));
    }

    @Test
    void detachMessageNamesTheCause() {
        cell.detach();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, cell::reset);

        assertEquals("Cell handle was detached from its layer", thrown.getMessage());
    }
}
