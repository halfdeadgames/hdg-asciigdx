package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;

/** The derive actions of the paint screen: opening the menu, picking sources, applying, and cancelling. */
final class PaintDerive {

    private final DeriveFlow flow;
    private final PaintDerivePick derivePick;
    private final PaintDeriveMenu deriveMenu;
    private final FontEditor editor;
    private final MessageStrip messages;
    private final Host host;

    /** What the derive actions need from the screen they are shown on. */
    interface Host {

        /** The code point of every sheet slot. */
        int[] codePoints();

        /** The slot the cursor is on. */
        int cursor();

        void setCursor(int slot);

        /** Ends a pixel stroke in progress. */
        void endStroke();

        /** Scrolls the sheet so the slot is shown. */
        void keepVisible(int slot);

        /** The slot holding the code point, or -1. */
        int slotOf(int codePoint);

        /** The code and name of the glyph followed by the suffix. */
        String describeGlyph(int codePoint, String suffix);

        /** Stores the edit problem the screen compares against to spot a newly rejected edit. */
        void rememberProblem(String problem);

        /** Follows the pointer to its current cell. */
        void trackPointer();

        /** Highlights the row under a click on the menu and returns whether the click chose an operation. */
        boolean menuClicked(PaintDeriveMenu menu);
    }

    PaintDerive(DeriveFlow flow, PaintDerivePick derivePick, PaintDeriveMenu deriveMenu, FontEditor editor,
                MessageStrip messages, Host host) {
        this.flow = flow;
        this.derivePick = derivePick;
        this.deriveMenu = deriveMenu;
        this.editor = editor;
        this.messages = messages;
        this.host = host;
    }

    OverlayCheck.Result overlayCheck() {
        return derivePick.overlayCheck(host.codePoints());
    }

    void openDerive() {
        messages.nextAction();
        host.endStroke();
        flow.openMenu(host.cursor());
    }

    void unlockDerived() {
        int codePoint = host.codePoints()[host.cursor()];
        messages.nextAction();
        host.endStroke();
        if (editor.unlock(codePoint)) {
            messages.ok(host.describeGlyph(codePoint, " unlocked: now editable"));
        } else {
            messages.warn("Only a derived glyph can be unlocked");
        }
    }

    void goToSource() {
        int codePoint = host.codePoints()[host.cursor()];
        messages.nextAction();
        host.endStroke();
        int sourceSlot = host.slotOf(editor.derivedFrom(codePoint));
        if (sourceSlot >= 0) {
            host.setCursor(sourceSlot);
            host.keepVisible(sourceSlot);
        } else {
            messages.warn("Only a derived glyph has a source to go to");
        }
    }

    void handleDeriveMenu() {
        boolean choseByKey = deriveMenu.handleKeys();
        if (flow.mode() != DeriveFlow.Mode.MENU) {
            return;
        }
        host.trackPointer();
        boolean choseByClick = host.menuClicked(deriveMenu);
        if (choseByKey || choseByClick) {
            beginPick();
        }
    }

    void beginPick() {
        messages.nextAction();
        flow.chooseOperation();
        host.endStroke();
        derivePick.begin(host.codePoints(), host.cursor());
    }

    // Overlay keys: [ and ] shift the pending part, Backspace removes the last part, F finishes.
    void handleOverlayKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            messages.nextAction();
            flow.shiftSource(-1);
            refreshPick();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            messages.nextAction();
            flow.shiftSource(1);
            refreshPick();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            messages.nextAction();
            if (flow.removeLastPart()) {
                refreshPick();
            } else {
                messages.warn("No part to remove");
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            finishOverlay();
        }
    }

    // Runs only when the source changes.
    void refreshPick() {
        derivePick.refresh(host.codePoints(), host.cursor());
        if (derivePick.didPreview()) {
            host.rememberProblem(derivePick.previewProblem());
        }
    }

    // A refused source is already listed as a pick problem, so only the same-glyph case needs a result.
    void confirmSource() {
        messages.nextAction();
        if (flow.isOverlay()) {
            addOverlayPart();
            return;
        }
        if (!derivePick.isValid()) {
            if (flow.sourceSlot() == host.cursor()) {
                messages.warn(OverlayCheck.SAME_AS_TARGET_MESSAGE);
            }
            return;
        }
        applyOrAskReplace();
    }

    // Leaves derive from any mode. Nothing reaches the font before applyDerive's editor.derive call.
    void cancelDerive() {
        messages.nextAction();
        flow.cancelAll();
        derivePick.clear();
        host.keepVisible(host.cursor());
        messages.ok("Derive cancelled");
    }

    void handleReplacePrompt() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            applyDerive();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            flow.cancel();
        }
    }

    private void addOverlayPart() {
        OverlayCheck.Result check = overlayCheck();
        if (check.blocksAdd()) {
            messages.warn(check.message());
        } else if (!derivePick.isValid()) {
            String problem = derivePick.problemText();
            messages.warn(problem.isEmpty() ? "This part cannot be added" : problem);
        } else {
            flow.addPart();
            refreshPick();
        }
    }

    private void finishOverlay() {
        messages.nextAction();
        if (flow.parts().isEmpty()) {
            messages.warn("Add at least one part first");
        } else if (overlayCheck().blocksFinish()) {
            messages.warn(overlayCheck().message());
        } else {
            applyOrAskReplace();
        }
    }

    private void applyOrAskReplace() {
        if (editor.wouldReplaceDrawing(host.codePoints()[host.cursor()])) {
            flow.askReplace();
        } else {
            applyDerive();
        }
    }

    private void applyDerive() {
        int[] codePoints = host.codePoints();
        int target = codePoints[host.cursor()];
        String summary = derivePick.summary(codePoints, target);
        messages.nextAction();
        if (editor.derive(target, derivePick.currentOp(codePoints))) {
            host.rememberProblem("");
            flow.finish();
            derivePick.clear();
            host.keepVisible(host.cursor());
            messages.ok("Derived: " + summary);
        } else {
            host.rememberProblem(editor.problem());
            messages.warn(editor.problem());
            if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
                flow.cancel();
            }
        }
    }
}
