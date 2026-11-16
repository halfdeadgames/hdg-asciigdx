package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LayerStackPanelCameraTest {

    private static final float TOLERANCE = 1e-3f;

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

    private final List<Rectangle> pushed = new ArrayList<>();
    private final Affine2 drawnTransform = new Affine2();

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
            pushed.add(new Rectangle(windowRect));
            return true;
        }

        @Override
        public void pop() {
        }
    };

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new LayerRenderer() {
        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            drawnTransform.set(layerTransform);
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }, backend);
    private final Vector2 point = new Vector2();
    private final GridPoint2 cell = new GridPoint2();

    @BeforeEach
    void turnSnappingOff() {
        stack.setPixelSnapping(false);
    }

    private Layer addLayer() {
        return stack.add(new TerminalGrid(4, 3), new FakeSource());
    }

    private Layer addMember(Panel panel) {
        Layer layer = addLayer();
        layer.setPanel(panel);
        return layer;
    }

    @Test
    void defaultPanelCameraIsOrigin() {
        Panel panel = stack.addPanel(0f, 0f, 10f, 10f);

        assertEquals(0f, panel.getCameraX());
        assertEquals(0f, panel.getCameraY());
    }

    @Test
    void panelCameraShiftsMemberByMinusCameraTimesParallax() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        layer.setOffset(10f, 5f);
        layer.setParallax(0.5f, 2f);
        panel.setCamera(8f, 3f);

        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(126f, point.x, TOLERANCE);
        assertEquals(171f, point.y, TOLERANCE);
    }

    @Test
    void layerPointAtInvertsPanelCameraShift() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        layer.setOffset(10f, 5f);
        panel.setCamera(8f, 3f);

        stack.layerPointAt(layer, 100f, 200f, 122f, 168f, point);

        assertEquals(0f, point.x, TOLERANCE);
        assertEquals(0f, point.y, TOLERANCE);
    }

    @Test
    void cellAtFollowsPanelCamera() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        panel.setCamera(-8f, 0f);

        stack.cellAt(layer, 100f, 200f, 137f, 168f, cell);

        assertEquals(1, cell.x);
        assertEquals(0, cell.y);
    }

    @Test
    void stackCameraIsIgnoredByMembers() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        stack.setCamera(40f, 40f);

        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(120f, point.x, TOLERANCE);
        assertEquals(170f, point.y, TOLERANCE);
    }

    @Test
    void nonMemberFollowsStackCameraAndIgnoresPanelCamera() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        panel.setCamera(40f, 40f);
        Layer layer = addLayer();
        stack.setCamera(5f, 7f);

        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(95f, point.x, TOLERANCE);
        assertEquals(207f, point.y, TOLERANCE);
    }

    @Test
    void zeroParallaxMemberStaysPut() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        layer.setParallax(0f);
        panel.setCamera(40f, 40f);

        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(120f, point.x, TOLERANCE);
        assertEquals(170f, point.y, TOLERANCE);
    }

    @Test
    void panelClipDoesNotMoveWithPanelCamera() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        addMember(panel);
        panel.setCamera(15f, 15f);

        stack.draw(null, 0f, 100f);

        Rectangle window = pushed.get(0);
        assertEquals(20f, window.x, TOLERANCE);
        assertEquals(30f, window.y, TOLERANCE);
        assertEquals(50f, window.width, TOLERANCE);
        assertEquals(40f, window.height, TOLERANCE);
    }

    @Test
    void drawnMemberTranslationFollowsPanelCamera() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        layer.setOffset(10f, 5f);
        layer.setParallax(0.5f, 2f);
        panel.setCamera(8f, 3f);

        stack.draw(null, 0f, 100f);

        assertEquals(26f, drawnTransform.m02, TOLERANCE);
        assertEquals(71f, drawnTransform.m12, TOLERANCE);
    }

    @Test
    void drawnNonMemberTranslationIgnoresPanelCamera() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addLayer();
        layer.setOffset(10f, 5f);
        panel.setCamera(8f, 3f);

        stack.draw(null, 0f, 100f);

        assertEquals(10f, drawnTransform.m02, TOLERANCE);
        assertEquals(95f, drawnTransform.m12, TOLERANCE);
    }

    @Test
    void pickWindowStaysAtPanelRectWhenCameraMoves() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);
        panel.setCamera(-30f, 0f);

        assertTrue(stack.isPickableAt(layer, 0f, 100f, 25f, 50f));
        assertFalse(stack.isPickableAt(layer, 0f, 100f, 80f, 50f));
    }

    @Test
    void setCameraRejectsNonFiniteValues() {
        Panel panel = stack.addPanel(0f, 0f, 10f, 10f);

        assertThrows(IllegalArgumentException.class, () -> panel.setCamera(Float.NaN, 0f));
        assertThrows(IllegalArgumentException.class, () -> panel.setCamera(0f, Float.POSITIVE_INFINITY));
        assertEquals(0f, panel.getCameraX());
    }

    @Test
    void setCameraOnRemovedPanelThrows() {
        Panel panel = stack.addPanel(0f, 0f, 10f, 10f);
        stack.removePanel(panel);

        assertThrows(IllegalStateException.class, () -> panel.setCamera(1f, 1f));
    }
}
