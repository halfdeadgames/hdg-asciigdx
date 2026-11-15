package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PondMapTest {

    @Test
    void startsAsEmptyWater() {
        var map = new PondMap(5, 3);
        assertEquals(5, map.getWidth());
        assertEquals(3, map.getHeight());
        assertEquals(Terrain.WATER, map.cell(4, 2).getTerrain());
        assertFalse(map.cell(0, 0).hasLily(LilySlot.SOUTH_EAST));
    }

    @Test
    void containsMatchesBounds() {
        var map = new PondMap(5, 3);
        assertTrue(map.contains(0, 0));
        assertTrue(map.contains(4, 2));
        assertFalse(map.contains(-1, 0));
        assertFalse(map.contains(0, -1));
        assertFalse(map.contains(5, 0));
        assertFalse(map.contains(0, 3));
    }

    @Test
    void cellOutsideThrows() {
        var map = new PondMap(5, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> map.cell(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> map.cell(0, -1));
    }

    @Test
    void cellsAreDistinct() {
        var map = new PondMap(2, 2);
        map.cell(1, 0).setTerrain(Terrain.LAND);
        assertEquals(Terrain.WATER, map.cell(0, 0).getTerrain());
        assertEquals(Terrain.LAND, map.cell(1, 0).getTerrain());
    }

    @Test
    void cellHoldsLiliesPerSlot() {
        var cell = new PondCell();
        var lily = new Lily();
        cell.putLily(LilySlot.NORTH_EAST, lily);
        assertSame(lily, cell.lilyAt(LilySlot.NORTH_EAST));
        assertTrue(cell.hasLily(LilySlot.NORTH_EAST));
        assertNull(cell.lilyAt(LilySlot.NORTH_WEST));
    }

    @Test
    void slotOffsetsAndLilyDefaults() {
        assertEquals(0, LilySlot.NORTH_WEST.columnOffset());
        assertEquals(1, LilySlot.SOUTH_EAST.columnOffset());
        assertEquals(1, LilySlot.SOUTH_WEST.rowOffset());
        assertEquals(0, LilySlot.NORTH_EAST.rowOffset());
        var lily = new Lily();
        assertEquals(1f, lily.getScale());
        assertEquals(0x2022, lily.codePoint());
    }

    @Test
    void playerStepsOnlyInsideMap() {
        var map = new PondMap(5, 3);
        var player = new PondPlayer(0, 0);
        assertFalse(player.canStep(-1, 0, map));
        assertFalse(player.canStep(0, -1, map));
        assertTrue(player.canStep(1, 0, map));
        player.step(1, 0);
        assertEquals(1, player.getColumn());
        var corner = new PondPlayer(4, 2);
        assertFalse(corner.canStep(1, 0, map));
        assertFalse(corner.canStep(0, 1, map));
    }
}
