package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlankBreakTest {

    @Test
    void isIntactBeforeCrack() {
        assertEquals(PlankBreak.INTACT, PlankBreak.at(0f));
        assertEquals(PlankBreak.INTACT, PlankBreak.at(5.39f));
    }

    @Test
    void cracksFromCrackTime() {
        assertEquals(PlankBreak.CRACKED, PlankBreak.at(5.4f));
        assertEquals(PlankBreak.CRACKED, PlankBreak.at(5.59f));
    }

    @Test
    void breaksFromBreakTime() {
        assertEquals(PlankBreak.BROKEN, PlankBreak.at(5.6f));
        assertEquals(PlankBreak.BROKEN, PlankBreak.at(11f));
    }

    @Test
    void isIntactAgainWhenLoopWraps() {
        assertEquals(PlankBreak.BROKEN, PlankBreak.at(11f));
        assertEquals(PlankBreak.INTACT, PlankBreak.at(0.02f));
    }
}
