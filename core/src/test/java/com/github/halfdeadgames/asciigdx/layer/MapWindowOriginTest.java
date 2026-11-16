package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MapWindowOriginTest {

    private static final float CELL = 16f;

    @Test
    void startsAtZeroWithoutMargin() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);

        assertEquals(0, origin.getOriginColumn());
        assertEquals(0, origin.getOriginRow());
        assertEquals(0, origin.getShiftColumns());
        assertEquals(0, origin.getShiftRows());
        assertFalse(origin.hasShifted());
    }

    @Test
    void startsAtNegativeMargin() {
        MapWindowOrigin origin = new MapWindowOrigin(3, 2);

        assertEquals(-3, origin.getOriginColumn());
        assertEquals(-2, origin.getOriginRow());
        assertEquals(3, origin.getMarginColumns());
        assertEquals(2, origin.getMarginRows());
        assertFalse(origin.hasShifted());
    }

    @Test
    void cameraInsideFirstCellIsZero() {
        assertEquals(0, MapWindowOrigin.originFor(15.9f, CELL, 0));
    }

    @Test
    void exactBoundaryAdvancesOneCell() {
        assertEquals(1, MapWindowOrigin.originFor(16f, CELL, 0));
    }

    @Test
    void negativeCameraFloorsDownward() {
        assertEquals(-1, MapWindowOrigin.originFor(-0.5f, CELL, 0));
        assertEquals(-1, MapWindowOrigin.originFor(-1f, CELL, 0));
        assertEquals(-1, MapWindowOrigin.originFor(-16f, CELL, 0));
        assertEquals(-2, MapWindowOrigin.originFor(-16.5f, CELL, 0));
    }

    @Test
    void smallerCellsGiveLargerOrigin() {
        assertEquals(6, MapWindowOrigin.originFor(48f, 8f, 0));
        assertEquals(3, MapWindowOrigin.originFor(48f, 16f, 0));
    }

    @Test
    void marginIsSubtracted() {
        assertEquals(1, MapWindowOrigin.originFor(48f, CELL, 2));
        assertEquals(-3, MapWindowOrigin.originFor(-1f, CELL, 2));
    }

    @Test
    void movingRightOneCellShiftsOne() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);

        origin.follow(16f, 0f, CELL, CELL);

        assertEquals(1, origin.getOriginColumn());
        assertEquals(1, origin.getShiftColumns());
        assertEquals(0, origin.getShiftRows());
        assertTrue(origin.hasShifted());
    }

    @Test
    void movingBackLeftShiftsNegative() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);
        origin.follow(16f, 0f, CELL, CELL);

        origin.follow(0f, 0f, CELL, CELL);

        assertEquals(0, origin.getOriginColumn());
        assertEquals(-1, origin.getShiftColumns());
    }

    @Test
    void movingSeveralCellsShiftsByDifference() {
        MapWindowOrigin origin = new MapWindowOrigin(1, 1);

        origin.follow(80f, 48f, CELL, CELL);

        assertEquals(4, origin.getOriginColumn());
        assertEquals(2, origin.getOriginRow());
        assertEquals(5, origin.getShiftColumns());
        assertEquals(3, origin.getShiftRows());
    }

    @Test
    void repeatedSameCameraLeavesNoShift() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);
        origin.follow(48f, 32f, CELL, CELL);

        origin.follow(48f, 32f, CELL, CELL);

        assertEquals(0, origin.getShiftColumns());
        assertEquals(0, origin.getShiftRows());
        assertFalse(origin.hasShifted());
    }

    @Test
    void movementInsideACellLeavesNoShift() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);

        origin.follow(10f, 10f, CELL, CELL);

        assertFalse(origin.hasShifted());
    }

    @Test
    void tallCellsUseEachAxisSize() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);

        origin.follow(32f, 32f, 8f, 16f);

        assertEquals(4, origin.getOriginColumn());
        assertEquals(2, origin.getOriginRow());
    }

    @Test
    void negativeMarginIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MapWindowOrigin(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MapWindowOrigin(0, -1));
    }

    @Test
    void invalidCellSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MapWindowOrigin.originFor(0f, 0f, 0));
        assertThrows(IllegalArgumentException.class, () -> MapWindowOrigin.originFor(0f, -8f, 0));
        assertThrows(IllegalArgumentException.class,
                () -> MapWindowOrigin.originFor(0f, Float.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> MapWindowOrigin.originFor(0f, Float.POSITIVE_INFINITY, 0));
    }

    @Test
    void hugeAndInfiniteCamerasClampToMaxOrigin() {
        int max = MapWindowOrigin.MAX_ORIGIN;

        assertEquals(max, MapWindowOrigin.originFor(Float.POSITIVE_INFINITY, CELL, 0));
        assertEquals(-max, MapWindowOrigin.originFor(Float.NEGATIVE_INFINITY, CELL, 0));
        assertEquals(max, MapWindowOrigin.originFor(1e30f, CELL, 0));
        assertEquals(-max, MapWindowOrigin.originFor(-1e30f, CELL, 0));
        assertEquals(-max,
                MapWindowOrigin.originFor(Float.POSITIVE_INFINITY, CELL, Integer.MAX_VALUE));
    }

    @Test
    void nanCameraCountsAsZero() {
        assertEquals(0, MapWindowOrigin.originFor(Float.NaN, CELL, 0));
        assertEquals(-2, MapWindowOrigin.originFor(Float.NaN, CELL, 2));
    }

    @Test
    void shiftBetweenClampedExtremesDoesNotWrap() {
        MapWindowOrigin origin = new MapWindowOrigin(0, 0);
        origin.follow(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, CELL, CELL);

        origin.follow(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, CELL, CELL);

        assertEquals(MapWindowOrigin.MAX_ORIGIN, origin.getOriginColumn());
        assertEquals(2L * MapWindowOrigin.MAX_ORIGIN, origin.getShiftColumns());
        assertEquals(2L * MapWindowOrigin.MAX_ORIGIN, origin.getShiftRows());
        assertTrue(origin.getOriginColumn() + 1_000_000L < Integer.MAX_VALUE);
    }

    @Test
    void hugeMarginStartsAtClampedOrigin() {
        MapWindowOrigin origin = new MapWindowOrigin(Integer.MAX_VALUE, 0);

        assertEquals(-MapWindowOrigin.MAX_ORIGIN, origin.getOriginColumn());
    }
}
