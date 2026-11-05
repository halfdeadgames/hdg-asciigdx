package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.function.Predicate;

/**
 * Turns one frame of pointer state into {@link MenuBar} calls and says whether the menu owns that
 * frame's input. Holds only the press that the menu took, so that press's drag and release never reach
 * the rest of the screen. Nothing here reads input devices.
 */
public final class MenuPointer<A> {

    /** Rows from the bar row down to the first item; the drop-down's top border row sits between them. */
    private static final int FIRST_ITEM_ROW_OFFSET = 2;

    private final int barRow;
    private boolean pressHeld;
    private boolean captured;
    private boolean cellKnown;
    private int lastColumn;
    private int lastRow;

    public MenuPointer(int barRow) {
        this.barRow = barRow;
    }

    /** True while a menu is open or a press the menu took is still held: the screen must ignore all input. */
    public boolean isCapturing(MenuBar<A> bar) {
        return pressHeld || bar.isOpen();
    }

    /** Whether the last {@link #update} left the menu owning the frame, including the frame it closed on. */
    public boolean captured() {
        return captured;
    }

    /** @return the action chosen this frame, or null */
    public A update(MenuBar<A> bar, Predicate<A> availability, MenuPointerInput input) {
        int column = input.column();
        int row = input.row();
        int totalColumns = input.totalColumns();
        boolean pointerBusy = input.pointerBusy();
        bar.refresh(availability);
        captured = isCapturing(bar);
        if (captured && input.escapePressed()) {
            bar.close();
        }
        int rowOffset = dropDownRowOffset(row);
        if (input.pressStarted() && !pointerBusy
                && press(bar, availability, column, row, rowOffset, totalColumns)) {
            captured = true;
        }
        A chosen = null;
        boolean pressActive = pressHeld;
        if (pressHeld && !input.buttonDown()) {
            pressHeld = false;
            captured = true;
            chosen = bar.releaseOnItem(column, rowOffset, totalColumns, availability);
        }
        boolean cellChanged = hasCellChanged(column, row);
        rememberCell(column, row);
        if (pointerBusy) {
            bar.hover(-1, MenuBar.NOT_IN_DROP_DOWN, 0, availability);
        } else if (shouldHover(bar.isOpen(), cellChanged, pressActive)) {
            bar.hover(column, rowOffset, totalColumns, availability);
        }
        captured |= bar.isOpen();
        return chosen;
    }

    private int dropDownRowOffset(int row) {
        return row == barRow ? MenuBar.NOT_IN_DROP_DOWN : row - barRow - FIRST_ITEM_ROW_OFFSET;
    }

    private boolean hasCellChanged(int column, int row) {
        return !cellKnown || column != lastColumn || row != lastRow;
    }

    private void rememberCell(int column, int row) {
        cellKnown = true;
        lastColumn = column;
        lastRow = row;
    }

    /**
     * Whether the pointer may move the highlight this frame. A pointer that sits still must not undo
     * keyboard movement in an open menu, so it counts only when it changed cell or a press is active. With
     * nothing open there is no highlight to protect, so title hover keeps following live availability.
     */
    static boolean shouldHover(boolean menuOpen, boolean cellChanged, boolean pressActive) {
        return !menuOpen || cellChanged || pressActive;
    }

    // Returns whether the menu took the press. Any press on the bar row is taken, even on no title.
    private boolean press(MenuBar<A> bar, Predicate<A> availability, int column, int row, int rowOffset,
                          int totalColumns) {
        if (row == barRow) {
            int title = bar.titleAt(column);
            if (title >= 0 && !bar.isLocked(title, availability)) {
                bar.pressTitle(title, availability);
            } else {
                bar.close();
            }
        } else if (bar.isOpen()) {
            if (!bar.isOnDropDown(column, rowOffset, totalColumns)) {
                bar.close();
            }
        } else {
            return false;
        }
        pressHeld = true;
        return true;
    }
}
