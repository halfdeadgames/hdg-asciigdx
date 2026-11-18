package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import java.util.List;
import java.util.Optional;

/**
 * Fills a large grid once, then rewrites an adjustable share of distinct cells each frame (1% by
 * default) in a fixed-seed shuffled order.
 */
class SparseUpdateScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final int CELL_COUNT = COLUMNS * ROWS;
    private static final List<Integer> PERCENT_LEVELS =
            List.of(1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
    private static final List<Integer> LAYER_LEVELS = List.of(1, 2, 4);
    private static final int EXTRA_LAYER_FILL_PERCENT = 10;
    private static final long SEED = 20240607L;

    private final RandomXS128 random = new RandomXS128(SEED);
    private final int[] shuffledCells = new int[CELL_COUNT];
    private int dirtyPercent = PERCENT_LEVELS.get(0);
    private int rotatingOffset;

    private final GlyphSource glyphSource;
    private final Array<Layer> extraLayers = new Array<>();
    private int layerCount = 1;

    SparseUpdateScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        glyphSource = glyphs;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                setRandomCell(column, row);
            }
        }
        shuffleCells();
    }

    @Override
    public String id() {
        return "sparse-update";
    }

    @Override
    public String title() {
        return "Large grid: sparse updates";
    }

    @Override
    public SceneGroup group() {
        return SceneGroup.BENCHMARKS;
    }

    @Override
    public Optional<WindowSize> preferredWindowSize() {
        return Optional.of(WindowSize.FULL_HD);
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public String statusText() {
        return "Layers " + layerCount + ", " + dirtyPercent + "% cells/frame";
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.L) {
            setLayerCount(nextLayerCountLevel());
            return true;
        }
        if (keycode == Input.Keys.PLUS || keycode == Input.Keys.EQUALS
                || keycode == Input.Keys.NUMPAD_ADD) {
            stepLevel(1);
            return true;
        }
        if (keycode == Input.Keys.MINUS || keycode == Input.Keys.NUMPAD_SUBTRACT) {
            stepLevel(-1);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        stepLevel(screenX >= Gdx.graphics.getWidth() / 2 ? 1 : -1);
        return true;
    }

    @Override
    public boolean supportsDirtyPercent() {
        return true;
    }

    @Override
    public void setDirtyPercent(int percent) {
        dirtyPercent = MathUtils.clamp(percent, 0, 100);
    }

    @Override
    public boolean supportsLayerCount() {
        return true;
    }

    @Override
    public void setLayerCount(int count) {
        layerCount = MathUtils.clamp(count, 1, CaptureConfig.MAX_LAYER_COUNT);
        while (extraLayers.size > layerCount - 1) {
            layers.remove(extraLayers.pop());
        }
        while (extraLayers.size < layerCount - 1) {
            extraLayers.add(layers.add(createSparseStaticGrid(extraLayers.size + 1), glyphSource));
        }
    }

    private int nextLayerCountLevel() {
        for (int i = 0; i < LAYER_LEVELS.size(); i++) {
            int level = LAYER_LEVELS.get(i);
            if (level > layerCount) {
                return level;
            }
        }
        return LAYER_LEVELS.get(0);
    }

    private static TerminalGrid createSparseStaticGrid(int layerNumber) {
        RandomXS128 layerRandom = new RandomXS128(SEED + layerNumber);
        TerminalGrid sparse = new TerminalGrid(COLUMNS, ROWS);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (layerRandom.nextInt(100) < EXTRA_LAYER_FILL_PERCENT) {
                    int glyph = SceneDrawing.randomGlyph(layerRandom);
                    sparse.set(column, row, glyph,
                            SceneDrawing.randomOpaqueColor(layerRandom), CLEAR);
                }
            }
        }
        return sparse;
    }

    private void stepLevel(int direction) {
        int nextIndex = direction > 0 ? nextLevelIndexAbove(dirtyPercent)
                : nextLevelIndexBelow(dirtyPercent);
        dirtyPercent = PERCENT_LEVELS.get(nextIndex);
    }

    private static int nextLevelIndexAbove(int percent) {
        for (int i = 0; i < PERCENT_LEVELS.size(); i++) {
            if (PERCENT_LEVELS.get(i) > percent) {
                return i;
            }
        }
        return PERCENT_LEVELS.size() - 1;
    }

    private static int nextLevelIndexBelow(int percent) {
        for (int i = PERCENT_LEVELS.size() - 1; i >= 0; i--) {
            if (PERCENT_LEVELS.get(i) < percent) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void update(float delta) {
        int cellsThisFrame = CELL_COUNT * dirtyPercent / 100;
        for (int i = 0; i < cellsThisFrame; i++) {
            int cellIndex = shuffledCells[(rotatingOffset + i) % CELL_COUNT];
            setRandomCell(cellIndex % COLUMNS, cellIndex / COLUMNS);
        }
        rotatingOffset = (rotatingOffset + cellsThisFrame) % CELL_COUNT;
    }

    private void shuffleCells() {
        for (int i = 0; i < CELL_COUNT; i++) {
            shuffledCells[i] = i;
        }
        for (int i = CELL_COUNT - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swapped = shuffledCells[i];
            shuffledCells[i] = shuffledCells[j];
            shuffledCells[j] = swapped;
        }
    }

    private void setRandomCell(int column, int row) {
        int glyph = SceneDrawing.randomGlyph(random);
        grid.set(column, row, glyph, SceneDrawing.randomOpaqueColor(random),
                SceneDrawing.randomOpaqueColor(random));
    }
}
