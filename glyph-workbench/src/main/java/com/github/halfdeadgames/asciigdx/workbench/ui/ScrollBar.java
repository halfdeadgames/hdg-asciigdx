package com.github.halfdeadgames.asciigdx.workbench.ui;


/** Shared ▲ ░ █ ▼ scrollbar drawing and thumb math. */
public final class ScrollBar {

    private ScrollBar() {
    }

    /** Thumb length in track cells: proportional to visible over total, at least 1. */
    private static int thumbSize(int track, int total, int visible) {
        return ScrollState.thumbSize(track, total, visible);
    }

    /** First thumb cell; the thumb touches the track end exactly at the last offset. */
    private static int thumbStart(int track, int total, int visible, int offset) {
        return ScrollState.thumbStart(track, total, visible, offset);
    }

    /**
     * The shared scrollbar in a one-column rectangle: ▲ and ▼ at the ends, a ░ track, and a █
     * thumb. Callers show it only when text overflows.
     */
    public static void drawBar(Painter painter, CellRect bar, int total, int visible, int offset) {
        drawBar(painter, bar.x(), bar.y(), bar.height(), total, visible, offset);
    }

    /** Same bar given by its column, top row, and height in cells. */
    public static void drawBar(Painter painter, int column, int top, int height, int total,
            int visible, int offset) {
        int cellPixels = painter.cellPixels();
        drawBarAtPixels(painter, column * cellPixels, top * cellPixels, height, total, visible,
                offset, true);
    }

    /**
     * Same bar at a pixel position. Without a thumb the whole track shows as ░, for a bar that has
     * nothing to scroll.
     */
    public static void drawBarAtPixels(Painter painter, int pixelX, int pixelTop, int height,
            int total, int visible, int offset, boolean showThumb) {
        int cellPixels = painter.cellPixels();
        painter.text("▲", pixelX, pixelTop, UiPalette.FRAME);
        painter.text("▼", pixelX, pixelTop + (height - 1) * cellPixels, UiPalette.FRAME);
        int track = height - 2;
        int start = thumbStart(track, total, visible, offset);
        int size = thumbSize(track, total, visible);
        for (int i = 0; i < track; i++) {
            boolean thumb = showThumb && i >= start && i < start + size;
            int color = thumb ? UiPalette.FOCUS : UiPalette.FRAME;
            painter.text(thumb ? "█" : "░", pixelX, pixelTop + (1 + i) * cellPixels, color);
        }
    }
}
