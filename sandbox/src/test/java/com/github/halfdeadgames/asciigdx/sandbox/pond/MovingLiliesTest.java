package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MovingLiliesTest {

    private static final float FRAME_SECONDS = 1f / 60f;

    private final MovingLilies moving = new MovingLilies();

    @Test
    void addingTheSameLilyTwiceKeepsOneEntry() {
        var lily = new Lily();
        moving.add(lily, 3, 4);
        moving.add(lily, 3, 4);
        assertEquals(1, moving.size());
        assertSame(lily, moving.lilyAt(0));
        assertEquals(3, moving.columnAt(0));
        assertEquals(4, moving.rowAt(0));
    }

    @Test
    void pushedLilyStaysUntilItSettlesThenLeavesOnce() {
        var lily = new Lily();
        lily.push(2f, 0f);
        moving.add(lily, 0, 0);
        moving.advance(FRAME_SECONDS);
        assertEquals(1, moving.size());
        assertFalse(moving.settledInLastAdvance());

        var advances = 0;
        while (moving.size() > 0 && advances < 1000) {
            moving.advance(FRAME_SECONDS);
            advances++;
        }
        assertEquals(0, moving.size());
        assertTrue(moving.settledInLastAdvance());
        assertTrue(lily.isHome());

        moving.advance(FRAME_SECONDS);
        assertFalse(moving.settledInLastAdvance());
    }

    @Test
    void lilyHeldAwayFromZeroStaysEvenAtRest() {
        var lily = new Lily();
        lily.setTarget(0.3f, 0f);
        moving.add(lily, 0, 0);
        for (var i = 0; i < 600; i++) {
            moving.advance(FRAME_SECONDS);
        }
        assertTrue(lily.isAtRest());
        assertEquals(1, moving.size());
    }

    @Test
    void resetAllSendsLiliesHomeAndEmptiesTheSet() {
        var lily = new Lily();
        lily.push(2f, 1f);
        lily.setTarget(0.3f, 0.2f);
        moving.add(lily, 1, 1);
        moving.advance(FRAME_SECONDS);
        moving.resetAll();
        assertEquals(0, moving.size());
        assertTrue(lily.isHome());
        assertEquals(0f, lily.getOffsetX());
        assertEquals(0f, lily.getOffsetY());
    }

    @Test
    void growingPastStartCapacityKeepsEveryEntry() {
        var count = 200;
        var all = new Lily[count];
        for (var i = 0; i < count; i++) {
            all[i] = new Lily();
            moving.add(all[i], i, i + 1);
        }
        assertEquals(count, moving.size());
        for (var i = 0; i < count; i++) {
            assertSame(all[i], moving.lilyAt(i));
            assertEquals(i, moving.columnAt(i));
            assertEquals(i + 1, moving.rowAt(i));
        }
    }

    @Test
    void settledLiliesAreRemovedWhileOthersStay() {
        var held = new Lily();
        held.setTarget(0.3f, 0f);
        var homeLily = new Lily();
        var otherHeld = new Lily();
        otherHeld.setTarget(0f, 0.3f);
        moving.add(held, 0, 0);
        moving.add(homeLily, 1, 1);
        moving.add(otherHeld, 2, 2);
        moving.advance(FRAME_SECONDS);
        assertEquals(2, moving.size());
        assertTrue(moving.settledInLastAdvance());
    }
}
