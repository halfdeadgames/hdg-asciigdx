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

class LayerScrollTest {

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
    void contentMovesWithTheShift() {
        grid.setGlyph(1, 1, 'a');

        layer.scroll(1, 0);

        assertEquals('a', grid.getGlyph(2, 1));
        assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(1, 1));
    }

    @Test
    void handleMovesWithItsContent() {
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(2f, 0f);

        layer.scroll(1, 2);

        assertEquals(2, handle.getColumn());
        assertEquals(3, handle.getRow());
        assertSame(handle, layer.cell(2, 3));
        assertNotSame(handle, layer.cell(1, 1));
        assertEquals(2f, handle.getOffsetX());
        assertFalse(handle.isDetached());
    }

    @Test
    void handlePushedOutIsDetached() {
        CellHandle staying = layer.cell(1, 1);
        CellHandle pushedOut = layer.cell(5, 1);

        layer.scroll(1, 0);

        assertTrue(pushedOut.isDetached());
        assertEquals(6, pushedOut.getColumn());
        assertThrows(IllegalStateException.class, () -> pushedOut.setOffset(1f, 0f));
        assertFalse(staying.isDetached());
        assertEquals(1, layer.getCellHandleCount());
    }

    @Test
    void liftedHandleIsUnhiddenAtTheOldPositionAndHiddenAtTheNewOne() {
        CellHandle handle = layer.cell(1, 1);
        handle.setOffset(2f, 0f);
        draw();
        renderer.events.clear();

        layer.scroll(1, 0);

        assertEquals(List.of("show 1,1"), renderer.events);
        draw();
        assertEquals(List.of("show 1,1", "hide 2,1"), renderer.events);
    }

    @Test
    void hiddenHandleOutsideAShrunkGridDoesNotReachTheRendererAndIsMovedBack() {
        CellHandle handle = layer.cell(5, 1);
        handle.setOffset(2f, 0f);
        draw();
        grid.resize(5, 4);
        renderer.events.clear();

        layer.scroll(-1, 0);

        assertTrue(renderer.events.isEmpty());
        assertFalse(handle.isDetached());
        assertEquals(4, handle.getColumn());
        assertSame(handle, layer.cell(4, 1));
    }

    @Test
    void rendererIsRebound() {
        layer.cell(1, 1);
        int bindsBefore = renderer.bindCount;

        layer.scroll(1, 0);

        assertEquals(bindsBefore + 1, renderer.bindCount);
        assertSame(layer.getCellHandles(), renderer.cellHandles);
    }

    @Test
    void zeroShiftChangesNothing() {
        grid.setGlyph(1, 1, 'a');
        CellHandle handle = layer.cell(1, 1);
        int bindsBefore = renderer.bindCount;

        layer.scroll(0, 0);

        assertEquals('a', grid.getGlyph(1, 1));
        assertSame(handle, layer.cell(1, 1));
        assertEquals(bindsBefore, renderer.bindCount);
    }

    @Test
    void scrollsAccumulate() {
        CellHandle handle = layer.cell(1, 1);
        grid.setGlyph(1, 1, 'a');

        layer.scroll(1, 0);
        layer.scroll(1, 1);

        assertEquals(3, handle.getColumn());
        assertEquals(2, handle.getRow());
        assertSame(handle, layer.cell(3, 2));
        assertEquals('a', grid.getGlyph(3, 2));
    }

    @Test
    void scrollLargerThanTheGridDetachesEveryHandleAndEmptiesTheGrid() {
        grid.setGlyph(1, 1, 'a');
        CellHandle first = layer.cell(0, 0);
        CellHandle second = layer.cell(5, 3);

        layer.scroll(6, 0);

        assertTrue(first.isDetached());
        assertTrue(second.isDetached());
        assertEquals(0, layer.getCellHandleCount());
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int column = 0; column < grid.getWidth(); column++) {
                assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(column, row));
            }
        }
    }

    @Test
    void removedLayerThrows() {
        layer.remove();

        assertThrows(IllegalStateException.class, () -> layer.scroll(1, 0));
        assertThrows(IllegalStateException.class, () -> layer.scroll(0, 0));
    }
}
