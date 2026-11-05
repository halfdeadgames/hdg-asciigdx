package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.NewFontInput.Shape;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollBarHit;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
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
        void create(String name, Shape shape, int height);
    }

    private static final String TAG = "NewFont";
    private static final String TITLE = "NEW FONT";
    // Typed characters of the key that opened the dialog can arrive a frame or two after it.
    private static final int OPENING_SWALLOW_FRAMES = 2;
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
    // The list highlight follows the pointer only after it moves, so arrow keys are not undone by a resting pointer.
    private boolean lastPointerKnown;
    private int lastPointerX;
    private int lastPointerY;
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

    /** Typed characters go to the name page only. */
    void type(char character) {
        if (swallowFrames == 0) {
            namePage.type(character);
        }
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
            for (int i = 0; i < sizeRequest.moves(); i++) {
                sizePage.move(1);
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
        lastPointerKnown = false;
    }

    private void continueToSize(String name) {
        namePage.close();
        repeatBackspace.update(false, 0f);
        openSize(name);
    }

    private void cancel() {
        close();
        onCancel.run();
    }

    private void chooseSize(Shape shape, int height) {
        onCreate.create(sizePage.name(), shape, height);
    }

    /**
     * Handles this frame's keys and mouse. Enter and Esc return right away so the key does not also act on
     * what lies beneath in the same frame.
     *
     * @param pointer this frame's wheel, click, and pointer position (pixels from the left and top)
     * @param area the cell rectangle the dialog is centred in
     */
    void handleInput(float delta, NewFontPointer pointer, ReviewLayout.Rect area) {
        swallowFrames = Math.max(0, swallowFrames - 1);
        if (namePage.isOpen()) {
            handleName(delta);
        } else if (sizePage.isOpen()) {
            handleSize(pointer, area);
        }
    }

    private void handleName(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            namePage.cancel();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            namePage.enter();
            return;
        }
        int backspaces = repeatBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            namePage.backspace();
        }
    }

    private void handleSize(NewFontPointer pointer, ReviewLayout.Rect area) {
        if (handleSizeKeys()) {
            return;
        }
        Dialog.Layout layout = sizeLayout(area);
        sizePage.wheel(pointer.wheelTicks());
        int listTop = layout.textY() + NewFontSizePrompt.LINES_ABOVE_LIST;
        SizeListHit hit = SizeListHit.of(layout, pointer.pointerX(), pointer.pointerY(), pointer.cell(), listTop,
            sizePage.shownRows(), sizePage.window().offset(), sizePage.scrolls());
        trackSizeHover(pointer.pointerX(), pointer.pointerY(), hit);
        if (pointer.clicked()) {
            handleSizeClick(hit, listTop);
        }
    }

    /** @return whether Esc or Enter consumed the frame */
    private boolean handleSizeKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            sizePage.back();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            sizePage.enter();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            sizePage.move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            sizePage.move(1);
        }
        return false;
    }

    private void trackSizeHover(int pointerX, int pointerY, SizeListHit hit) {
        if (!lastPointerKnown) {
            lastPointerKnown = true;
            lastPointerX = pointerX;
            lastPointerY = pointerY;
            return;
        }
        if (pointerX == lastPointerX && pointerY == lastPointerY) {
            return;
        }
        lastPointerX = pointerX;
        lastPointerY = pointerY;
        sizePage.hover(hit.row());
    }

    private void handleSizeClick(SizeListHit hit, int listTop) {
        if (!hit.inside()) {
            sizePage.back();
            return;
        }
        if (hit.onBar()) {
            int barCells = sizePage.shownRows() * SelectList.PITCH - 1;
            ScrollBarHit.click(sizePage.window(),
                ScrollBarHit.part(sizePage.window(), hit.cellRow() - listTop, barCells));
            return;
        }
        sizePage.pick(hit.row());
    }

    private Dialog.Layout sizeLayout(ReviewLayout.Rect area) {
        sizePage.fitTo(area.height());
        return Dialog.layout(DialogContent.of(TITLE, sizePage.lines(), SIZE_BUTTONS),
            area.toCellRect());
    }

    /** Draws the open page, dimming the whole screen first. */
    void draw(Painter painter, int columns, int rows, ReviewLayout.Rect area) {
        if (namePage.isOpen()) {
            Dialog.draw(painter, new CellRect(0, 0, columns, rows),
                new DialogContent(TITLE, namePage.lines(),
                    namePage.error() == null ? NAME_BUTTONS : NAME_BLOCKED_BUTTONS,
                    0, NewFontNamePrompt.LINE_CELLS),
                area.toCellRect());
        } else if (sizePage.isOpen()) {
            Dialog.Layout list = sizeLayout(area);
            Dialog.draw(painter, new CellRect(0, 0, columns, rows),
                DialogContent.of(TITLE, sizePage.lines(), SIZE_BUTTONS),
                area.toCellRect());
            int listTop = list.textY() + NewFontSizePrompt.LINES_ABOVE_LIST;
            List<SelectList.Row> visibleRows = sizePage.visibleRows();
            SelectList.draw(painter,
                SelectList.areaOf(list.textX(), listTop, Math.min(sizePage.listWidth(), list.textWidth()),
                    visibleRows.size()),
                visibleRows, sizePage.visibleSelectedIndex());
            if (sizePage.scrolls()) {
                drawSizeBar(painter, list.textX() + list.textWidth() - 1, listTop);
            }
        }
    }

    // The bar sits in the last text column beside the list: arrows on the first and last cell, track between.
    private void drawSizeBar(Painter painter, int column, int listTop) {
        int cell = painter.cell();
        ScrollState window = sizePage.window();
        int barCells = sizePage.shownRows() * SelectList.PITCH - 1;
        int trackCells = barCells - 2;
        int x = column * cell;
        painter.text("▲", x, listTop * cell, UiPalette.FRAME);
        painter.text("▼", x, (listTop + barCells - 1) * cell, UiPalette.FRAME);
        int thumbStart = window.thumbStart(trackCells);
        int thumbEnd = thumbStart + window.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            boolean thumb = i >= thumbStart && i < thumbEnd;
            painter.text(thumb ? "█" : "░", x, (listTop + 1 + i) * cell, thumb ? UiPalette.FOCUS : UiPalette.FRAME);
        }
    }
}
