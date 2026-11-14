package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellTransformTest {

    private final CellTransform cell = new CellTransform(3, 2);

    @Test
    void startsAtDefaultsAndIsNotLifted() {
        assertEquals(3, cell.getColumn());
        assertEquals(2, cell.getRow());
        assertEquals(0f, cell.getOffsetX());
        assertEquals(0f, cell.getOffsetY());
        assertEquals(0f, cell.getRotation());
        assertEquals(1f, cell.getScaleX());
        assertEquals(1f, cell.getScaleY());
        assertEquals(0.5f, cell.getPivotX());
        assertEquals(0.5f, cell.getPivotY());
        assertEquals(1f, cell.getAlphaMultiplier());
        assertFalse(cell.isLifted());
        assertFalse(cell.isDetached());
    }

    @Test
    void eachValueLiftsTheGlyph() {
        cell.setOffset(1f, 0f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setOffset(0f, -1f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setRotation(10f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setScale(1f, 2f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setScale(-1f, 1f);
        assertTrue(cell.isLifted());
        cell.reset();
        cell.setAlphaMultiplier(0.9f);
        assertTrue(cell.isLifted());
    }

    @Test
    void wholeTurnsAndPivotDoNotLift() {
        cell.setRotation(360f);
        assertFalse(cell.isLifted());
        cell.setRotation(-720f);
        assertFalse(cell.isLifted());
        cell.setRotation(0f);
        cell.setPivot(0f, 1f);
        assertFalse(cell.isLifted());
    }

    @Test
    void alphaIsClamped() {
        cell.setAlphaMultiplier(2f);
        assertEquals(1f, cell.getAlphaMultiplier());
        cell.setAlphaMultiplier(-1f);
        assertEquals(0f, cell.getAlphaMultiplier());
    }

    @Test
    void relativeMutatorsAdd() {
        cell.offsetBy(1f, 2f);
        cell.offsetBy(1f, 2f);
        cell.rotateBy(30f);
        cell.rotateBy(30f);
        assertEquals(2f, cell.getOffsetX());
        assertEquals(4f, cell.getOffsetY());
        assertEquals(60f, cell.getRotation());
    }

    @Test
    void resetRestoresEveryDefaultIncludingThePivot() {
        cell.setOffset(1f, 1f);
        cell.setRotation(5f);
        cell.setScale(2f);
        cell.setPivot(0f, 0f);
        cell.setAlphaMultiplier(0.2f);

        cell.reset();

        assertFalse(cell.isLifted());
        assertEquals(0.5f, cell.getPivotX());
        assertEquals(0.5f, cell.getPivotY());
        assertEquals(1f, cell.getScaleX());
        assertEquals(1f, cell.getAlphaMultiplier());
    }

    @Test
    void detachedHandlesKeepGettersAndRejectMutators() {
        cell.setOffset(4f, 5f);
        cell.detach();

        assertTrue(cell.isDetached());
        assertEquals(4f, cell.getOffsetX());
        assertEquals(5f, cell.getOffsetY());
        assertThrows(IllegalStateException.class, () -> cell.setOffset(0f, 0f));
        assertThrows(IllegalStateException.class, () -> cell.offsetBy(1f, 1f));
        assertThrows(IllegalStateException.class, () -> cell.setRotation(1f));
        assertThrows(IllegalStateException.class, () -> cell.rotateBy(1f));
        assertThrows(IllegalStateException.class, () -> cell.setScale(1f));
        assertThrows(IllegalStateException.class, () -> cell.setScale(1f, 1f));
        assertThrows(IllegalStateException.class, () -> cell.setPivot(0f, 0f));
        assertThrows(IllegalStateException.class, () -> cell.setAlphaMultiplier(0.5f));
        assertThrows(IllegalStateException.class, cell::reset);
    }

    @Test
    void detachMessageNamesTheCause() {
        cell.detach();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, cell::reset);

        assertEquals("Cell transform was detached from its layer", thrown.getMessage());
    }
}
