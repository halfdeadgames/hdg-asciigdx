package com.github.halfdeadgames.asciigdx.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.CellHandle;
import com.github.halfdeadgames.asciigdx.render.HeadlessLayers;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectZoneTest {

    private record Call(int column, int row, int zoneColumn, int zoneRow, float distance, float fraction,
                        float time) {
    }

    private static final class RecordingEffect implements CellEffect {
        final List<Call> calls = new ArrayList<>();

        @Override
        public void apply(CellHandle cell, ZoneCell where, float timeSeconds) {
            assertEquals(where.getColumn(), cell.getColumn());
            assertEquals(where.getRow(), cell.getRow());
            calls.add(new Call(where.getColumn(), where.getRow(), where.getZoneColumn(), where.getZoneRow(),
                    where.getDistance(), where.getDistanceFraction(), timeSeconds));
        }
    }

    private final TerminalGrid grid = new TerminalGrid(6, 4);
    private final Layer layer = HeadlessLayers.create(grid);
    private final RecordingEffect effect = new RecordingEffect();

    private static RectangleShape filledRectangle(int left, int top, int width, int height) {
        return new RectangleShape(left, top, width, height, ShapeFill.FILLED);
    }

    private static List<Call> expectedCalls(CellShape shape, int gridWidth, int gridHeight, float time) {
        List<Call> expected = new ArrayList<>();
        shape.forEachCell((column, row, distance, fraction) -> {
            if (column >= 0 && row >= 0 && column < gridWidth && row < gridHeight) {
                expected.add(new Call(column, row, column - shape.getLeft(), row - shape.getTop(),
                        distance, fraction, time));
            }
        });
        return expected;
    }

    @Test
    void everyInGridCellGetsExactlyOneCallWithItsPositionAndDistances() {
        RectangleShape shape = filledRectangle(1, 1, 3, 2);

        new EffectZone(shape, effect).apply(layer, 1.5f);

        assertEquals(6, effect.calls.size());
        assertEquals(expectedCalls(shape, 6, 4, 1.5f), effect.calls);
        Call first = effect.calls.get(0);
        assertEquals(1, first.column());
        assertEquals(1, first.row());
        assertEquals(0, first.zoneColumn());
        assertEquals(0, first.zoneRow());
        Call last = effect.calls.get(5);
        assertEquals(2, last.zoneColumn());
        assertEquals(1, last.zoneRow());
        assertEquals(1f, last.fraction());
    }

    @Test
    void cellsOutsideTheGridAreSkippedWithoutThrowing() {
        RectangleShape shape = filledRectangle(-1, -1, 3, 3);

        new EffectZone(shape, effect).apply(layer, 0f);

        assertEquals(4, effect.calls.size());
        assertEquals(expectedCalls(shape, 6, 4, 0f), effect.calls);
        assertEquals(1, effect.calls.get(0).zoneColumn());
        assertEquals(1, effect.calls.get(0).zoneRow());
    }

    @Test
    void cellsPastTheFarEdgesAreSkipped() {
        RectangleShape shape = filledRectangle(5, 3, 3, 3);

        new EffectZone(shape, effect).apply(layer, 0f);

        assertEquals(1, effect.calls.size());
        assertEquals(5, effect.calls.get(0).column());
        assertEquals(3, effect.calls.get(0).row());
    }

    @Test
    void shapesFullyOffTheGridMakeNoCalls() {
        new EffectZone(filledRectangle(-5, 0, 2, 2), effect).apply(layer, 0f);
        new EffectZone(filledRectangle(0, -5, 2, 2), effect).apply(layer, 0f);
        new EffectZone(filledRectangle(6, 0, 2, 2), effect).apply(layer, 0f);
        new EffectZone(filledRectangle(0, 4, 2, 2), effect).apply(layer, 0f);

        assertTrue(effect.calls.isEmpty());
        assertEquals(0, layer.getCellHandleCount());
    }

    @Test
    void overlappingZonesComposeMotionAfterAReset() {
        CellShape shape = filledRectangle(2, 2, 1, 1);
        EffectZone first = new EffectZone(shape, (cell, where, time) -> {
            cell.offsetBy(1f, 2f);
            cell.rotateBy(10f);
            cell.scaleBy(2f);
        });
        EffectZone second = new EffectZone(shape, (cell, where, time) -> {
            cell.offsetBy(3f, -1f);
            cell.rotateBy(5f);
            cell.scaleBy(3f, 0.5f);
        });

        layer.resetCellHandles();
        first.apply(layer, 0f);
        second.apply(layer, 0f);

        CellHandle cell = layer.cell(2, 2);
        assertEquals(4f, cell.getOffsetX());
        assertEquals(1f, cell.getOffsetY());
        assertEquals(15f, cell.getRotation());
        assertEquals(6f, cell.getScaleX());
        assertEquals(1f, cell.getScaleY());

        layer.resetCellHandles();
        first.apply(layer, 0f);

        assertEquals(1f, cell.getOffsetX());
        assertEquals(2f, cell.getScaleX());
    }

    @Test
    void overlappingZonesLetTheLastColorAndGlyphWin() {
        CellShape shape = filledRectangle(0, 0, 1, 1);
        EffectZone first = new EffectZone(shape, (cell, where, time) -> {
            cell.setForegroundMix(0xFF0000FF, 1f);
            cell.setCodePoint('a');
        });
        EffectZone second = new EffectZone(shape, (cell, where, time) -> {
            cell.setForegroundMix(0x00FF00FF, 1f);
            cell.setCodePoint('b');
        });

        layer.resetCellHandles();
        first.apply(layer, 0f);
        second.apply(layer, 0f);

        assertEquals(0x00FF00FF, layer.cell(0, 0).getForegroundMixColor());
        assertEquals('b', layer.cell(0, 0).getCodePoint());
    }

    @Test
    void applyResetReleaseApplyAgainLeavesNoStaleHandles() {
        EffectZone zone = new EffectZone(filledRectangle(0, 0, 2, 2),
                (cell, where, time) -> cell.offsetBy(1f, 0f));
        zone.apply(layer, 0f);
        CellHandle firstHandle = layer.cell(0, 0);

        layer.resetCellHandles();
        layer.releaseIdleCellHandles();
        zone.apply(layer, 0f);

        assertTrue(firstHandle.isDetached());
        CellHandle freshHandle = layer.cell(0, 0);
        assertFalse(freshHandle.isDetached());
        assertEquals(1f, freshHandle.getOffsetX());
        assertEquals(4, layer.getCellHandleCount());
    }

    @Test
    void setShapeAndSetEffectRetarget() {
        EffectZone zone = new EffectZone(filledRectangle(0, 0, 1, 1), effect);
        RecordingEffect otherEffect = new RecordingEffect();
        RectangleShape otherShape = filledRectangle(3, 2, 2, 1);

        zone.setShape(otherShape);
        zone.setEffect(otherEffect);
        zone.apply(layer, 2f);

        assertSame(otherShape, zone.getShape());
        assertSame(otherEffect, zone.getEffect());
        assertTrue(effect.calls.isEmpty());
        assertEquals(expectedCalls(otherShape, 6, 4, 2f), otherEffect.calls);
        assertEquals(2, otherEffect.calls.size());
    }

    @Test
    void shrinkingTheGridBetweenAppliesSkipsCellsOutsideTheNewSize() {
        RectangleShape shape = filledRectangle(0, 0, 6, 1);
        EffectZone zone = new EffectZone(shape, effect);
        zone.apply(layer, 0f);
        assertEquals(6, effect.calls.size());
        effect.calls.clear();

        grid.resize(3, 4);
        zone.apply(layer, 0f);

        assertEquals(3, effect.calls.size());
        assertEquals(expectedCalls(shape, 3, 4, 0f), effect.calls);
    }

    @Test
    void constructorAndSettersRejectNull() {
        CellShape shape = filledRectangle(0, 0, 1, 1);
        EffectZone zone = new EffectZone(shape, effect);

        assertThrows(NullPointerException.class, () -> new EffectZone(null, effect));
        assertThrows(NullPointerException.class, () -> new EffectZone(shape, null));
        assertThrows(NullPointerException.class, () -> zone.setShape(null));
        assertThrows(NullPointerException.class, () -> zone.setEffect(null));
        assertNotNull(zone.getShape());
    }

    @Test
    void applyingFromInsideAnEffectThrowsAndTheZoneStaysUsable() {
        EffectZone[] holder = new EffectZone[1];
        holder[0] = new EffectZone(filledRectangle(0, 0, 1, 1),
                (cell, where, time) -> holder[0].apply(layer, time));

        assertThrows(IllegalStateException.class, () -> holder[0].apply(layer, 0f));

        holder[0].setEffect(effect);
        holder[0].apply(layer, 0f);
        assertEquals(1, effect.calls.size());
    }
}
