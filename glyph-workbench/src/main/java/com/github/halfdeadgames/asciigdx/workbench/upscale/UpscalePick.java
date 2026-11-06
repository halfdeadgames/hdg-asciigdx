package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.workbench.CursorNavigator;

/** The cursor on the source sheet while the user picks a glyph to upscale. It only rests on pickable slots. */
public final class UpscalePick {

    private final UpscaleSourceSheet sheet;
    private final int columns;
    private int cursor;

    /**
     * @param startSlot a pickable slot of the sheet
     * @param columns glyph columns of the sheet view
     */
    public UpscalePick(UpscaleSourceSheet sheet, int startSlot, int columns) {
        this.sheet = sheet;
        this.columns = columns;
        this.cursor = startSlot;
    }

    public UpscaleSourceSheet sheet() {
        return sheet;
    }

    public int cursor() {
        return cursor;
    }

    public int codePoint() {
        return sheet.codePointAt(cursor);
    }

    /** Moves one step in a direction, skipping slots that cannot be picked. */
    public void move(int dx, int dy) {
        int rows = sheet.blocked().length / columns;
        cursor = CursorNavigator.move(sheet.blocked(), columns, rows, cursor, dx, dy);
    }

    /** Puts the cursor on the slot when it can be picked; otherwise nothing changes. */
    public void select(int slot) {
        if (sheet.isPickable(slot)) {
            cursor = slot;
        }
    }

    /** Whether a click on the slot confirms the pick: it is the slot the cursor already rests on. */
    public boolean isConfirmingClick(int slot) {
        return slot == cursor && sheet.isPickable(slot);
    }
}
