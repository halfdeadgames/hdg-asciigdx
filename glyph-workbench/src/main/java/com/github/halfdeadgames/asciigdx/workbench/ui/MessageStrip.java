package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
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

    /** Columns the panel keeps free around the text: frame, arrow cell, marker, gaps, and the scroll button column. */
    public static final int RESERVED_COLUMNS = 7;

    private static final float BLINK_HALF_CYCLE_SECONDS = 0.25f;
    private int firstIndex;
    private int viewRows;
    private int totalCount;
    private boolean revealNewestResult;
    private int boundsX;
    private int boundsY;
    private int boundsWidth;
    private int boundsHeight;
    private final Map<String, Message> conditions = new LinkedHashMap<>();
    private final List<Message> results = new ArrayList<>();
    private final Map<Message, Float> scrollStartSeconds = new IdentityHashMap<>();
    private float clockSeconds;

    /** Reports something that just happened; it stays until the next call to {@link #nextAction()}. */
    public void ok(String text) {
        results.add(new Message(false, text));
        revealNewestResult = true;
    }

    public void warn(String text) {
        results.add(new Message(true, text));
        revealNewestResult = true;
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

    /**
     * Up to {@code count} messages to show, starting at the scroll position. The order is warnings first, then the
     * rest; conditions before results in each group. A result posted since the last call is scrolled into view.
     */
    public List<Message> visible(int count) {
        List<Message> ordered = orderedMessages();
        viewRows = Math.max(0, count);
        totalCount = ordered.size();
        if (revealNewestResult && viewRows > 0) {
            revealNewestResult = false;
            scrollNewestResultIntoView(ordered);
        }
        firstIndex = clampedIndex(firstIndex);
        int end = Math.min(totalCount, firstIndex + viewRows);
        return new ArrayList<>(ordered.subList(firstIndex, end));
    }

    /** Whether messages are cut off above the view, going by the last {@link #visible} call. */
    public boolean hiddenAbove() {
        return firstIndex > 0;
    }

    /** Whether messages are cut off below the view, going by the last {@link #visible} call. */
    public boolean hiddenBelow() {
        return firstIndex + viewRows < totalCount;
    }

    /** Scrolls by whole messages (negative goes up); the position stays within the list. */
    public void scrollBy(int messages) {
        firstIndex = clampedIndex(firstIndex + messages);
    }

    /** Whether the cell lies inside the panel as it was last drawn. */
    public boolean contains(int cellX, int cellY) {
        return cellX >= boundsX && cellX < boundsX + boundsWidth
            && cellY >= boundsY && cellY < boundsY + boundsHeight;
    }

    /** Presses the scroll button under the cell, if any. */
    public void click(int cellX, int cellY) {
        if (!buttonsFit() || cellX != buttonColumn()) {
            return;
        }
        if (cellY == topButtonRow()) {
            scrollBy(-1);
            return;
        }
        if (cellY == bottomButtonRow()) {
            scrollBy(1);
        }
    }

    void place(int x, int y, int width, int height) {
        boundsX = x;
        boundsY = y;
        boundsWidth = width;
        boundsHeight = height;
    }

    private boolean buttonsFit() {
        return boundsHeight - 2 >= 2 && boundsWidth > RESERVED_COLUMNS;
    }

    private int buttonColumn() {
        return boundsX + boundsWidth - 2;
    }

    private int topButtonRow() {
        return boundsY + 1;
    }

    private int bottomButtonRow() {
        return boundsY + boundsHeight - 2;
    }

    private int clampedIndex(int index) {
        return Math.max(0, Math.min(Math.max(0, totalCount - viewRows), index));
    }

    private void scrollNewestResultIntoView(List<Message> ordered) {
        if (results.isEmpty()) {
            return;
        }
        int index = indexOfInstance(ordered, results.get(results.size() - 1));
        if (index < firstIndex) {
            firstIndex = index;
        } else if (index >= firstIndex + viewRows) {
            firstIndex = index - viewRows + 1;
        }
    }

    private List<Message> orderedMessages() {
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
        return ordered;
    }

    private static int indexOfInstance(List<Message> messages, Message target) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    void advance(float deltaSeconds) {
        clockSeconds += deltaSeconds;
    }

    String lineText(Message message, int room) {
        if (message.text().length() <= room) {
            return message.text();
        }
        Float startSeconds = scrollStartSeconds.get(message);
        if (startSeconds == null) {
            startSeconds = clockSeconds;
            scrollStartSeconds.put(message, startSeconds);
        }
        return BounceScroll.window(message.text(), room, clockSeconds - startSeconds);
    }

    private void forgetScrollsExcept(List<Message> shown) {
        if (scrollStartSeconds.isEmpty()) {
            return;
        }
        Iterator<Message> tracked = scrollStartSeconds.keySet().iterator();
        while (tracked.hasNext()) {
            if (indexOfInstance(shown, tracked.next()) < 0) {
                tracked.remove();
            }
        }
    }

    private boolean blinkOn() {
        return (int) (clockSeconds / BLINK_HALF_CYCLE_SECONDS) % 2 == 0;
    }

    private void drawScrollIndicators(Painter painter) {
        int cell = painter.cell();
        if (blinkOn()) {
            int arrowColumn = (boundsX + 1) * cell;
            if (hiddenAbove()) {
                painter.text("▲", arrowColumn, topButtonRow() * cell, UiPalette.ACCENT);
            }
            if (hiddenBelow()) {
                painter.text("▼", arrowColumn, bottomButtonRow() * cell, UiPalette.ACCENT);
            }
        }
        if (buttonsFit()) {
            painter.text("▲", buttonColumn() * cell, topButtonRow() * cell,
                hiddenAbove() ? UiPalette.TEXT : UiPalette.DIM);
            painter.text("▼", buttonColumn() * cell, bottomButtonRow() * cell,
                hiddenBelow() ? UiPalette.TEXT : UiPalette.DIM);
        }
    }

    /**
     * Draws the panel; messages wider than the text area scroll back and forth, each on its own timer. When messages
     * are cut off, blinking arrows mark the hidden side and two buttons at the right edge scroll the list.
     */
    public void draw(Painter painter, CellRect area, float deltaSeconds) {
        int x = area.x();
        int y = area.y();
        int width = area.width();
        int height = area.height();
        Panel.draw(painter, area, "MESSAGES", false);
        advance(deltaSeconds);
        place(x, y, width, height);
        List<Message> shown = visible(height - 2);
        int room = width - RESERVED_COLUMNS;
        int row = y + 1;
        int cell = painter.cell();
        for (Message message : shown) {
            painter.text(message.warning() ? "!" : "✓", (x + 2) * cell, row * cell,
                message.warning() ? UiPalette.WARN : UiPalette.OK);
            painter.text(lineText(message, room), (x + 4) * cell, row * cell, UiPalette.TEXT);
            row++;
        }
        drawScrollIndicators(painter);
        forgetScrollsExcept(shown);
    }
}
