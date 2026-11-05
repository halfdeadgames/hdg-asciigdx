package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/** The "new glyph" dialog: a typed character to add or go to, with its input handling and drawing. */
final class NewGlyphDialog {

    private static final String TITLE = "NEW GLYPH";
    // The N that opens the prompt can also arrive as a typed character a frame later.
    private static final int KEY_SWALLOW_FRAMES = 2;
    private static final List<Dialog.Button> BUTTONS = List.of(
        new Dialog.Button("ENTER", "ADD", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> GO_TO_BUTTONS = List.of(
        new Dialog.Button("ENTER", "GO TO", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private final NewGlyphPrompt prompt = new NewGlyphPrompt();
    private final KeyRepeat repeatBackspace = new KeyRepeat();
    private final IntConsumer onAdd;
    private final IntPredicate slotExists;
    private int keySwallowFrames;

    /**
     * @param onAdd called with the code point to add, before the dialog closes
     * @param slotExists whether a glyph slot already exists for a code point
     */
    NewGlyphDialog(IntConsumer onAdd, IntPredicate slotExists) {
        this.onAdd = onAdd;
        this.slotExists = slotExists;
    }

    boolean isOpen() {
        return prompt.isOpen();
    }

    void open() {
        prompt.open();
        repeatBackspace.hold(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE));
    }

    void swallowOpeningKey() {
        keySwallowFrames = KEY_SWALLOW_FRAMES;
    }

    void type(char character) {
        boolean openingKey = character == 'n' || character == 'N';
        if (keySwallowFrames > 0 && openingKey) {
            return;
        }
        prompt.type(character);
    }

    void typeForCapture(String characters) {
        for (int i = 0; i < characters.length(); i++) {
            prompt.type(characters.charAt(i));
        }
    }

    // Typed characters arrive through keyTyped. Returning right after Enter or Esc keeps that key from also
    // acting on the screen beneath in the same frame.
    void handle(float delta) {
        keySwallowFrames = Math.max(0, keySwallowFrames - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            close();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            addIfValid();
            return;
        }
        repeatBackspaces(delta);
    }

    void draw(Painter painter, int columns, int rows, CellRect area) {
        Dialog.draw(painter, new CellRect(0, 0, columns, rows),
            new DialogContent(TITLE, prompt.lines(slotExists),
                prompt.goesToExisting(slotExists) ? GO_TO_BUTTONS : BUTTONS,
                0, NewGlyphPrompt.LINE_CELLS),
            area);
    }

    private void addIfValid() {
        NewGlyphInput.Result result = prompt.result();
        if (!result.isValid()) {
            return;
        }
        onAdd.accept(result.codePoint());
        close();
    }

    private void repeatBackspaces(float delta) {
        int backspaces = repeatBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            prompt.backspace();
        }
    }

    private void close() {
        prompt.close();
        repeatBackspace.update(false, 0f);
    }
}
