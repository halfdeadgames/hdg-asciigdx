package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayerStackTest {

    private static final class FakeSource implements GlyphSource {
        private final int cellWidth;
        private final int cellHeight;

        FakeSource(int cellWidth, int cellHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }

        @Override
        public int getCellWidth() {
            return cellWidth;
        }

        @Override
        public int getCellHeight() {
            return cellHeight;
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

    private static final class FakeRenderer implements LayerRenderer {
        final String name;
        final List<String> drawLog;
        final List<float[]> positions = new ArrayList<>();
        final List<float[]> tints = new ArrayList<>();
        final List<GlyphSource> glyphSources = new ArrayList<>();
        int disposeCount;

        FakeRenderer(String name, List<String> drawLog) {
            this.name = name;
            this.drawLog = drawLog;
        }

        @Override
        public void draw(SpriteBatch batch, float x, float y, float tintR, float tintG, float tintB, float tintA) {
            drawLog.add(name);
            positions.add(new float[] {x, y});
            tints.add(new float[] {tintR, tintG, tintB, tintA});
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
            glyphSources.add(glyphSource);
        }

        @Override
        public void dispose() {
            disposeCount++;
        }
    }

    private final List<String> drawLog = new ArrayList<>();
    private final List<FakeRenderer> renderers = new ArrayList<>();
    private int created;
    private final LayerStack stack = new LayerStack((grid, source) -> {
        FakeRenderer renderer = new FakeRenderer("L" + created++, drawLog);
        renderers.add(renderer);
        return renderer;
    });
    private final FakeSource font = new FakeSource(8, 12);

    private Layer addLayer() {
        return stack.add(new TerminalGrid(4, 3), font);
    }

    private void assertOrder(String... expected) {
        drawLog.clear();
        stack.draw(null, 0f, 0f);
        assertEquals(List.of(expected), drawLog);
    }

    private float[] lastPosition(int rendererIndex) {
        List<float[]> positions = renderers.get(rendererIndex).positions;
        return positions.get(positions.size() - 1);
    }

    @Test
    void drawsBottomToTop() {
        addLayer();
        addLayer();
        addLayer();
        assertOrder("L0", "L1", "L2");
    }

    @Test
    void moveToChangesOrder() {
        Layer bottom = addLayer();
        addLayer();
        Layer top = addLayer();

        stack.moveTo(top, 0);
        assertOrder("L2", "L0", "L1");

        stack.moveTo(bottom, 2);
        assertOrder("L2", "L1", "L0");
        assertEquals(2, stack.indexOf(bottom));
    }

    @Test
    void layerMoveToDelegatesToStack() {
        addLayer();
        Layer top = addLayer();
        top.moveTo(0);
        assertOrder("L1", "L0");
    }

    @Test
    void insertPlacesLayerAtIndex() {
        addLayer();
        addLayer();
        Layer middle = stack.insert(1, new TerminalGrid(2, 2), font);
        assertOrder("L0", "L2", "L1");
        assertSame(middle, stack.get(1));
        assertEquals(3, stack.size());
    }

    @Test
    void insertRejectsBadIndex() {
        addLayer();
        assertThrows(IndexOutOfBoundsException.class, () -> stack.insert(2, new TerminalGrid(2, 2), font));
        assertThrows(IndexOutOfBoundsException.class, () -> stack.insert(-1, new TerminalGrid(2, 2), font));
    }

    @Test
    void removeKeepsRemainingOrderAndDisposesOnce() {
        addLayer();
        Layer middle = addLayer();
        addLayer();

        stack.remove(middle);

        assertOrder("L0", "L2");
        assertEquals(1, renderers.get(1).disposeCount);
        assertEquals(0, renderers.get(0).disposeCount);
    }

    @Test
    void unchangedLayerStillDrawsEveryFrameInPlace() {
        addLayer();
        addLayer();
        stack.draw(null, 0f, 0f);
        stack.draw(null, 0f, 0f);
        assertEquals(List.of("L0", "L1", "L0", "L1"), drawLog);
    }

    @Test
    void hiddenLayersAreSkippedAndKeepTheirPlace() {
        addLayer();
        Layer middle = addLayer();
        addLayer();

        middle.setVisible(false);
        assertFalse(middle.isVisible());
        assertOrder("L0", "L2");

        middle.setVisible(true);
        assertOrder("L0", "L1", "L2");
    }

    @Test
    void offsetIsYDownFromStackTopLeft() {
        Layer layer = addLayer();
        layer.setOffset(80f, 32f);
        assertEquals(80f, layer.getOffsetX());
        assertEquals(32f, layer.getOffsetY());

        stack.draw(null, 100f, 500f);

        assertEquals(180f, lastPosition(0)[0]);
        assertEquals(468f, lastPosition(0)[1]);
    }

    @Test
    void cellOffsetUsesThatLayersCellSize() {
        Layer small = stack.add(new TerminalGrid(4, 3), new FakeSource(8, 12));
        Layer big = stack.add(new TerminalGrid(4, 3), new FakeSource(16, 24));

        small.setCellOffset(10, 4);
        big.setCellOffset(10, 4);

        assertEquals(80f, small.getOffsetX());
        assertEquals(48f, small.getOffsetY());
        assertEquals(160f, big.getOffsetX());
        assertEquals(96f, big.getOffsetY());
    }

    @Test
    void snappingIsOnByDefaultAndRoundsHalfUp() {
        assertTrue(stack.isPixelSnapping());
        Layer layer = addLayer();

        layer.setOffset(0f, 0f);
        stack.draw(null, 10.5f, 20.5f);
        assertEquals(11f, lastPosition(0)[0]);
        assertEquals(21f, lastPosition(0)[1]);

        stack.draw(null, 10.4f, 20.6f);
        assertEquals(10f, lastPosition(0)[0]);
        assertEquals(21f, lastPosition(0)[1]);

        stack.draw(null, -10.5f, -20.6f);
        assertEquals(-10f, lastPosition(0)[0]);
        assertEquals(-21f, lastPosition(0)[1]);

        stack.draw(null, -10.4f, -0.5f);
        assertEquals(-10f, lastPosition(0)[0]);
        assertEquals(0f, lastPosition(0)[1]);
    }

    @Test
    void snappingRoundsTheFinalPositionNotTheParts() {
        Layer layer = addLayer();
        layer.setOffset(0.4f, 0.4f);
        stack.draw(null, 0.4f, 10.4f);
        assertEquals(1f, lastPosition(0)[0]);
        assertEquals(10f, lastPosition(0)[1]);
    }

    @Test
    void snappingOffPassesRawFloats() {
        stack.setPixelSnapping(false);
        assertFalse(stack.isPixelSnapping());
        Layer layer = addLayer();
        layer.setOffset(0.25f, 0.5f);

        stack.draw(null, 10.5f, 20.5f);

        assertEquals(10.75f, lastPosition(0)[0]);
        assertEquals(20f, lastPosition(0)[1]);
    }

    @Test
    void layersOfDifferentGridSizesCoexist() {
        stack.add(new TerminalGrid(80, 25), font);
        stack.add(new TerminalGrid(10, 4), new FakeSource(16, 24));
        assertOrder("L0", "L1");
        assertEquals(80, stack.get(0).getGrid().getWidth());
        assertEquals(10, stack.get(1).getGrid().getWidth());
    }

    @Test
    void disposeDisposesEachRendererOnce() {
        addLayer();
        addLayer();
        Layer removed = addLayer();
        stack.remove(removed);

        stack.dispose();
        stack.dispose();

        assertEquals(1, renderers.get(0).disposeCount);
        assertEquals(1, renderers.get(1).disposeCount);
        assertEquals(1, renderers.get(2).disposeCount);
        assertEquals(0, stack.size());
    }

    @Test
    void removedLayerCannotBeUsed() {
        Layer layer = addLayer();
        stack.remove(layer);

        assertThrows(IllegalStateException.class, () -> layer.setOffset(1f, 1f));
        assertThrows(IllegalStateException.class, () -> layer.setCellOffset(1, 1));
        assertThrows(IllegalStateException.class, () -> layer.setVisible(false));
        assertThrows(IllegalStateException.class, () -> layer.moveTo(0));
        assertThrows(IllegalStateException.class, layer::remove);
        assertThrows(IllegalStateException.class, () -> stack.remove(layer));
        assertThrows(IllegalStateException.class, () -> stack.moveTo(layer, 0));
        assertEquals(1, renderers.get(0).disposeCount);
    }

    private float[] lastTint(int rendererIndex) {
        List<float[]> tints = renderers.get(rendererIndex).tints;
        return tints.get(tints.size() - 1);
    }

    @Test
    void tintDefaultsToWhite() {
        Layer layer = addLayer();
        assertEquals(1f, layer.getTintR());
        assertEquals(1f, layer.getTintG());
        assertEquals(1f, layer.getTintB());
        assertEquals(1f, layer.getTintA());
        stack.draw(null, 0f, 0f);
        assertArrayEquals(new float[] {1f, 1f, 1f, 1f}, lastTint(0));
    }

    @Test
    void tintReachesItsOwnRendererOnly() {
        Layer first = addLayer();
        addLayer();
        first.setTint(0.25f, 0.5f, 0.75f, 0.5f);
        stack.draw(null, 0f, 0f);
        assertArrayEquals(new float[] {0.25f, 0.5f, 0.75f, 0.5f}, lastTint(0));
        assertArrayEquals(new float[] {1f, 1f, 1f, 1f}, lastTint(1));
    }

    @Test
    void setAlphaKeepsRgb() {
        Layer layer = addLayer();
        layer.setTint(0.25f, 0.5f, 0.75f, 1f);
        layer.setAlpha(0.4f);
        assertEquals(0.25f, layer.getTintR());
        assertEquals(0.5f, layer.getTintG());
        assertEquals(0.75f, layer.getTintB());
        assertEquals(0.4f, layer.getTintA());
    }

    @Test
    void setTintFromColorCopiesValues() {
        Layer layer = addLayer();
        Color color = new Color(0.1f, 0.2f, 0.3f, 0.4f);
        layer.setTint(color);
        color.set(1f, 1f, 1f, 1f);
        Color out = new Color();
        layer.getTint(out);
        assertEquals(0.1f, out.r);
        assertEquals(0.2f, out.g);
        assertEquals(0.3f, out.b);
        assertEquals(0.4f, out.a);
    }

    @Test
    void tintCannotChangeAfterRemoval() {
        Layer layer = addLayer();
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setTint(0f, 0f, 0f, 0f));
        assertThrows(IllegalStateException.class, () -> layer.setTint(Color.RED));
        assertThrows(IllegalStateException.class, () -> layer.setAlpha(0.5f));
        assertEquals(1f, layer.getTintA());
    }

    @Test
    void layersAfterStackDisposeCannotBeUsed() {
        Layer layer = addLayer();
        stack.dispose();
        assertThrows(IllegalStateException.class, () -> layer.setVisible(false));
        assertThrows(IllegalStateException.class, this::addLayer);
    }

    @Test
    void layerFromAnotherStackIsRejected() {
        LayerStack other = new LayerStack((grid, source) -> new FakeRenderer("other", drawLog));
        Layer foreign = other.add(new TerminalGrid(2, 2), font);
        addLayer();

        assertThrows(IllegalArgumentException.class, () -> stack.remove(foreign));
        assertThrows(IllegalArgumentException.class, () -> stack.moveTo(foreign, 0));
        assertEquals(1, stack.size());
        assertEquals(1, other.size());
    }

    @Test
    void setGlyphSourceReachesRendererAndGetterReturnsIt() {
        Layer layer = addLayer();
        assertSame(font, layer.getGlyphSource());
        FakeSource bigger = new FakeSource(16, 24);

        layer.setGlyphSource(bigger);

        assertSame(bigger, layer.getGlyphSource());
        assertEquals(List.of(bigger), renderers.get(0).glyphSources);
        layer.setCellOffset(1, 1);
        assertEquals(16f, layer.getOffsetX());
        assertEquals(24f, layer.getOffsetY());
    }

    @Test
    void setGlyphSourceWithSameSourceIsNoOp() {
        Layer layer = addLayer();
        layer.setGlyphSource(font);
        assertEquals(0, renderers.get(0).glyphSources.size());
    }

    @Test
    void setGlyphSourceRejectsNull() {
        Layer layer = addLayer();
        assertThrows(IllegalArgumentException.class, () -> layer.setGlyphSource(null));
        assertSame(font, layer.getGlyphSource());
        assertEquals(0, renderers.get(0).glyphSources.size());
    }

    @Test
    void removedLayerCannotChangeGlyphSource() {
        Layer layer = addLayer();
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setGlyphSource(new FakeSource(16, 24)));
        assertSame(font, layer.getGlyphSource());
        assertEquals(0, renderers.get(0).glyphSources.size());
    }

    @Test
    void moveToRejectsOutOfRangeIndex() {
        Layer layer = addLayer();
        addLayer();
        assertThrows(IndexOutOfBoundsException.class, () -> stack.moveTo(layer, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> stack.moveTo(layer, -1));
        assertOrder("L0", "L1");
    }
}
