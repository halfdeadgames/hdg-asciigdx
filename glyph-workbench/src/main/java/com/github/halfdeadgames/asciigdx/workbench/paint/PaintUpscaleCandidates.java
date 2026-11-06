package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.help.PaintHelp;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.upscale.GlyphCandidates;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleCandidatePick;
import java.util.List;

/**
 * The last stage of the upscale mode: choosing one upscaled version of the picked source glyph and applying it
 * to the glyph being painted. While it runs, the grid and the preview show the selected version.
 */
final class PaintUpscaleCandidates {

    private static final int MOST_NUMBERED_CANDIDATES = 9;
    private static final List<Dialog.Button> REPLACE_BUTTONS = List.of(
        new Dialog.Button("R", "REPLACE", Dialog.Tone.WARN), new Dialog.Button("ESC", "BACK", Dialog.Tone.DIM));

    /** What this stage needs from the upscale mode it belongs to. */
    interface Host {

        /** The editor of the font being painted. */
        FontEditor targetEditor();

        /** The code point of the glyph being painted. */
        int targetCodePoint();

        /** Stores the edit problem the screen compares against to spot a newly rejected edit. */
        void rememberProblem(String problem);

        /** Scrolls the main sheet so the painted glyph is shown. */
        void keepTargetVisible();
    }

    private final StringBuilder line = new StringBuilder();
    private final MessageStrip messages;
    private final Host host;
    private final Runnable onBack;
    private final Runnable onApplied;

    private UpscaleCandidatePick pick;
    private List<String> labels = List.of();
    private String sourceLabel = "";
    private PixelGlyph shown;
    private boolean confirmingReplace;

    PaintUpscaleCandidates(MessageStrip messages, Host host, Runnable onBack, Runnable onApplied) {
        this.messages = messages;
        this.host = host;
        this.onBack = onBack;
        this.onApplied = onApplied;
    }

    boolean isChoosing() {
        return pick != null;
    }

    boolean isConfirmingReplace() {
        return confirmingReplace;
    }

    /** The glyph to show in place of the painted one, or null outside this stage. */
    PixelGlyph previewGlyph() {
        return shown;
    }

    PaintHelp.State helpState() {
        return PaintHelp.State.upscaleChoosing(labels, pick.selectedIndex());
    }

    /** @param sourceLabel the source font name used in the done message */
    void begin(String sourceLabel, List<GlyphCandidates.Candidate> candidates) {
        this.sourceLabel = sourceLabel;
        pick = new UpscaleCandidatePick(candidates);
        labels = pick.labels();
        confirmingReplace = false;
        refreshShown();
    }

    void clear() {
        pick = null;
        shown = null;
        labels = List.of();
        confirmingReplace = false;
    }

    void selectNumber(int number) {
        pick.selectNumber(number);
        refreshShown();
    }

    void handleInput() {
        if (confirmingReplace) {
            handleReplacePrompt();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            onBack.run();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            apply();
            return;
        }
        handleSelectionKeys();
    }

    void drawReplaceDialog(Painter painter, int columns, int rows, CellRect area) {
        if (!confirmingReplace) {
            return;
        }
        Dialog.draw(painter, new CellRect(0, 0, columns, rows),
            DialogContent.of("REPLACE DRAWING",
                List.of("This glyph already holds a drawing.", "Upscaling replaces it.", "Undo restores it."),
                REPLACE_BUTTONS),
            area);
    }

    private void handleReplacePrompt() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            replaceTarget();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            confirmingReplace = false;
        }
    }

    private void handleSelectionKeys() {
        for (int number = 1; number <= MOST_NUMBERED_CANDIDATES; number++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + number - 1)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + number - 1)) {
                messages.nextAction();
                selectNumber(number);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            move(1);
        }
    }

    private void move(int step) {
        messages.nextAction();
        pick.move(step);
        refreshShown();
    }

    private void refreshShown() {
        shown = new PixelGlyph(host.targetCodePoint(), true, pick.selected().cell());
    }

    private void apply() {
        messages.nextAction();
        if (host.targetEditor().wouldReplaceDrawing(host.targetCodePoint())) {
            confirmingReplace = true;
            return;
        }
        replaceTarget();
    }

    private void replaceTarget() {
        messages.nextAction();
        confirmingReplace = false;
        FontEditor editor = host.targetEditor();
        GlyphCandidates.Candidate candidate = pick.selected();
        editor.replaceGlyph(host.targetCodePoint(), candidate.full(), candidate.cell());
        if (!editor.problem().isEmpty()) {
            host.rememberProblem(editor.problem());
            messages.warn(editor.problem());
            return;
        }
        host.rememberProblem("");
        host.keepTargetVisible();
        messages.ok(doneMessage(candidate.label()));
        onApplied.run();
    }

    private String doneMessage(String candidateLabel) {
        line.setLength(0);
        line.append("Upscaled U+");
        GlyphLabel.appendHex(line, host.targetCodePoint());
        line.append(" from ").append(sourceLabel).append(" with ").append(candidateLabel);
        return line.toString();
    }
}
