package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;

/** Draws rectangles, glyphs, and text using window pixels with the origin at the top-left. */
public final class Painter {

    /** The cell size at {@link UiScale#DEFAULT}, for screens that do not scale. */
    public static final int TEXT_SIZE = 24;

    private final SpriteBatch batch;
    private final Texture whitePixel;
    private final GlyphSource font;
    private final Viewport viewport;
    private UiScale scale = UiScale.DEFAULT;

    public Painter(SpriteBatch batch, Texture whitePixel, GlyphSource font, Viewport viewport) {
        this.batch = batch;
        this.whitePixel = whitePixel;
        this.font = font;
        this.viewport = viewport;
    }

    /** Sets the cell size used by text, frames, and the views drawn through this painter. */
    public void setScale(UiScale scale) {
        this.scale = scale;
    }

    public UiScale scale() {
        return scale;
    }

    /** Pixels of one text cell at this painter's scale. */
    public int cell() {
        return scale.cell();
    }

    public void fill(Color color, int x, int top, int width, int height) {
        batch.setColor(color);
        batch.draw(whitePixel, x, flipY(top, height), width, height);
    }

    /** Draws the lit pixels of a grid as filled squares of {@code scale} pixels. */
    public void pixels(PixelGrid grid, int x, int top, int scale, Color color) {
        for (int py = 0; py < grid.getHeight(); py++) {
            for (int px = 0; px < grid.getWidth(); px++) {
                if (grid.isLit(px, py)) {
                    fill(color, x + px * scale, top + py * scale, scale, scale);
                }
            }
        }
    }

    public void region(TextureRegion region, int x, int top, int size, Color tint) {
        batch.setColor(tint);
        batch.draw(region, x, flipY(top, size), size, size);
    }

    public void text(CharSequence value, int x, int top, Color color) {
        int size = cell();
        batch.setColor(color);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != ' ') {
                batch.draw(font.glyph(c), x + i * size, flipY(top, size), size, size);
            }
        }
    }

    /** Draws a double-line border of text-sized cells; the size must be at least 2x2 cells. */
    public void frame(Color color, int x, int top, int cellsWide, int cellsHigh) {
        int size = cell();
        int right = x + (cellsWide - 1) * size;
        int bottom = top + (cellsHigh - 1) * size;
        batch.setColor(color);
        cell('╔', x, top);
        cell('╗', right, top);
        cell('╚', x, bottom);
        cell('╝', right, bottom);
        for (int col = 1; col < cellsWide - 1; col++) {
            cell('═', x + col * size, top);
            cell('═', x + col * size, bottom);
        }
        for (int row = 1; row < cellsHigh - 1; row++) {
            cell('║', x, top + row * size);
            cell('║', right, top + row * size);
        }
    }

    private void cell(char c, int x, int top) {
        int size = cell();
        batch.draw(font.glyph(c), x, flipY(top, size), size, size);
    }

    /** Rounds a pixel length up to whole text cells at the default scale. */
    public static int roundUpToCells(int pixels) {
        return roundUpToCells(pixels, TEXT_SIZE);
    }

    static int roundUpToCells(int pixels, int cellPixels) {
        return (pixels + cellPixels - 1) / cellPixels * cellPixels;
    }

    private float flipY(int top, int height) {
        return viewport.getWorldHeight() - top - height;
    }
}
