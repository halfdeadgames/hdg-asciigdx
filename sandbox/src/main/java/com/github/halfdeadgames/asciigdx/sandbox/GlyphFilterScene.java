package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphFilter;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.SaiksisttSize;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.GlyphSprite;
import com.github.halfdeadgames.asciigdx.layer.Layer;

/**
 * Nearest filtering on the left half, linear on the right, with identical sprites in both. Scaled
 * and rotated edge-touching glyphs (full block, double cross, dark shade) show any bleed from
 * neighboring sheet cells as faint lines; the linear source has a transparent gutter to prevent it.
 */
class GlyphFilterScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 22;
    private static final int HALF_COLUMNS = COLUMNS / 2;

    private static final int BACKGROUND = 0x303840FF;
    private static final float FAST_DEGREES_PER_SECOND = -30f;
    private static final float SLOW_DEGREES_PER_SECOND = -12f;
    private static final float CAPPED_DELTA = 0.1f;

    private static final float[] STATIC_SCALES = {0.37f, 0.5f, 1.3f, 2.5f};
    private static final int[] STATIC_COLUMNS = {1, 3, 5, 8};
    private static final int[] EDGE_GLYPHS = {0x2588, 0x256C, 0x2593};
    private static final int[] EDGE_COLUMNS = {2, 7, 12};

    private final TextureGlyphSource linearGlyphs;
    private final float cellWidth;
    private final float cellHeight;
    private final Array<GlyphSprite> fastSpinners = new Array<>();
    private final Array<GlyphSprite> slowSpinners = new Array<>();

    GlyphFilterScene(GlyphSource nearestGlyphs) {
        super(nearestGlyphs, COLUMNS, ROWS);
        linearGlyphs =
                TextureGlyphSource.loadSaiksistt(SaiksisttSize.SQUARE_16X16, GlyphFilter.LINEAR);
        cellWidth = nearestGlyphs.getCellWidth();
        cellHeight = nearestGlyphs.getCellHeight();
        fillBackground();
        buildHalf(nearestGlyphs, "NEAREST", 0f);
        buildHalf(linearGlyphs, "LINEAR", HALF_COLUMNS * cellWidth);
    }

    @Override
    public String id() {
        return "glyph-filter";
    }

    @Override
    public String title() {
        return "Glyph filtering: nearest vs linear";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    protected void update(float delta) {
        float step = Math.min(delta, CAPPED_DELTA);
        rotateAll(fastSpinners, FAST_DEGREES_PER_SECOND * step);
        rotateAll(slowSpinners, SLOW_DEGREES_PER_SECOND * step);
    }

    @Override
    public void dispose() {
        super.dispose();
        linearGlyphs.dispose();
    }

    private static void rotateAll(Array<GlyphSprite> sprites, float degrees) {
        for (int i = 0; i < sprites.size; i++) {
            sprites.get(i).rotateBy(degrees);
        }
    }

    private void fillBackground() {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                grid.set(column, row, ' ', WHITE, BACKGROUND);
            }
        }
    }

    private void buildHalf(GlyphSource glyphs, String label, float offsetX) {
        TerminalGrid halfGrid = new TerminalGrid(HALF_COLUMNS, ROWS);
        writeLabel(halfGrid, label);
        Layer layer = layers.add(halfGrid, glyphs);
        layer.setOffset(offsetX, 0f);
        addStaticSprites(layer);
        addSpinner(layer);
        addEdgeSprites(layer);
        addLargeBlock(layer);
    }

    private static void writeLabel(TerminalGrid halfGrid, String label) {
        for (int i = 0; i < label.length(); i++) {
            halfGrid.set(i, 0, label.charAt(i), WHITE, CLEAR);
        }
    }

    private void addStaticSprites(Layer layer) {
        for (int i = 0; i < STATIC_SCALES.length; i++) {
            GlyphSprite sprite =
                    layer.addSprite('@', WHITE, STATIC_COLUMNS[i] * cellWidth, 3f * cellHeight);
            sprite.setScale(STATIC_SCALES[i]);
        }
    }

    private void addSpinner(Layer layer) {
        GlyphSprite sprite = layer.addSprite('@', WHITE, 14f * cellWidth, 4f * cellHeight);
        sprite.setScale(3f);
        fastSpinners.add(sprite);
    }

    private void addEdgeSprites(Layer layer) {
        for (int i = 0; i < EDGE_GLYPHS.length; i++) {
            GlyphSprite sprite = layer.addSprite(EDGE_GLYPHS[i], WHITE, EDGE_COLUMNS[i] * cellWidth,
                    10f * cellHeight);
            sprite.setScale(2.7f);
            slowSpinners.add(sprite);
        }
    }

    private void addLargeBlock(Layer layer) {
        GlyphSprite sprite = layer.addSprite(0x2588, WHITE, 3.3f * cellWidth, 15.4f * cellHeight);
        sprite.setScale(4f);
    }
}
