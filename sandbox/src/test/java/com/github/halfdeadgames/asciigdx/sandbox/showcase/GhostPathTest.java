package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GhostPathTest {

    private static final float EPSILON = 1e-5f;
    private static final float SETTLED_ALPHA = 0.8f;
    private static final float FLICKER_FLOOR_ALPHA = 0.8f * 0.55f;

    private final GhostPath path = new GhostPath(-1f, 9f);

    @Test
    void isInvisibleBeforeDriftStart() {
        assertFalse(path.isVisibleAt(1.99f));
        assertEquals(0f, path.alphaAt(1.99f), EPSILON);
        assertEquals(0f, path.alphaAt(0f), EPSILON);
    }

    @Test
    void isVisibleFromDriftStart() {
        assertTrue(path.isVisibleAt(2f));
    }

    @Test
    void columnRunsFromStartToEndColumn() {
        assertEquals(-1f, path.columnAt(2f), EPSILON);
        assertEquals(9f, path.columnAt(3.4f), EPSILON);
        assertEquals(9f, path.columnAt(10f), EPSILON);
    }

    @Test
    void alphaIsSettledAfterDriftAndAfterWail() {
        assertEquals(SETTLED_ALPHA, path.alphaAt(3.4f - 0.0001f), 1e-3f);
        assertEquals(SETTLED_ALPHA, path.alphaAt(4.4f), EPSILON);
        assertEquals(SETTLED_ALPHA, path.alphaAt(9f), EPSILON);
    }

    @Test
    void flickerStaysInRangeAndVaries() {
        float lowest = Float.MAX_VALUE;
        float highest = -Float.MAX_VALUE;
        for (float time = 3.4f; time < 4.4f; time += 0.01f) {
            float alpha = path.alphaAt(time);
            assertTrue(alpha >= FLICKER_FLOOR_ALPHA - EPSILON && alpha <= SETTLED_ALPHA + EPSILON,
                    "alpha " + alpha);
            lowest = Math.min(lowest, alpha);
            highest = Math.max(highest, alpha);
        }
        assertTrue(highest - lowest > 0.1f);
    }

    @Test
    void wobbleStaysWithinAmplitude() {
        for (float time = 0f; time < 11.5f; time += 0.05f) {
            assertTrue(Math.abs(path.wobbleRowsAt(time)) <= 0.15f + EPSILON);
        }
    }

    @Test
    void sameTimeGivesSameValues() {
        assertEquals(path.alphaAt(3.7f), path.alphaAt(3.7f), 0f);
        assertEquals(path.wobbleRowsAt(3.7f), path.wobbleRowsAt(3.7f), 0f);
    }
}
