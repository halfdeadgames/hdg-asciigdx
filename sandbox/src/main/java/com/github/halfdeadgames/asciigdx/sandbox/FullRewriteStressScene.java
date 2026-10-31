package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.math.MathUtils;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Rewrites every cell with a random glyph and colors each frame. */
class FullRewriteStressScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final int FIRST_RANDOM_GLYPH = '!';
    private static final int LAST_RANDOM_GLYPH = '~';

    FullRewriteStressScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
    }

    @Override
    public String id() {
        return "full-rewrite-stress";
    }

    @Override
    public String title() {
        return "Stress: full rewrite";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    protected void update(float delta) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int glyph = MathUtils.random(FIRST_RANDOM_GLYPH, LAST_RANDOM_GLYPH);
                grid.set(column, row, glyph, randomOpaqueColor(), randomOpaqueColor());
            }
        }
    }

    private static int randomOpaqueColor() {
        return (MathUtils.random(0xFFFFFF) << 8) | 0xFF;
    }
}
