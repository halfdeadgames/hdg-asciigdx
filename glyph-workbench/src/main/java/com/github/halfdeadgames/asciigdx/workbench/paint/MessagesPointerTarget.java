package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import java.util.function.IntSupplier;

/** Presses the message panel's scroll buttons. */
final class MessagesPointerTarget implements PointerTarget {

    private final MessageStrip messages;
    private final IntSupplier cell;

    MessagesPointerTarget(MessageStrip messages, IntSupplier cell) {
        this.messages = messages;
        this.cell = cell;
    }

    @Override
    public void notifyPress(int x, int y) {
        int cellSize = cell.getAsInt();
        messages.click(Math.floorDiv(x, cellSize), Math.floorDiv(y, cellSize));
    }
}
