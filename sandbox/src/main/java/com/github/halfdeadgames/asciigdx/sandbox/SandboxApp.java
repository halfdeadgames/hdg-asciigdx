package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;

/** Sandbox application for visually checking rendering features. */
public class SandboxApp extends ApplicationAdapter {
    private static final int SHEET_COLUMNS = 16;
    private static final int SHEET_ROWS = 16;
    private static final String FALLBACK_DEMO_TEXT = "Hello @ 5€";
    private static final int TEXT_ROWS_BELOW_SHEET = 2;

    private SpriteBatch batch;
    private GlyphSource glyphs;

    @Override
    public void create() {
        batch = new SpriteBatch();
        glyphs = TextureGlyphSource.loadDungeonMode();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        int cellWidth = glyphs.getCellWidth();
        int cellHeight = glyphs.getCellHeight();
        int scale = integerScale(cellWidth, cellHeight);
        int drawWidth = cellWidth * scale;
        int drawHeight = cellHeight * scale;
        int top = Gdx.graphics.getHeight() - drawHeight;

        batch.begin();
        batch.setColor(Color.WHITE);
        for (int slot = 0; slot < SHEET_COLUMNS * SHEET_ROWS; slot++) {
            TextureRegion glyph = glyphs.glyph(GlyphOrder.CP437.codePointAt(slot));
            int x = (slot % SHEET_COLUMNS) * drawWidth;
            int y = top - (slot / SHEET_COLUMNS) * drawHeight;
            batch.draw(glyph, x, y, drawWidth, drawHeight);
        }

        batch.setColor(Color.GOLD);
        int textY = top - (SHEET_ROWS + 1) * drawHeight;
        int column = 0;
        for (int i = 0; i < FALLBACK_DEMO_TEXT.length(); ) {
            int codePoint = FALLBACK_DEMO_TEXT.codePointAt(i);
            batch.draw(glyphs.glyph(codePoint), column * drawWidth, textY, drawWidth, drawHeight);
            column++;
            i += Character.charCount(codePoint);
        }
        batch.end();
    }

    private static int integerScale(int cellWidth, int cellHeight) {
        int columnScale = Gdx.graphics.getWidth() / (SHEET_COLUMNS * cellWidth);
        int rowScale = Gdx.graphics.getHeight() / ((SHEET_ROWS + TEXT_ROWS_BELOW_SHEET) * cellHeight);
        return Math.max(1, Math.min(columnScale, rowScale));
    }

    @Override
    public void dispose() {
        batch.dispose();
        glyphs.dispose();
    }
}
