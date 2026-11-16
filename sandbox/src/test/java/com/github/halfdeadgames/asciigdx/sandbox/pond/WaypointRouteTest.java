package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaypointRouteTest {

    private static int[] endOf(int startColumn, int startRow, String steps) {
        int column = startColumn;
        int row = startRow;
        for (char step : steps.toCharArray()) {
            column += step == 'L' ? -1 : step == 'R' ? 1 : 0;
            row += step == 'U' ? -1 : step == 'D' ? 1 : 0;
        }
        return new int[]{column, row};
    }

    private static int count(String steps, char step) {
        return (int) steps.chars().filter(c -> c == step).count();
    }

    @Test
    void eachLegEndsOnItsWaypoint() {
        String firstLeg = WaypointRoute.from(40, 22).to(30, 17).back().route();
        int firstLegLength = 10 + 5;
        int[] end = endOf(40, 22, firstLeg.substring(0, firstLegLength));
        assertEquals(30, end[0]);
        assertEquals(17, end[1]);
    }

    @Test
    void routeIsClosed() {
        String route =
                WaypointRoute.from(40, 22).to(30, 17).to(8, 5).pause(2).to(50, 40).back().route();
        int[] end = endOf(40, 22, route);
        assertEquals(40, end[0]);
        assertEquals(22, end[1]);
    }

    @Test
    void routeThatDoesNotEndAtItsStartIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> WaypointRoute.from(40, 22).to(30, 17).route());
    }

    @Test
    void diagonalLegSpreadsStepsEvenlyWithNoLongRunsAndNoAdjacentVerticalSteps() {
        String leg = WaypointRoute.from(0, 0).to(10, 5).back().route().substring(0, 15);
        assertEquals(10, count(leg, 'R'));
        assertEquals(5, count(leg, 'D'));
        assertFalse(leg.contains("RRR"), leg);
        assertFalse(leg.contains("DD"), leg);
    }

    @Test
    void verticalMajorLegSpreadsHorizontalStepsEvenly() {
        String leg = WaypointRoute.from(0, 0).to(-4, -9).back().route().substring(0, 13);
        assertEquals(4, count(leg, 'L'));
        assertEquals(9, count(leg, 'U'));
        assertFalse(leg.contains("LL"), leg);
        assertFalse(leg.contains("UUU"), leg);
    }

    @Test
    void pauseAddsExactlyThatManyWaitSteps() {
        String route = WaypointRoute.from(0, 0).to(3, 0).pause(4).back().route();
        assertEquals(4, count(route, 'W'));
        assertEquals("RRRWWWWLLL", route);
    }

    @Test
    void negativePauseIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WaypointRoute.from(0, 0).pause(-1));
    }

    @Test
    void pureHorizontalAndVerticalLegsUseOneAxis() {
        assertEquals("RRRLLL", WaypointRoute.from(0, 0).to(3, 0).back().route());
        assertEquals("UUDD", WaypointRoute.from(0, 0).to(0, -2).back().route());
    }

    @Test
    void zeroLengthLegAddsNothing() {
        assertTrue(WaypointRoute.from(5, 5).to(5, 5).back().route().isEmpty());
        assertEquals("RL", WaypointRoute.from(0, 0).to(0, 0).to(1, 0).to(1, 0).back().route());
    }
}
