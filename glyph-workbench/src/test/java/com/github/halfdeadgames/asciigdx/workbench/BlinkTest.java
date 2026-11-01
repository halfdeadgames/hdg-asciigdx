package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlinkTest {

    @Test
    void highlightsBrieflyAtTheStartOfEachPeriod() {
        assertTrue(Blink.isOn(0f));
        assertTrue(Blink.isOn(0.3f));
        assertFalse(Blink.isOn(0.5f));
        assertFalse(Blink.isOn(1.9f));
        assertTrue(Blink.isOn(2.1f));
    }
}
