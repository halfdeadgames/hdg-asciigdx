package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayerStackClipTest {

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

    private final List<String> log = new ArrayList<>();
    private final List<Rectangle> pushed = new ArrayList<>();
    private boolean pushResult = true;
    private int created;

    private final ClipBackend backend = new ClipBackend() {
        @Override
        public void flush(SpriteBatch batch) {
        }

        @Override
        public void getCombinedMatrix(SpriteBatch batch, Matrix4 out) {
            out.idt().setToOrtho2D(0f, 0f, 200f, 100f);
        }

        @Override
        public void getViewport(int[] out) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 200;
            out[3] = 100;
        }

        @Override
        public boolean push(Rectangle windowRect) {
            log.add("push");
            pushed.add(new Rectangle(windowRect));
            return pushResult;
        }

        @Override
        public void pop() {
            log.add("pop");
        }
    };

    private final LayerStack stack = new LayerStack((grid, source) -> {
        String name = "L" + created++;
        return new LayerRenderer() {
            @Override
            public void draw(SpriteBatch batch, float x, float y, float r, float g, float b, float a) {
                log.add(name);
            }

            @Override
            public void dispose() {
            }
        };
    }, backend);

    private Layer addLayer() {
        return stack.add(new TerminalGrid(4, 3), new FakeSource());
    }

    @Test
    void clippedLayerPushesBeforeAndPopsAfterItsDraw() {
        addLayer();
        Layer clipped = addLayer();
        addLayer();
        clipped.setClip(0f, 0f, 10f, 10f);

        stack.draw(null, 0f, 100f);

        assertEquals(List.of("L0", "push", "L1", "pop", "L2"), log);
    }

    @Test
    void unclippedLayerNeverPushes() {
        addLayer();
        stack.draw(null, 0f, 0f);
        assertEquals(List.of("L0"), log);
    }

    @Test
    void emptyIntersectionSkipsDrawWithoutPopAndKeepsOrder() {
        addLayer();
        Layer clipped = addLayer();
        addLayer();
        clipped.setClip(0f, 0f, 10f, 10f);
        pushResult = false;

        stack.draw(null, 0f, 100f);

        assertEquals(List.of("L0", "push", "L2"), log);
    }

    @Test
    void clipIsInStackSpaceIndependentOfLayerOffset() {
        Layer layer = addLayer();
        layer.setClip(10f, 20f, 30f, 40f);
        layer.setOffset(50f, 5f);

        stack.draw(null, 0f, 100f);

        Rectangle r = pushed.get(0);
        assertEquals(10f, r.x);
        assertEquals(40f, r.y);
        assertEquals(30f, r.width);
        assertEquals(40f, r.height);
    }

    @Test
    void clipToStackUsesCurrentStackSize() {
        Layer layer = addLayer();
        stack.setSize(100f, 50f);
        layer.clipToStack();
        stack.setSize(60f, 30f);

        stack.draw(null, 0f, 100f);

        Rectangle r = pushed.get(0);
        assertEquals(0f, r.x);
        assertEquals(70f, r.y);
        assertEquals(60f, r.width);
        assertEquals(30f, r.height);
    }

    @Test
    void clipToStackNeedsSizeAtCallAndAtDraw() {
        Layer layer = addLayer();
        assertThrows(IllegalStateException.class, layer::clipToStack);

        stack.setSize(10f, 10f);
        layer.clipToStack();
        stack.setSize(0f, 0f);
        assertThrows(IllegalStateException.class, () -> stack.draw(null, 0f, 0f));
    }

    @Test
    void clearClipAndGetters() {
        Layer layer = addLayer();
        assertFalse(layer.hasClip());
        layer.setClip(1f, 2f, 3f, 4f);
        assertTrue(layer.hasClip());
        assertEquals(3f, layer.getClipWidth());
        layer.clearClip();
        assertFalse(layer.hasClip());
        stack.draw(null, 0f, 0f);
        assertEquals(List.of("L0"), log);
    }

    @Test
    void removedLayerClipSettersThrow() {
        Layer layer = addLayer();
        stack.setSize(10f, 10f);
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setClip(0f, 0f, 1f, 1f));
        assertThrows(IllegalStateException.class, layer::clipToStack);
        assertThrows(IllegalStateException.class, layer::clearClip);
    }

    @Test
    void stackToWorldFlipsYAndSnapsCorners() {
        Rectangle out = new Rectangle();
        ClipMath.stackToWorld(10.4f, 100.4f, 1.2f, 2.2f, 5f, 5f, true, out);
        assertEquals(12f, out.x);
        assertEquals(93f, out.y);
        assertEquals(5f, out.width);
        assertEquals(5f, out.height);

        ClipMath.stackToWorld(10.5f, 20f, 0f, 0f, 4f, 6f, false, out);
        assertEquals(10.5f, out.x);
        assertEquals(14f, out.y);
    }

    @Test
    void worldToWindowRoundsOutwardThroughViewport() {
        Matrix4 ortho = new Matrix4().setToOrtho2D(0f, 0f, 100f, 50f);
        Rectangle out = new Rectangle();
        Vector3 tmp = new Vector3();

        ClipMath.worldToWindow(new Rectangle(10f, 5f, 20f, 10f), ortho, 0, 0, 200, 100, tmp, out);
        assertEquals(20f, out.x);
        assertEquals(10f, out.y);
        assertEquals(40f, out.width);
        assertEquals(20f, out.height);

        ClipMath.worldToWindow(new Rectangle(10.2f, 5f, 20f, 10f), ortho, 5, 7, 200, 100, tmp, out);
        assertEquals(25f, out.x);
        assertEquals(17f, out.y);
        assertEquals(41f, out.width);
    }
}
