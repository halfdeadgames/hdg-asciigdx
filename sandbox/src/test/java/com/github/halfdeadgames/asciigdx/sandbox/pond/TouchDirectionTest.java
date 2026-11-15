package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TouchDirectionTest {

    private final CellPoint direction = new CellPoint();

    private void assertDirection(float offsetX, float offsetY, int column, int row) {
        TouchDirection.stepToward(offsetX, offsetY, direction);
        assertEquals(column, direction.getColumn(), "column");
        assertEquals(row, direction.getRow(), "row");
    }

    @Test
    void horizontalAxisWinsWhenLarger() {
        assertDirection(30f, 10f, 1, 0);
        assertDirection(-30f, 10f, -1, 0);
    }

    @Test
    void verticalAxisWinsWhenLargerAndDownIsPositive() {
        assertDirection(5f, 40f, 0, 1);
        assertDirection(5f, -40f, 0, -1);
    }

    @Test
    void tieGoesToTheHorizontalAxis() {
        assertDirection(-8f, 8f, -1, 0);
    }

    @Test
    void touchOnThePlayerGivesNoStep() {
        assertDirection(0f, 0f, 0, 0);
    }
}
