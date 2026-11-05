package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KeyRepeatTest {

    @Test
    void movesOncePerPressAndNothingWhileUp() {
        KeyRepeat repeat = new KeyRepeat();
        assertEquals(0, repeat.update(false, 0.016f));
        assertEquals(1, repeat.update(true, 0.016f));
    }

    @Test
    void holdingBelowTheDelayDoesNotRepeat() {
        KeyRepeat repeat = new KeyRepeat();
        assertEquals(1, repeat.update(true, 0.016f));
        for (int i = 0; i < 10; i++) {
            assertEquals(0, repeat.update(true, 0.04f));
        }
    }

    @Test
    void repeatsAfterTheDelayAtTwelvePerSecond() {
        KeyRepeat repeat = new KeyRepeat();
        repeat.update(true, 0f);
        assertEquals(0, repeat.update(true, 0.49f));
        assertEquals(1, repeat.update(true, 0.02f));
        int total = 0;
        for (int i = 0; i < 100; i++) {
            total += repeat.update(true, 0.01f);
        }
        assertEquals(12, total, 1);
    }

    @Test
    void aLongFrameCatchesUpOnMissedRepeats() {
        KeyRepeat repeat = new KeyRepeat();
        repeat.update(true, 0f);
        assertEquals(3, repeat.update(true, 0.5f + 2f / 12f + 0.001f));
    }

    @Test
    void holdIgnoresAHeldKeyUntilItIsReleased() {
        KeyRepeat repeat = new KeyRepeat();
        repeat.hold(true);
        assertEquals(0, repeat.update(true, 0.016f));
        assertEquals(0, repeat.update(true, 5f));
        assertEquals(0, repeat.update(false, 0.016f));
        assertEquals(1, repeat.update(true, 0.016f));
    }

    @Test
    void holdWithAnUpKeyMakesTheNextPressFresh() {
        KeyRepeat repeat = new KeyRepeat();
        repeat.update(true, 0f);
        repeat.hold(false);
        assertEquals(1, repeat.update(true, 0.016f));
    }

    @Test
    void releasingAndPressingAgainRestartsTheDelay() {
        KeyRepeat repeat = new KeyRepeat();
        repeat.update(true, 0f);
        repeat.update(true, 0.6f);
        assertEquals(0, repeat.update(false, 0.016f));
        assertEquals(1, repeat.update(true, 0.016f));
        assertEquals(0, repeat.update(true, 0.3f));
    }
}
