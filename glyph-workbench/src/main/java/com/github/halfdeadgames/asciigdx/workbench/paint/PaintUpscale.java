package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.ui.ArrowRepeats;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscalePick;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSourceDialog;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSourceSheet;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * The upscale mode of the paint screen: first the source font dialog, then a pick of one glyph on that font's
 * sheet, which takes the place of the main sheet. While either stage runs, nothing reaches painting.
 */
final class PaintUpscale {

    private static final int WHEEL_ROWS = 3;

    /** What the upscale mode needs from the screen it is shown on. */
    interface Host {

        /** A sheet view over another font's editor, placed like the main sheet. */
        PaintSheetView createSheetView(FontEditor sourceEditor);

        /** The font being painted. */
        UpscaleSources.Font targetFont();

        /** Every font that could be offered as a source. */
        List<UpscaleSources.Font> sourceFonts();

        Path fontsRoot();

        /** The code point of the glyph being painted. */
        int targetCodePoint();

        void endStroke();

        int pointerX();

        int pointerY();

        boolean leftClicked();

        int cell();

        /** The cell rectangle dialogs are centred in. */
        CellRect dialogArea();
    }

    private final UpscaleSourceDialog dialog = new UpscaleSourceDialog(this::chooseSource);
    private final StringBuilder line = new StringBuilder();
    private final MessageStrip messages;
    private final ArrowRepeats arrowRepeats;
    private final Host host;

    private UpscaleSources.Font target;
    private List<UpscaleSources.Font> fonts = List.of();
    private UpscaleSources.Option chosen;
    private UpscalePick pick;
    private PaintSheetView view;
    private PointerTargets pointerTargets;
    private boolean layoutPending;
    private boolean revealCursor;

    PaintUpscale(MessageStrip messages, ArrowRepeats arrowRepeats, Host host) {
        this.messages = messages;
        this.arrowRepeats = arrowRepeats;
        this.host = host;
    }

    boolean isActive() {
        return isChoosingFont() || isPicking();
    }

    boolean isChoosingFont() {
        return dialog.isOpen();
    }

    boolean isPicking() {
        return pick != null;
    }

    /** True until the source sheet has been fitted to the window once. */
    boolean needsLayout() {
        return layoutPending;
    }

    void open() {
        host.endStroke();
        target = host.targetFont();
        fonts = host.sourceFonts();
        dialog.open(target, fonts);
    }

    void moveListForCapture(int moves) {
        dialog.moveForCapture(moves);
    }

    void chooseSelectedForCapture() {
        dialog.chooseSelectedForCapture();
    }

    /** Handles one frame of the dialog or of the glyph pick, whichever stage is running. */
    void handleInput(float delta, int verticalTicks, int sidewaysTicks) {
        if (isChoosingFont()) {
            dialog.handleInput(verticalTicks, host.leftClicked(), host.pointerX(), host.pointerY(), host.cell(),
                host.dialogArea());
            return;
        }
        if (isPicking()) {
            handlePicking(delta, verticalTicks, sidewaysTicks);
        }
    }

    void drawDialog(Painter painter, int columns, int rows, CellRect area) {
        if (isChoosingFont()) {
            dialog.draw(painter, columns, rows, area);
        }
    }

    void drawSheet() {
        if (isPicking()) {
            view.draw(pick.cursor());
        }
    }

    /** Fits the source sheet like the main one; its frame is at least as wide as the main sheet's. */
    void layoutSheet(int visibleColumns, int minimumFrameCells, int worldHeight, boolean statusBarShown,
                     boolean revealNow) {
        if (!isPicking()) {
            return;
        }
        view.setVisibleColumns(visibleColumns);
        view.setMinimumFrameCells(minimumFrameCells);
        view.fitToHeight(worldHeight, statusBarShown, pick.cursor(), revealCursor || revealNow);
        revealCursor = false;
        layoutPending = false;
    }

    private void chooseSource(UpscaleSources.Option option) {
        messages.nextAction();
        UpscaleSources.Font font = option.font();
        UpscaleSourceSheet loaded;
        try {
            loaded = UpscaleSourceSheet.load(host.fontsRoot(), font);
        } catch (IOException | FontDataException e) {
            messages.warn("Cannot read " + label(font) + ": " + e.getMessage());
            return;
        }
        int startSlot = loaded.startSlot(host.targetCodePoint());
        if (startSlot < 0) {
            messages.warn(label(font) + " has no glyphs with ink");
            return;
        }
        beginPicking(option, loaded, startSlot);
    }

    private void beginPicking(UpscaleSources.Option option, UpscaleSourceSheet loaded, int startSlot) {
        chosen = option;
        pick = new UpscalePick(loaded, startSlot, PaintSheetView.COLUMNS);
        view = host.createSheetView(loaded.editor());
        view.setTitle(loaded.title(), loaded.shortTitle());
        messages.ok("Pick a glyph from " + label(option.font())
            + ". Enter or click it again to choose, Esc to go back.");
        pointerTargets = new PointerTargets(List.of(new SheetBarPointerTarget(view), new SourceSlotTarget()));
        layoutPending = true;
        revealCursor = true;
        holdHeldArrows();
    }

    // A key still down from the dialog must not count as a fresh press on the sheet.
    private void holdHeldArrows() {
        arrowRepeats.left().hold(Gdx.input.isKeyPressed(Input.Keys.LEFT));
        arrowRepeats.right().hold(Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        arrowRepeats.up().hold(Gdx.input.isKeyPressed(Input.Keys.UP));
        arrowRepeats.down().hold(Gdx.input.isKeyPressed(Input.Keys.DOWN));
    }

    private void handlePicking(float delta, int verticalTicks, int sidewaysTicks) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            backToFontDialog();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            confirm();
            return;
        }
        moveCursor(delta);
        scrollByKeys();
        scrollByWheel(verticalTicks, sidewaysTicks);
        pointerTargets.dispatch(host.pointerX(), host.pointerY(), host.leftClicked(),
            Gdx.input.isButtonPressed(Input.Buttons.LEFT));
    }

    private void moveCursor(float delta) {
        int before = pick.cursor();
        moveWhileHeld(arrowRepeats.left(), Input.Keys.LEFT, delta, -1, 0);
        moveWhileHeld(arrowRepeats.right(), Input.Keys.RIGHT, delta, 1, 0);
        moveWhileHeld(arrowRepeats.up(), Input.Keys.UP, delta, 0, -1);
        moveWhileHeld(arrowRepeats.down(), Input.Keys.DOWN, delta, 0, 1);
        if (pick.cursor() != before) {
            messages.nextAction();
            view.keepVisible(pick.cursor());
        }
    }

    private void moveWhileHeld(KeyRepeat repeat, int key, float delta, int dx, int dy) {
        int steps = repeat.update(Gdx.input.isKeyPressed(key), delta);
        for (int step = 0; step < steps; step++) {
            pick.move(dx, dy);
        }
    }

    private void scrollByKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            view.scroll().pageUp();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            view.scroll().pageDown();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            view.scroll().toTop();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            view.scroll().toBottom();
        }
    }

    // Scrolling the view never moves the cursor.
    private void scrollByWheel(int verticalTicks, int sidewaysTicks) {
        if (!view.contains(host.pointerX(), host.pointerY())) {
            return;
        }
        view.scroll().scrollBy(verticalTicks * WHEEL_ROWS);
        view.columnScroll().scrollBy(sidewaysTicks);
    }

    private void backToFontDialog() {
        messages.nextAction();
        end();
        dialog.open(target, fonts);
    }

    // Stage 5 replaces this message with the candidate step.
    private void confirm() {
        messages.nextAction();
        line.setLength(0);
        line.append("Would upscale U+");
        GlyphLabel.appendHex(line, host.targetCodePoint());
        line.append(" from ").append(label(chosen.font())).append(" U+");
        GlyphLabel.appendHex(line, pick.codePoint());
        line.append(" at ").append(chosen.factor()).append('X');
        messages.ok(line.toString());
        end();
    }

    private void end() {
        pick = null;
        view = null;
        pointerTargets = null;
        chosen = null;
        layoutPending = false;
    }

    private static String label(UpscaleSources.Font font) {
        return font.folderName().toUpperCase(Locale.ROOT) + " " + font.cellWidth() + "X" + font.cellHeight();
    }

    /** A press on a source slot selects it, or confirms the pick when the slot is already selected. */
    private final class SourceSlotTarget implements PointerTarget {

        @Override
        public boolean claimPress(int x, int y) {
            int slot = view.slotAt(x, y);
            if (!pick.sheet().isPickable(slot)) {
                return false;
            }
            if (pick.isConfirmingClick(slot)) {
                confirm();
                return true;
            }
            messages.nextAction();
            pick.select(slot);
            return true;
        }
    }
}
