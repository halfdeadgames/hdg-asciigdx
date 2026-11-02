package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class ExactScalingTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void ignoresTargetSmallerThanResult() {
        Vector2 result = new ExactScaling(3).apply(80, 45, 10, 10);
        assertEquals(240f, result.x, EPSILON);
        assertEquals(135f, result.y, EPSILON);
    }

    @Test
    void ignoresTargetLargerThanResult() {
        Vector2 result = new ExactScaling(2).apply(80, 45, 5000, 5000);
        assertEquals(160f, result.x, EPSILON);
        assertEquals(90f, result.y, EPSILON);
    }
}
