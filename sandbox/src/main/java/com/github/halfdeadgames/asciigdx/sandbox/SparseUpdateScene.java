package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Fills a large grid once, then rewrites an adjustable share of distinct cells each frame (1% by default) in a fixed-seed shuffled order. */
class SparseUpdateScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final int CELL_COUNT = COLUMNS * ROWS;
    private static final int[] PERCENT_LEVELS = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private static final int FIRST_RANDOM_GLYPH = '!';
    private static final int LAST_RANDOM_GLYPH = '~';
    private static final long SEED = 20240607L;

    private final RandomXS128 random = new RandomXS128(SEED);
    private final int[] shuffledCells = new int[CELL_COUNT];
    private int dirtyPercent = PERCENT_LEVELS[0];
    private int rotatingOffset;

    SparseUpdateScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
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
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public String statusText() {
        return "Large grid: " + dirtyPercent + "% cells/frame";
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.PLUS || keycode == Input.Keys.EQUALS || keycode == Input.Keys.NUMPAD_ADD) {
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

    private void stepLevel(int direction) {
        int nextIndex = direction > 0 ? nextLevelIndexAbove(dirtyPercent) : nextLevelIndexBelow(dirtyPercent);
        dirtyPercent = PERCENT_LEVELS[nextIndex];
    }

    private static int nextLevelIndexAbove(int percent) {
        for (int i = 0; i < PERCENT_LEVELS.length; i++) {
            if (PERCENT_LEVELS[i] > percent) {
                return i;
            }
        }
        return PERCENT_LEVELS.length - 1;
    }

    private static int nextLevelIndexBelow(int percent) {
        for (int i = PERCENT_LEVELS.length - 1; i >= 0; i--) {
            if (PERCENT_LEVELS[i] < percent) {
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
        int glyph = FIRST_RANDOM_GLYPH + random.nextInt(LAST_RANDOM_GLYPH - FIRST_RANDOM_GLYPH + 1);
        grid.set(column, row, glyph, randomOpaqueColor(), randomOpaqueColor());
    }

    private int randomOpaqueColor() {
        return (random.nextInt(0x1000000) << 8) | 0xFF;
    }
}
