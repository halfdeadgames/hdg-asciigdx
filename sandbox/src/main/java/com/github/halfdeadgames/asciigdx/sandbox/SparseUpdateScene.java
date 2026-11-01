package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.math.RandomXS128;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Fills a large grid once, then rewrites about one percent of random cells each frame from a fixed-seed generator. */
class SparseUpdateScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final int CELLS_PER_FRAME = COLUMNS * ROWS / 100;
    private static final int FIRST_RANDOM_GLYPH = '!';
    private static final int LAST_RANDOM_GLYPH = '~';
    private static final long SEED = 20240607L;

    private final RandomXS128 random = new RandomXS128(SEED);

    SparseUpdateScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                setRandomCell(column, row);
            }
        }
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
    protected void update(float delta) {
        for (int i = 0; i < CELLS_PER_FRAME; i++) {
            setRandomCell(random.nextInt(COLUMNS), random.nextInt(ROWS));
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
