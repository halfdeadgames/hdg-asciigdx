package com.github.halfdeadgames.asciigdx.workbench.ui;

/** Shared ▲ ░ █ ▼ scrollbar drawing and thumb math. */
public final class ScrollText {

    private ScrollText() {
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
