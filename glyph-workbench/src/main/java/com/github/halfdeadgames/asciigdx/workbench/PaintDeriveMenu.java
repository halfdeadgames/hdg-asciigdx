package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.derive.DeriveFlow;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
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
    // Blank dialog lines reserve the room the select list is drawn into, two cells per operation.
    private static final List<String> MENU_LINES = Collections.nCopies(
        DeriveFlow.OPERATIONS.size() * SelectList.PITCH - 1, " ".repeat(MENU_WIDTH));
    private static final List<SelectList.Row> MENU_ROWS = menuRows();

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
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
    }

    /** Highlights the clicked row; returns whether a click chose an operation. */
    boolean handleClick(CellRect grid, int cell, int pointerX, int pointerY, boolean leftClicked) {
        if (!leftClicked) {
            return false;
        }
        Dialog.Layout layout = layout(grid);
        int column = pointerX / cell;
        int row = SelectList.rowAt(pointerY / cell, layout.textY(), DeriveFlow.OPERATIONS.size());
        if (row < 0 || column < layout.x() || column >= layout.x() + layout.width()) {
            return false;
        }
        flow.highlightMenuRow(row);
        return true;
    }

    void draw(Painter painter, int columns, int rows, CellRect grid) {
        Dialog.draw(painter, new CellRect(0, 0, columns, rows),
            DialogContent.of(MENU_TITLE, MENU_LINES, MENU_BUTTONS),
            grid);
        Dialog.Layout menu = layout(grid);
        SelectList.draw(painter,
            SelectList.areaOf(menu.textX(), menu.textY(), Math.min(MENU_WIDTH, menu.textWidth()), MENU_ROWS.size()),
            MENU_ROWS, flow.menuIndex());
    }

    private static Dialog.Layout layout(CellRect area) {
        return Dialog.layout(DialogContent.of(MENU_TITLE, MENU_LINES, MENU_BUTTONS),
            area);
    }

    private static List<SelectList.Row> menuRows() {
        List<SelectList.Row> rows = new ArrayList<>();
        for (GlyphOp.Kind kind : DeriveFlow.OPERATIONS) {
            rows.add(new SelectList.Row(DeriveFlow.label(kind), true, false, 0));
        }
        return List.copyOf(rows);
    }
}
