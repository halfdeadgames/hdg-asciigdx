package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EighthBarTest {

    private static final int FULL = 0x2588;
    private static final List<Integer> LEFT_EIGHTHS =
            List.of((int) ' ', 0x258F, 0x258E, 0x258D, 0x258C, 0x258B, 0x258A, 0x2589);

    private static String render(int cellCount, float fraction) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < cellCount; i++) {
            bar.appendCodePoint(EighthBar.codePointAt(i, cellCount, fraction));
        }
        return bar.toString();
    }

    @Test
    void fullBarFillsEveryCell() {
        assertEquals("████", render(4, 1f));
    }

    @Test
    void emptyBarIsSpaces() {
        assertEquals("    ", render(4, 0f));
    }

    @Test
    void eachPartialEighthUsesItsBlock() {
        for (int eighths = 1; eighths <= 7; eighths++) {
            float fraction = eighths / 32f;
            assertEquals((int) LEFT_EIGHTHS.get(eighths), EighthBar.codePointAt(0, 4, fraction));
            assertEquals(' ', EighthBar.codePointAt(1, 4, fraction));
        }
    }

    @Test
    void partialCellFollowsFullCells() {
        assertEquals("█▌  ", render(4, 12f / 32f));
    }

    @Test
    void fractionIsClamped() {
        assertEquals(render(4, 1f), render(4, 5f));
        assertEquals(render(4, 0f), render(4, -2f));
        assertEquals(render(4, 0f), render(4, Float.NaN));
    }

    @Test
    void totalEighthsAreRounded() {
        assertEquals((int) LEFT_EIGHTHS.get(1), EighthBar.codePointAt(0, 1, 0.14f));
        assertEquals(' ', EighthBar.codePointAt(0, 1, 0.06f));
        assertEquals(FULL, EighthBar.codePointAt(0, 1, 0.97f));
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> EighthBar.codePointAt(0, 0, 0.5f));
        assertThrows(IllegalArgumentException.class, () -> EighthBar.codePointAt(-1, 4, 0.5f));
        assertThrows(IllegalArgumentException.class, () -> EighthBar.codePointAt(4, 4, 0.5f));
    }
}
