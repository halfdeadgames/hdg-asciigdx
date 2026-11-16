package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayerStackOffscreenTest {

    private static final int[] LETTERBOX = {100, 50, 600, 500};

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 8;
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

    private final List<String> events = new ArrayList<>();
    private int pushesRemainingToAccept = Integer.MAX_VALUE;

    private final ClipBackend clipBackend = new ClipBackend() {
        @Override
        public void flush(SpriteBatch batch) {
        }

        @Override
        public void getCombinedMatrix(SpriteBatch batch, Matrix4 out) {
            out.idt().setToOrtho2D(0f, 0f, 800f, 600f);
        }

        @Override
        public void getViewport(int[] out) {
            System.arraycopy(LETTERBOX, 0, out, 0, 4);
        }

        @Override
        public boolean push(Rectangle windowRect) {
            events.add("clip push " + (int) windowRect.x + "," + (int) windowRect.y + ","
                    + (int) windowRect.width + "," + (int) windowRect.height);
            pushesRemainingToAccept--;
            return pushesRemainingToAccept >= 0;
        }

        @Override
        public void pop() {
            events.add("clip pop");
        }
    };

    private final OffscreenBackend offscreenBackend = new OffscreenBackend() {
        @Override
        public void begin(SpriteBatch batch, int[] viewport) {
            events.add("begin " + rect(viewport));
        }

        @Override
        public void end(int[] viewport) {
            events.add("end " + rect(viewport));
        }

        @Override
        public void composite(SpriteBatch batch, float alpha, int[] viewport) {
            events.add("composite " + alpha + " " + rect(viewport));
        }

        @Override
        public void dispose() {
            events.add("dispose");
        }
    };

    private final LayerStack stack = new LayerStack((grid, source, shared) -> new LayerRenderer() {
        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            events.add("draw alpha " + effects.getAlpha());
        }

        @Override
        public void drawPremultiplied(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            events.add("drawPremultiplied alpha " + effects.getAlpha() + " "
                    + Arrays.toString(effects.getMatrix().getValues()));
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }, clipBackend, offscreenBackend);

    private static String matrixOf(float saturation, float invert) {
        ColorEffects effects = new ColorEffects();
        effects.setSaturation(saturation);
        effects.setInvert(invert);
        ColorMatrix matrix = new ColorMatrix();
        effects.writeColorMatrix(matrix);
        return Arrays.toString(matrix.getValues());
    }

    private static String rect(int[] viewport) {
        return viewport[0] + "," + viewport[1] + "," + viewport[2] + "," + viewport[3];
    }

    private Layer addLayer() {
        return stack.add(new TerminalGrid(4, 3), new FakeSource());
    }

    @Test
    void fadedLayerWithASpriteDrawsOffscreenThenComposites() {
        Layer layer = addLayer();
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        layer.getColorEffects().setAlpha(0.4f);
        layer.getColorEffects().setSaturation(0.5f);

        stack.draw(null, 0f, 0f);

        assertEquals(List.of(
                "begin 100,50,600,500",
                "drawPremultiplied alpha 1.0 " + matrixOf(0.5f, 0f),
                "end 100,50,600,500",
                "composite 0.4 100,50,600,500"), events);
        assertEquals(0.4f, layer.getColorEffects().getTintAlpha());
    }

    @Test
    void fadedLayerKeepsItsInvertOnTheOffscreenDraw() {
        Layer layer = addLayer();
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        layer.getColorEffects().setAlpha(0.4f);
        layer.getColorEffects().setInvert(0.75f);

        stack.draw(null, 0f, 0f);

        assertEquals(List.of(
                "begin 100,50,600,500",
                "drawPremultiplied alpha 1.0 " + matrixOf(1f, 0.75f),
                "end 100,50,600,500",
                "composite 0.4 100,50,600,500"), events);
    }

    @Test
    void fadedLayerIsSkippedWhenItsClipIsEmpty() {
        Layer layer = addLayer();
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        layer.getColorEffects().setAlpha(0.5f);
        layer.setClip(0f, 0f, 10f, 10f);
        pushesRemainingToAccept = 0;

        stack.draw(null, 0f, 100f);

        assertEquals(List.of("clip push 100,125,8,9"), events);
    }

    @Test
    void clippedFadedLayerRunsInsideTheClip() {
        Layer layer = addLayer();
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        layer.getColorEffects().setAlpha(0.5f);
        layer.setClip(0f, 0f, 10f, 10f);

        stack.draw(null, 0f, 100f);

        assertEquals(List.of(
                "clip push 100,125,8,9",
                "begin 100,50,600,500",
                "drawPremultiplied alpha 1.0 " + matrixOf(1f, 0f),
                "end 100,50,600,500",
                "composite 0.5 100,50,600,500",
                "clip pop"), events);
    }

    @Test
    void layersThatDoNotQualifyDrawDirectly() {
        Layer faded = addLayer();
        faded.getColorEffects().setAlpha(0.5f);
        Layer opaqueWithSprite = addLayer();
        opaqueWithSprite.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        Layer hiddenSprite = addLayer();
        hiddenSprite.getColorEffects().setAlpha(0.5f);
        hiddenSprite.addSprite('a', 0xFFFFFFFF, 0f, 0f).setVisible(false);

        stack.draw(null, 0f, 0f);

        assertEquals(List.of("draw alpha 0.5", "draw alpha 1.0", "draw alpha 0.5"), events);
    }

    @Test
    void hiddenLayersAreSkippedEvenWhenTheyQualify() {
        Layer layer = addLayer();
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        layer.getColorEffects().setAlpha(0.5f);
        layer.setVisible(false);

        stack.draw(null, 0f, 0f);

        assertTrue(events.isEmpty());
    }

    @Test
    void layersWithZeroTintAlphaDrawNothingEvenWithSprites() {
        Layer layer = addLayer();
        layer.addSprite('a', 0xFFFFFFFF, 0f, 0f);
        layer.getColorEffects().setAlpha(0f);

        stack.draw(null, 0f, 0f);

        assertTrue(events.isEmpty());
    }

    @Test
    void backendIsUntouchedUntilNeededAndDisposedOnce() {
        addLayer().getColorEffects().setAlpha(0.5f);
        stack.draw(null, 0f, 0f);
        assertEquals(List.of("draw alpha 0.5"), events);

        stack.dispose();
        stack.dispose();

        assertEquals(List.of("draw alpha 0.5", "dispose"), events);
    }
}
