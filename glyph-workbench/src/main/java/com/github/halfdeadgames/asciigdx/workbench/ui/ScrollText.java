package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/** Wrapped text with a ▲ ░ █ ▼ scrollbar in the last column; never clips silently. */
public final class ScrollText {

    private List<String> lines = new ArrayList<>();
    private int offset;
    private int visible = 1;

    /** Breaks at the last space that fits; a word longer than the width is cut. Always at least one line. */
    public static List<String> wrap(String text, int width) {
        int limit = Math.max(1, width);
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            int start = 0;
            while (true) {
                if (paragraph.length() - start <= limit) {
                    lines.add(paragraph.substring(start));
                    break;
                }
                int space = paragraph.lastIndexOf(' ', start + limit);
                if (space > start) {
                    lines.add(paragraph.substring(start, space));
                    start = space + 1;
                } else {
                    lines.add(paragraph.substring(start, start + limit));
                    start += limit;
                }
            }
        }
        return lines;
    }

    /** Thumb length in track cells: proportional to visible over total, at least 1. */
    public static int thumbSize(int track, int total, int visible) {
        if (track <= 0) {
            return 0;
        }
        if (total <= visible) {
            return track;
        }
        return Math.max(1, Math.min(track, Math.round((float) track * visible / total)));
    }

    /** First thumb cell; the thumb touches the track end exactly at the last offset. */
    public static int thumbStart(int track, int total, int visible, int offset) {
        int travel = track - thumbSize(track, total, visible);
        int max = total - visible;
        if (travel <= 0 || max <= 0) {
            return 0;
        }
        return Math.round((float) offset * travel / max);
    }

    public void setText(String text, int widthCells) {
        lines = wrap(text, widthCells);
        offset = Math.max(0, Math.min(maxOffset(), offset));
    }

    public int lineCount() {
        return lines.size();
    }

    /** Whether more lines exist than the last {@link #draw} showed. */
    public boolean overflows() {
        return lines.size() > visible;
    }

    public int offset() {
        return offset;
    }

    public void scrollBy(int rows) {
        offset = Math.max(0, Math.min(maxOffset(), offset + rows));
    }

    public void toTop() {
        offset = 0;
    }

    private int maxOffset() {
        return Math.max(0, lines.size() - visible);
    }

    /**
     * Draws into a cell rectangle: text in the first {@code widthCells - 1} columns and the scrollbar
     * in the last. Wrap with {@code widthCells - 1} when calling {@link #setText}.
     */
    public void draw(Painter painter, CellRect area, Color color) {
        int x = area.x();
        int y = area.y();
        int widthCells = area.width();
        int heightCells = area.height();
        visible = Math.max(1, heightCells);
        offset = Math.max(0, Math.min(maxOffset(), offset));
        int end = Math.min(lines.size(), offset + visible);
        int cell = painter.cell();
        for (int i = offset; i < end; i++) {
            painter.text(lines.get(i), x * cell, (y + i - offset) * cell, color);
        }
        if (lines.size() > visible) {
            drawBar(painter, new CellRect(x + widthCells - 1, y, 1, heightCells), lines.size(), visible, offset);
        }
    }

    /** The shared scrollbar in a one-column rectangle: ▲ and ▼ at the ends, a ░ track, and a █ thumb. Callers show it only when text overflows. */
    public static void drawBar(Painter painter, CellRect bar, int total, int visible, int offset) {
        int x = bar.x();
        int y = bar.y();
        int height = bar.height();
        int cell = painter.cell();
        painter.text("▲", x * cell, y * cell, UiPalette.FRAME);
        painter.text("▼", x * cell, (y + height - 1) * cell, UiPalette.FRAME);
        int track = height - 2;
        int start = thumbStart(track, total, visible, offset);
        int size = thumbSize(track, total, visible);
        for (int i = 0; i < track; i++) {
            boolean thumb = i >= start && i < start + size;
            painter.text(thumb ? "█" : "░", x * cell, (y + 1 + i) * cell, thumb ? UiPalette.FOCUS : UiPalette.FRAME);
        }
    }
}
