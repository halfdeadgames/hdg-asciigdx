package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ButtonStyleTest {
    private static final LabelStyle LOOK_A = LabelStyle.of(1, 2);
    private static final LabelStyle LOOK_B = LabelStyle.of(3, 4);

    @Test
    void nullLookIsRejected() {
        assertThrows(NullPointerException.class,
                () -> ButtonStyle.of(LOOK_A, null, LOOK_A, LOOK_A));
    }

    @Test
    void singleLookIsUsedForAllStatesWithDefaultPadding() {
        ButtonStyle style = ButtonStyle.of(LOOK_A);

        assertSame(LOOK_A, style.getNormal());
        assertSame(LOOK_A, style.getPointerOver());
        assertSame(LOOK_A, style.getPressed());
        assertSame(LOOK_A, style.getDisabled());
        assertEquals(1, style.getHorizontalPadding());
    }

    @Test
    void withHorizontalPaddingReturnsACopy() {
        ButtonStyle original = ButtonStyle.of(LOOK_A, LOOK_B, LOOK_A, LOOK_B);

        ButtonStyle padded = original.withHorizontalPadding(4);

        assertEquals(1, original.getHorizontalPadding());
        assertEquals(4, padded.getHorizontalPadding());
        assertSame(LOOK_B, padded.getPointerOver());
        assertThrows(IllegalArgumentException.class, () -> original.withHorizontalPadding(-1));
    }

    @Test
    void equalsAndHashCodeCoverLooksAndPadding() {
        ButtonStyle first = ButtonStyle.of(LOOK_A, LOOK_B, LOOK_A, LOOK_B);
        ButtonStyle same = ButtonStyle.of(LOOK_A, LOOK_B, LOOK_A, LOOK_B);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, first.withHorizontalPadding(2));
        assertNotEquals(first, ButtonStyle.of(LOOK_A, LOOK_B, LOOK_B, LOOK_B));
    }
}
