package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.UiScale;
import org.junit.jupiter.api.Test;

class PaintUiScaleTest {

    private static PaintUiScale eightByEight(Integer override) {
        return new PaintUiScale(8, 8, override);
    }

    @Test
    void startsAtTheDefaultScaleBeforeTheFirstUpdate() {
        PaintUiScale scale = eightByEight(null);

        assertEquals(UiScale.DEFAULT_FACTOR, scale.factor());
        assertTrue(scale.isAuto());
    }

    @Test
    void autoPicksTheProportionalFactor() {
        PaintUiScale scale = eightByEight(null);

        scale.update(1900, 1000);

        assertEquals(2, scale.factor());
        assertTrue(scale.isAuto());
        assertFalse(scale.overrideTooBig());
        assertNull(scale.overrideFactor());
    }

    @Test
    void overrideIsAppliedWhenItFits() {
        PaintUiScale scale = eightByEight(4);

        scale.update(3840, 2160);

        assertEquals(4, scale.factor());
        assertFalse(scale.isAuto());
        assertFalse(scale.overrideTooBig());
        assertEquals(4, scale.overrideFactor());
    }

    @Test
    void overrideIsCutDownToFitButKeptAsRequested() {
        PaintUiScale scale = eightByEight(4);

        scale.update(800, 1000);

        assertEquals(2, scale.factor());
        assertTrue(scale.overrideTooBig());
        assertEquals(4, scale.overrideFactor());
    }

    @Test
    void enlargingTheWindowRestoresTheRequestedOverride() {
        PaintUiScale scale = eightByEight(4);
        scale.update(800, 1000);

        scale.update(1900, 1000);

        assertEquals(4, scale.factor());
        assertFalse(scale.overrideTooBig());
    }

    @Test
    void stepUpAndDownStartFromTheAppliedFactor() {
        PaintUiScale scale = eightByEight(null);
        scale.update(2560, 1440);
        assertEquals(3, scale.factor());

        scale.step(1, 2560, 1440);
        assertEquals(4, scale.factor());
        assertEquals(4, scale.overrideFactor());

        scale.step(-1, 2560, 1440);
        scale.step(-1, 2560, 1440);
        assertEquals(2, scale.factor());
        assertFalse(scale.isAuto());
    }

    @Test
    void stepClampsAtTheLimits() {
        PaintUiScale scale = eightByEight(null);
        scale.update(3840, 2160);

        scale.step(1, 3840, 2160);
        assertEquals(4, scale.factor());

        for (int i = 0; i < 5; i++) {
            scale.step(-1, 3840, 2160);
        }
        assertEquals(1, scale.factor());
    }

    @Test
    void steppingUpInASmallWindowReportsTheOverrideAsTooBig() {
        PaintUiScale scale = eightByEight(null);
        scale.update(900, 480);
        assertEquals(2, scale.factor());

        scale.step(1, 900, 480);

        assertEquals(2, scale.factor());
        assertEquals(3, scale.overrideFactor());
        assertTrue(scale.overrideTooBig());
    }

    @Test
    void useAutoDropsTheOverride() {
        PaintUiScale scale = eightByEight(4);
        scale.update(1900, 1000);
        assertEquals(4, scale.factor());

        scale.useAuto(1900, 1000);

        assertEquals(2, scale.factor());
        assertTrue(scale.isAuto());
        assertNull(scale.overrideFactor());
        assertFalse(scale.overrideTooBig());
    }

    @Test
    void labelFollowsTheFactorAndMode() {
        PaintUiScale scale = eightByEight(null);
        scale.update(2560, 1440);
        assertEquals("UI 3X AUTO", scale.label());

        scale.step(-1, 2560, 1440);
        assertEquals("UI 2X", scale.label());
    }

    @Test
    void scaleMatchesTheFactor() {
        PaintUiScale scale = eightByEight(null);

        scale.update(3840, 2160);

        assertEquals(4, scale.scale().factor());
    }
}
