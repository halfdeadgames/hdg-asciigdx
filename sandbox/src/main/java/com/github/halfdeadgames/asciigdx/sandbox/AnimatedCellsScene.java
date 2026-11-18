package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.CellHandle;
import com.github.halfdeadgames.asciigdx.layer.Layer;

/**
 * Recolors every cell of a full-screen grid on every frame, and moves, rotates, scales, and fades
 * a checkerboard half of it, the most lifted cells one layer draws. Motion and fades go through
 * frame handles; the recoloring writes the grid's own colors.
 */
class AnimatedCellsScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final long SEED = 20240612L;

    private static final int PALETTE_SIZE = 256;
    private static final float PALETTE_SATURATION = 0.8f;
    private static final float FOREGROUND_VALUE = 1f;
    private static final float BACKGROUND_VALUE = 0.2f;
    private static final float DEGREES_PER_HUE_STEP = 360f / PALETTE_SIZE;
    private static final float HUE_STEPS_PER_SECOND = 24f;

    private static final float PHASE_PER_COLUMN = 0.21f;
    private static final float PHASE_PER_ROW = 0.37f;
    private static final float OFFSET_RADIUS_CELL_FRACTION = 0.4f;
    private static final float OFFSET_SPEED = 2.1f;
    private static final float ROTATION_AMPLITUDE_DEGREES = 40f;
    private static final float ROTATION_SPEED = 1.7f;
    private static final float SCALE_BASE = 1f;
    private static final float SCALE_AMPLITUDE = 0.3f;
    private static final float SCALE_SPEED = 2.9f;
    private static final float FADE_SPEED = 1.3f;
    private static final float BACKGROUND_FADE_SPEED = 0.9f;
    private static final float MINIMUM_ALPHA = 0.3f;
    private static final float QUARTER_TURN = MathUtils.HALF_PI;

    private final Layer layer;
    private final float[] cellPhases = new float[COLUMNS * ROWS];
    private final int[] foregroundPalette = new int[PALETTE_SIZE];
    private final int[] backgroundPalette = new int[PALETTE_SIZE];
    private final float offsetRadiusX;
    private final float offsetRadiusY;
    private float elapsedSeconds;

    AnimatedCellsScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        layer = layers.getLayer(0);
        offsetRadiusX = glyphs.getCellWidth() * OFFSET_RADIUS_CELL_FRACTION;
        offsetRadiusY = glyphs.getCellHeight() * OFFSET_RADIUS_CELL_FRACTION;
        buildPalettes();
        fillGrid();
        animateCells();
    }

    @Override
    public String id() {
        return "animated-cells";
    }

    @Override
    public String title() {
        return "Animated cells: half the cells move and fade, all recolor";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public String statusText() {
        return COLUMNS + "x" + ROWS + " animated";
    }

    @Override
    protected void update(float delta) {
        elapsedSeconds += delta;
        layers.beginCellEffects(delta);
        animateCells();
    }

    private void animateCells() {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                float phase = cellPhases[row * COLUMNS + column];
                recolorCell(column, row, phase);
                if (isLiftedSquare(column, row)) {
                    liftCell(column, row, phase);
                }
            }
        }
    }

    /**
     * A layer draws at most {@link Layer#MAX_SPRITES} lifted cells, so only the checkerboard half
     * of the grid is lifted.
     */
    private static boolean isLiftedSquare(int column, int row) {
        return ((column + row) & 1) == 0;
    }

    private void liftCell(int column, int row, float phase) {
        CellHandle handle = layer.obtainFrameCellHandle(column, row);
        float offsetAngle = OFFSET_SPEED * elapsedSeconds + phase;
        handle.setOffset(offsetRadiusX * MathUtils.sin(offsetAngle),
                offsetRadiusY * MathUtils.cos(offsetAngle));
        handle.setRotation(ROTATION_AMPLITUDE_DEGREES
                * MathUtils.sin(ROTATION_SPEED * elapsedSeconds + phase));
        handle.setScale(SCALE_BASE
                + SCALE_AMPLITUDE * MathUtils.sin(SCALE_SPEED * elapsedSeconds + phase));
        handle.setForegroundAlpha(fadeFactor(FADE_SPEED * elapsedSeconds + phase));
        handle.setBackgroundAlpha(fadeFactor(BACKGROUND_FADE_SPEED * elapsedSeconds + phase));
        handle.setCellAlpha(fadeFactor(FADE_SPEED * elapsedSeconds + phase + QUARTER_TURN));
    }

    private void recolorCell(int column, int row, float phase) {
        int paletteIndex = ((int) (HUE_STEPS_PER_SECOND * elapsedSeconds + phase * PALETTE_SIZE))
                & (PALETTE_SIZE - 1);
        grid.setForeground(column, row, foregroundPalette[paletteIndex]);
        grid.setBackground(column, row,
                backgroundPalette[(paletteIndex + PALETTE_SIZE / 2) & (PALETTE_SIZE - 1)]);
    }

    private static float fadeFactor(float angle) {
        float unitWave = 0.5f + 0.5f * MathUtils.sin(angle);
        return MINIMUM_ALPHA + (1f - MINIMUM_ALPHA) * unitWave;
    }

    private void buildPalettes() {
        Color color = new Color();
        for (int index = 0; index < PALETTE_SIZE; index++) {
            color.fromHsv(index * DEGREES_PER_HUE_STEP, PALETTE_SATURATION, FOREGROUND_VALUE);
            color.a = 1f;
            foregroundPalette[index] = Color.rgba8888(color);
            color.fromHsv(index * DEGREES_PER_HUE_STEP, PALETTE_SATURATION, BACKGROUND_VALUE);
            color.a = 1f;
            backgroundPalette[index] = Color.rgba8888(color);
        }
    }

    private void fillGrid() {
        RandomXS128 random = new RandomXS128(SEED);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                grid.set(column, row, SceneDrawing.randomGlyph(random), WHITE, BLACK);
                cellPhases[row * COLUMNS + column] =
                        PHASE_PER_COLUMN * column + PHASE_PER_ROW * row;
            }
        }
    }
}
