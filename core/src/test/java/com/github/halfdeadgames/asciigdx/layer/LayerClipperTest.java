package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerClipperTest {

    private static final float TOLERANCE = 1e-3f;
    private static final float STACK_Y = 100f;

    private static final class FakeClipBackend implements ClipBackend {
        final Rectangle lastPushed = new Rectangle();
        boolean pushResult = true;
        int viewportQueries;
        int pops;

        @Override
        public void flush(SpriteBatch batch) {
        }

        @Override
        public void getCombinedMatrix(SpriteBatch batch, Matrix4 out) {
            out.idt().setToOrtho2D(0f, 0f, 200f, 100f);
        }

        @Override
        public void getViewport(int[] out) {
            viewportQueries++;
            out[0] = 0;
            out[1] = 0;
            out[2] = 200;
            out[3] = 100;
        }

        @Override
        public boolean push(Rectangle windowRect) {
            lastPushed.set(windowRect);
            return pushResult;
        }

        @Override
        public void pop() {
            pops++;
        }
    }

    private final FakeClipBackend backend = new FakeClipBackend();
    private final LayerClipper clipper = new LayerClipper(backend);
    private final LayerStack stack =
            TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer(),
                    backend);

    private Layer addLayer() {
        return HeadlessLayers.add(stack, new TerminalGrid(4, 3));
    }

    private void resolveAndPush(Layer layer, float stackWidth, float stackHeight,
            boolean stackHasSize) {
        clipper.beginFrame();
        clipper.queryViewportOnce(null);
        clipper.resolve(layer, stackWidth, stackHeight, stackHasSize);
        clipper.pushScissor(null, 0f, STACK_Y, true);
    }

    private void assertPushed(float x, float y, float width, float height) {
        assertEquals(x, backend.lastPushed.x, TOLERANCE);
        assertEquals(y, backend.lastPushed.y, TOLERANCE);
        assertEquals(width, backend.lastPushed.width, TOLERANCE);
        assertEquals(height, backend.lastPushed.height, TOLERANCE);
    }

    @Test
    void rectClipIsOffsetByPanelCornerAndIntersectedWithPanel() {
        Panel panel = stack.addPanel(10f, 20f, 50f, 40f);
        Layer layer = addLayer();
        layer.setPanel(panel);
        layer.setClip(5f, 5f, 100f, 100f);

        resolveAndPush(layer, 0f, 0f, false);

        assertPushed(15f, 40f, 45f, 35f);
    }

    @Test
    void panelMemberWithoutOwnClipUsesPanelRectangle() {
        Panel panel = stack.addPanel(10f, 20f, 50f, 40f);
        Layer layer = addLayer();
        layer.setPanel(panel);

        resolveAndPush(layer, 0f, 0f, false);

        assertPushed(10f, 40f, 50f, 40f);
    }

    @Test
    void clipOutsidePanelResolvesToEmpty() {
        Panel panel = stack.addPanel(10f, 20f, 50f, 40f);
        Layer layer = addLayer();
        layer.setPanel(panel);
        layer.setClip(200f, 200f, 10f, 10f);

        clipper.resolve(layer, 0f, 0f, false);

        assertTrue(clipper.isEmpty());
    }

    @Test
    void clipToStackResolvesToStackSize() {
        stack.setSize(80f, 60f);
        Layer layer = addLayer();
        layer.clipToStack();

        resolveAndPush(layer, 80f, 60f, true);

        assertFalse(clipper.isEmpty());
        assertPushed(0f, 40f, 80f, 60f);
    }

    @Test
    void clipToStackWithoutStackSizeThrows() {
        stack.setSize(80f, 60f);
        Layer layer = addLayer();
        layer.clipToStack();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> clipper.resolve(layer, 0f, 0f, false));

        assertEquals("Layer is clipped to the stack but the stack size is not set",
                thrown.getMessage());
    }

    @Test
    void insideKeepsLeftAndTopEdgesAndExcludesRightAndBottom() {
        Layer layer = addLayer();
        layer.setClip(10f, 10f, 20f, 20f);
        clipper.resolve(layer, 0f, 0f, false);

        assertTrue(clipper.isInside(0f, STACK_Y, 10f, 80f, true));
        assertTrue(clipper.isInside(0f, STACK_Y, 20f, 90f, true));
        assertFalse(clipper.isInside(0f, STACK_Y, 30f, 80f, true));
        assertFalse(clipper.isInside(0f, STACK_Y, 20f, 70f, true));
    }

    @Test
    void pushScissorReportsInactiveWhenBackendRefuses() {
        Layer layer = addLayer();
        layer.setClip(0f, 0f, 10f, 10f);
        backend.pushResult = false;

        resolveAndPush(layer, 0f, 0f, false);

        assertFalse(clipper.isScissorActive());
    }

    @Test
    void popScissorPopsBackendAndClearsActiveState() {
        Layer layer = addLayer();
        layer.setClip(0f, 0f, 10f, 10f);
        resolveAndPush(layer, 0f, 0f, false);
        assertTrue(clipper.isScissorActive());

        clipper.popScissor(null);

        assertEquals(1, backend.pops);
        assertFalse(clipper.isScissorActive());
    }

    @Test
    void viewportIsQueriedOncePerFrame() {
        clipper.beginFrame();
        clipper.queryViewportOnce(null);
        clipper.queryViewportOnce(null);
        clipper.queryViewportOnce(null);
        assertEquals(1, backend.viewportQueries);

        clipper.beginFrame();
        clipper.queryViewportOnce(null);
        assertEquals(2, backend.viewportQueries);
    }
}
