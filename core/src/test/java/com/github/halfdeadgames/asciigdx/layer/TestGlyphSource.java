package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** A glyph source for tests: fixed cell size and either no glyph regions or code point regions. */
final class TestGlyphSource implements GlyphSource {

    private final int cellWidth;
    private final int cellHeight;
    private final boolean hasCodePointRegions;

    private TestGlyphSource(int cellWidth, int cellHeight, boolean hasCodePointRegions) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.hasCodePointRegions = hasCodePointRegions;
    }

    /** Creates a source whose cells have the given size and that has no glyphs or regions. */
    static TestGlyphSource ofCellSize(int cellWidth, int cellHeight) {
        return new TestGlyphSource(cellWidth, cellHeight, false);
    }

    /** Creates a source of square cells that has no glyphs or regions. */
    static TestGlyphSource ofSquareCells(int cellSize) {
        return ofCellSize(cellSize, cellSize);
    }

    /**
     * Creates a source that has every glyph, with regions whose u is the code point and whose
     * u2 is the code point plus one, so a test can tell which glyph was written.
     */
    static TestGlyphSource ofCodePointRegions(int cellWidth, int cellHeight) {
        return new TestGlyphSource(cellWidth, cellHeight, true);
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
    public TextureRegion getGlyphRegion(int codePoint) {
        if (!hasCodePointRegions) {
            return null;
        }
        return new TextureRegion() {
            @Override
            public float getU() {
                return codePoint;
            }

            @Override
            public float getV() {
                return 0f;
            }

            @Override
            public float getU2() {
                return codePoint + 1f;
            }

            @Override
            public float getV2() {
                return 1f;
            }
        };
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return hasCodePointRegions;
    }

    @Override
    public void dispose() {
    }
}
