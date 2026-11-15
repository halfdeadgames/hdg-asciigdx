package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChangeGateTest {

    @Test
    void firstValueIsAlwaysChanged() {
        var gate = new ChangeGate(1);

        assertFalse(gate.isUnchanged(0));
    }

    @Test
    void rememberedValueIsUnchangedUntilItDiffers() {
        var gate = new ChangeGate(1);
        gate.remember(5);

        assertTrue(gate.isUnchanged(5));
        assertFalse(gate.isUnchanged(6));
    }

    @Test
    void forgetAllMakesEverySlotChangedAgain() {
        var gate = new ChangeGate(2);
        gate.remember(0, 1);
        gate.remember(1, 2);

        gate.forgetAll();

        assertFalse(gate.isUnchanged(0, 1));
        assertFalse(gate.isUnchanged(1, 2));
    }

    @Test
    void slotsAreIndependent() {
        var gate = new ChangeGate(2);
        gate.remember(0, 7);

        assertTrue(gate.isUnchanged(0, 7));
        assertFalse(gate.isUnchanged(1, 7));
    }

    @Test
    void floatBitsMatchFloatCompare() {
        assertTrue(ChangeGate.bitsOf(Float.NaN) == ChangeGate.bitsOf(0f / 0f));
        assertFalse(ChangeGate.bitsOf(0f) == ChangeGate.bitsOf(-0f));
    }
}
