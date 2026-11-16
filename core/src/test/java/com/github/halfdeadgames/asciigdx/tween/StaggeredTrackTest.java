package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaggeredTrackTest {

    private static final float TOLERANCE = 1e-6f;

    private final Tween track = Tween.linear(1f, 2f, 0f, 10f);
    private final StaggeredTrack staggered = new StaggeredTrack(track, 0.1f);

    @Test
    void indexZeroEqualsTheTrack() {
        assertEquals(track.getValueAt(1.5f), staggered.getValueAt(0, 1.5f), TOLERANCE);
    }

    @Test
    void indexTwoIsShiftedByTwoSteps() {
        assertEquals(track.getValueAt(1.3f), staggered.getValueAt(2, 1.5f), TOLERANCE);
    }

    @Test
    void startAndEndAreShiftedPerIndex() {
        assertEquals(1.3f, staggered.getStartSecondsOf(3), TOLERANCE);
        assertEquals(3.3f, staggered.getEndSecondsOf(3), TOLERANCE);
    }

    @Test
    void hasStartedAtIsFalseJustBeforeStartAndTrueAtStart() {
        assertFalse(staggered.hasStartedAt(2, 1.1f));
        assertTrue(staggered.hasStartedAt(2, 1.2f));
    }

    @Test
    void worksWithATweenSequence() {
        TweenSequence sequence = TweenSequence.startingAt(0f, 0f).to(1f, 1f).hold(1f).build();
        StaggeredTrack sequenceStaggered = new StaggeredTrack(sequence, 0.5f);
        assertEquals(0.5f, sequenceStaggered.getValueAt(1, 1f), TOLERANCE);
        assertEquals(2.5f, sequenceStaggered.getEndSecondsOf(1), TOLERANCE);
    }

    @Test
    void zeroStepMakesAllItemsEqual() {
        StaggeredTrack together = new StaggeredTrack(track, 0f);
        assertEquals(together.getValueAt(0, 1.5f), together.getValueAt(7, 1.5f), 0f);
    }

    @Test
    void negativeIndexThrows() {
        assertThrows(IllegalArgumentException.class, () -> staggered.getValueAt(-1, 0f));
        assertThrows(IllegalArgumentException.class, () -> staggered.getStartSecondsOf(-1));
        assertThrows(IllegalArgumentException.class, () -> staggered.getEndSecondsOf(-1));
        assertThrows(IllegalArgumentException.class, () -> staggered.hasStartedAt(-1, 0f));
    }

    @Test
    void negativeStepThrows() {
        assertThrows(IllegalArgumentException.class, () -> new StaggeredTrack(track, -0.1f));
    }

    @Test
    void nanStepThrows() {
        assertThrows(IllegalArgumentException.class, () -> new StaggeredTrack(track, Float.NaN));
    }

    @Test
    void nullTrackThrows() {
        assertThrows(IllegalArgumentException.class, () -> new StaggeredTrack(null, 0.1f));
    }
}
