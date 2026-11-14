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
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
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
        final List<Affine2> transforms = new ArrayList<>();
        final List<float[]> tints = new ArrayList<>();
        final List<float[]> mixes = new ArrayList<>();
        final List<Float> saturations = new ArrayList<>();
        final List<Float> inverts = new ArrayList<>();
        final List<GlyphSource> glyphSources = new ArrayList<>();
        int disposeCount;

        FakeRenderer(String name, List<String> drawLog) {
            this.name = name;
            this.drawLog = drawLog;
        }

        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, LayerEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            drawLog.add(name);
            positions.add(new float[] {layerTransform.m02, layerTransform.m12});
            transforms.add(new Affine2(layerTransform));
            tints.add(new float[] {effects.getTintR(), effects.getTintG(), effects.getTintB(), effects.getTintA()});
            mixes.add(new float[] {effects.getMixR(), effects.getMixG(), effects.getMixB(), effects.getMixAmount()});
            saturations.add(effects.getSaturation());
            inverts.add(effects.getInvert());
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
    private final LayerStack stack = new LayerStack((grid, source, shared) -> {
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
    void layerWithZeroTintAlphaIsNotDrawn() {
        Layer faded = addLayer();
        addLayer();
        faded.setAlpha(0f);
        assertOrder("L1");

        faded.setAlpha(0.01f);
        assertOrder("L0", "L1");
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
    void offsetByAddsToTheCurrentOffset() {
        Layer layer = addLayer();
        layer.setOffset(10f, 20f);

        layer.offsetBy(5f, -8f);
        assertEquals(15f, layer.getOffsetX());
        assertEquals(12f, layer.getOffsetY());

        layer.offsetBy(-15f, 0f);
        assertEquals(0f, layer.getOffsetX());
        assertEquals(12f, layer.getOffsetY());
    }

    @Test
    void cellOffsetByUsesThatLayersCellSize() {
        Layer layer = stack.add(new TerminalGrid(4, 3), new FakeSource(8, 12));
        layer.setCellOffset(1, 1);

        layer.cellOffsetBy(2, -3);

        assertEquals(24f, layer.getOffsetX());
        assertEquals(-24f, layer.getOffsetY());
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

    private Affine2 lastTransform(int rendererIndex) {
        List<Affine2> transforms = renderers.get(rendererIndex).transforms;
        return transforms.get(transforms.size() - 1);
    }

    @Test
    void zoomDefaultsAreNeutral() {
        Layer layer = addLayer();
        assertEquals(1f, layer.getZoom());
        assertEquals(0f, layer.getPivotX());
        assertEquals(0f, layer.getPivotY());
        assertEquals(ZoomMode.SMOOTH, layer.getZoomMode());
        assertEquals(1f, layer.getEffectiveZoom());
    }

    @Test
    void zoomSettersStoreValues() {
        Layer layer = addLayer();
        layer.setZoom(2.5f);
        layer.setPivot(4f, 6f);
        layer.setZoomMode(ZoomMode.WHOLE);
        assertEquals(2.5f, layer.getZoom());
        assertEquals(4f, layer.getPivotX());
        assertEquals(6f, layer.getPivotY());
        assertEquals(ZoomMode.WHOLE, layer.getZoomMode());
    }

    @Test
    void pivotCellPointsAtTheCellCenter() {
        Layer layer = addLayer();
        layer.setPivotCell(2, 1);
        assertEquals(20f, layer.getPivotX());
        assertEquals(18f, layer.getPivotY());
    }

    @Test
    void pivotCellOutsideTheGridIsAllowed() {
        Layer layer = addLayer();
        layer.setPivotCell(-1, 10);
        assertEquals(-4f, layer.getPivotX());
        assertEquals(126f, layer.getPivotY());
    }

    @Test
    void zoomRejectsValuesThatAreNotPositiveAndFinite() {
        Layer layer = addLayer();
        assertThrows(IllegalArgumentException.class, () -> layer.setZoom(0f));
        assertThrows(IllegalArgumentException.class, () -> layer.setZoom(-1f));
        assertThrows(IllegalArgumentException.class, () -> layer.setZoom(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> layer.setZoom(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> layer.setZoomMode(null));
        assertEquals(1f, layer.getZoom());
        assertEquals(ZoomMode.SMOOTH, layer.getZoomMode());
    }

    @Test
    void effectiveZoomFollowsTheMode() {
        Layer layer = addLayer();
        layer.setZoom(2.6f);
        assertEquals(2.6f, layer.getEffectiveZoom());
        layer.setZoomMode(ZoomMode.WHOLE);
        assertEquals(3f, layer.getEffectiveZoom());
        layer.setZoom(0.3f);
        assertEquals(1f, layer.getEffectiveZoom());
    }

    @Test
    void removedLayerCannotChangeZoom() {
        Layer layer = addLayer();
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setZoom(2f));
        assertThrows(IllegalStateException.class, () -> layer.setPivot(1f, 1f));
        assertThrows(IllegalStateException.class, () -> layer.setPivotCell(1, 1));
        assertThrows(IllegalStateException.class, () -> layer.setZoomMode(ZoomMode.WHOLE));
        assertEquals(1f, layer.getZoom());
    }

    @Test
    void zoomedLayerPassesScaleAndPivotFixedTranslation() {
        Layer layer = addLayer();
        layer.setZoom(2f);
        layer.setPivot(10f, 4f);

        stack.draw(null, 100f, 200f);

        Affine2 transform = lastTransform(0);
        assertEquals(2f, transform.m00);
        assertEquals(2f, transform.m11);
        assertEquals(90f, transform.m02);
        assertEquals(204f, transform.m12);
    }

    @Test
    void rotationDefaultsToZero() {
        assertEquals(0f, addLayer().getRotation());
    }

    @Test
    void rotationRejectsValuesThatAreNotFinite() {
        Layer layer = addLayer();
        assertThrows(IllegalArgumentException.class, () -> layer.setRotation(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> layer.setRotation(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> layer.setRotation(Float.NEGATIVE_INFINITY));
        assertEquals(0f, layer.getRotation());
    }

    @Test
    void removedLayerCannotChangeRotation() {
        Layer layer = addLayer();
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setRotation(45f));
        assertEquals(0f, layer.getRotation());
    }

    @Test
    void rotatedLayerPassesRotationAndKeepsPivotFixed() {
        Layer layer = addLayer();
        layer.setRotation(90f);
        layer.setPivot(10f, 4f);

        stack.draw(null, 100f, 200f);

        Affine2 transform = lastTransform(0);
        assertEquals(0f, transform.m00, 1e-4f);
        assertEquals(-1f, transform.m01, 1e-4f);
        assertEquals(1f, transform.m10, 1e-4f);
        assertEquals(0f, transform.m11, 1e-4f);
        assertPivotStaysFixed(transform, 100f, 200f, 10f, 4f);
    }

    @Test
    void rotationCombinesWithZoom() {
        Layer layer = addLayer();
        layer.setRotation(90f);
        layer.setZoom(2f);
        layer.setPivot(10f, 4f);

        stack.draw(null, 100f, 200f);

        Affine2 transform = lastTransform(0);
        assertEquals(0f, transform.m00, 1e-4f);
        assertEquals(-2f, transform.m01, 1e-4f);
        assertEquals(2f, transform.m10, 1e-4f);
        assertEquals(0f, transform.m11, 1e-4f);
        assertPivotStaysFixed(transform, 100f, 200f, 10f, 4f);
    }

    private static void assertPivotStaysFixed(Affine2 transform, float screenX, float screenY,
            float pivotX, float pivotY) {
        float localX = pivotX;
        float localY = -pivotY;
        float mappedX = transform.m00 * localX + transform.m01 * localY + transform.m02;
        float mappedY = transform.m10 * localX + transform.m11 * localY + transform.m12;
        assertEquals(screenX + pivotX, mappedX, 1e-3f);
        assertEquals(screenY - pivotY, mappedY, 1e-3f);
    }

    @Test
    void smoothZoomKeepsFractionsWhenSnappingIsOff() {
        stack.setPixelSnapping(false);
        Layer layer = addLayer();
        layer.setZoom(1.5f);
        layer.setPivot(5f, 0f);

        stack.draw(null, 10f, 20f);

        assertEquals(7.5f, lastTransform(0).m02);
        assertEquals(20f, lastTransform(0).m12);
    }

    @Test
    void wholeZoomSnapsTheCornerEvenWithSnappingOff() {
        stack.setPixelSnapping(false);
        Layer layer = addLayer();
        layer.setZoomMode(ZoomMode.WHOLE);
        layer.setZoom(1.4f);
        layer.setOffset(0.25f, 0.5f);

        stack.draw(null, 10.5f, 20.5f);

        assertEquals(1f, lastTransform(0).m00);
        assertEquals(11f, lastTransform(0).m02);
        assertEquals(20f, lastTransform(0).m12);
    }

    @Test
    void zoomOneWithSnappingMatchesSnappedPosition() {
        Layer layer = addLayer();
        layer.setOffset(0.4f, 0.4f);

        stack.draw(null, 0.4f, 10.4f);

        Affine2 transform = lastTransform(0);
        assertEquals(1f, transform.m00);
        assertEquals(1f, transform.m11);
        assertEquals(1f, transform.m02);
        assertEquals(10f, transform.m12);
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
        assertThrows(IllegalStateException.class, () -> layer.offsetBy(1f, 1f));
        assertThrows(IllegalStateException.class, () -> layer.cellOffsetBy(1, 1));
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

    private float[] lastMix(int rendererIndex) {
        List<float[]> mixes = renderers.get(rendererIndex).mixes;
        return mixes.get(mixes.size() - 1);
    }

    private float lastSaturation(int rendererIndex) {
        List<Float> saturations = renderers.get(rendererIndex).saturations;
        return saturations.get(saturations.size() - 1);
    }

    @Test
    void mixAndSaturationHaveNeutralDefaults() {
        Layer layer = addLayer();
        assertEquals(1f, layer.getMixR());
        assertEquals(1f, layer.getMixG());
        assertEquals(1f, layer.getMixB());
        assertEquals(0f, layer.getMixAmount());
        assertEquals(1f, layer.getSaturation());
    }

    @Test
    void mixAndSaturationSettersStoreValuesUnclamped() {
        Layer layer = addLayer();
        layer.setMix(0.1f, 0.2f, 0.3f, 1.5f);
        layer.setSaturation(2.5f);
        assertEquals(0.1f, layer.getMixR());
        assertEquals(0.2f, layer.getMixG());
        assertEquals(0.3f, layer.getMixB());
        assertEquals(1.5f, layer.getMixAmount());
        assertEquals(2.5f, layer.getSaturation());
    }

    @Test
    void setMixFromColorCopiesRgbAndIgnoresAlpha() {
        Layer layer = addLayer();
        Color color = new Color(0.1f, 0.2f, 0.3f, 0.4f);
        layer.setMix(color, 0.6f);
        color.set(1f, 1f, 1f, 1f);
        assertEquals(0.1f, layer.getMixR());
        assertEquals(0.2f, layer.getMixG());
        assertEquals(0.3f, layer.getMixB());
        assertEquals(0.6f, layer.getMixAmount());
        assertEquals(1f, layer.getTintA());
    }

    @Test
    void mixAndSaturationCannotChangeAfterRemoval() {
        Layer layer = addLayer();
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setMix(1f, 0f, 0f, 0.5f));
        assertThrows(IllegalStateException.class, () -> layer.setMix(Color.RED, 0.5f));
        assertThrows(IllegalStateException.class, () -> layer.setSaturation(0f));
        assertEquals(0f, layer.getMixAmount());
        assertEquals(1f, layer.getSaturation());
    }

    @Test
    void stackPassesEachLayersEffectsToItsOwnRenderer() {
        Layer first = addLayer();
        addLayer();
        first.setMix(1f, 0.5f, 0.25f, 0.75f);
        first.setSaturation(0f);
        stack.draw(null, 0f, 0f);
        assertArrayEquals(new float[] {1f, 0.5f, 0.25f, 0.75f}, lastMix(0));
        assertEquals(0f, lastSaturation(0));
        assertArrayEquals(new float[] {1f, 1f, 1f, 0f}, lastMix(1));
        assertEquals(1f, lastSaturation(1));
    }

    private float lastInvert(int rendererIndex) {
        List<Float> inverts = renderers.get(rendererIndex).inverts;
        return inverts.get(inverts.size() - 1);
    }

    @Test
    void invertHasNeutralDefault() {
        assertEquals(0f, addLayer().getInvert());
    }

    @Test
    void invertSetterStoresValueUnclamped() {
        Layer layer = addLayer();
        layer.setInvert(1.5f);
        assertEquals(1.5f, layer.getInvert());
    }

    @Test
    void invertCannotChangeAfterRemoval() {
        Layer layer = addLayer();
        stack.remove(layer);
        assertThrows(IllegalStateException.class, () -> layer.setInvert(1f));
        assertEquals(0f, layer.getInvert());
    }

    @Test
    void stackPassesEachLayersInvertToItsOwnRenderer() {
        Layer first = addLayer();
        addLayer();
        first.setInvert(1f);
        stack.draw(null, 0f, 0f);
        assertEquals(1f, lastInvert(0));
        assertEquals(0f, lastInvert(1));
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
        LayerStack other = new LayerStack((grid, source, shared) -> new FakeRenderer("other", drawLog));
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
