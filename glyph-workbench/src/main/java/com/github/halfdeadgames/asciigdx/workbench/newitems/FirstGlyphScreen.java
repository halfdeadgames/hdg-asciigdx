package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.WorkbenchScreen;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBarStatus;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Shown instead of the paint screen for a font with no glyph slots: asks for the first glyph and lists it. */
public final class FirstGlyphScreen implements WorkbenchScreen {

    private static final String TAG = "FirstGlyph";
    private static final int CELL = Painter.TEXT_SIZE;
    private static final int CRUMB_ROW = 0;
    private static final int NOTICE_MARGIN_CELLS = 2;
    private static final int NOTICE_ROWS_FROM_BOTTOM = 3;

    private final Path fontFolder;
    private final Runnable onBack;
    private final Runnable onAdded;
    private final List<String> crumbs;
    private final NewGlyphDialog dialog = new NewGlyphDialog(this::addToList, codePoint -> false);
    private final ScreenViewport viewport = new ScreenViewport();
    private final DevCapture devCapture = new DevCapture();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private String notice = "";
    private boolean added;
    private boolean saveFailed;
    private boolean finished;

    /**
     * @param fontFolder the font's folder, which gets the {@code glyphs.txt} entry
     * @param size the size file that will be painted next
     * @param onBack called once when the user closes the prompt without adding a glyph
     * @param onAdded called once after the glyph was written to the list
     */
    public FirstGlyphScreen(Path fontFolder, CellSize size, Runnable onBack, Runnable onAdded) {
        this.fontFolder = fontFolder;
        this.onBack = onBack;
        this.onAdded = onAdded;
        this.crumbs = List.of("GLYPH WORKBENCH", fontFolder.getFileName().toString().toUpperCase(Locale.ROOT),
            size.toString().toUpperCase(Locale.ROOT), "FIRST GLYPH");
    }

    @Override
    public void show() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();
        source = TextureGlyphSource.loadDungeonMode();
        batch = new SpriteBatch();
        painter = new Painter(batch, whitePixel, source, viewport);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                notice = "";
                dialog.type(character);
                return true;
            }
        });
        dialog.open();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        if (!finished) {
            act();
        }
        draw();
        devCapture.afterRender();
    }

    private void act() {
        FirstGlyphClose step = FirstGlyphClose.decide(dialog.isOpen(), added, saveFailed);
        switch (step) {
            case WAIT -> dialog.handle(Gdx.graphics.getDeltaTime());
            case REOPEN -> reopen();
            case GO_BACK -> goBack();
            case FINISHED -> finished = true;
        }
    }

    private void reopen() {
        saveFailed = false;
        dialog.open();
    }

    private void goBack() {
        finished = true;
        onBack.run();
    }

    private void addToList(int codePoint) {
        try {
            GlyphListFile.append(fontFolder.toFile(), codePoint);
        } catch (IOException e) {
            Gdx.app.error(TAG, "Cannot update glyphs.txt: " + e.getMessage());
            notice = "Cannot save to glyphs.txt: " + e.getMessage();
            saveFailed = true;
            return;
        }
        added = true;
        onAdded.run();
    }

    private int columns() {
        return Math.round(viewport.getWorldWidth()) / CELL;
    }

    private int rows() {
        return Math.round(viewport.getWorldHeight()) / CELL;
    }

    private CellRect dialogArea() {
        return new CellRect(0, CRUMB_ROW + 1, columns(), Math.max(1, rows() - CRUMB_ROW - 1));
    }

    private void draw() {
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int columns = columns();
        int rows = rows();
        TopBar.draw(painter, columns, CRUMB_ROW, crumbs, TopBarStatus.of("", UiPalette.TEXT));
        dialog.draw(painter, columns, rows, dialogArea());
        if (!notice.isEmpty()) {
            int room = Math.max(1, columns - 2 * NOTICE_MARGIN_CELLS);
            painter.text(notice.length() > room ? notice.substring(0, room) : notice,
                NOTICE_MARGIN_CELLS * CELL, (rows - NOTICE_ROWS_FROM_BOTTOM) * CELL, UiPalette.WARN);
        }
        batch.end();
    }

    @Override
    public void hide() {
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
