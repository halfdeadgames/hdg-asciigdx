package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickList;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListPointer;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListPointer.Click;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListView;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Keys;
import com.github.halfdeadgames.asciigdx.workbench.ui.ListHit;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.util.List;
import java.util.function.Consumer;

/** The "new size" dialog: a list of sizes to add plus a typed page for any other size, with input handling and drawing. */
public final class NewSizeDialog {

    private static final String TITLE = "NEW SIZE";
    // Typed characters of the key that opened the dialog can arrive a frame or two after it.
    private static final int OPENING_SWALLOW_FRAMES = 2;
    private static final List<Dialog.Button> LIST_BUTTONS = List.of(
        new Dialog.Button("ENTER", "CHOOSE", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> TYPED_BUTTONS = List.of(
        new Dialog.Button("ENTER", "CREATE", Dialog.Tone.OK), new Dialog.Button("ESC", "BACK", Dialog.Tone.DIM));

    private final NewSizePrompt prompt;
    private final KeyRepeat repeatBackspace = new KeyRepeat();
    private final Runnable backspacePrompt = this::backspaceInPrompt;
    private final GroupedPickListPointer pointer = new GroupedPickListPointer();
    private int swallowFrames;

    /** @param onChoose called with the chosen size after the dialog closes */
    public NewSizeDialog(Consumer<CellSize> onChoose) {
        prompt = new NewSizePrompt(onChoose);
    }

    /**
     * @param onChoose called with the chosen size after the dialog closes
     * @param onDismiss called after the dialog closes without a size being chosen
     */
    public NewSizeDialog(Consumer<CellSize> onChoose, Runnable onDismiss) {
        prompt = new NewSizePrompt(onChoose, onDismiss);
    }

    public boolean isOpen() {
        return prompt.isOpen();
    }

    /**
     * Opens the list, or straight on the typed page when the font has no sizes.
     *
     * @param folderName the font folder name shown on the typed page
     */
    public void open(List<NewSizePlan.SizeOption> options, String folderName, int pixelX, int pixelY) {
        pointer.prime(pixelX, pixelY);
        swallowFrames = OPENING_SWALLOW_FRAMES;
        prompt.open(options, folderName);
        repeatBackspace.hold(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE));
    }

    /** Applies the {@code workbench.newsize} hook: moves down the list, or goes to OTHER SIZE... and types the text. */
    public void applyCaptureRequest(int moves, String otherTyped) {
        swallowFrames = 0;
        if (otherTyped == null) {
            for (int i = 0; i < moves; i++) {
                prompt.move(1);
            }
            return;
        }
        while (prompt.selectedIndex() < prompt.rowCount() - 1) {
            prompt.move(1);
        }
        prompt.chooseSelected();
        for (int i = 0; i < otherTyped.length(); i++) {
            prompt.type(otherTyped.charAt(i));
        }
    }

    /** Typed characters go to the typed page; the list ignores them. */
    public void type(char character) {
        if (swallowFrames > 0) {
            return;
        }
        prompt.type(character);
    }

    /**
     * Handles Esc, Enter, the arrow keys, and Backspace. Enter or Esc returns right away so the key does not also
     * act on the screen beneath in the same frame.
     *
     * @return whether Esc or Enter consumed the frame
     */
    public boolean handleKeys(float delta) {
        swallowFrames = Math.max(0, swallowFrames - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            prompt.cancel();
            return true;
        }
        if (Keys.enterJustPressed()) {
            prompt.enter();
            return true;
        }
        if (prompt.isTyping()) {
            Keys.repeatBackspace(repeatBackspace, delta, backspacePrompt);
            return false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            prompt.move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            prompt.move(1);
        }
        return false;
    }

    /**
     * Handles the pointer: hover follows it once it moves, a click outside closes the dialog, and a click on a selectable row picks it.
     *
     * @param pixelX pointer position in pixels from the left
     * @param pixelY pointer position in pixels from the top
     * @param cellPixels the cell size in pixels
     * @param area the cell rectangle the dialog is centred in
     */
    public void handlePointer(boolean clicked, int pixelX, int pixelY, int cellPixels, CellRect area) {
        Dialog.Layout layout = fittedLayout(area);
        if (prompt.isTyping()) {
            closeOnOutsideClick(clicked, layout.containsPixel(pixelX, pixelY, cellPixels));
            return;
        }
        GroupedPickList<SizeChoice> list = prompt.list();
        ListHit hit = ListHit.of(layout, pixelX, pixelY, cellPixels, layout.textY(), list.shownRows(),
            list.window().offset(), list.scrolls());
        applyClick(pointer.handle(list, hit, pixelX, pixelY, clicked, layout.textY()));
    }

    public void draw(Painter painter, int columns, int rows, CellRect area) {
        Dialog.Layout layout = fittedLayout(area);
        Dialog.draw(painter, new CellRect(0, 0, columns, rows), content(), area);
        if (prompt.isTyping()) {
            NewFontDialogs.drawSizeHint(painter, prompt.typedPage(), layout);
            return;
        }
        GroupedPickListView.draw(painter, prompt.list(), layout.textX(), layout.textY(), layout.textWidth());
    }

    private void backspaceInPrompt() {
        prompt.backspace();
    }

    private void closeOnOutsideClick(boolean clicked, boolean inside) {
        if (clicked && !inside) {
            prompt.dismiss();
        }
    }

    private void applyClick(Click click) {
        if (click == Click.OUTSIDE) {
            prompt.dismiss();
        } else if (click == Click.PICK) {
            prompt.chooseSelected();
        }
    }

    private Dialog.Layout fittedLayout(CellRect area) {
        prompt.fitTo(area.height());
        return Dialog.layout(content(), area);
    }

    private DialogContent content() {
        if (prompt.isTyping()) {
            return new DialogContent(TITLE, prompt.lines(), TYPED_BUTTONS,
                NewFontSizePrompt.INPUT_LINE, NewFontSizePrompt.LINE_CELLS);
        }
        return DialogContent.of(TITLE, prompt.lines(), LIST_BUTTONS);
    }
}
