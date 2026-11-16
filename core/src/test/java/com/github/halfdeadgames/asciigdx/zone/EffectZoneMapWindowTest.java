package com.github.halfdeadgames.asciigdx.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.HeadlessLayers;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EffectZoneMapWindowTest {

    private static final float CELL_WIDTH = 8f;
    private static final float CELL_HEIGHT = 12f;

    /** Records the grid cell of the handle and the zone cell position of every call. */
    private record Call(int handleColumn, int handleRow, int whereColumn, int whereRow,
                        int zoneColumn,
                        int zoneRow) {
    }

    private final LayerStack stack = HeadlessLayers.createStack();
    private final Layer layer = HeadlessLayers.add(stack, new TerminalGrid(6, 4));
    private final List<Call> calls = new ArrayList<>();
    private final CellEffect recordingEffect =
            (cell, where, time) -> calls.add(new Call(cell.getColumn(),
                    cell.getRow(), where.getColumn(), where.getRow(), where.getZoneColumn(),
                    where.getZoneRow()));

    private static RectangleShape filledRectangle(int left, int top, int width, int height) {
        return RectangleShape.forSquareCells(left, top, width, height, ShapeFill.FILLED);
    }

    private void moveCameraToCell(int column, int row) {
        stack.setCamera(column * CELL_WIDTH, row * CELL_HEIGHT);
    }

    @Test
    void shapeCellIsReadInMapCoordinatesAfterTheCameraMoved() {
        layer.enableMapWindow();
        moveCameraToCell(3, 1);

        new EffectZone(filledRectangle(5, 2, 1, 1), recordingEffect).apply(layer, 0f);

        assertEquals(List.of(new Call(2, 1, 5, 2, 0, 0)), calls);
    }

    @Test
    void shapeFollowsTheWorldWhenTheCameraStepsAgain() {
        layer.enableMapWindow();
        EffectZone zone = new EffectZone(filledRectangle(5, 2, 1, 1), recordingEffect);

        moveCameraToCell(3, 1);
        zone.apply(layer, 0f);
        moveCameraToCell(4, 1);
        stack.beginCellEffects(0f);
        zone.apply(layer, 0f);

        assertEquals(List.of(new Call(2, 1, 5, 2, 0, 0), new Call(1, 1, 5, 2, 0, 0)), calls);
    }

    @Test
    void shapeCellsOutsideTheWindowAreSkipped() {
        layer.enableMapWindow();
        moveCameraToCell(3, 1);

        new EffectZone(filledRectangle(1, 0, 10, 10), recordingEffect).apply(layer, 0f);

        assertEquals(24, calls.size());
        for (Call call : calls) {
            assertTrue(call.whereColumn() >= 3 && call.whereColumn() < 9, "column " + call);
            assertTrue(call.whereRow() >= 1 && call.whereRow() < 5, "row " + call);
            assertEquals(call.whereColumn() - 3, call.handleColumn());
            assertEquals(call.whereRow() - 1, call.handleRow());
            assertEquals(call.whereColumn() - 1, call.zoneColumn());
            assertEquals(call.whereRow(), call.zoneRow());
        }
    }

    @Test
    void shapeOutsideTheWindowMakesNoCalls() {
        layer.enableMapWindow();
        moveCameraToCell(10, 10);

        new EffectZone(filledRectangle(0, 0, 10, 10), recordingEffect).apply(layer, 0f);
        new EffectZone(filledRectangle(16, 14, 3, 3), recordingEffect).apply(layer, 0f);

        assertTrue(calls.isEmpty());
        assertEquals(0, layer.getFrameCellHandleCount());
    }

    @Test
    void shapeStraddlingTheWindowEdgeAffectsOnlyTheInsideCells() {
        layer.enableMapWindow();
        moveCameraToCell(3, 1);

        new EffectZone(filledRectangle(1, 0, 3, 2), recordingEffect).apply(layer, 0f);

        assertEquals(List.of(new Call(0, 0, 3, 1, 2, 1)), calls);
    }

    @Test
    void seededVariationStaysWithTheMapCellAcrossACameraStep() {
        Map<Long, Float> phaseByMapCell = new HashMap<>();
        Phase phase = Phase.scattered(7);
        CellEffect phaseEffect = (cell, where, time) ->
                phaseByMapCell.put(key(where.getColumn(), where.getRow()),
                        phase.getCyclesAt(where));
        EffectZone zone = new EffectZone(filledRectangle(0, 0, 20, 20), phaseEffect);
        layer.enableMapWindow();

        moveCameraToCell(3, 1);
        zone.apply(layer, 0f);
        Map<Long, Float> beforeStep = new HashMap<>(phaseByMapCell);
        phaseByMapCell.clear();
        moveCameraToCell(4, 1);
        stack.beginCellEffects(0f);
        zone.apply(layer, 0f);

        int shared = 0;
        for (Map.Entry<Long, Float> entry : phaseByMapCell.entrySet()) {
            Float before = beforeStep.get(entry.getKey());
            if (before != null) {
                assertEquals(before, entry.getValue());
                shared++;
            }
        }
        assertEquals(20, shared);
    }

    @Test
    void nonMapLayerKeepsGridCoordinates() {
        Layer plain = HeadlessLayers.create(new TerminalGrid(6, 4));

        new EffectZone(filledRectangle(5, 2, 1, 1), recordingEffect).apply(plain, 0f);

        assertEquals(List.of(new Call(5, 2, 5, 2, 0, 0)), calls);
    }

    @Test
    void disablingMapWindowReturnsToGridCoordinates() {
        layer.enableMapWindow();
        moveCameraToCell(3, 1);
        layer.disableMapWindow();

        new EffectZone(filledRectangle(5, 2, 1, 1), recordingEffect).apply(layer, 0f);

        assertEquals(List.of(new Call(5, 2, 5, 2, 0, 0)), calls);
    }

    private static long key(int column, int row) {
        return ((long) column << 32) | (row & 0xFFFFFFFFL);
    }
}
