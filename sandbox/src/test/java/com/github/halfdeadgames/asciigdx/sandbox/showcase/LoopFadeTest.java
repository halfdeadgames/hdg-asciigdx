package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.tween.TweenSequence;
import org.junit.jupiter.api.Test;

class LoopFadeTest {

    private static final float EPSILON = 1e-6f;
    private final TweenSequence fade = LoopFade.track(0.5f, 9.5f, 0.5f);

    @Test
    void startsFullyCovered() {
        assertEquals(1f, fade.valueAt(0f), EPSILON);
    }

    @Test
    void fadesOutLinearly() {
        assertEquals(0.5f, fade.valueAt(0.25f), EPSILON);
        assertEquals(0f, fade.valueAt(0.5f), EPSILON);
    }

    @Test
    void staysClearUntilTheFadeBack() {
        assertEquals(0f, fade.valueAt(5f), EPSILON);
        assertEquals(0f, fade.valueAt(9.5f), EPSILON);
    }

    @Test
    void fadesBackInLinearlyAndStaysCovered() {
        assertEquals(0.5f, fade.valueAt(9.75f), EPSILON);
        assertEquals(1f, fade.valueAt(10f), EPSILON);
        assertEquals(1f, fade.valueAt(11f), EPSILON);
    }
}
