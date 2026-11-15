package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What the renderer sees when cells have frame handles: the layer's combined draw handles. */
class FrameCellDrawHandlesTest {

    private static final float DELTA = 0.0001f;
    private static final int RED = 0xFF0000FF;

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 12;
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

    private static final class RecordingRenderer implements LayerRenderer {
        final List<String> events = new ArrayList<>();
        Array<CellHandle> cellHandles;
        int bindCount;

        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
        }

        @Override
        public void setCellHandles(Array<CellHandle> cellHandles) {
            this.cellHandles = cellHandles;
            bindCount++;
        }

        @Override
        public void setCellGlyphHidden(int column, int row, boolean hidden) {
            events.add((hidden ? "hide " : "show ") + column + "," + row);
        }

        @Override
        public void refreshCell(int column, int row) {
            events.add("refresh " + column + "," + row);
            refreshedWithOverridesStillApplied = boundHandleAppliesOverrides(column, row);
        }

        boolean refreshedWithOverridesStillApplied;

        /** Mirrors GridRenderer, which still indexes a released entry until the next rebind. */
        private boolean boundHandleAppliesOverrides(int column, int row) {
            for (int i = 0; i < cellHandles.size; i++) {
                CellHandle handle = cellHandles.get(i);
                if (handle.getColumn() == column && handle.getRow() == row) {
                    return !handle.isDetached() && handle.hasOverrides();
                }
            }
            return false;
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }

    private final RecordingRenderer renderer = new RecordingRenderer();
    private final LayerStack stack = new LayerStack((grid, source, shared) -> renderer);
    private final TerminalGrid grid = new TerminalGrid(6, 4);
    private final Layer layer = stack.add(grid, new FakeSource());

    private void draw() {
        stack.draw(null, 0f, 0f);
    }

    private CellHandle drawnHandle(int index) {
        return layer.getCellHandles().get(index);
    }

    @Test
    void aDrawHandleExistsWhenOnlyAFrameHandleDoes() {
        CellHandle frame = layer.frameCell(1, 1);

        assertEquals(1, layer.getCellHandles().size);
        assertSame(layer.getCellHandles(), renderer.cellHandles);
        assertNotSame(frame, drawnHandle(0));
        assertFalse(drawnHandle(0).isFrameHandle());
        assertEquals(1, drawnHandle(0).getColumn());
        assertEquals(0, layer.getCellHandleCount());
        assertEquals(1, layer.getFrameCellHandleCount());
    }

    @Test
    void aCellWithOnlyALastingHandleIsDrawnThroughThatHandle() {
        CellHandle lasting = layer.cell(2, 2);
        layer.frameCell(0, 0);

        assertEquals(2, layer.getCellHandles().size);
        assertSame(lasting, drawnHandle(0));
    }

    @Test
    void aCellWithBothHandlesHasOneDrawHandleThatKeepsItsPlaceInTheOrder() {
        CellHandle first = layer.cell(2, 2);
        CellHandle second = layer.cell(3, 3);

        layer.frameCell(2, 2);

        assertEquals(2, layer.getCellHandles().size);
        assertNotSame(first, drawnHandle(0));
        assertEquals(2, drawnHandle(0).getColumn());
        assertSame(second, drawnHandle(1));
    }

    @Test
    void frameOnlyMotionHidesTheGlyphOnceAndShowsItAfterTheSweepResetsTheHandle() {
        CellHandle frame = layer.frameCell(1, 1);
        frame.setOffset(2f, 0f);

        draw();
        draw();
        assertEquals(List.of("hide 1,1"), renderer.events);

        stack.beginCellEffects(0.1f);
        draw();
        assertEquals(List.of("hide 1,1", "show 1,1"), renderer.events);
    }

    @Test
    void frameOnlyOverridesRefreshTheCellOnceAndReachTheRenderersHandle() {
        CellHandle frame = layer.frameCell(2, 1);
        frame.setForegroundMix(RED, 0.5f);
        frame.setCodePoint('f');

        draw();
        draw();

        assertEquals(List.of("refresh 2,1"), renderer.events);
        assertEquals(RED, drawnHandle(0).getForegroundMixColor());
        assertEquals(0.5f, drawnHandle(0).getForegroundMixAmount(), DELTA);
        assertEquals('f', drawnHandle(0).getCodePoint());
    }

    @Test
    void releasingAFrameOnlyCellRewritesItWithoutTheOldOverrides() {
        CellHandle frame = layer.frameCell(2, 1);
        frame.setForegroundMix(RED, 0.5f);
        draw();
        layer.setFrameReleaseSeconds(0.5f);

        stack.beginCellEffects(0.1f);
        stack.beginCellEffects(1f);

        assertEquals(0, layer.getFrameCellHandleCount());
        assertEquals(List.of("refresh 2,1", "refresh 2,1"), renderer.events);
        assertFalse(renderer.refreshedWithOverridesStillApplied);
    }

    @Test
    void anIdleFrameHandleNeverTouchesTheRenderer() {
        layer.frameCell(1, 1);

        draw();

        assertTrue(renderer.events.isEmpty());
    }

    @Test
    void combinedValuesReachTheDrawHandleAndLeaveBothHandlesAlone() {
        CellHandle lasting = layer.cell(1, 1);
        CellHandle frame = layer.frameCell(1, 1);
        lasting.setOffset(3f, 0f);
        lasting.setPivot(0.25f, 0.75f);
        frame.setOffset(1.5f, 0f);

        draw();

        assertEquals(4.5f, drawnHandle(0).getOffsetX(), DELTA);
        assertEquals(0.25f, drawnHandle(0).getPivotX(), DELTA);
        assertEquals(3f, lasting.getOffsetX(), DELTA);
        assertEquals(1.5f, frame.getOffsetX(), DELTA);
        assertEquals(List.of("hide 1,1"), renderer.events);
    }

    @Test
    void addingAFrameHandleToALiftedCellCausesNoRendererEventsUntilItChangesSomething() {
        CellHandle lasting = layer.cell(1, 1);
        lasting.setOffset(2f, 0f);
        lasting.setCodePoint('l');
        draw();
        renderer.events.clear();
        int bindsBefore = renderer.bindCount;

        layer.frameCell(1, 1);
        draw();

        assertTrue(renderer.events.isEmpty());
        assertEquals(bindsBefore + 1, renderer.bindCount);
        assertNotSame(lasting, drawnHandle(0));
        assertEquals('l', drawnHandle(0).getCodePoint());
    }

    @Test
    void aFrameEffectThatCancelsTheLastingMotionSettlesTheGlyph() {
        CellHandle lasting = layer.cell(1, 1);
        CellHandle frame = layer.frameCell(1, 1);
        lasting.setOffset(2f, 0f);
        draw();
        renderer.events.clear();

        frame.setOffset(-2f, 0f);
        draw();

        assertEquals(List.of("show 1,1"), renderer.events);
    }

    @Test
    void releasingTheFrameHandleHandsTheCellBackToTheLastingHandle() {
        CellHandle lasting = layer.cell(1, 1);
        layer.frameCell(1, 1);
        lasting.setOffset(2f, 0f);
        draw();
        renderer.events.clear();
        int bindsBefore = renderer.bindCount;

        stack.beginCellEffects(1f);
        draw();

        assertSame(lasting, drawnHandle(0));
        assertEquals(1, layer.getCellHandles().size);
        assertEquals(bindsBefore + 1, renderer.bindCount);
        assertTrue(renderer.events.isEmpty());
    }

    @Test
    void releasingAFrameHandleThatHeldOverridesRefreshesTheCellFromTheLastingHandle() {
        layer.cell(1, 1);
        CellHandle frame = layer.frameCell(1, 1);
        frame.setCodePoint('f');
        draw();
        renderer.events.clear();

        stack.beginCellEffects(0.1f);
        stack.beginCellEffects(1f);
        draw();

        assertEquals(List.of("refresh 1,1"), renderer.events);
    }

    @Test
    void theDrawHandleIsReleasedOnlyAfterBothHandlesAreGone() {
        CellHandle lasting = layer.cell(1, 1);
        CellHandle frame = layer.frameCell(1, 1);
        CellHandle drawn = drawnHandle(0);

        layer.releaseIdleCellHandles();

        assertTrue(lasting.isDetached());
        assertEquals(1, layer.getCellHandles().size);
        assertSame(drawn, drawnHandle(0));
        assertFalse(drawn.isDetached());

        stack.beginCellEffects(1f);

        assertTrue(frame.isDetached());
        assertTrue(drawn.isDetached());
        assertEquals(0, layer.getCellHandles().size);
    }

    @Test
    void theDrawHandleIsAlsoReleasedWhenTheLastingHandleOutlivesTheFrameHandle() {
        CellHandle lasting = layer.cell(1, 1);
        layer.frameCell(1, 1);
        stack.beginCellEffects(1f);
        assertSame(lasting, drawnHandle(0));

        layer.releaseIdleCellHandles();

        assertEquals(0, layer.getCellHandles().size);
        assertTrue(lasting.isDetached());
    }

    @Test
    void releasingAFrameOnlyCellShowsTheGlyphAndRefreshesItsOverridesAndRebinds() {
        CellHandle frame = layer.frameCell(1, 1);
        frame.setOffset(2f, 0f);
        frame.setCodePoint('x');
        draw();
        renderer.events.clear();
        int bindsBefore = renderer.bindCount;

        stack.beginCellEffects(0.1f);
        stack.beginCellEffects(1f);

        assertEquals(List.of("show 1,1", "refresh 1,1"), renderer.events);
        assertEquals(bindsBefore + 1, renderer.bindCount);
        assertEquals(0, layer.getCellHandles().size);
    }

    @Test
    void releasingTheLastingHandleKeepsTheFrameEffectDrawn() {
        CellHandle lasting = layer.cell(1, 1);
        CellHandle frame = layer.frameCell(1, 1);
        lasting.setOffset(2f, 0f);
        frame.setOffset(1f, 0f);
        draw();

        layer.releaseIdleCellHandles();
        assertFalse(lasting.isDetached());
        lasting.reset();
        layer.releaseIdleCellHandles();
        draw();

        assertTrue(lasting.isDetached());
        assertEquals(1, layer.getCellHandles().size);
        assertEquals(1f, drawnHandle(0).getOffsetX(), DELTA);
    }

    @Test
    void scrollMovesTheDrawHandlesWithTheirHandlesAndRebindsOnce() {
        CellHandle lasting = layer.cell(1, 1);
        layer.frameCell(1, 1);
        CellHandle frameOnly = layer.frameCell(2, 2);
        layer.frameCell(5, 0);
        lasting.setOffset(2f, 0f);
        frameOnly.setOffset(1f, 0f);
        draw();
        renderer.events.clear();
        int bindsBefore = renderer.bindCount;

        layer.scroll(1, 0);

        assertEquals(bindsBefore + 1, renderer.bindCount);
        assertEquals(List.of("show 1,1", "show 2,2"), renderer.events);
        assertEquals(2, layer.getCellHandles().size);
        assertEquals(2, drawnHandle(0).getColumn());
        assertEquals(1, drawnHandle(0).getRow());
        assertEquals(3, drawnHandle(1).getColumn());
        assertEquals(2, drawnHandle(1).getRow());
        assertEquals(2, layer.getFrameCellHandleCount());

        renderer.events.clear();
        draw();
        assertEquals(List.of("hide 2,1", "hide 3,2"), renderer.events);
    }

    @Test
    void shrinkingTheGridDropsDrawHandlesOutsideAndKeepsTheRest() {
        layer.cell(5, 1);
        layer.frameCell(5, 1);
        layer.frameCell(1, 1);
        CellHandle outsideDrawn = drawnHandle(0);
        draw();

        grid.resize(3, 4);
        draw();

        assertTrue(outsideDrawn.isDetached());
        assertEquals(1, layer.getCellHandles().size);
        assertEquals(1, drawnHandle(0).getColumn());
        assertEquals(0, layer.getCellHandleCount());
        assertEquals(1, layer.getFrameCellHandleCount());
    }

    @Test
    void removingTheLayerDetachesDrawHandles() {
        layer.frameCell(1, 1);
        CellHandle drawn = drawnHandle(0);

        layer.remove();

        assertTrue(drawn.isDetached());
        assertEquals(0, layer.getCellHandles().size);
    }
}
