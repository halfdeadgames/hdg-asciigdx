package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.junit.jupiter.api.Test;

class LayerStackPanelTest {

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

    private final List<String> log = new ArrayList<>();
    private final List<Rectangle> pushed = new ArrayList<>();
    private int created;

    private final ClipBackend backend = new ClipBackend() {
        @Override
        public void flush(SpriteBatch batch) {
            log.add("flush");
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
            return true;
        }

        @Override
        public void pop() {
            log.add("pop");
        }
    };

    private final LayerStack stack = newStack(backend);
    private final Vector2 point = new Vector2();
    private final GridPoint2 cell = new GridPoint2();

    private LayerStack newStack(ClipBackend clipBackend) {
        return new LayerStack((grid, source, shared) -> {
            String name = "L" + created++;
            return new LayerRenderer() {
                @Override
                public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                        Array<GlyphSprite> sprites, boolean pixelSnapping) {
                    log.add(name);
                }

                @Override
                public void setGlyphSource(GlyphSource glyphSource) {
                }

                @Override
                public void dispose() {
                }
            };
        }, clipBackend);
    }

    private Layer addLayer() {
        return stack.add(new TerminalGrid(4, 3), new FakeSource());
    }

    private Layer addMember(Panel panel) {
        Layer layer = addLayer();
        layer.setPanel(panel);
        return layer;
    }

    private void assertWindow(Rectangle actual, float x, float y, float width, float height) {
        assertEquals(x, actual.x, TOLERANCE);
        assertEquals(y, actual.y, TOLERANCE);
        assertEquals(width, actual.width, TOLERANCE);
        assertEquals(height, actual.height, TOLERANCE);
    }

    @Test
    void removePanelRejectsPanelWithMembers() {
        Panel panel = stack.addPanel(0f, 0f, 10f, 10f);
        Layer member = addMember(panel);

        assertThrows(IllegalStateException.class, () -> stack.removePanel(panel));

        member.setPanel(null);
        stack.removePanel(panel);
    }

    @Test
    void removePanelRejectsOtherStackAndRemovedPanel() {
        LayerStack other = newStack(backend);
        Panel foreign = other.addPanel(0f, 0f, 10f, 10f);
        Panel own = stack.addPanel(0f, 0f, 10f, 10f);

        assertThrows(IllegalArgumentException.class, () -> stack.removePanel(foreign));
        assertThrows(IllegalArgumentException.class, () -> stack.removePanel(null));
        stack.removePanel(own);
        assertThrows(IllegalStateException.class, () -> stack.removePanel(own));
    }

    @Test
    void setPanelRejectsOtherStackAndRemovedPanel() {
        Layer layer = addLayer();
        Panel foreign = newStack(backend).addPanel(0f, 0f, 10f, 10f);
        Panel removed = stack.addPanel(0f, 0f, 10f, 10f);
        stack.removePanel(removed);

        assertThrows(IllegalArgumentException.class, () -> layer.setPanel(foreign));
        assertThrows(IllegalStateException.class, () -> layer.setPanel(removed));
    }

    @Test
    void setPanelOnRemovedLayerThrows() {
        Layer layer = addLayer();
        Panel panel = stack.addPanel(0f, 0f, 10f, 10f);
        layer.remove();

        assertThrows(IllegalStateException.class, () -> layer.setPanel(panel));
    }

    @Test
    void setPanelStoresPanelAndKeepsOffset() {
        Layer layer = addLayer();
        layer.setOffset(10f, 5f);
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);

        layer.setPanel(panel);

        assertSame(panel, layer.getPanel());
        assertEquals(10f, layer.getOffsetX());
        assertEquals(5f, layer.getOffsetY());
    }

    @Test
    void removedMemberLayerKeepsPanelReadableAndPanelCanBeRemoved() {
        Panel panel = stack.addPanel(0f, 0f, 10f, 10f);
        Layer member = addMember(panel);
        member.remove();

        assertSame(panel, member.getPanel());
        stack.removePanel(panel);
    }

    @Test
    void memberOffsetIsMeasuredFromPanelCorner() {
        stack.setPixelSnapping(false);
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setOffset(10f, 5f);

        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(130f, point.x, TOLERANCE);
        assertEquals(165f, point.y, TOLERANCE);
    }

    @Test
    void layerPointAtInvertsWorldPointOfWithPanel() {
        stack.setPixelSnapping(false);
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setOffset(10f, 5f);

        stack.layerPointAt(layer, 100f, 200f, 139f, 152f, point);

        assertEquals(9f, point.x, TOLERANCE);
        assertEquals(13f, point.y, TOLERANCE);
    }

    @Test
    void cellAtIncludesPanelCorner() {
        stack.setPixelSnapping(false);
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setOffset(10f, 5f);

        stack.cellAt(layer, 100f, 200f, 139f, 152f, cell);

        assertEquals(1, cell.x);
        assertEquals(1, cell.y);
    }

    @Test
    void movingPanelMovesMemberAtNextQuery() {
        stack.setPixelSnapping(false);
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addMember(panel);

        panel.setPosition(40f, 10f);
        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(140f, point.x, TOLERANCE);
        assertEquals(190f, point.y, TOLERANCE);
    }

    @Test
    void panelCornerAndOffsetRoundTogetherWhenSnapping() {
        Layer layer = addMember(stack.addPanel(0.4f, 0f, 50f, 40f));
        layer.setOffset(0.4f, 0f);

        stack.worldPointOf(layer, 0f, 100f, 0f, 0f, point);

        assertEquals(1f, point.x, TOLERANCE);
    }

    @Test
    void nonMemberLayerIgnoresPanels() {
        stack.setPixelSnapping(false);
        stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addLayer();
        layer.setOffset(10f, 5f);

        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);

        assertEquals(110f, point.x, TOLERANCE);
        assertEquals(195f, point.y, TOLERANCE);
    }

    @Test
    void leavingPanelRestoresStackRelativeBehavior() {
        stack.setPixelSnapping(false);
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setOffset(10f, 5f);

        layer.setPanel(null);
        stack.worldPointOf(layer, 100f, 200f, 0f, 0f, point);
        stack.draw(null, 0f, 100f);

        assertNull(layer.getPanel());
        assertEquals(110f, point.x, TOLERANCE);
        assertEquals(List.of("L0"), log);
    }

    @Test
    void memberWithoutOwnClipPushesThePanelRectangle() {
        addMember(stack.addPanel(20f, 30f, 50f, 40f));

        stack.draw(null, 0f, 100f);

        assertEquals(List.of("flush", "push", "L0", "flush", "pop"), log);
        assertWindow(pushed.get(0), 20f, 30f, 50f, 40f);
    }

    @Test
    void ownClipIsShiftedByPanelCornerAndIntersected() {
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setClip(5f, 5f, 100f, 10f);

        stack.draw(null, 0f, 100f);

        assertWindow(pushed.get(0), 25f, 55f, 45f, 10f);
    }

    @Test
    void clipToStackMemberUsesStackAreaIntersectedWithPanel() {
        stack.setSize(60f, 100f);
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.clipToStack();

        stack.draw(null, 0f, 100f);

        assertWindow(pushed.get(0), 20f, 30f, 40f, 40f);
    }

    @Test
    void emptyIntersectionSkipsDrawWithoutFlush() {
        addLayer();
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setClip(200f, 0f, 10f, 10f);

        stack.draw(null, 0f, 100f);

        assertEquals(List.of("L0"), log);
    }

    @Test
    void zeroSizePanelSkipsMemberDraw() {
        addMember(stack.addPanel(20f, 30f, 0f, 40f));

        stack.draw(null, 0f, 100f);

        assertTrue(log.isEmpty());
    }

    @Test
    void movingPanelChangesNextScissor() {
        Panel panel = stack.addPanel(20f, 30f, 50f, 40f);
        addMember(panel);

        stack.draw(null, 0f, 100f);
        panel.setBounds(0f, 0f, 10f, 10f);
        stack.draw(null, 0f, 100f);

        assertWindow(pushed.get(1), 0f, 90f, 10f, 10f);
    }

    @Test
    void nonMemberLayerIsNotClippedByPanels() {
        stack.addPanel(20f, 30f, 50f, 40f);
        addLayer();

        stack.draw(null, 0f, 100f);

        assertEquals(List.of("L0"), log);
        assertTrue(pushed.isEmpty());
    }

    @Test
    void memberIsPickableOnlyInsidePanel() {
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));

        assertTrue(stack.isPickableAt(layer, 0f, 100f, 30f, 50f));
        assertFalse(stack.isPickableAt(layer, 0f, 100f, 80f, 50f));
    }

    @Test
    void memberIsPickableOnlyInsideOwnClipWithinPanel() {
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setClip(0f, 0f, 10f, 40f);

        assertTrue(stack.isPickableAt(layer, 0f, 100f, 25f, 50f));
        assertFalse(stack.isPickableAt(layer, 0f, 100f, 40f, 50f));
    }

    @Test
    void emptyIntersectionIsNeverPickable() {
        Layer layer = addMember(stack.addPanel(20f, 30f, 50f, 40f));
        layer.setClip(200f, 0f, 10f, 10f);

        assertFalse(stack.isPickableAt(layer, 0f, 100f, 30f, 50f));
    }

    @Test
    void nonMemberWithoutClipIsPickableEverywhere() {
        stack.addPanel(20f, 30f, 50f, 40f);
        Layer layer = addLayer();

        assertTrue(stack.isPickableAt(layer, 0f, 100f, 500f, 500f));
    }
}
