package com.github.halfdeadgames.asciigdx.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhaseTest {

    private static ZoneCell cellAt(int column, int row, int zoneColumn, float distance) {
        ZoneCell cell = new ZoneCell();
        cell.set(column, row, zoneColumn, 0, distance, 0f);
        return cell;
    }

    @Test
    void syncIsZeroEverywhere() {
        assertEquals(0f, Phase.sync().cyclesAt(cellAt(3, 4, 2, 5f)));
        assertEquals(0f, Phase.sync().cyclesAt(cellAt(0, 0, 0, 0f)));
    }

    @Test
    void alongColumnsDividesZoneColumnByCellsPerCycle() {
        Phase phase = Phase.alongColumns(4f);

        assertEquals(0f, phase.cyclesAt(cellAt(7, 1, 0, 9f)));
        assertEquals(0.5f, phase.cyclesAt(cellAt(7, 1, 2, 9f)));
        assertEquals(1.5f, phase.cyclesAt(cellAt(0, 0, 6, 0f)));
    }

    @Test
    void byDistanceDividesDistanceByCellsPerCycle() {
        Phase phase = Phase.byDistance(2f);

        assertEquals(0f, phase.cyclesAt(cellAt(0, 0, 0, 0f)));
        assertEquals(1.5f, phase.cyclesAt(cellAt(0, 0, 0, 3f)));
    }

    @Test
    void byDistanceMovesRingsOutwardOverTime() {
        Phase phase = Phase.byDistance(4f);
        ZoneCell inner = cellAt(0, 0, 0, 1f);
        ZoneCell outer = cellAt(0, 0, 0, 3f);
        float period = 2f;
        float crestPosition = 0.25f;

        float innerCrestTime = (crestPosition + phase.cyclesAt(inner)) * period;
        float outerCrestTime = (crestPosition + phase.cyclesAt(outer)) * period;

        assertEquals(crestPosition, wrappedPosition(innerCrestTime, period, phase, inner), 1e-5f);
        assertEquals(crestPosition, wrappedPosition(outerCrestTime, period, phase, outer), 1e-5f);
        assertTrue(outerCrestTime > innerCrestTime);
    }

    private static float wrappedPosition(float time, float period, Phase phase, ZoneCell where) {
        float position = CycleMath.position(time, period, phase, where);
        return position - (float) Math.floor(position);
    }

    @Test
    void scatteredIsStableAndInUnitRange() {
        Phase phase = Phase.scattered(42);
        boolean anyDifferent = false;
        float first = phase.cyclesAt(cellAt(0, 0, 0, 0f));

        for (int column = 0; column < 30; column++) {
            for (int row = 0; row < 30; row++) {
                float value = phase.cyclesAt(cellAt(column, row, 0, 0f));
                assertTrue(value >= 0f && value < 1f, "out of range: " + value);
                assertEquals(value, phase.cyclesAt(cellAt(column, row, 5, 9f)));
                anyDifferent |= value != first;
            }
        }
        assertTrue(anyDifferent);
    }

    @Test
    void scatteredDependsOnSeed() {
        float first = Phase.scattered(1).cyclesAt(cellAt(3, 4, 0, 0f));
        float second = Phase.scattered(2).cyclesAt(cellAt(3, 4, 0, 0f));

        assertNotEquals(first, second);
    }

    @Test
    void invalidArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> Phase.alongColumns(0f));
        assertThrows(IllegalArgumentException.class, () -> Phase.alongColumns(-1f));
        assertThrows(IllegalArgumentException.class, () -> Phase.alongColumns(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> Phase.byDistance(0f));
        assertThrows(IllegalArgumentException.class,
                () -> Phase.byDistance(Float.POSITIVE_INFINITY));
    }
}
