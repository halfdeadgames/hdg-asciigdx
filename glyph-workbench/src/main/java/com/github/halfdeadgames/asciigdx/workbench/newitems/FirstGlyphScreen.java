package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.WorkbenchScreen;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
import com.github.halfdeadgames.asciigdx.workbench.ui.NoticeLine;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScreenKit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Shown instead of the paint screen for a font with no glyph slots: asks for the first glyph and lists it. */
public final class FirstGlyphScreen implements WorkbenchScreen {

    private static final String TAG = "FirstGlyph";

    private final Path fontFolder;
    private final Runnable onBack;
    private final Runnable onAdded;
    private final NewItemPage page;
    private final NewGlyphDialog dialog = new NewGlyphDialog(this::addToList, codePoint -> false);
    private final ScreenKit kit = new ScreenKit();
    private final DevCapture devCapture = new DevCapture();

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
        this.page = new NewItemPage(List.of("GLYPH WORKBENCH",
            fontFolder.getFileName().toString().toUpperCase(Locale.ROOT),
            size.toString().toUpperCase(Locale.ROOT), "FIRST GLYPH"));
    }

    @Override
    public void show() {
        kit.create();
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
        kit.resize(width, height);
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

    private void draw() {
        Painter painter = kit.painter();
        kit.beginFrame();
        int columns = kit.columns();
        int rows = kit.rows();
        page.drawTopBar(painter, columns);
        dialog.draw(painter, columns, rows, NewItemPage.dialogArea(columns, rows));
        NoticeLine.draw(painter, notice, columns, rows);
        kit.endFrame();
    }

    @Override
    public void hide() {
        kit.dispose();
    }
}
