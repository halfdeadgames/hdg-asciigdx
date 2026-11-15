package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class FrameCellHandlesTest {

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

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final Layer layer = stack.add(new TerminalGrid(6, 4), new FakeSource());

    @Test
    void repeatRequestsReturnTheSameHandle() {
        CellHandle handle = layer.frameCell(2, 1);

        assertSame(handle, layer.frameCell(2, 1));
        assertTrue(handle.isFrameHandle());
        assertEquals(1, layer.getFrameCellHandleCount());
    }

    @Test
    void frameHandleIsDistinctFromLastingHandle() {
        CellHandle lasting = layer.cell(2, 1);
        CellHandle frame = layer.frameCell(2, 1);

        assertNotSame(lasting, frame);
        assertFalse(lasting.isFrameHandle());
        assertEquals(1, layer.getCellHandleCount());
        assertEquals(1, layer.getFrameCellHandleCount());
    }

    @Test
    void cellOutsideTheGridThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> layer.frameCell(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.frameCell(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.frameCell(6, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.frameCell(0, 4));
    }

    @Test
    void removedLayerRefusesFrameHandles() {
        layer.remove();

        assertThrows(IllegalStateException.class, () -> layer.frameCell(0, 0));
    }

    @Test
    void sweepResetsFrameHandleButLeavesLastingHandleUntouched() {
        CellHandle lasting = layer.cell(1, 1);
        CellHandle frame = layer.frameCell(1, 1);
        lasting.setOffset(3f, 0f);
        frame.setOffset(5f, 0f);

        stack.beginCellEffects(0.1f);

        assertEquals(0f, frame.getOffsetX(), DELTA);
        assertEquals(3f, lasting.getOffsetX(), DELTA);
        assertSame(frame, layer.frameCell(1, 1));
        assertSame(lasting, layer.cell(1, 1));
    }

    @Test
    void defaultReleaseSecondsIsOne() {
        assertEquals(1f, layer.getFrameReleaseSeconds(), DELTA);
    }

    @Test
    void releaseSecondsMustBeFiniteAndPositive() {
        assertThrows(IllegalArgumentException.class, () -> layer.setFrameReleaseSeconds(0f));
        assertThrows(IllegalArgumentException.class, () -> layer.setFrameReleaseSeconds(-1f));
        assertThrows(IllegalArgumentException.class, () -> layer.setFrameReleaseSeconds(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> layer.setFrameReleaseSeconds(Float.POSITIVE_INFINITY));
    }

    @Test
    void mapWindowFrameHandleIsReleasedAfterScrollAndIndexStaysConsistent() {
        layer.enableMapWindow();
        CellHandle handle = layer.frameCell(2, 1);
        handle.setOffset(1f, 0f);

        stack.setCamera(8f, 0f);
        assertEquals(1, handle.getColumn());
        stack.beginCellEffects(0.6f);
        stack.beginCellEffects(0.6f);
        stack.beginCellEffects(0.6f);

        assertEquals(0, layer.getFrameCellHandleCount());
        CellHandle fresh = layer.frameCell(1, 1);
        assertNotSame(handle, fresh);
        assertEquals(1, layer.getFrameCellHandleCount());
    }

    @Test
    void sweepReleasesIdleFrameHandlesOnAFreshLayer() {
        CellHandle frame = layer.frameCell(0, 0);

        stack.beginCellEffects(1f);

        assertEquals(0, layer.getFrameCellHandleCount());
        assertTrue(frame.isDetached());
    }

    @Test
    void idleFrameHandleIsReleasedOnlyOnceReleaseTimeIsReached() {
        CellHandle frame = layer.frameCell(2, 2);

        stack.beginCellEffects(0.4f);
        stack.beginCellEffects(0.4f);
        assertSame(frame, layer.frameCell(2, 2));

        stack.beginCellEffects(0.4f);
        assertEquals(0, layer.getFrameCellHandleCount());
        assertNotSame(frame, layer.frameCell(2, 2));
        assertThrows(IllegalStateException.class, () -> frame.setOffset(1f, 1f));
    }

    @Test
    void frameHandleUsedEveryFrameIsNeverReleased() {
        CellHandle frame = layer.frameCell(2, 2);

        for (int tick = 0; tick < 300; tick++) {
            frame.setOffset(1f, 0f);
            stack.beginCellEffects(0.1f);
            assertSame(frame, layer.frameCell(2, 2));
        }
    }

    @Test
    void customReleaseSecondsAppliesToFrameHandles() {
        layer.setFrameReleaseSeconds(0.25f);
        CellHandle frame = layer.frameCell(0, 0);

        stack.beginCellEffects(0.2f);
        assertSame(frame, layer.frameCell(0, 0));

        stack.beginCellEffects(0.1f);
        assertNotSame(frame, layer.frameCell(0, 0));
    }

    @Test
    void unusableDeltasAddNoTime() {
        CellHandle frame = layer.frameCell(0, 0);
        float[] deltas = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY};

        for (float delta : deltas) {
            frame.setOffset(2f, 0f);
            stack.beginCellEffects(delta);

            assertEquals(0f, frame.getOffsetX(), DELTA);
            assertEquals(0f, frame.getUnusedSeconds(), DELTA);
            assertSame(frame, layer.frameCell(0, 0));
        }
    }

    @Test
    void lastingHandlesStayUntouchedByTheFrameSweep() {
        CellHandle lasting = layer.cell(1, 1);
        lasting.setOffset(3f, 0f);

        stack.beginCellEffects(5f);

        assertEquals(3f, lasting.getOffsetX(), DELTA);
        assertSame(lasting, layer.cell(1, 1));
    }

    @Test
    void pivotSettersThrowOnFrameHandlesAndWorkOnLastingOnes() {
        CellHandle frame = layer.frameCell(1, 1);
        CellHandle lasting = layer.cell(1, 1);

        assertThrows(IllegalStateException.class, () -> frame.setPivot(0f, 0f));
        assertThrows(IllegalStateException.class, () -> frame.setPivotKeepingPlacement(0f, 0f));
        assertEquals(0.5f, frame.getPivotX(), DELTA);
        assertEquals(0.5f, frame.getPivotY(), DELTA);

        lasting.setPivot(0.25f, 0.75f);
        assertEquals(0.25f, lasting.getPivotX(), DELTA);
        lasting.setPivotKeepingPlacement(0f, 0f);
        assertEquals(0f, lasting.getPivotY(), DELTA);
        assertEquals(0.5f, frame.getPivotX(), DELTA);
    }

    @Test
    void scrollMovesFrameHandlesAndDetachesPushedOutOnes() {
        CellHandle moving = layer.frameCell(2, 1);
        CellHandle pushedOut = layer.frameCell(5, 0);

        layer.scroll(1, 0);

        assertEquals(3, moving.getColumn());
        assertFalse(moving.isDetached());
        assertSame(moving, layer.frameCell(3, 1));
        assertTrue(pushedOut.isDetached());
        assertEquals(1, layer.getFrameCellHandleCount());
        assertNotSame(moving, layer.frameCell(2, 1));
        assertNotSame(pushedOut, layer.frameCell(5, 0));
    }

    @Test
    void gridShrinkDetachesFrameHandlesOutsideTheGrid() {
        LayerStack bigStack = HeadlessLayers.createStack();
        TerminalGrid bigGrid = new TerminalGrid(12, 12);
        Layer bigLayer = HeadlessLayers.add(bigStack, bigGrid);
        CellHandle outside = bigLayer.frameCell(9, 9);
        CellHandle inside = bigLayer.frameCell(1, 1);

        bigGrid.resize(6, 4);
        bigLayer.syncCellHandles();

        assertTrue(outside.isDetached());
        assertFalse(inside.isDetached());
        assertEquals(1, bigLayer.getFrameCellHandleCount());

        bigGrid.resize(12, 12);
        assertNotSame(outside, bigLayer.frameCell(9, 9));
    }

    @Test
    void layerRemovalDetachesFrameHandles() {
        CellHandle frame = layer.frameCell(1, 1);

        layer.remove();

        assertTrue(frame.isDetached());
        assertEquals(0, layer.getFrameCellHandleCount());
        assertThrows(IllegalStateException.class, () -> frame.setOffset(1f, 0f));
    }

    @Test
    void releaseIdleAndResetCellHandlesLeaveFrameHandlesAlone() {
        CellHandle frame = layer.frameCell(1, 1);
        frame.setOffset(4f, 0f);

        layer.resetCellHandles();
        layer.releaseIdleCellHandles();

        assertEquals(4f, frame.getOffsetX(), DELTA);
        assertSame(frame, layer.frameCell(1, 1));
    }

    @Test
    void panelMemberAndGroupMemberLayersAreSwept() {
        Panel panel = stack.addPanel(0f, 0f, 100f, 100f);
        layer.setPanel(panel);
        Layer grouped = stack.add(new TerminalGrid(6, 4), new FakeSource());
        stack.addGroup().add(grouped);
        CellHandle panelHandle = layer.frameCell(0, 0);
        CellHandle groupedHandle = grouped.frameCell(0, 0);
        panelHandle.setOffset(2f, 0f);
        groupedHandle.setOffset(2f, 0f);

        stack.beginCellEffects(0.1f);

        assertEquals(0f, panelHandle.getOffsetX(), DELTA);
        assertEquals(0f, groupedHandle.getOffsetX(), DELTA);
    }
}
