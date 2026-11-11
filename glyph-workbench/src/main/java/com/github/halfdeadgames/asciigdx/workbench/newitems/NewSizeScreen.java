package com.github.halfdeadgames.asciigdx.workbench.newitems;

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
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.WorkbenchScreen;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBarStatus;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** The start screen's "new size" flow: an empty page that hosts the New size dialog for one font folder. */
public final class NewSizeScreen implements WorkbenchScreen {

    private static final String TAG = "NewSize";
    private static final int CELL = Painter.TEXT_SIZE;
    private static final int CRUMB_ROW = 0;

    private final Path folder;
    private final Runnable onBack;
    private final Consumer<String> onListFailed;
    private final NewSizeDialog dialog;
    private final ScreenViewport viewport = new ScreenViewport();
    private final DevCapture devCapture = new DevCapture();
    private final Vector2 pointer = new Vector2();

    private int pixelX;
    private int pixelY;
    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;

    /**
     * @param folder the font folder that gets the new size file
     * @param onBack called when the dialog closes without a size being chosen
     * @param onChosen called with the chosen size after the dialog has closed
     * @param onListFailed called with the reason when the existing sizes cannot be read
     */
    public NewSizeScreen(Path folder, Runnable onBack, Consumer<CellSize> onChosen, Consumer<String> onListFailed) {
        this.folder = folder;
        this.onBack = onBack;
        this.onListFailed = onListFailed;
        this.dialog = new NewSizeDialog(onChosen, onBack);
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
                dialog.type(character);
                return true;
            }
        });
        openDialog();
    }

    private void openDialog() {
        List<NewSizePlan.SizeOption> options;
        try {
            options = NewSizePlan.options(folder);
        } catch (IOException e) {
            Gdx.app.error(TAG, "Cannot list sizes: " + e.getMessage());
            onListFailed.accept(e.getMessage());
            return;
        }
        updatePointer();
        dialog.open(options, folder.getFileName().toString(), pixelX, pixelY);
        applyCaptureRequest();
    }

    private void applyCaptureRequest() {
        int moves = DevCapture.newSizeRequest();
        if (moves >= 0) {
            dialog.applyCaptureRequest(moves, DevCapture.newSizeOtherRequest());
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        handleInput();
        draw();
        devCapture.afterRender();
    }

    private void handleInput() {
        if (!dialog.isOpen() || dialog.handleKeys(Gdx.graphics.getDeltaTime())) {
            return;
        }
        if (!dialog.isOpen()) {
            return;
        }
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        updatePointer();
        dialog.handlePointer(clicked, pixelX, pixelY, CELL, dialogArea());
    }

    // Window pixels go through the viewport so UI scale and HiDPI are honoured; Y is measured from the top.
    private void updatePointer() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        pixelX = (int) Math.floor(pointer.x);
        pixelY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
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
        TopBar.draw(painter, columns(), CRUMB_ROW,
            List.of("GLYPH WORKBENCH", folder.getFileName().toString().toUpperCase(Locale.ROOT), "NEW SIZE"),
            TopBarStatus.of("", UiPalette.TEXT));
        if (dialog.isOpen()) {
            dialog.draw(painter, columns(), rows(), dialogArea());
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
