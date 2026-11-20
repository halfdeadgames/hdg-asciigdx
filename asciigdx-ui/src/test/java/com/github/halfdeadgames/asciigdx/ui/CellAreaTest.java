package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellAreaTest {

    @Test
    void keepsItsFields() {
        CellArea area = new CellArea(1, 2, 3, 4);

        assertEquals(1, area.column());
        assertEquals(2, area.row());
        assertEquals(3, area.columnCount());
        assertEquals(4, area.rowCount());
    }

    @Test
    void acceptsTheOriginAndASingleCell() {
        CellArea area = new CellArea(0, 0, 1, 1);

        assertEquals(1, area.columnCount());
    }

    @Test
    void rejectsANegativeColumnAndNamesIt() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> new CellArea(-1, 0, 1, 1));

        assertTrue(thrown.getMessage().contains("-1"));
    }

    @Test
    void rejectsANegativeRow() {
        assertThrows(IllegalArgumentException.class, () -> new CellArea(0, -3, 1, 1));
    }

    @Test
    void rejectsAColumnCountBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new CellArea(0, 0, 0, 1));
    }

    @Test
    void rejectsARowCountBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new CellArea(0, 0, 1, 0));
    }

    @Test
    void equalsComparesEveryField() {
        assertEquals(new CellArea(1, 2, 3, 4), new CellArea(1, 2, 3, 4));
        assertEquals(new CellArea(1, 2, 3, 4).hashCode(), new CellArea(1, 2, 3, 4).hashCode());
        assertNotEquals(new CellArea(1, 2, 3, 4), new CellArea(1, 2, 3, 5));
    }
}
