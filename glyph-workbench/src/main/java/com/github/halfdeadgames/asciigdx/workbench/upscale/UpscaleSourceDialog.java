package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListPointer;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListPointer.Click;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListView;
import com.github.halfdeadgames.asciigdx.workbench.ui.ListHit;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.util.List;
import java.util.function.Consumer;

/** The "upscale from" dialog: source fonts grouped by folder, with its input handling and drawing. */
public final class UpscaleSourceDialog {

    private static final String TITLE = "UPSCALE FROM";
    private static final List<Dialog.Button> CHOOSE_BUTTONS = List.of(
        new Dialog.Button("ENTER", "CHOOSE", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> CLOSE_BUTTONS = List.of(
        new Dialog.Button("ENTER", "CLOSE", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private final UpscaleSourceList sources = new UpscaleSourceList();
    private final GroupedPickListPointer pointer = new GroupedPickListPointer();
    private final Consumer<UpscaleSources.Option> onChoose;

    /** @param onChoose called with the chosen source after the dialog closes */
    public UpscaleSourceDialog(Consumer<UpscaleSources.Option> onChoose) {
        this.onChoose = onChoose;
    }

    public boolean isOpen() {
        return sources.isOpen();
    }

    public void open(UpscaleSources.Font target, List<UpscaleSources.Font> fonts) {
        sources.open(target, fonts);
        pointer.reset();
    }

    public void close() {
        sources.close();
    }

    public void moveForCapture(int moves) {
        for (int i = 0; i < moves; i++) {
            sources.move(1);
        }
    }

    /**
     * Handles this frame's keys and mouse. Enter and Esc return right away so the key does not also act on
     * what lies beneath in the same frame.
     *
     * @param wheelTicks wheel notches this frame
     * @param pointerX pointer position in pixels from the left
     * @param pointerY pointer position in pixels from the top
     * @param cell the cell size in pixels
     * @param area the cell rectangle the dialog is centred in
     */
    public void handleInput(int wheelTicks, boolean clicked, int pointerX, int pointerY, int cell, CellRect area) {
        if (!isOpen() || handleKeys()) {
            return;
        }
        Dialog.Layout layout = fittedLayout(area);
        if (!sources.hasChoices()) {
            closeOnOutsideClick(layout, clicked, pointerX, pointerY, cell);
            return;
        }
        sources.list().wheel(wheelTicks);
        int listTop = layout.textY() + UpscaleSourceList.LINES_ABOVE_LIST;
        ListHit hit = ListHit.of(layout, pointerX, pointerY, cell, listTop, sources.list().shownRows(),
            sources.list().window().offset(), sources.list().scrolls());
        applyClick(pointer.handle(sources.list(), hit, pointerX, pointerY, clicked, listTop));
    }

    /** Draws the dialog, dimming the whole screen first. */
    public void draw(Painter painter, int columns, int rows, CellRect area) {
        Dialog.Layout layout = fittedLayout(area);
        Dialog.draw(painter, new CellRect(0, 0, columns, rows), content(), area);
        if (!sources.hasChoices()) {
            return;
        }
        int listTop = layout.textY() + UpscaleSourceList.LINES_ABOVE_LIST;
        GroupedPickListView.draw(painter, sources.list(), layout.textX(), listTop, layout.textWidth());
    }

    /** @return whether Esc or Enter consumed the frame */
    private boolean handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            close();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            chooseSelected();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            sources.move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            sources.move(1);
        }
        return false;
    }

    private void applyClick(Click click) {
        if (click == Click.OUTSIDE) {
            close();
        } else if (click == Click.PICK) {
            chooseSelected();
        }
    }

    private void chooseSelected() {
        UpscaleSources.Option chosen = sources.selected();
        close();
        if (chosen != null) {
            onChoose.accept(chosen);
        }
    }

    private void closeOnOutsideClick(Dialog.Layout layout, boolean clicked, int pointerX, int pointerY, int cell) {
        if (clicked && !ListHit.of(layout, pointerX, pointerY, cell, 0, 0, 0, false).inside()) {
            close();
        }
    }

    private Dialog.Layout fittedLayout(CellRect area) {
        sources.fitTo(area.height());
        return Dialog.layout(content(), area);
    }

    private DialogContent content() {
        return DialogContent.of(TITLE, sources.lines(), sources.hasChoices() ? CHOOSE_BUTTONS : CLOSE_BUTTONS);
    }
}
