package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * The two pages of the "new font" dialog (name, then first size) with their input handling and drawing.
 * The paint screen and the start screen both host it; the host decides what creating the font means.
 */
final class NewFontDialogs {

    /** Called when Enter is pressed on a size. The host creates the font, then closes this or reports a problem. */
    interface Creator {
        void create(String name, CellSize size);
    }

    private static final String TAG = "NewFont";
    private static final String TITLE = "NEW FONT";
    // Typed characters of the key that opened the dialog can arrive a frame or two after it.
    private static final int OPENING_SWALLOW_FRAMES = 2;
    // The hint starts after the cursor cell of the empty field.
    private static final int HINT_COLUMN = 1;
    private static final List<Dialog.Button> NAME_BUTTONS = List.of(
        new Dialog.Button("ENTER", "NEXT", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> NAME_BLOCKED_BUTTONS = List.of(
        new Dialog.Button("ENTER", "NEXT", Dialog.Tone.DIM), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> SIZE_BUTTONS = List.of(
        new Dialog.Button("ENTER", "CREATE", Dialog.Tone.OK), new Dialog.Button("ESC", "BACK", Dialog.Tone.DIM));

    private final Supplier<List<String>> existingFolders;
    private final Creator onCreate;
    private final Runnable onCancel;
    private final NewFontNamePrompt namePage = new NewFontNamePrompt(this::continueToSize, this::cancel);
    private final NewFontSizePrompt sizePage = new NewFontSizePrompt(this::chooseSize, this::backToName);
    private final KeyRepeat repeatBackspace = new KeyRepeat();
    private int swallowFrames;

    /**
     * @param existingFolders the font folder names already taken, read each time the name page opens
     * @param onCreate called when a size is chosen
     * @param onCancel called after Esc closed the name page
     */
    NewFontDialogs(Supplier<List<String>> existingFolders, Creator onCreate, Runnable onCancel) {
        this.existingFolders = existingFolders;
        this.onCreate = onCreate;
        this.onCancel = onCancel;
    }

    /** The names of the folders in {@code fontsRoot}; empty (and logged) when it cannot be listed. */
    static List<String> foldersIn(Path fontsRoot) {
        if (fontsRoot == null) {
            return List.of();
        }
        try {
            return NewFontPlan.folderNames(fontsRoot);
        } catch (IOException e) {
            Gdx.app.error(TAG, "Cannot list font folders: " + e.getMessage());
            return List.of();
        }
    }

    boolean isOpen() {
        return namePage.isOpen() || sizePage.isOpen();
    }

    /** Opens the name page empty and ignores typed characters for the first frames. */
    void open() {
        swallowFrames = OPENING_SWALLOW_FRAMES;
        openName("");
    }

    void close() {
        namePage.close();
        sizePage.close();
        repeatBackspace.update(false, 0f);
    }

    /** Returns from the size page to the name page with the name kept, and re-reads the taken names. */
    void backToName() {
        String name = sizePage.name();
        sizePage.close();
        openName(name);
    }

    /** Shows a creation problem inside the size page. */
    void showSizeProblem(String message) {
        sizePage.showProblem(message);
    }

    /** Typed characters go to whichever page is open. */
    void type(char character) {
        if (swallowFrames > 0) {
            return;
        }
        namePage.type(character);
        sizePage.type(character);
    }

    /**
     * Opens the page the dev hooks {@code workbench.newfont} and {@code workbench.newfontsize} ask for;
     * does nothing when no hook asked (see {@link #isCaptureRequested}).
     */
    void openForCapture() {
        String typed = DevCapture.newFontRequest();
        DevCapture.NewFontSizeRequest sizeRequest = DevCapture.newFontSizeRequest();
        if (typed != null) {
            openName("");
            for (int i = 0; i < typed.length(); i++) {
                namePage.type(typed.charAt(i));
            }
            return;
        }
        if (sizeRequest != null) {
            openSize(sizeRequest.name());
            for (int i = 0; i < sizeRequest.typed().length(); i++) {
                sizePage.type(sizeRequest.typed().charAt(i));
            }
        }
    }

    /** Whether a dev hook ({@code workbench.newfont} or {@code workbench.newfontsize}) asks for a page to open. */
    boolean isCaptureRequested() {
        return DevCapture.newFontRequest() != null || DevCapture.newFontSizeRequest() != null;
    }

    private void openName(String initialText) {
        namePage.open(existingFolders.get(), initialText);
        repeatBackspace.hold(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE));
    }

    private void openSize(String name) {
        sizePage.open(name);
        repeatBackspace.hold(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE));
    }

    private void continueToSize(String name) {
        namePage.close();
        openSize(name);
    }

    private void cancel() {
        close();
        onCancel.run();
    }

    private void chooseSize(CellSize size) {
        onCreate.create(sizePage.name(), size);
    }

    /**
     * Handles this frame's keys. Enter and Esc return right away so the key does not also act on
     * what lies beneath in the same frame.
     */
    void handleInput(float delta) {
        swallowFrames = Math.max(0, swallowFrames - 1);
        if (namePage.isOpen()) {
            handleName(delta);
        } else if (sizePage.isOpen()) {
            handleSize(delta);
        }
    }

    private void handleName(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            namePage.cancel();
            return;
        }
        if (isEnterJustPressed()) {
            namePage.enter();
            return;
        }
        repeatBackspaces(delta, namePage::backspace);
    }

    private void handleSize(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            sizePage.back();
            return;
        }
        if (isEnterJustPressed()) {
            sizePage.enter();
            return;
        }
        repeatBackspaces(delta, sizePage::backspace);
    }

    private static boolean isEnterJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
    }

    private void repeatBackspaces(float delta, Runnable backspace) {
        int backspaces = repeatBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            backspace.run();
        }
    }

    private DialogContent sizeContent() {
        return new DialogContent(TITLE, sizePage.lines(), SIZE_BUTTONS,
            NewFontSizePrompt.INPUT_LINE, NewFontSizePrompt.LINE_CELLS);
    }

    // The dialog draws line text in one colour, so the grey example is laid over the blank field after it.
    private void drawSizeHint(Painter painter, CellRect area) {
        drawSizeHint(painter, sizePage, Dialog.layout(sizeContent(), area));
    }

    static void drawSizeHint(Painter painter, NewFontSizePrompt page, Dialog.Layout layout) {
        if (!page.showsHint()) {
            return;
        }
        int cell = painter.cell();
        painter.text(NewFontSizePrompt.HINT, (layout.textX() + HINT_COLUMN) * cell,
            (layout.textY() + NewFontSizePrompt.INPUT_LINE) * cell, UiPalette.INACTIVE);
    }

    /** Draws the open page, dimming the whole screen first. */
    void draw(Painter painter, int columns, int rows, CellRect area) {
        if (namePage.isOpen()) {
            Dialog.draw(painter, new CellRect(0, 0, columns, rows),
                new DialogContent(TITLE, namePage.lines(),
                    namePage.error() == null ? NAME_BUTTONS : NAME_BLOCKED_BUTTONS,
                    0, NewFontNamePrompt.LINE_CELLS),
                area);
        } else if (sizePage.isOpen()) {
            Dialog.draw(painter, new CellRect(0, 0, columns, rows), sizeContent(), area);
            drawSizeHint(painter, area);
        }
    }
}
