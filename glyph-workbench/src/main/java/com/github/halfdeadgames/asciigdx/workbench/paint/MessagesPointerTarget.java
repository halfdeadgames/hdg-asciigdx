package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import java.util.function.IntSupplier;

/** Presses the message panel's scroll buttons. */
final class MessagesPointerTarget implements PointerTarget {

    private final MessageStrip messages;
    private final IntSupplier cellPixels;

    MessagesPointerTarget(MessageStrip messages, IntSupplier cellPixels) {
        this.messages = messages;
        this.cellPixels = cellPixels;
    }

    @Override
    public void notifyPress(int x, int y) {
        int size = cellPixels.getAsInt();
        messages.click(Math.floorDiv(x, size), Math.floorDiv(y, size));
    }
}
