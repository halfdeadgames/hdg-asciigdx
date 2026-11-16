package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.WorkbenchScreen;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.NoticeLine;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScreenKit;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The start screen's "new font" flow: an empty page that hosts the name, size, and glyph-set
 * dialogs.
 */
public final class NewFontScreen implements WorkbenchScreen {

    private static final String TAG = "NewFont";

    private final Path fontsRoot;
    private final Runnable onBack;
    private final BiConsumer<Path, CellSize> onCreated;
    private final NewFontDialogs dialogs =
            new NewFontDialogs(this::existingFolders, this::create, this::cancel);
    private final NewItemPage page = new NewItemPage(List.of("GLYPH WORKBENCH", "NEW FONT"));
    private final ScreenKit kit = new ScreenKit();
    private final DevCapture devCapture = new DevCapture();

    private String notice = "";

    /**
     * @param fontsRoot the folder that holds all font folders, or null when none is set
     * @param onBack called when the user cancels on the name page
     * @param onCreated called with the new font folder and the cell size of its first size file
     */
    public NewFontScreen(Path fontsRoot, Runnable onBack, BiConsumer<Path, CellSize> onCreated) {
        this.fontsRoot = fontsRoot;
        this.onBack = onBack;
        this.onCreated = onCreated;
    }

    @Override
    public void show() {
        kit.create();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                notice = "";
                dialogs.type(character);
                return true;
            }
        });
        if (dialogs.isCaptureRequested()) {
            dialogs.openForCapture();
        } else {
            dialogs.open();
        }
    }

    @Override
    public void resize(int width, int height) {
        kit.resize(width, height);
    }

    @Override
    public void render() {
        dialogs.handleInput(Gdx.graphics.getDeltaTime());
        draw();
        devCapture.afterRender();
    }

    private void create(String name, CellSize size, NewFontGlyphs glyphs) {
        notice = "";
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(fontsRoot, name, size, glyphs);
        if (outcome.created()) {
            dialogs.close();
            onCreated.accept(outcome.folder(), size);
            return;
        }
        if (outcome.nameProblem()) {
            notice = outcome.problem();
            dialogs.backToName();
            return;
        }
        Gdx.app.error(TAG, outcome.problem());
        dialogs.showGlyphsProblem(outcome.problem());
    }

    private List<String> existingFolders() {
        return NewFontDialogs.foldersIn(fontsRoot);
    }

    private void cancel() {
        onBack.run();
    }

    private void draw() {
        Painter painter = kit.painter();
        kit.beginFrame();
        int columns = kit.columns();
        int rows = kit.rows();
        page.drawTopBar(painter, columns);
        dialogs.draw(painter, columns, rows, NewItemPage.dialogArea(columns, rows));
        NoticeLine.draw(painter, notice, columns, rows);
        kit.endFrame();
    }

    @Override
    public void hide() {
        kit.dispose();
    }
}
