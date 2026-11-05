package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** The "new font" dialog as the paint screen hosts it: which folders are taken, and what creating the font does. */
final class PaintNewFont {

    private static final String TAG = "Paint";
    private static final String NEW_FONT_QUESTION = "before starting a new font?";

    private final NewFontDialogs dialogs = new NewFontDialogs(this::existingFontFolders, this::createNewFont, () -> { });
    private final Supplier<Path> fontsRoot;
    private final BiConsumer<Runnable, String> runAfterUnsavedCheck;
    private final BiConsumer<Path, Integer> openFont;
    private final MessageStrip messages;

    /**
     * @param fontsRoot the folder that holds the font folders, read each time it is needed
     * @param runAfterUnsavedCheck runs the action now, or after the unsaved-changes prompt with the given question
     * @param openFont called with the folder and first size height of the font just created
     * @param messages the strip that shows warnings
     */
    PaintNewFont(Supplier<Path> fontsRoot, BiConsumer<Runnable, String> runAfterUnsavedCheck,
                 BiConsumer<Path, Integer> openFont, MessageStrip messages) {
        this.fontsRoot = fontsRoot;
        this.runAfterUnsavedCheck = runAfterUnsavedCheck;
        this.openFont = openFont;
        this.messages = messages;
    }

    boolean isOpen() {
        return dialogs.isOpen();
    }

    void open() {
        messages.nextAction();
        dialogs.open();
    }

    void openForCapture() {
        dialogs.openForCapture();
    }

    /** Whether a dev hook asks for a new-font page to open. */
    boolean isCaptureRequested() {
        return dialogs.isCaptureRequested();
    }

    void type(char character) {
        dialogs.type(character);
    }

    void handleInput(float delta, NewFontPointer pointer, CellRect area) {
        dialogs.handleInput(delta, pointer, area);
    }

    void draw(Painter painter, int columns, int rows, CellRect area) {
        dialogs.draw(painter, columns, rows, area);
    }

    // Font folders sit side by side, so the names already taken are the folders next to the current one.
    private List<String> existingFontFolders() {
        return NewFontDialogs.foldersIn(fontsRoot.get());
    }

    // Unsaved changes are asked about after the size is chosen. The dialog stays open (and hidden) under the
    // prompt, so cancelling it returns to the size page with the selection kept.
    private void createNewFont(String name, NewFontInput.Shape shape, int height) {
        runAfterUnsavedCheck.accept(() -> finishNewFont(name, shape, height), NEW_FONT_QUESTION);
    }

    // On success the host opens a fresh paint screen on the new font. A name problem (for example a folder made
    // since the name page opened) goes back to the name page, which shows it; anything else is shown on the size page.
    private void finishNewFont(String name, NewFontInput.Shape shape, int height) {
        messages.nextAction();
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(fontsRoot.get(), name, shape, height);
        if (outcome.created()) {
            dialogs.close();
            openFont.accept(outcome.folder(), height);
            return;
        }
        if (outcome.nameProblem()) {
            dialogs.backToName();
            messages.warn(outcome.problem());
        } else {
            Gdx.app.error(TAG, outcome.problem());
            dialogs.showSizeProblem(outcome.problem());
        }
    }
}
