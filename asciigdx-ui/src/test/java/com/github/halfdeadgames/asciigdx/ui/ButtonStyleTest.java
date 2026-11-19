package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ButtonStyleTest {
    private static final LabelStyle TALL_A = LabelStyle.of(UiFont.TALL, 1, 2);
    private static final LabelStyle TALL_B = LabelStyle.of(UiFont.TALL, 3, 4);
    private static final LabelStyle SQUARE_A = LabelStyle.of(UiFont.SQUARE, 1, 2);

    @Test
    void mixedFontsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ButtonStyle.of(TALL_A, TALL_A, SQUARE_A, TALL_A));
    }

    @Test
    void nullLookIsRejected() {
        assertThrows(NullPointerException.class,
                () -> ButtonStyle.of(TALL_A, null, TALL_A, TALL_A));
    }

    @Test
    void singleLookIsUsedForAllStatesWithDefaultPadding() {
        ButtonStyle style = ButtonStyle.of(TALL_A);

        assertSame(TALL_A, style.getNormal());
        assertSame(TALL_A, style.getPointerOver());
        assertSame(TALL_A, style.getPressed());
        assertSame(TALL_A, style.getDisabled());
        assertEquals(1, style.getHorizontalPadding());
        assertEquals(UiFont.TALL, style.getFont());
    }

    @Test
    void withHorizontalPaddingReturnsACopy() {
        ButtonStyle original = ButtonStyle.of(TALL_A, TALL_B, TALL_A, TALL_B);

        ButtonStyle padded = original.withHorizontalPadding(4);

        assertEquals(1, original.getHorizontalPadding());
        assertEquals(4, padded.getHorizontalPadding());
        assertSame(TALL_B, padded.getPointerOver());
        assertThrows(IllegalArgumentException.class, () -> original.withHorizontalPadding(-1));
    }

    @Test
    void equalsAndHashCodeCoverLooksAndPadding() {
        ButtonStyle first = ButtonStyle.of(TALL_A, TALL_B, TALL_A, TALL_B);
        ButtonStyle same = ButtonStyle.of(TALL_A, TALL_B, TALL_A, TALL_B);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, first.withHorizontalPadding(2));
        assertNotEquals(first, ButtonStyle.of(TALL_A, TALL_B, TALL_B, TALL_B));
    }
}
