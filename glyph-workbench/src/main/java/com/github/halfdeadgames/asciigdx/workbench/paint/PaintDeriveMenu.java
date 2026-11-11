package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.derive.DeriveFlow;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.Keys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The derive operation menu: a dialog with a select list of operations, driven by keys and clicks. */
final class PaintDeriveMenu {

    private static final String MENU_TITLE = "DERIVE";
    private static final int MENU_WIDTH = 26;
    private static final List<Dialog.Button> MENU_BUTTONS = List.of(
        new Dialog.Button("ENTER", "SELECT", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final Content SQUARE_CONTENT = contentOf(DeriveFlow.operationsFor(true));
    private static final Content NON_SQUARE_CONTENT = contentOf(DeriveFlow.operationsFor(false));

    /** The dialog lines and select rows for one operation list; the lines are the room the rows are drawn into. */
    private record Content(List<String> lines, List<SelectList.Row> rows) { }

    private final DeriveFlow flow;
    private final MessageStrip messages;

    PaintDeriveMenu(DeriveFlow flow, MessageStrip messages) {
        this.flow = flow;
        this.messages = messages;
    }

    /** Handles Esc, Up, and Down; returns whether Enter chose the highlighted operation. */
    boolean handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            flow.cancel();
            return false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            messages.nextAction();
            flow.moveMenu(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            messages.nextAction();
            flow.moveMenu(1);
        }
        return Keys.enterJustPressed();
    }

    /** Highlights the clicked row; returns whether a click chose an operation. */
    boolean handleClick(CellRect grid, int cellPixels, int pixelX, int pixelY, boolean leftClicked) {
        if (!leftClicked) {
            return false;
        }
        Dialog.Layout layout = layout(grid);
        int column = pixelX / cellPixels;
        int row = SelectList.rowAt(pixelY / cellPixels, layout.textY(), flow.operations().size());
        if (row < 0 || column < layout.x() || column >= layout.x() + layout.width()) {
            return false;
        }
        flow.highlightMenuRow(row);
        return true;
    }

    void draw(Painter painter, int columns, int rows, CellRect grid) {
        Content content = content();
        Dialog.draw(painter, new CellRect(0, 0, columns, rows),
            DialogContent.of(MENU_TITLE, content.lines(), MENU_BUTTONS),
            grid);
        Dialog.Layout menu = layout(grid);
        SelectList.draw(painter,
            SelectList.areaOf(menu.textX(), menu.textY(), Math.min(MENU_WIDTH, menu.textWidth()),
                content.rows().size()),
            content.rows(), flow.menuIndex());
    }

    private Content content() {
        return flow.operations().equals(DeriveFlow.operationsFor(true)) ? SQUARE_CONTENT : NON_SQUARE_CONTENT;
    }

    private Dialog.Layout layout(CellRect area) {
        return Dialog.layout(DialogContent.of(MENU_TITLE, content().lines(), MENU_BUTTONS), area);
    }

    private static Content contentOf(List<GlyphOp.Kind> operations) {
        List<SelectList.Row> rows = new ArrayList<>();
        for (GlyphOp.Kind kind : operations) {
            rows.add(new SelectList.Row(DeriveFlow.label(kind), true, false, 0));
        }
        return new Content(Collections.nCopies(operations.size() * SelectList.PITCH - 1, " ".repeat(MENU_WIDTH)),
            List.copyOf(rows));
    }
}
