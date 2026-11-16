package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerMapWindowTest {

    private static final class SquareSource implements GlyphSource {
        private final int cellSize;

        SquareSource(int cellSize) {
            this.cellSize = cellSize;
        }

        @Override
        public int getCellWidth() {
            return cellSize;
        }

        @Override
        public int getCellHeight() {
            return cellSize;
        }

        @Override
        public TextureRegion glyph(int codePoint) {
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

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final TerminalGrid grid = new TerminalGrid(10, 6);
    private final Layer layer = stack.add(grid, new SquareSource(16));
    private final Vector2 point = new Vector2();

    private Layer addLayer(int cellSize) {
        return stack.add(new TerminalGrid(10, 6), new SquareSource(cellSize));
    }

    private static int originColumn(Layer target) {
        return target.getMapWindow().getOriginColumn();
    }

    private float screenXOfMapPixel(Layer target, float mapPixelX) {
        float gridPixelX = mapPixelX - originColumn(target) * target.getGlyphSource().getCellWidth();
        stack.worldPointOf(target, 0f, 0f, gridPixelX, 0f, point);
        return point.x;
    }

    @Test
    void enableTakesOriginFromCameraWithoutScrolling() {
        stack.setCamera(48f, 32f);
        grid.setGlyph(2, 0, 'a');

        layer.enableMapWindow();

        assertTrue(layer.isMapWindow());
        assertEquals(3, originColumn(layer));
        assertEquals(2, layer.getMapWindow().getOriginRow());
        assertEquals('a', grid.getGlyph(2, 0));
    }

    @Test
    void cameraCrossingACellScrollsContentAndUpdatesOrigin() {
        layer.enableMapWindow();
        grid.setGlyph(2, 0, 'a');

        stack.setCamera(16f, 0f);

        assertEquals(1, originColumn(layer));
        assertEquals('a', grid.getGlyph(1, 0));
        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(2, 0));
    }

    @Test
    void cameraMovingWithinACellDoesNotScroll() {
        layer.enableMapWindow();
        grid.setGlyph(2, 0, 'a');

        stack.setCamera(15f, 15f);

        assertEquals(0, originColumn(layer));
        assertEquals('a', grid.getGlyph(2, 0));
    }

    @Test
    void differentCellSizesGetDifferentOriginsFromOneCamera() {
        Layer small = addLayer(8);
        layer.enableMapWindow();
        small.enableMapWindow();

        stack.setCamera(48f, 0f);

        assertEquals(3, originColumn(layer));
        assertEquals(6, originColumn(small));
    }

    @Test
    void parallaxScalesTheOrigin() {
        layer.setParallax(0.5f);
        layer.enableMapWindow();

        stack.setCamera(96f, 0f);

        assertEquals(3, originColumn(layer));
    }

    @Test
    void settingParallaxFollowsAgain() {
        stack.setCamera(64f, 0f);
        layer.enableMapWindow();
        grid.setGlyph(5, 0, 'a');

        layer.setParallax(0.5f);

        assertEquals(2, originColumn(layer));
        assertEquals('a', grid.getGlyph(7, 0));
    }

    @Test
    void panelMemberFollowsPanelCameraOnly() {
        Panel panel = stack.addPanel(0f, 0f, 100f, 100f);
        layer.setPanel(panel);
        layer.enableMapWindow();

        stack.setCamera(48f, 0f);
        assertEquals(0, originColumn(layer));

        panel.setCamera(32f, 0f);
        assertEquals(2, originColumn(layer));
    }

    @Test
    void settingPanelFollowsAgain() {
        Panel panel = stack.addPanel(0f, 0f, 100f, 100f);
        panel.setCamera(32f, 0f);
        layer.enableMapWindow();
        assertEquals(0, originColumn(layer));

        layer.setPanel(panel);
        assertEquals(2, originColumn(layer));

        layer.setPanel(null);
        assertEquals(0, originColumn(layer));
    }

    @Test
    void settingGlyphSourceFollowsAgain() {
        stack.setCamera(48f, 0f);
        layer.enableMapWindow();

        layer.setGlyphSource(new SquareSource(8));

        assertEquals(6, originColumn(layer));
    }

    @Test
    void settingGlyphSourceFlagsEveryCellForRefill() {
        stack.setCamera(160f, 0f);
        layer.enableMapWindow();
        MapWindow window = layer.getMapWindow();
        window.markAllFilled();

        layer.setGlyphSource(new SquareSource(8));

        int[] visited = {0};
        window.forEachCellToFill((mapColumn, mapRow) -> visited[0]++);
        assertEquals(10 * 6, visited[0]);
        assertEquals(20, window.getOriginColumn());
    }

    @Test
    void marginsSubtractFromTheOrigin() {
        stack.setCamera(48f, 32f);

        layer.enableMapWindow(1, 2);

        assertEquals(2, originColumn(layer));
        assertEquals(0, layer.getMapWindow().getOriginRow());
    }

    @Test
    void negativeMarginsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> layer.enableMapWindow(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> layer.enableMapWindow(0, -1));
        assertFalse(layer.isMapWindow());
    }

    @Test
    void disableStopsFollowingAndKeepsContent() {
        layer.enableMapWindow();
        grid.setGlyph(2, 0, 'a');

        layer.disableMapWindow();
        stack.setCamera(48f, 0f);

        assertFalse(layer.isMapWindow());
        assertEquals('a', grid.getGlyph(2, 0));
    }

    @Test
    void getMapWindowThrowsWhenDisabled() {
        assertThrows(IllegalStateException.class, layer::getMapWindow);
    }

    @Test
    void removedLayerThrows() {
        layer.enableMapWindow();
        layer.remove();

        assertThrows(IllegalStateException.class, layer::enableMapWindow);
        assertThrows(IllegalStateException.class, layer::disableMapWindow);
        assertThrows(IllegalStateException.class, layer::isMapWindow);
        assertThrows(IllegalStateException.class, layer::getMapWindow);
    }

    @Test
    void screenPositionOfAMapCellFollowsTheCameraExactly() {
        layer.enableMapWindow();
        float mapCellLeft = 5 * 16f;

        for (float cameraX : new float[] {0f, 4f, 16f, 20f, 37f, 48f}) {
            stack.setCamera(cameraX, 0f);
            assertEquals(mapCellLeft - cameraX, screenXOfMapPixel(layer, mapCellLeft), 1e-4f,
                    "camera " + cameraX);
        }
    }

    @Test
    void smallCellLayerFollowsTheSameCamera() {
        Layer small = addLayer(8);
        small.enableMapWindow();

        for (float cameraX : new float[] {0f, 3f, 8f, 21f, 48f}) {
            stack.setCamera(cameraX, 0f);
            assertEquals(80f - cameraX, screenXOfMapPixel(small, 80f), 1e-4f, "camera " + cameraX);
        }
    }

    @Test
    void plainLayerTransformIsUnchanged() {
        stack.setCamera(20f, 0f);

        stack.worldPointOf(layer, 0f, 0f, 30f, 0f, point);

        assertEquals(10f, point.x, 1e-4f);
    }

    @Test
    void pivotInMapPixelsStaysFixedWhileTheWindowScrolls() {
        layer.setZoom(2f);
        layer.setPivot(80f, 0f);
        layer.enableMapWindow();

        for (float cameraX : new float[] {0f, 16f, 32f}) {
            stack.setCamera(cameraX, 0f);
            assertEquals(80f - cameraX, screenXOfMapPixel(layer, 80f), 1e-4f, "pivot, camera " + cameraX);
            assertEquals(120f - cameraX, screenXOfMapPixel(layer, 100f), 1e-4f, "off pivot, camera " + cameraX);
        }
    }
}
