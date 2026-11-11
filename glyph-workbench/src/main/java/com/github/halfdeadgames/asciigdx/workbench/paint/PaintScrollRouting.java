package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.Keys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import com.github.halfdeadgames.asciigdx.workbench.ui.WheelTicks;

final class PaintScrollRouting {

    interface Host {

        boolean helpShown();

        boolean helpContains(int pixelX, int pixelY);

        boolean isOverlayOperation();

        int cellPixels();

        void unprojectInputToPointer();

        int unprojectedPixelX();

        int unprojectedPixelY();
    }

    private final MessageStrip messages;
    private final ScrollState helpScroll;
    private final PaintSheetView sheet;
    private final WheelTicks wheelTicks;
    private final Host host;

    PaintScrollRouting(MessageStrip messages, ScrollState helpScroll, PaintSheetView sheet,
                       WheelTicks wheelTicks, Host host) {
        this.messages = messages;
        this.helpScroll = helpScroll;
        this.sheet = sheet;
        this.wheelTicks = wheelTicks;
        this.host = host;
    }

    void handleScrollKeys() {
        Keys.scrollByPageKeys(sheet.scroll());
        if (host.isOverlayOperation()) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            scrollHelp(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            scrollHelp(1);
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

    void routeVerticalWheel(int pixelX, int pixelY) {
        int ticks = wheelTicks.vertical();
        if (ticks != 0) {
            scrollPanelUnderPointer(ticks, pixelX, pixelY);
        }
        wheelTicks.clearVertical();
    }

    private void scrollPanelUnderPointer(int ticks, int pixelX, int pixelY) {
        int cellPixels = host.cellPixels();
        if (messages.contains(Math.floorDiv(pixelX, cellPixels), Math.floorDiv(pixelY, cellPixels))) {
            messages.scrollBy(ticks);
        } else if (host.helpContains(pixelX, pixelY)) {
            helpScroll.scrollBy(ticks * WheelTicks.ROWS_PER_TICK);
        } else if (sheet.contains(pixelX, pixelY)) {
            sheet.scroll().scrollBy(ticks * WheelTicks.ROWS_PER_TICK);
        }
    }

    private void scrollHelp(int rows) {
        if (host.helpShown()) {
            helpScroll.scrollBy(rows);
        }
    }
}
