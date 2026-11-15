package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

class LayerCellTest {

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
        final List<float[]> positions = new ArrayList<>();
        Array<CellHandle> cellHandles;
        int bindCount;

        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            positions.add(new float[] {layerTransform.m02, layerTransform.m12});
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
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }

    private final RecordingRenderer renderer = new RecordingRenderer();
    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> renderer);
    private final TerminalGrid grid = new TerminalGrid(6, 4);
    private final Layer layer = stack.add(grid, new FakeSource());

    private void draw() {
        stack.draw(null, 0f, 0f);
    }

    @Test
    void sameHandleIsReturnedForTheSameCell() {
        CellHandle first = layer.cell(2, 1);

        assertSame(first, layer.cell(2, 1));
        assertNotSame(first, layer.cell(1, 2));
        assertEquals(2, first.getColumn());
        assertEquals(1, first.getRow());
        assertEquals(1, renderer.bindCount);
    }

    @Test
    void cellsOutsideTheGridThrow() {
        assertThrows(IndexOutOfBoundsException.class, () -> layer.cell(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.cell(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.cell(6, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.cell(0, 4));
    }

    @Test
    void removedLayerThrowsAndDetachesItsHandles() {
        CellHandle handle = layer.cell(0, 0);
        handle.setOffset(1f, 1f);

        layer.remove();

        assertTrue(handle.isDetached());
        assertEquals(1f, handle.getOffsetX());
        assertThrows(IllegalStateException.class, () -> handle.setOffset(0f, 0f));
        assertThrows(IllegalStateException.class, () -> layer.cell(0, 0));
        assertThrows(IllegalStateException.class, layer::resetCellHandles);
    }

    @Test
    void disposingTheStackDetachesHandles() {
        CellHandle handle = layer.cell(0, 0);

        stack.dispose();

        assertTrue(handle.isDetached());
    }

    @Test
    void resetCellHandlesResetsEveryHandle() {
        CellHandle a = layer.cell(0, 0);
        CellHandle b = layer.cell(1, 0);
        a.setOffset(3f, 0f);
        b.setRotation(45f);
        b.setMix(0xFF0000FF, 0.5f);
        b.setCodePoint('x');

        layer.resetCellHandles();

        assertFalse(a.isLifted());
        assertFalse(b.isLifted());
        assertFalse(b.hasOverrides());
    }

    @Test
    void rendererHearsAboutLiftingAndSettlingOnlyWhenTheStateChanges() {
        CellHandle handle = layer.cell(1, 1);
        draw();
        assertTrue(renderer.events.isEmpty());

        handle.setOffset(2f, 0f);
        draw();
        draw();
        assertEquals(List.of("hide 1,1"), renderer.events);

        handle.setOffset(4f, 0f);
        draw();
        assertEquals(List.of("hide 1,1"), renderer.events);

        layer.resetCellHandles();
        draw();
        draw();
        assertEquals(List.of("hide 1,1", "show 1,1"), renderer.events);
    }

    @Test
    void resizingTheGridDetachesOnlyHandlesOutsideAndReappliesTheRest() {
        CellHandle inside = layer.cell(1, 1);
        CellHandle outside = layer.cell(5, 1);
        inside.setAlphaMultiplier(0.5f);
        outside.setAlphaMultiplier(0.5f);
        draw();
        renderer.events.clear();

        grid.resize(3, 4);
        draw();

        assertFalse(inside.isDetached());
        assertTrue(outside.isDetached());
        assertEquals(1, layer.getCellHandles().size);
        assertSame(inside, layer.getCellHandles().get(0));
        assertEquals(List.of("hide 1,1"), renderer.events);
        assertThrows(IndexOutOfBoundsException.class, () -> layer.cell(5, 1));
    }

    @Test
    void growingBackNeedsAFreshHandle() {
        CellHandle outside = layer.cell(5, 1);
        outside.setOffset(1f, 0f);
        grid.resize(3, 4);
        draw();
        grid.resize(6, 4);
        draw();

        CellHandle fresh = layer.cell(5, 1);

        assertNotSame(outside, fresh);
        assertFalse(fresh.isDetached());
        assertFalse(fresh.isLifted());
        assertTrue(outside.isDetached());
    }

    @Test
    void resizeWhileNoHandlesExistStillReappliesHiddenFlagsAfterLaterResizes() {
        draw();
        grid.resize(8, 4);
        draw();
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(2f, 0f);
        draw();
        renderer.events.clear();

        grid.resize(6, 4);
        draw();

        assertEquals(List.of("hide 1,1"), renderer.events);
        assertFalse(handle.isDetached());
    }

    @Test
    void handleOutsideTheGridAfterResizingBackIsDetachedEvenIfNoHandlesExistedAtFirstResize() {
        draw();
        grid.resize(8, 4);
        draw();
        CellHandle handle = layer.cell(7, 1);
        grid.resize(6, 4);
        draw();

        assertTrue(handle.isDetached());
        assertEquals(0, layer.getCellHandles().size);
        assertThrows(IndexOutOfBoundsException.class, () -> layer.cell(7, 1));
    }

    @Test
    void liftedCellsKeepCreationOrderInTheListTheRendererDraws() {
        CellHandle late = layer.cell(4, 3);
        CellHandle early = layer.cell(0, 0);
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);

        assertSame(renderer.cellHandles, layer.getCellHandles());
        assertSame(late, renderer.cellHandles.get(0));
        assertSame(early, renderer.cellHandles.get(1));
    }

    @Test
    void movingTheLayerLeavesLayerRelativeDataUntouched() {
        CellHandle handle = layer.cell(2, 1);
        handle.setOffset(3f, -2f);
        layer.setOffset(10f, 4f);

        draw();

        float[] position = renderer.positions.get(0);
        assertEquals(10f, position[0]);
        assertEquals(-4f, position[1]);
        assertEquals(3f, renderer.cellHandles.get(0).getOffsetX());
        assertEquals(-2f, renderer.cellHandles.get(0).getOffsetY());
    }

    @Test
    void removeFreeSpritesDetachesEverySpriteAndEmptiesTheLayer() {
        GlyphSprite first = layer.addSprite('a', 0x11223344, 5f, 6f);
        GlyphSprite second = layer.addSprite('b', 0, 0f, 0f);

        layer.removeFreeSprites();

        assertEquals(0, layer.getSpriteCount());
        assertEquals('a', first.getCodePoint());
        assertEquals(5f, first.getX());
        assertThrows(IllegalStateException.class, () -> first.setVisible(false));
        assertThrows(IllegalStateException.class, () -> second.setPosition(1f, 1f));
    }

    @Test
    void removeFreeSpritesLeavesLiftedCellsLifted() {
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(2f, 0f);
        layer.addSprite('a', 0, 0f, 0f);

        layer.removeFreeSprites();

        assertTrue(handle.isLifted());
        assertFalse(handle.isDetached());
        assertEquals(2f, handle.getOffsetX());
        assertEquals(1, layer.getCellHandles().size);
    }

    @Test
    void removeFreeSpritesOnAnEmptyLayerIsFine() {
        layer.removeFreeSprites();

        assertEquals(0, layer.getSpriteCount());
    }

    @Test
    void spritesCanBeAddedAgainAfterRemoveFreeSprites() {
        GlyphSprite old = layer.addSprite('a', 0, 0f, 0f);
        layer.removeFreeSprites();

        GlyphSprite fresh = layer.addSprite('b', 0, 1f, 1f);

        assertEquals(1, layer.getSpriteCount());
        assertSame(fresh, layer.getSprite(0));
        assertNotSame(old, fresh);
        fresh.setPosition(2f, 2f);
        assertEquals(2f, fresh.getX());
    }

    @Test
    void clearAllSpritesResetsHandlesAndRemovesFreeSprites() {
        CellHandle handle = layer.cell(0, 0);
        handle.setOffset(3f, 0f);
        GlyphSprite sprite = layer.addSprite('a', 0, 0f, 0f);

        layer.clearAllSprites();

        assertFalse(handle.isLifted());
        assertFalse(handle.isDetached());
        assertEquals(0, layer.getSpriteCount());
        assertThrows(IllegalStateException.class, () -> sprite.setVisible(false));
        assertSame(handle, layer.cell(0, 0));
    }

    @Test
    void changedOverrideRefreshesItsCellOnceAndNothingWhenUnchanged() {
        CellHandle handle = layer.cell(2, 1);
        handle.setForegroundMix(0xFF0000FF, 0.5f);

        draw();
        draw();
        handle.setForegroundMix(0xFF0000FF, 0.5f);
        draw();

        assertEquals(List.of("refresh 2,1"), renderer.events);
    }

    @Test
    void eachChangeOfAnOverrideRefreshesTheCellAgain() {
        CellHandle handle = layer.cell(0, 3);
        handle.setCodePoint('x');
        draw();
        handle.clearCodePoint();
        draw();

        assertEquals(List.of("refresh 0,3", "refresh 0,3"), renderer.events);
    }

    @Test
    void handleWithoutOverrideChangesNeverRefreshes() {
        layer.cell(1, 1);
        draw();

        assertTrue(renderer.events.isEmpty());
    }

    @Test
    void handleDetachedByShrinkingDoesNotRefreshACellThatNoLongerExists() {
        CellHandle outside = layer.cell(5, 3);
        outside.setCodePoint('x');
        grid.resize(3, 2);

        draw();

        assertTrue(outside.isDetached());
        assertTrue(renderer.events.isEmpty());
    }

    @Test
    void releaseDropsIdleHandlesAndDetachesThem() {
        CellHandle idle = layer.cell(0, 0);
        layer.cell(1, 0);
        assertEquals(2, layer.getCellHandleCount());

        layer.releaseIdleCellHandles();

        assertEquals(0, layer.getCellHandleCount());
        assertTrue(idle.isDetached());
        assertThrows(IllegalStateException.class, () -> idle.setOffset(1f, 0f));
    }

    @Test
    void releaseKeepsLiftedMixedAndCodePointHandles() {
        CellHandle lifted = layer.cell(0, 0);
        CellHandle foregroundMixed = layer.cell(1, 0);
        CellHandle backgroundMixed = layer.cell(2, 0);
        CellHandle withCodePoint = layer.cell(3, 0);
        CellHandle idle = layer.cell(4, 0);
        lifted.setOffset(2f, 0f);
        foregroundMixed.setForegroundMix(0xFF0000FF, 0.5f);
        backgroundMixed.setBackgroundMix(0xFF0000FF, 0.5f);
        withCodePoint.setCodePoint('x');

        layer.releaseIdleCellHandles();

        assertEquals(4, layer.getCellHandleCount());
        assertSame(lifted, layer.cell(0, 0));
        assertSame(foregroundMixed, layer.cell(1, 0));
        assertSame(backgroundMixed, layer.cell(2, 0));
        assertSame(withCodePoint, layer.cell(3, 0));
        assertTrue(idle.isDetached());
    }

    @Test
    void cellAfterReleaseReturnsAFreshHandle() {
        CellHandle old = layer.cell(2, 2);
        layer.releaseIdleCellHandles();

        CellHandle fresh = layer.cell(2, 2);

        assertNotSame(old, fresh);
        assertFalse(fresh.isDetached());
        assertEquals(1, layer.getCellHandleCount());
    }

    @Test
    void releaseRebindsTheRendererSoItsIndexIsRebuiltEvenWhenTheCountIsUnchanged() {
        layer.cell(1, 1).setOffset(1f, 0f);
        layer.cell(2, 2);
        int bindsBefore = renderer.bindCount;

        layer.releaseIdleCellHandles();
        layer.cell(3, 3);

        assertEquals(bindsBefore + 1, renderer.bindCount);
        assertEquals(2, renderer.cellHandles.size);
    }

    @Test
    void releaseWithNothingIdleDoesNotRebind() {
        layer.cell(2, 2).setOffset(1f, 0f);
        int bindsBefore = renderer.bindCount;

        layer.releaseIdleCellHandles();

        assertEquals(bindsBefore, renderer.bindCount);
    }

    @Test
    void releaseUnhidesAndRefreshesCellsOfHandlesResetButNotYetSynced() {
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(2f, 0f);
        handle.setCodePoint('x');
        draw();
        renderer.events.clear();
        layer.resetCellHandles();

        layer.releaseIdleCellHandles();

        assertEquals(List.of("show 1,1", "refresh 1,1"), renderer.events);
        assertTrue(handle.isDetached());
    }

    @Test
    void releaseSkipsTheRendererForACellThatLeftTheGridSinceTheLastDraw() {
        CellHandle outside = layer.cell(5, 3);
        outside.setOffset(2f, 0f);
        draw();
        layer.resetCellHandles();
        grid.resize(3, 2);
        renderer.events.clear();

        layer.releaseIdleCellHandles();

        assertEquals(0, layer.getCellHandleCount());
        assertTrue(outside.isDetached());
        assertTrue(renderer.events.isEmpty());
    }

    @Test
    void releaseOnARemovedLayerThrows() {
        layer.remove();

        assertThrows(IllegalStateException.class, layer::releaseIdleCellHandles);
    }

    @Test
    void removeFreeSpritesAndClearAllSpritesThrowOnARemovedLayer() {
        layer.remove();

        assertThrows(IllegalStateException.class, layer::removeFreeSprites);
        assertThrows(IllegalStateException.class, layer::clearAllSprites);
    }
}
