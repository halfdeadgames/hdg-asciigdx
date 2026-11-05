package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.NewFontInput.Shape;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBarStatus;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

/** The start screen's "new font" flow: an empty page that hosts the name and size dialogs. */
final class NewFontScreen implements WorkbenchScreen {

    private static final String TAG = "NewFont";
    private static final int CELL = Painter.TEXT_SIZE;
    private static final int CRUMB_ROW = 0;
    private static final int NOTICE_MARGIN_CELLS = 2;
    private static final int NOTICE_ROWS_FROM_BOTTOM = 3;

    private final Path fontsRoot;
    private final Runnable onBack;
    private final BiConsumer<Path, Integer> onCreated;
    private final NewFontDialogs dialogs = new NewFontDialogs(this::existingFolders, this::create, this::cancel);
    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();
    private final DevCapture devCapture = new DevCapture();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private int wheelTicks;
    private String notice = "";

    /**
     * @param fontsRoot the folder that holds all font folders, or null when none is set
     * @param onBack called when the user cancels on the name page
     * @param onCreated called with the new font folder and the height of its first size file
     */
    NewFontScreen(Path fontsRoot, Runnable onBack, BiConsumer<Path, Integer> onCreated) {
        this.fontsRoot = fontsRoot;
        this.onBack = onBack;
        this.onCreated = onCreated;
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
            public boolean scrolled(float amountX, float amountY) {
                wheelTicks += amountY > 0 ? 1 : amountY < 0 ? -1 : 0;
                return true;
            }

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
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        int pointerX = (int) Math.floor(pointer.x);
        int pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        boolean clicked = Gdx.input.justTouched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        int wheel = wheelTicks;
        wheelTicks = 0;
        dialogs.handleInput(Gdx.graphics.getDeltaTime(), wheel, clicked, pointerX, pointerY, CELL, dialogArea());
        draw();
        devCapture.afterRender();
    }

    private int columns() {
        return Math.round(viewport.getWorldWidth()) / CELL;
    }

    private int rows() {
        return Math.round(viewport.getWorldHeight()) / CELL;
    }

    // Everything below the breadcrumb row.
    private ReviewLayout.Rect dialogArea() {
        return new ReviewLayout.Rect(0, CRUMB_ROW + 1, columns(), Math.max(1, rows() - CRUMB_ROW - 1));
    }

    private void create(String name, Shape shape, int height) {
        notice = "";
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(fontsRoot, name, shape, height);
        if (outcome.created()) {
            dialogs.close();
            onCreated.accept(outcome.folder(), height);
            return;
        }
        notice = outcome.problem();
        if (outcome.nameProblem()) {
            dialogs.backToName();
        } else {
            Gdx.app.error(TAG, outcome.problem());
        }
    }

    private List<String> existingFolders() {
        return NewFontDialogs.foldersIn(fontsRoot);
    }

    private void cancel() {
        onBack.run();
    }

    private void draw() {
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int columns = columns();
        int rows = rows();
        TopBar.draw(painter, columns, CRUMB_ROW, List.of("GLYPH WORKBENCH", "NEW FONT"),
            TopBarStatus.of("", UiPalette.TEXT));
        dialogs.draw(painter, columns, rows, dialogArea());
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
