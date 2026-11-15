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
import org.junit.jupiter.api.Test;

class TransientCellHandlesTest {

    private static final float DELTA = 0.0001f;

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

    private static final class QuietRenderer implements LayerRenderer {
        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
        }

        @Override
        public void setCellHandles(Array<CellHandle> cellHandles) {
        }

        @Override
        public void setCellGlyphHidden(int column, int row, boolean hidden) {
        }

        @Override
        public void refreshCell(int column, int row) {
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }

    private final LayerStack stack = new LayerStack((grid, source, shared) -> new QuietRenderer());
    private final Layer layer = stack.add(new TerminalGrid(6, 4), new FakeSource());

    @Test
    void defaultsToLastingHandles() {
        assertFalse(layer.isTransientCellHandles());
        assertEquals(1f, layer.getTransientReleaseSeconds(), DELTA);
    }

    @Test
    void nonTransientLayerKeepsItsHandlesUntouched() {
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(3f, 0f);

        stack.beginCellEffects(5f);

        assertEquals(3f, handle.getOffsetX(), DELTA);
        assertSame(handle, layer.cell(1, 1));
    }

    @Test
    void transientLayerResetsHandlesButKeepsThem() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(3f, 0f);

        stack.beginCellEffects(0.1f);

        assertEquals(0f, handle.getOffsetX(), DELTA);
        assertSame(handle, layer.cell(1, 1));
    }

    @Test
    void idleHandleIsReleasedOnlyOnceReleaseTimeIsReached() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(2, 2);

        stack.beginCellEffects(0.4f);
        stack.beginCellEffects(0.4f);
        assertSame(handle, layer.cell(2, 2));

        stack.beginCellEffects(0.4f);
        assertEquals(0, layer.getCellHandleCount());
        assertNotSame(handle, layer.cell(2, 2));
    }

    @Test
    void handleLiftedEveryFrameIsNeverReleased() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(2, 2);

        for (int frame = 0; frame < 300; frame++) {
            handle.setOffset(1f, 0f);
            stack.beginCellEffects(0.1f);
            assertSame(handle, layer.cell(2, 2));
        }
    }

    @Test
    void nonIdleFrameResetsUnusedTime() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(0, 0);
        stack.beginCellEffects(0.4f);
        assertEquals(0.4f, handle.getUnusedSeconds(), DELTA);

        handle.setOffset(1f, 0f);
        stack.beginCellEffects(0.4f);

        assertEquals(0f, handle.getUnusedSeconds(), DELTA);
    }

    @Test
    void unusableDeltasStillResetButAddNoTime() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(0, 0);
        float[] deltas = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY};

        for (float delta : deltas) {
            handle.setOffset(2f, 0f);
            stack.beginCellEffects(delta);

            assertEquals(0f, handle.getOffsetX(), DELTA);
            assertEquals(0f, handle.getUnusedSeconds(), DELTA);
            assertSame(handle, layer.cell(0, 0));
        }
    }

    @Test
    void releaseSecondsMustBeFiniteAndPositive() {
        assertThrows(IllegalArgumentException.class, () -> layer.setTransientReleaseSeconds(0f));
        assertThrows(IllegalArgumentException.class, () -> layer.setTransientReleaseSeconds(-1f));
        assertThrows(IllegalArgumentException.class, () -> layer.setTransientReleaseSeconds(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> layer.setTransientReleaseSeconds(Float.POSITIVE_INFINITY));
    }

    @Test
    void customReleaseSecondsIsHonored() {
        layer.setTransientCellHandles(true);
        layer.setTransientReleaseSeconds(0.25f);
        CellHandle handle = layer.cell(0, 0);

        stack.beginCellEffects(0.2f);
        assertSame(handle, layer.cell(0, 0));

        stack.beginCellEffects(0.1f);
        assertNotSame(handle, layer.cell(0, 0));
        assertEquals(0.25f, layer.getTransientReleaseSeconds(), DELTA);
    }

    @Test
    void togglingTheModeClearsUnusedTime() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(0, 0);
        stack.beginCellEffects(0.4f);
        stack.beginCellEffects(0.4f);
        assertEquals(0.8f, handle.getUnusedSeconds(), DELTA);

        layer.setTransientCellHandles(false);
        layer.setTransientCellHandles(true);
        stack.beginCellEffects(0.4f);

        assertSame(handle, layer.cell(0, 0));
    }

    @Test
    void handleOutsideAShrunkGridIsReleasedWithoutError() {
        LayerStack bigStack = HeadlessLayers.createStack();
        TerminalGrid bigGrid = new TerminalGrid(12, 12);
        Layer bigLayer = HeadlessLayers.add(bigStack, bigGrid);
        bigLayer.setTransientCellHandles(true);
        bigLayer.cell(9, 9);

        bigGrid.resize(6, 4);
        bigStack.beginCellEffects(1.5f);

        assertEquals(0, bigLayer.getCellHandleCount());
    }

    @Test
    void mapWindowHandleIsReleasedAfterScrollAndIndexStaysConsistent() {
        layer.setTransientCellHandles(true);
        layer.enableMapWindow();
        CellHandle handle = layer.cell(2, 1);
        handle.setOffset(1f, 0f);

        stack.setCamera(8f, 0f);
        assertEquals(1, handle.getColumn());
        stack.beginCellEffects(0.6f);
        stack.beginCellEffects(0.6f);
        stack.beginCellEffects(0.6f);

        assertEquals(0, layer.getCellHandleCount());
        CellHandle fresh = layer.cell(1, 1);
        assertNotSame(handle, fresh);
        assertEquals(1, layer.getCellHandleCount());
    }

    @Test
    void panelMemberAndGroupMemberAreSwept() {
        Panel panel = stack.addPanel(0f, 0f, 100f, 100f);
        layer.setPanel(panel);
        layer.setTransientCellHandles(true);
        Layer grouped = stack.add(new TerminalGrid(6, 4), new FakeSource());
        stack.addGroup().add(grouped);
        grouped.setTransientCellHandles(true);
        CellHandle panelHandle = layer.cell(0, 0);
        CellHandle groupedHandle = grouped.cell(0, 0);
        panelHandle.setOffset(2f, 0f);
        groupedHandle.setOffset(2f, 0f);

        stack.beginCellEffects(0.1f);

        assertEquals(0f, panelHandle.getOffsetX(), DELTA);
        assertEquals(0f, groupedHandle.getOffsetX(), DELTA);
    }

    @Test
    void releasedHandlesAreDetached() {
        layer.setTransientCellHandles(true);
        CellHandle handle = layer.cell(3, 1);

        stack.beginCellEffects(1f);

        assertTrue(layer.getCellHandleCount() == 0);
        assertThrows(IllegalStateException.class, () -> handle.setOffset(1f, 1f));
    }
}
