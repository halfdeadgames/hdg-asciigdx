package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.github.tommyettinger.colorful.rgb.ColorTools;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;

/**
 * Draws a {@link TerminalGrid} with a {@link GlyphSource} into a caller-managed
 * {@link SpriteBatch}: all cell backgrounds first, then all glyphs.
 *
 * <p>The grid and glyph source are not owned and are not disposed. Disposing the
 * renderer frees only its internal background texture.
 */
public final class GridRenderer implements Disposable {

    private final TerminalGrid grid;
    private final GlyphSource glyphSource;
    private final Texture whitePixel;

    /**
     * @param grid the grid to draw
     * @param glyphSource supplies glyph regions and the cell size
     */
    public GridRenderer(TerminalGrid grid, GlyphSource glyphSource) {
        this.grid = grid;
        this.glyphSource = glyphSource;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * Draws the grid. The batch must already be begun; this method never begins
     * or ends it. The batch color is restored before returning.
     *
     * @param batch the active batch
     * @param x screen x of the grid's top-left corner
     * @param y screen y of the grid's top-left corner (y grows upward)
     */
    public void draw(SpriteBatch batch, float x, float y) {
        float previousColor = batch.getPackedColor();
        drawBackgrounds(batch, x, y);
        drawGlyphs(batch, x, y);
        batch.setPackedColor(previousColor);
    }

    private void drawBackgrounds(SpriteBatch batch, float x, float y) {
        float cellWidth = glyphSource.getCellWidth();
        float cellHeight = glyphSource.getCellHeight();
        for (int row = 0; row < grid.getHeight(); row++) {
            float screenY = cellToScreenY(row, y, cellHeight);
            for (int column = 0; column < grid.getWidth(); column++) {
                int background = grid.getBackground(column, row);
                if (background == TerminalGrid.TRANSPARENT_RGBA) {
                    continue;
                }
                batch.setPackedColor(rgbaToPackedFloat(background));
                batch.draw(whitePixel, x + column * cellWidth, screenY, cellWidth, cellHeight);
            }
        }
    }

    private void drawGlyphs(SpriteBatch batch, float x, float y) {
        float cellWidth = glyphSource.getCellWidth();
        float cellHeight = glyphSource.getCellHeight();
        for (int row = 0; row < grid.getHeight(); row++) {
            float screenY = cellToScreenY(row, y, cellHeight);
            for (int column = 0; column < grid.getWidth(); column++) {
                int codePoint = grid.getGlyph(column, row);
                int foreground = grid.getForeground(column, row);
                if (codePoint == ' ' || foreground == TerminalGrid.TRANSPARENT_RGBA) {
                    continue;
                }
                batch.setPackedColor(rgbaToPackedFloat(foreground));
                batch.draw(glyphSource.glyph(codePoint), x + column * cellWidth, screenY, cellWidth, cellHeight);
            }
        }
    }

    private static float cellToScreenY(int row, float gridTopY, float cellHeight) {
        return gridTopY - (row + 1) * cellHeight;
    }

    private static float rgbaToPackedFloat(int rgba) {
        return ColorTools.fromRGBA8888(rgba);
    }

    /** Frees the background texture only. */
    @Override
    public void dispose() {
        whitePixel.dispose();
    }
}
