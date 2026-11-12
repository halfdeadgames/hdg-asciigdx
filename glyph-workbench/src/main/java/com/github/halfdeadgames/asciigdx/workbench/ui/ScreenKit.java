package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;

/** The drawing setup every workbench menu and dialog screen shares: viewport, batch, UI font, painter, and the pointer in world pixels. */
public final class ScreenKit {

    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private int pointerX;
    private int pointerY;

    /** Builds the white pixel, UI font, batch, and painter; call from {@code show()}. */
    public void create() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();
        source = TextureGlyphSource.loadDungeonMode();
        batch = new SpriteBatch();
        painter = new Painter(batch, whitePixel, source, viewport);
    }

    /** Releases the batch, white pixel, and font; call from {@code hide()}. */
    public void dispose() {
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }

    /** Fits the viewport to the window. */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /** The visible width in text cells. */
    public int columns() {
        return Math.round(viewport.getWorldWidth()) / Painter.TEXT_SIZE;
    }

    /** The visible height in text cells. */
    public int rows() {
        return Math.round(viewport.getWorldHeight()) / Painter.TEXT_SIZE;
    }

    /** Clears the screen, applies the viewport, and starts the batch. */
    public void beginFrame() {
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
    }

    /** Ends the batch. */
    public void endFrame() {
        batch.end();
    }

    /**
     * Reads the mouse position into {@link #pointerX()} and {@link #pointerY()}. Window pixels go through the
     * viewport so UI scale and HiDPI are honoured.
     */
    public void updatePointer() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        pointerX = (int) Math.floor(pointer.x);
        pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
    }

    /** The pointer's X in world pixels as of the last {@link #updatePointer()}. */
    public int pointerX() {
        return pointerX;
    }

    /** The pointer's Y in world pixels, measured from the top, as of the last {@link #updatePointer()}. */
    public int pointerY() {
        return pointerY;
    }

    /** The painter that draws into this kit's batch. */
    public Painter painter() {
        return painter;
    }

    /** The viewport, for screens that read the pointer or world size themselves. */
    public ScreenViewport viewport() {
        return viewport;
    }

    /** Another painter on the same batch, font, and viewport, for screens that draw with two scales. */
    public Painter newPainter() {
        return new Painter(batch, whitePixel, source, viewport);
    }
}
