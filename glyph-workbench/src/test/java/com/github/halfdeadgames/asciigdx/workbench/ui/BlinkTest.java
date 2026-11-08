package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlinkTest {

    @Test
    void startsLit() {
        assertTrue(new Blink().isOn());
    }

    @Test
    void goesDarkAfterOneIntervalAndLitAfterTwo() {
        Blink blink = new Blink();
        blink.advance(Blink.HALF_CYCLE_SECONDS);
        assertFalse(blink.isOn());
        blink.advance(Blink.HALF_CYCLE_SECONDS);
        assertTrue(blink.isOn());
    }

    @Test
    void restartMakesItLitAgain() {
        Blink blink = new Blink();
        blink.advance(Blink.HALF_CYCLE_SECONDS);
        blink.restart();
        assertTrue(blink.isOn());
    }
}
