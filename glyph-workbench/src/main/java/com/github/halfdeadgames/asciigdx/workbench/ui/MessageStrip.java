package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.github.halfdeadgames.asciigdx.workbench.Painter;
import java.util.ArrayList;
import java.util.List;

/** A framed "MESSAGES" panel listing the most recent messages with ✓ (ok) or ! (warning) icons. */
public final class MessageStrip {

    public record Message(boolean warning, String text) {
    }

    private static final int CELL = Painter.TEXT_SIZE;
    private final List<Message> messages = new ArrayList<>();

    public void ok(String text) {
        messages.add(new Message(false, text));
    }

    public void warn(String text) {
        messages.add(new Message(true, text));
    }

    /** The newest {@code count} messages, oldest first. */
    public List<Message> recent(int count) {
        int from = Math.max(0, messages.size() - Math.max(0, count));
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    public void draw(Painter painter, int x, int y, int width, int height) {
        Panel.draw(painter, x, y, width, height, "MESSAGES", false);
        int row = y + 1;
        for (Message message : recent(height - 2)) {
            painter.text(message.warning() ? "!" : "✓", (x + 2) * CELL, row * CELL,
                message.warning() ? UiPalette.WARN : UiPalette.OK);
            String text = message.text();
            int room = width - 6;
            painter.text(text.length() > room ? text.substring(0, Math.max(0, room)) : text,
                (x + 4) * CELL, row * CELL, UiPalette.TEXT);
            row++;
        }
    }
}
