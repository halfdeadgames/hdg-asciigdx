package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.github.halfdeadgames.asciigdx.workbench.Painter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A framed "MESSAGES" panel listing what is true now (conditions, set and cleared by key) and what
 * just happened (results, which stay until {@link #nextAction()}). Warnings are listed before ok messages.
 */
public final class MessageStrip {

    public record Message(boolean warning, String text) {
    }

    private static final int CELL = Painter.TEXT_SIZE;
    private final Map<String, Message> conditions = new LinkedHashMap<>();
    private final List<Message> results = new ArrayList<>();

    /** Reports something that just happened; it stays until the next call to {@link #nextAction()}. */
    public void ok(String text) {
        results.add(new Message(false, text));
    }

    public void warn(String text) {
        results.add(new Message(true, text));
    }

    /** Shows a condition until {@link #clearCondition} is called for the key; setting it again replaces it. */
    public void setCondition(String key, boolean warning, String text) {
        Message existing = conditions.get(key);
        if (existing != null && existing.warning() == warning && existing.text().equals(text)) {
            return;
        }
        conditions.put(key, new Message(warning, text));
    }

    public void clearCondition(String key) {
        conditions.remove(key);
    }

    /** The user did something: drops every result. Call before posting the new action's own result. */
    public void nextAction() {
        results.clear();
    }

    /** Up to {@code count} messages to show: warnings first, then the rest; conditions before results in each group. */
    public List<Message> visible(int count) {
        List<Message> ordered = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            boolean warning = pass == 0;
            for (Message message : conditions.values()) {
                if (message.warning() == warning) {
                    ordered.add(message);
                }
            }
            for (Message message : results) {
                if (message.warning() == warning) {
                    ordered.add(message);
                }
            }
        }
        int shown = Math.min(ordered.size(), Math.max(0, count));
        List<Message> visible = new ArrayList<>(ordered.subList(0, shown));
        if (shown > 0 && !results.isEmpty()) {
            Message newestResult = results.get(results.size() - 1);
            if (indexOfInstance(ordered, newestResult) >= shown) {
                visible.set(shown - 1, newestResult);
            }
        }
        return visible;
    }

    private static int indexOfInstance(List<Message> messages, Message target) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    public void draw(Painter painter, int x, int y, int width, int height) {
        Panel.draw(painter, x, y, width, height, "MESSAGES", false);
        int row = y + 1;
        for (Message message : visible(height - 2)) {
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
