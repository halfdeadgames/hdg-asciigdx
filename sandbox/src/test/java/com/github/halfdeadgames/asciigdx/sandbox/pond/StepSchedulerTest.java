package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepSchedulerTest {

    private static final float STEP = 0.3f;
    private static final float TOLERANCE = 1e-4f;

    private final PondMap map = new PondMap(10, 10);

    private StepScheduler schedulerAt(int column, int row) {
        return new StepScheduler(new PondPlayer(column, row), map, STEP);
    }

    @Test
    void freshPressStartsAnEaseOutStepAndMovesTheCellFirst() {
        var scheduler = schedulerAt(5, 5);
        scheduler.press(1, 0, 0f);
        assertTrue(scheduler.isStepping());
        assertEquals(6, scheduler.getColumn());
        assertEquals(1f, scheduler.slideRemainingAt(0f), TOLERANCE);
        assertEquals(0.25f, scheduler.slideRemainingAt(STEP / 2f), TOLERANCE);
    }

    @Test
    void releasedKeyStopsAfterOneStep() {
        var scheduler = schedulerAt(5, 5);
        scheduler.press(1, 0, 0f);
        scheduler.update(STEP + 0.01f, 0, 0);
        assertFalse(scheduler.isStepping());
        assertEquals(6, scheduler.getColumn());
        assertEquals(0f, scheduler.slideRemainingAt(STEP + 0.01f), TOLERANCE);
    }

    @Test
    void heldDirectionAtStepEndChainsALinearStepStartingAtTheEnd() {
        var scheduler = schedulerAt(5, 5);
        scheduler.press(1, 0, 0f);
        scheduler.update(STEP + 0.01f, 1, 0);
        assertTrue(scheduler.isStepping());
        assertEquals(7, scheduler.getColumn());
        assertEquals(0.5f, scheduler.slideRemainingAt(STEP * 1.5f), TOLERANCE);
    }

    @Test
    void heldDirectionWalksAtConstantSpeed() {
        var scheduler = schedulerAt(1, 5);
        scheduler.press(1, 0, 0f);
        scheduler.update(0.95f, 1, 0);
        assertEquals(5, scheduler.getColumn());
        assertTrue(scheduler.isStepping());
    }

    @Test
    void pressDuringAStepStartsAnEaseOutStepAfterIt() {
        var scheduler = schedulerAt(5, 5);
        scheduler.press(1, 0, 0f);
        scheduler.press(0, 1, 0.1f);
        assertEquals(5, scheduler.getRow());
        scheduler.update(STEP + STEP / 6f, 0, 0);
        assertEquals(6, scheduler.getRow());
        assertEquals(1, scheduler.getStepRow());
        assertEquals(0.6944f, scheduler.slideRemainingAt(STEP + STEP / 6f), TOLERANCE);
    }

    @Test
    void heldDirectionWithNoPressStartsAnEaseOutStep() {
        var scheduler = schedulerAt(5, 5);
        scheduler.update(0f, 1, 0);
        assertEquals(6, scheduler.getColumn());
        assertEquals(0.25f, scheduler.slideRemainingAt(STEP / 2f), TOLERANCE);
    }

    @Test
    void stepOffTheMapIsNotTaken() {
        var scheduler = schedulerAt(0, 5);
        scheduler.press(-1, 0, 0f);
        assertFalse(scheduler.isStepping());
        assertEquals(0, scheduler.getColumn());
    }

    @Test
    void chainStopsAtTheMapEdge() {
        var scheduler = schedulerAt(8, 5);
        scheduler.press(1, 0, 0f);
        scheduler.update(STEP + 0.01f, 1, 0);
        assertFalse(scheduler.isStepping());
        assertEquals(9, scheduler.getColumn());
    }

    @Test
    void noHeldDirectionAndNoPressStaysIdle() {
        var scheduler = schedulerAt(5, 5);
        scheduler.update(1f, 0, 0);
        assertFalse(scheduler.isStepping());
        assertEquals(5, scheduler.getColumn());
        assertEquals(5, scheduler.getRow());
    }

    @Test
    void cancelDropsTheStepAndTheQueuedPress() {
        var scheduler = schedulerAt(5, 5);
        scheduler.press(1, 0, 0f);
        scheduler.press(0, 1, 0.1f);
        scheduler.cancelStep();
        scheduler.update(1f, 0, 0);
        assertFalse(scheduler.isStepping());
        assertEquals(5, scheduler.getRow());
    }
}
