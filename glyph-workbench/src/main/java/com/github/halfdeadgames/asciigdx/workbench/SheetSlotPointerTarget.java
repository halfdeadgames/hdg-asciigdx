package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.derive.DeriveFlow;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;

/** Selects the glyph slot under a press, or picks it as the derive source while picking. */
final class SheetSlotPointerTarget implements PointerTarget {

    /** What slot clicks need from the paint screen. */
    interface Selection {

        boolean isSelectable(int slot);

        void select(int slot);
    }

    private final PaintSheetView sheet;
    private final DeriveFlow flow;
    private final PaintDerive derive;
    private final MessageStrip messages;
    private final Selection selection;

    SheetSlotPointerTarget(PaintSheetView sheet, DeriveFlow flow, PaintDerive derive, MessageStrip messages,
                           Selection selection) {
        this.sheet = sheet;
        this.flow = flow;
        this.derive = derive;
        this.messages = messages;
        this.selection = selection;
    }

    @Override
    public boolean claimPress(int x, int y) {
        int slot = sheet.slotAt(x, y);
        if (slot < 0 || !selection.isSelectable(slot)) {
            return false;
        }
        messages.nextAction();
        if (flow.isPicking()) {
            derive.clickSource(slot);
        } else {
            selection.select(slot);
        }
        return true;
    }
}
