package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FadeTest {

    private static final float EPSILON = 1e-5f;
    private final Fade fade = new Fade(0.5f, 9.5f, 0.5f);

    @Test
    void startsOpaque() {
        assertEquals(1f, fade.alphaAt(0f), EPSILON);
    }

    @Test
    void fadesInLinearly() {
        assertEquals(0.5f, fade.alphaAt(0.25f), EPSILON);
    }

    @Test
    void isClearFromFadeInEndToFadeOutStart() {
        assertEquals(0f, fade.alphaAt(0.5f), EPSILON);
        assertEquals(0f, fade.alphaAt(5f), EPSILON);
        assertEquals(0f, fade.alphaAt(9.5f), EPSILON);
    }

    @Test
    void fadesOutLinearly() {
        assertEquals(0.5f, fade.alphaAt(9.75f), EPSILON);
    }

    @Test
    void staysOpaqueAfterFadeOut() {
        assertEquals(1f, fade.alphaAt(10f), EPSILON);
        assertEquals(1f, fade.alphaAt(11f), EPSILON);
    }
}
