package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import java.util.Optional;

/**
 * Replays precomputed frames of up to eight full layers with see-through backgrounds, so the
 * frame time goes to uploading and blending instead of generating random numbers.
 */
class LayeredBlendStressScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final int CELL_COUNT = COLUMNS * ROWS;
    private static final int LAYER_COUNT = CaptureConfig.MAX_LAYER_COUNT;
    private static final int FRAME_COUNT = 16;
    private static final int FRAME_SIZE = LAYER_COUNT * CELL_COUNT;
    private static final int BACKGROUND_ALPHA = 0x40;
    private static final long SEED = 20240611L;

    private final GlyphSource glyphSource;
    private final TerminalGrid[] layerGrids = new TerminalGrid[LAYER_COUNT];
    private final Array<Layer> extraLayers = new Array<>();
    private int layerCount = LAYER_COUNT;
    private int frameIndex;

    // Each frame holds layer 0 cells first, then layer 1 cells, and so on, row by row.
    private int[][] glyphFrames;
    private int[][] foregroundFrames;
    private int[][] backgroundFrames;

    LayeredBlendStressScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        glyphSource = glyphs;
        layerGrids[0] = grid;
        for (int layer = 1; layer < LAYER_COUNT; layer++) {
            layerGrids[layer] = new TerminalGrid(COLUMNS, ROWS);
            extraLayers.add(layers.add(layerGrids[layer], glyphs));
        }
    }

    @Override
    public String id() {
        return "layered-blend-stress";
    }

    @Override
    public String title() {
        return "Stress: layered blending";
    }

    @Override
    public Optional<WindowSize> preferredWindowSize() {
        return Optional.of(WindowSize.FULL_HD);
    }

    @Override
    public boolean recordsAnimation() {
        return false;
    }

    @Override
    public String statusText() {
        return "Layers " + layerCount;
    }

    @Override
    public boolean supportsLayerCount() {
        return true;
    }

    @Override
    public void setLayerCount(int count) {
        layerCount = MathUtils.clamp(count, 1, LAYER_COUNT);
        while (extraLayers.size > layerCount - 1) {
            layers.remove(extraLayers.pop());
        }
        while (extraLayers.size < layerCount - 1) {
            int layerNumber = extraLayers.size + 1;
            extraLayers.add(layers.add(layerGrids[layerNumber], glyphSource));
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode != Input.Keys.L) {
            return false;
        }
        setLayerCount(layerCount == 1 ? LAYER_COUNT : layerCount / 2);
        return true;
    }

    @Override
    public void show() {
        super.show();
        if (glyphFrames == null) {
            buildFrames();
            writeFrame(0);
        }
    }

    @Override
    public void dispose() {
        glyphFrames = null;
        foregroundFrames = null;
        backgroundFrames = null;
        super.dispose();
    }

    @Override
    protected void update(float delta) {
        if (glyphFrames == null) {
            return;
        }
        frameIndex = (frameIndex + 1) % FRAME_COUNT;
        writeFrame(frameIndex);
    }

    private void writeFrame(int frame) {
        int[] glyphs = glyphFrames[frame];
        int[] foregrounds = foregroundFrames[frame];
        int[] backgrounds = backgroundFrames[frame];
        for (int layer = 0; layer < layerCount; layer++) {
            TerminalGrid target = layerGrids[layer];
            int cellIndex = layer * CELL_COUNT;
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    target.set(column, row, glyphs[cellIndex], foregrounds[cellIndex],
                            backgrounds[cellIndex]);
                    cellIndex++;
                }
            }
        }
    }

    private void buildFrames() {
        RandomXS128 random = new RandomXS128(SEED);
        glyphFrames = new int[FRAME_COUNT][FRAME_SIZE];
        foregroundFrames = new int[FRAME_COUNT][FRAME_SIZE];
        backgroundFrames = new int[FRAME_COUNT][FRAME_SIZE];
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            for (int cell = 0; cell < FRAME_SIZE; cell++) {
                glyphFrames[frame][cell] = SceneDrawing.randomGlyph(random);
                foregroundFrames[frame][cell] = SceneDrawing.randomOpaqueColor(random);
                backgroundFrames[frame][cell] =
                        (SceneDrawing.randomOpaqueColor(random) & 0xFFFFFF00) | BACKGROUND_ALPHA;
            }
        }
    }
}
