package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class IntegerScalingTest {

    private static final float EPSILON = 1e-4f;

    private static Vector2 apply(float sw, float sh, float tw, float th) {
        return IntegerScaling.INSTANCE.apply(sw, sh, tw, th);
    }

    @Test
    void exactFitDoubles() {
        Vector2 result = apply(160, 100, 320, 200);
        assertEquals(320f, result.x, EPSILON);
        assertEquals(200f, result.y, EPSILON);
    }

    @Test
    void remainderFloorsToWholeFactor() {
        Vector2 result = apply(100, 50, 350, 180);
        assertEquals(300f, result.x, EPSILON);
        assertEquals(150f, result.y, EPSILON);
    }

    @Test
    void heightCanBeLimitingAxis() {
        Vector2 result = apply(100, 100, 1000, 250);
        assertEquals(200f, result.x, EPSILON);
        assertEquals(200f, result.y, EPSILON);
    }

    @Test
    void belowOneTimesStaysAtOneTimes() {
        Vector2 result = apply(200, 100, 100, 100);
        assertEquals(200f, result.x, EPSILON);
        assertEquals(100f, result.y, EPSILON);
    }

    @Test
    void oneAxisTooSmallStaysAtOneTimes() {
        Vector2 result = apply(100, 100, 1000, 50);
        assertEquals(100f, result.x, EPSILON);
        assertEquals(100f, result.y, EPSILON);
    }

    @Test
    void oddSizes() {
        Vector2 result = apply(7, 5, 22, 16);
        assertEquals(21f, result.x, EPSILON);
        assertEquals(15f, result.y, EPSILON);
    }

    @Test
    void zeroTargetGivesZero() {
        Vector2 result = apply(160, 100, 0, 0);
        assertEquals(0f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
    }

    @Test
    void zeroSourceGivesZero() {
        Vector2 result = apply(0, 0, 100, 100);
        assertEquals(0f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
    }
}
