package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerStackMapCellTest {

    private static final int CELL_SIZE = 16;

    private static final class SquareSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return CELL_SIZE;
        }

        @Override
        public int getCellHeight() {
            return CELL_SIZE;
        }

        @Override
        public TextureRegion getGlyphRegion(int codePoint) {
            return null;
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return false;
        }

        @Override
        public void dispose() {
        }
    }

    private final LayerStack stack =
            TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final Layer layer = stack.add(new TerminalGrid(10, 6), new SquareSource());
    private final GridPoint2 mapCell = new GridPoint2();
    private final GridPoint2 gridCell = new GridPoint2();

    @Test
    void addsTheWindowOriginToTheGridCell() {
        stack.setCamera(48f, 32f);
        layer.enableMapWindow();

        stack.toMapCell(layer, 0f, 0f, 24f, -20f, mapCell);
        stack.toCell(layer, 0f, 0f, 24f, -20f, gridCell);

        // Origin is (3, 2); world (24, -20) lies in grid cell (1, 1).
        assertEquals(1, gridCell.x);
        assertEquals(1, gridCell.y);
        assertEquals(4, mapCell.x);
        assertEquals(3, mapCell.y);
    }

    @Test
    void parallaxLayerReportsTheMapCellDrawnAtThatPosition() {
        layer.setParallax(0.5f);
        stack.setCamera(96f, 64f);
        layer.enableMapWindow();

        stack.toMapCell(layer, 0f, 0f, 24f, -20f, mapCell);

        // Effective camera is (48, 32), so map cell (4, 3) is drawn from screen x 16 to 32 and
        // from world y -16 down to -32, which contains (24, -20).
        assertEquals(4, mapCell.x);
        assertEquals(3, mapCell.y);
    }

    @Test
    void zoomedAndRotatedLayerMatchesWindowConversionOfTheGridCell() {
        layer.setZoom(1.5f);
        layer.setRotation(25f);
        layer.setPivot(20f, 10f);
        stack.setCamera(70f, 40f);
        layer.enableMapWindow();
        MapWindow window = layer.getMapWindow();

        stack.toCell(layer, 5f, 100f, 60f, 45f, gridCell);
        stack.toMapCell(layer, 5f, 100f, 60f, 45f, mapCell);

        assertEquals(window.toMapColumn(gridCell.x), mapCell.x);
        assertEquals(window.toMapRow(gridCell.y), mapCell.y);
    }

    @Test
    void layerWithoutMapWindowThrowsAndLeavesOutUnchanged() {
        mapCell.set(7, 8);

        assertThrows(IllegalStateException.class,
                () -> stack.toMapCell(layer, 0f, 0f, 24f, -20f, mapCell));

        assertEquals(7, mapCell.x);
        assertEquals(8, mapCell.y);
    }

    @Test
    void removedLayerThrowsIllegalState() {
        layer.enableMapWindow();
        stack.remove(layer);

        assertThrows(IllegalStateException.class,
                () -> stack.toMapCell(layer, 0f, 0f, 0f, 0f, mapCell));
    }

    @Test
    void layerOfAnotherStackThrowsIllegalArgument() {
        LayerStack other = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
        Layer foreign = other.add(new TerminalGrid(2, 2), new SquareSource());
        foreign.enableMapWindow();

        assertThrows(IllegalArgumentException.class,
                () -> stack.toMapCell(foreign, 0f, 0f, 0f, 0f, mapCell));
    }

    @Test
    void nullOutThrows() {
        layer.enableMapWindow();

        assertThrows(NullPointerException.class,
                () -> stack.toMapCell(layer, 0f, 0f, 0f, 0f, null));
    }
}
