package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/**
 * Hands out one region for every glyph, with no GL context. A real region needs a texture for its
 * setters, so this one overrides the getters.
 */
final class SingleRegionGlyphSource implements GlyphSource {

    static final int CELL_SIZE = 8;

    private final TextureRegion region = new TextureRegion() {
        @Override
        public float getU() {
            return 0.25f;
        }

        @Override
        public float getV() {
            return 0.25f;
        }

        @Override
        public float getU2() {
            return 0.5f;
        }

        @Override
        public float getV2() {
            return 0.5f;
        }
    };

    @Override
    public int getCellWidth() {
        return CELL_SIZE;
    }

    @Override
    public int getCellHeight() {
        return CELL_SIZE;
    }

    @Override
    public TextureRegion getGlyphRegion(int codePoint) {
        return region;
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return true;
    }

    @Override
    public void dispose() {
    }
}
