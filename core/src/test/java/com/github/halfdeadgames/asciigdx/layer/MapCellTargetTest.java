package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class MapCellTargetTest {

    private static final int COLUMNS = 10;
    private static final int ROWS = 6;
    private static final float CELL_WIDTH = 8f; // HeadlessLayers cell width

    private final LayerStack stack = HeadlessLayers.createStack();
    private final Layer layer = HeadlessLayers.add(stack, new TerminalGrid(COLUMNS, ROWS));

    private MapWindow enable() {
        layer.enableMapWindow();
        return layer.getMapWindow();
    }

    private void scrollColumns(int columns) {
        stack.setCamera(columns * CELL_WIDTH, 0f);
    }

    private static void writeOffsetX(CellHandle cell, float value) {
        cell.setOffset(value, 0f);
    }

    @Test
    void writeInsideTheWindowReachesTheCellHandle() {
        MapWindow window = enable();
        MapCellTarget target = window.createCellTarget(2, 1);

        target.writeIfShown(MapCellTargetTest::writeOffsetX, 3f);

        assertEquals(3f, window.obtainCellHandle(2, 1).getOffsetX());
    }

    @Test
    void writeAfterTheCellScrolledOutIsSkipped() {
        MapWindow window = enable();
        MapCellTarget target = window.createCellTarget(2, 1);

        scrollColumns(5);

        target.writeIfShown(MapCellTargetTest::writeOffsetX, 7f);
        scrollColumns(0);
        assertEquals(0f, window.obtainCellHandle(2, 1).getOffsetX());
    }

    @Test
    void writeLandsAgainAfterTheCellScrollsBackIn() {
        MapWindow window = enable();
        MapCellTarget target = window.createCellTarget(2, 1);
        scrollColumns(5);
        target.writeIfShown(MapCellTargetTest::writeOffsetX, 7f);
        scrollColumns(0);

        target.writeIfShown(MapCellTargetTest::writeOffsetX, 4f);

        assertEquals(4f, window.obtainCellHandle(2, 1).getOffsetX());
    }

    @Test
    void targetForAnOffWindowCellWorksOnceTheCellScrollsIn() {
        MapWindow window = enable();
        MapCellTarget target = window.createCellTarget(COLUMNS + 2, 1);
        target.writeIfShown(MapCellTargetTest::writeOffsetX, 9f);

        scrollColumns(5);
        assertEquals(0f, window.obtainCellHandle(COLUMNS + 2, 1).getOffsetX());
        target.writeIfShown(MapCellTargetTest::writeOffsetX, 5f);

        assertEquals(5f, window.obtainCellHandle(COLUMNS + 2, 1).getOffsetX());
    }

    @Test
    void nullWriterThrows() {
        MapCellTarget target = enable().createCellTarget(0, 0);

        assertThrows(IllegalArgumentException.class, () -> target.writeIfShown(null, 1f));
    }

    @Test
    void detachedWindowThrowsOnWriteAndOnCreate() {
        MapWindow window = enable();
        MapCellTarget target = window.createCellTarget(0, 0);
        layer.disableMapWindow();

        assertThrows(IllegalStateException.class,
                () -> target.writeIfShown(MapCellTargetTest::writeOffsetX, 1f));
        assertThrows(IllegalStateException.class, () -> window.createCellTarget(0, 0));
    }
}
