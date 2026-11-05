package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;

final class PaintScrollRouting {

    private static final int WHEEL_ROWS = 3;

    interface Host {

        boolean helpShown();

        boolean helpContains(int pixelX, int pixelY);

        boolean isOverlayOperation();

        int cell();

        void unprojectInputToPointer();

        int unprojectedPixelX();

        int unprojectedPixelY();
    }

    private final MessageStrip messages;
    private final NotePanel notePanel;
    private final ScrollState helpScroll;
    private final PaintSheetView sheet;
    private final WheelTicks wheelTicks;
    private final Host host;

    PaintScrollRouting(MessageStrip messages, NotePanel notePanel, ScrollState helpScroll, PaintSheetView sheet,
                       WheelTicks wheelTicks, Host host) {
        this.messages = messages;
        this.notePanel = notePanel;
        this.helpScroll = helpScroll;
        this.sheet = sheet;
        this.wheelTicks = wheelTicks;
        this.host = host;
    }

    void handleScrollKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            sheet.scroll().pageUp();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            sheet.scroll().pageDown();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            sheet.scroll().toTop();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            sheet.scroll().toBottom();
        }
        if (host.isOverlayOperation()) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            scrollNoteOrHelp(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            scrollNoteOrHelp(1);
        }
    }

    // Applied before the modal checks so ticks never pile up; the view scroll never moves the cursor.
    void scrollSheetSideways() {
        if (wheelTicks.sideways() == 0) {
            return;
        }
        host.unprojectInputToPointer();
        if (sheet.contains(host.unprojectedPixelX(), host.unprojectedPixelY())) {
            sheet.columnScroll().scrollBy(wheelTicks.sideways());
        }
        wheelTicks.clearSideways();
    }

    void routeVerticalWheel(int pointerX, int pointerY) {
        int ticks = wheelTicks.vertical();
        if (ticks != 0) {
            scrollPanelUnderPointer(ticks, pointerX, pointerY);
        }
        wheelTicks.clearVertical();
    }

    private void scrollPanelUnderPointer(int ticks, int pointerX, int pointerY) {
        int cell = host.cell();
        if (messages.contains(Math.floorDiv(pointerX, cell), Math.floorDiv(pointerY, cell))) {
            messages.scrollBy(ticks);
        } else if (notePanel.contains(pointerX, pointerY)) {
            notePanel.scrollDisplay(ticks * WHEEL_ROWS);
        } else if (host.helpContains(pointerX, pointerY)) {
            helpScroll.scrollBy(ticks * WHEEL_ROWS);
        } else if (sheet.contains(pointerX, pointerY)) {
            sheet.scroll().scrollBy(ticks * WHEEL_ROWS);
        }
    }

    // The keys follow the note while it overflows; otherwise they scroll the help.
    private void scrollNoteOrHelp(int rows) {
        if (notePanel.canScroll()) {
            notePanel.scrollDisplay(rows);
        } else if (host.helpShown()) {
            helpScroll.scrollBy(rows);
        }
    }
}
