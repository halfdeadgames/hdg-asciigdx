package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WheelTicksTest {

    @Test
    void verticalOnlyCountsVertical() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(0, 1, false);
        assertEquals(1, ticks.vertical());
        assertEquals(0, ticks.sideways());
        ticks.add(0, -3, false);
        assertEquals(0, ticks.vertical());
    }

    @Test
    void horizontalWheelCountsSidewaysAndVertical() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(-1, 1, false);
        assertEquals(-1, ticks.sideways());
        assertEquals(1, ticks.vertical());
    }

    @Test
    void shiftTurnsVerticalIntoSidewaysWithoutVerticalTick() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(0, 1, true);
        assertEquals(1, ticks.sideways());
        assertEquals(0, ticks.vertical());
    }

    @Test
    void shiftWithHorizontalCountsBoth() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(1, -1, true);
        assertEquals(1, ticks.sideways());
        assertEquals(-1, ticks.vertical());
    }

    @Test
    void fractionalAmountsGiveSignOne() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(0.25f, -0.1f, false);
        assertEquals(1, ticks.sideways());
        assertEquals(-1, ticks.vertical());
    }

    @Test
    void ticksAccumulateOverSeveralAdds() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(0, 1, false);
        ticks.add(0, 5, false);
        ticks.add(0, 2, true);
        ticks.add(0, 2, true);
        assertEquals(2, ticks.vertical());
        assertEquals(2, ticks.sideways());
    }

    @Test
    void clearsAreIndependent() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(1, 1, false);
        ticks.clearVertical();
        assertEquals(0, ticks.vertical());
        assertEquals(1, ticks.sideways());
        ticks.add(0, 1, false);
        ticks.clearSideways();
        assertEquals(0, ticks.sideways());
        assertEquals(1, ticks.vertical());
    }

    @Test
    void clearZeroesBothCounters() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(1, 1, false);
        ticks.clear();
        assertEquals(0, ticks.vertical());
        assertEquals(0, ticks.sideways());
    }

    @Test
    void zeroAmountsAddNothing() {
        WheelTicks ticks = new WheelTicks();
        ticks.add(0, 0, false);
        ticks.add(0, 0, true);
        assertEquals(0, ticks.vertical());
        assertEquals(0, ticks.sideways());
    }
}
