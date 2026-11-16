package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.WorkbenchScreen;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScreenKit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The start screen's "new size" flow: an empty page that hosts the New size dialog for one font
 * folder.
 */
public final class NewSizeScreen implements WorkbenchScreen {

    private static final String TAG = "NewSize";
    private static final int CELL = Painter.TEXT_SIZE;

    private final Path folder;
    private final NewItemPage page;
    private final Runnable onBack;
    private final Consumer<String> onListFailed;
    private final NewSizeDialog dialog;
    private final ScreenKit kit = new ScreenKit();
    private final DevCapture devCapture = new DevCapture();

    /**
     * @param folder the font folder that gets the new size file
     * @param onBack called when the dialog closes without a size being chosen
     * @param onChosen called with the chosen size after the dialog has closed
     * @param onListFailed called with the reason when the existing sizes cannot be read
     */
    public NewSizeScreen(Path folder, Runnable onBack, Consumer<CellSize> onChosen,
            Consumer<String> onListFailed) {
        this.folder = folder;
        this.page = new NewItemPage(List.of("GLYPH WORKBENCH",
                folder.getFileName().toString().toUpperCase(Locale.ROOT), "NEW SIZE"));
        this.onBack = onBack;
        this.onListFailed = onListFailed;
        this.dialog = new NewSizeDialog(onChosen, onBack);
    }

    @Override
    public void show() {
        kit.create();
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
        kit.updatePointer();
        dialog.open(options, folder.getFileName().toString(), kit.pointerX(), kit.pointerY());
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
        kit.resize(width, height);
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
        kit.updatePointer();
        dialog.handlePointer(clicked, kit.pointerX(), kit.pointerY(), CELL, dialogArea());
    }

    private CellRect dialogArea() {
        return NewItemPage.dialogArea(kit.columns(), kit.rows());
    }

    private void draw() {
        Painter painter = kit.painter();
        kit.beginFrame();
        page.drawTopBar(painter, kit.columns());
        if (dialog.isOpen()) {
            dialog.draw(painter, kit.columns(), kit.rows(), dialogArea());
        }
        kit.endFrame();
    }

    @Override
    public void hide() {
        kit.dispose();
    }
}
