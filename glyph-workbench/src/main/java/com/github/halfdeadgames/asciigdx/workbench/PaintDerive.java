package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.derive.DeriveFlow;
import com.github.halfdeadgames.asciigdx.workbench.derive.OverlayCheck;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.util.ArrayList;
import java.util.List;

/** The derive actions of the paint screen: opening the menu, picking sources, applying, and cancelling. */
final class PaintDerive {

    private static final List<Dialog.Button> REPLACE_BUTTONS = List.of(
        new Dialog.Button("R", "REPLACE", Dialog.Tone.WARN), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private final StringBuilder replaceLine = new StringBuilder();

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
        int cursorSlot();

        void setCursor(int slot);

        void endStroke();

        /** Scrolls the sheet so the slot is shown. */
        void keepVisible(int slot);

        /** The slot holding the code point, or -1. */
        int slotOf(int codePoint);

        /** The code and name of the glyph followed by the suffix. */
        String describeGlyph(int codePoint, String suffix);

        /** Stores the edit problem the screen compares against to spot a newly rejected edit. */
        void rememberProblem(String problem);

        /** Tracks the pointer's pixel position and routes the mouse wheel to the panel under it. */
        void updateAndRouteWheel();

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

    /** Whether a derive dialog is open and dims the screen, so the help must be drawn again above it. */
    boolean dialogShown() {
        return flow.mode() == DeriveFlow.Mode.MENU || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE;
    }

    void drawDialog(Painter painter, int columns, int rows, CellRect area, int codePoint) {
        if (flow.mode() == DeriveFlow.Mode.MENU) {
            deriveMenu.draw(painter, columns, rows, area);
        } else if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            drawReplaceDialog(painter, columns, rows, area, codePoint);
        }
    }

    PaintHelp.State helpState() {
        int[] codePoints = host.codePoints();
        DeriveFlow.Mode mode = flow.mode();
        GlyphOp.Kind operation = mode == DeriveFlow.Mode.MENU ? DeriveFlow.OPERATIONS.get(flow.menuIndex()) : flow.operation();
        if (!flow.isOverlayOperation() || mode != DeriveFlow.Mode.PICK_SOURCE) {
            return new PaintHelp.State(mode, operation, List.of(), null);
        }
        List<PaintHelp.PartView> committed = new ArrayList<>();
        for (DeriveFlow.Part part : flow.parts()) {
            committed.add(new PaintHelp.PartView(codePoints[part.slot()], part.shift()));
        }
        PaintHelp.PartView pending = flow.sourceIsTarget()
            ? null : new PaintHelp.PartView(codePoints[flow.sourceSlot()], flow.pendingShift());
        return new PaintHelp.State(mode, operation, committed, pending, overlayCheck());
    }

    // The pending overlay part shows as a ghost only while its shift puts ink outside the body.
    PaintGridView.Ghost pendingGhost() {
        if (!flow.isOverlayOperation() || !flow.isPicking() || flow.sourceIsTarget()
            || overlayCheck().problem() != OverlayCheck.Problem.OUTSIDE) {
            return null;
        }
        return new PaintGridView.Ghost(editor.glyph(host.codePoints()[flow.sourceSlot()]).pixels(), flow.pendingShift());
    }

    private void drawReplaceDialog(Painter painter, int columns, int rows, CellRect area, int codePoint) {
        replaceLine.setLength(0);
        replaceLine.append("U+");
        GlyphLabel.appendHex(replaceLine, codePoint);
        Dialog.draw(painter, new CellRect(0, 0, columns, rows),
            DialogContent.of("REPLACE DRAWING",
                List.of(replaceLine + " holds a drawing.", "Deriving replaces it.", "Undo restores it."),
                REPLACE_BUTTONS),
            area);
    }

    void openDerive() {
        messages.nextAction();
        host.endStroke();
        flow.openMenu(host.cursorSlot());
    }

    void unlockDerived() {
        int codePoint = host.codePoints()[host.cursorSlot()];
        messages.nextAction();
        host.endStroke();
        if (!editor.isDerived(codePoint)) {
            messages.warn("Only a derived glyph can be unlocked");
            return;
        }
        editor.unlock(codePoint);
        messages.ok(host.describeGlyph(codePoint, " unlocked: now editable"));
    }

    void goToSource() {
        int codePoint = host.codePoints()[host.cursorSlot()];
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

    /** Routes keys to the menu or replace prompt when one is open, and returns whether it consumed the frame. */
    boolean handleModalKeys() {
        if (flow.mode() == DeriveFlow.Mode.MENU) {
            handleDeriveMenu();
            return true;
        }
        if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            handleReplacePrompt();
            return true;
        }
        return false;
    }

    /** Handles keys while picking a source, and returns whether Enter confirmed the pick. */
    boolean handlePickingKeys() {
        if (flow.isOverlayOperation()) {
            handleOverlayKeys();
        }
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && !Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            return false;
        }
        confirmSource();
        return true;
    }

    void handleShortcutKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            openDerive();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            unlockDerived();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            goToSource();
        }
    }

    void clickSource(int slot) {
        if (flow.isConfirmingClick(slot)) {
            confirmSource();
            return;
        }
        flow.clickSource(slot);
        refreshPick();
    }

    void moveSource(int slot) {
        flow.moveSource(slot);
        refreshPick();
    }

    void handleDeriveMenu() {
        boolean choseByKey = deriveMenu.handleKeys();
        if (flow.mode() != DeriveFlow.Mode.MENU) {
            return;
        }
        host.updateAndRouteWheel();
        boolean choseByClick = host.menuClicked(deriveMenu);
        if (choseByKey || choseByClick) {
            beginPick();
        }
    }

    void beginPick() {
        messages.nextAction();
        flow.chooseOperation();
        host.endStroke();
        derivePick.begin(host.codePoints(), host.cursorSlot());
    }

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
            if (flow.canRemovePart()) {
                flow.removeLastPart();
                refreshPick();
            } else {
                messages.warn("No part to remove");
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            finishOverlay();
        }
    }

    void refreshPick() {
        derivePick.refresh(host.codePoints(), host.cursorSlot());
    }

    // A refused source is already listed as a pick problem, so only the same-glyph case needs a result.
    void confirmSource() {
        messages.nextAction();
        if (flow.isOverlayOperation()) {
            addOverlayPart();
            return;
        }
        if (!derivePick.isValid()) {
            if (flow.sourceSlot() == host.cursorSlot()) {
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
        host.keepVisible(host.cursorSlot());
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
        if (editor.wouldReplaceDrawing(host.codePoints()[host.cursorSlot()])) {
            flow.askReplace();
        } else {
            applyDerive();
        }
    }

    private void applyDerive() {
        int[] codePoints = host.codePoints();
        int target = codePoints[host.cursorSlot()];
        String summary = derivePick.summary(codePoints, target);
        messages.nextAction();
        editor.derive(target, derivePick.currentOp(codePoints));
        if (editor.problem().isEmpty()) {
            host.rememberProblem("");
            flow.finish();
            derivePick.clear();
            host.keepVisible(host.cursorSlot());
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
