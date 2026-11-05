package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UiScaleTest {

    private static final UiScale DEFAULT = new UiScale(3);

    @Test
    void defaultFactorMatchesTheOldCellSize() {
        assertEquals(Painter.TEXT_SIZE, DEFAULT.cell());
        assertEquals(DEFAULT, UiScale.DEFAULT);
    }

    @Test
    void defaultFactorReproducesTheOldDerivedLiterals() {
        assertEquals(12, DEFAULT.half());
        assertEquals(6, DEFAULT.px(2));
        assertEquals(3, DEFAULT.px(1));
        assertEquals(6, DEFAULT.cell() / 4);
    }

    @Test
    void defaultFactorKeepsEveryOldPixelLiteralExactly() {
        for (int value = 0; value <= 200; value++) {
            assertEquals(value, DEFAULT.scaled(value), "value " + value);
        }
    }

    @Test
    void scaledRoundsHalfUpAndNeverDropsAThinLineToNothing() {
        UiScale one = new UiScale(1);
        assertEquals(1, one.scaled(1));
        assertEquals(1, one.scaled(2));
        assertEquals(3, one.scaled(8));
        assertEquals(2, one.scaled(6));
        UiScale four = new UiScale(4);
        assertEquals(1, four.scaled(1));
        assertEquals(3, four.scaled(2));
        assertEquals(5, four.scaled(4));
        assertEquals(11, four.scaled(8));
        assertEquals(0, four.scaled(0));
    }

    @Test
    void cellSizesFollowTheFactor() {
        assertEquals(8, new UiScale(1).cell());
        assertEquals(16, new UiScale(2).cell());
        assertEquals(32, new UiScale(4).cell());
        assertEquals(16, new UiScale(4).half());
        assertEquals(8, new UiScale(4).px(2));
    }

    @Test
    void factorsOutsideTheRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UiScale(0));
        assertThrows(IllegalArgumentException.class, () -> new UiScale(5));
        assertTrue(new UiScale(UiScale.MIN_FACTOR).cell() > 0);
    }
}
