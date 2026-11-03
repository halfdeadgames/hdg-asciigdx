package com.github.halfdeadgames.asciigdx.workbench;

/**
 * Maps a cell of a scroll bar (arrow, track, arrow) to what a click or drag there does. Cell 0 and the last cell
 * are the arrows; the cells between them are the track.
 */
final class ScrollBarHit {

    enum Part { NONE, BACK_ARROW, FORWARD_ARROW, TRACK_BEFORE_THUMB, TRACK_AFTER_THUMB, THUMB }

    private ScrollBarHit() {
    }

    static Part part(ScrollState scroll, int cell, int barCells) {
        if (cell < 0 || cell >= barCells) {
            return Part.NONE;
        }
        if (cell == 0) {
            return Part.BACK_ARROW;
        }
        if (cell == barCells - 1) {
            return Part.FORWARD_ARROW;
        }
        int trackCells = barCells - 2;
        int trackCell = cell - 1;
        int start = scroll.thumbStart(trackCells);
        if (trackCell < start) {
            return Part.TRACK_BEFORE_THUMB;
        }
        return trackCell >= start + scroll.thumbSize(trackCells) ? Part.TRACK_AFTER_THUMB : Part.THUMB;
    }

    /** Applies the click for a part; the thumb itself does nothing here, it starts a drag. */
    static void click(ScrollState scroll, Part part) {
        switch (part) {
            case BACK_ARROW -> scroll.scrollBy(-1);
            case FORWARD_ARROW -> scroll.scrollBy(1);
            case TRACK_BEFORE_THUMB -> scroll.pageUp();
            case TRACK_AFTER_THUMB -> scroll.pageDown();
            default -> {
            }
        }
    }

    /** Cells between the thumb's first cell and the grabbed bar cell. */
    static int grabOffset(ScrollState scroll, int cell, int barCells) {
        return cell - 1 - scroll.thumbStart(barCells - 2);
    }

    /** Offset that puts the thumb where a drag holding {@code grabOffset} into the thumb ends up at {@code cell}. */
    static int offsetForDrag(ScrollState scroll, int cell, int barCells, int grabOffset) {
        int trackCells = barCells - 2;
        int travel = trackCells - scroll.thumbSize(trackCells);
        if (travel <= 0) {
            return 0;
        }
        int start = Math.max(0, Math.min(travel, cell - 1 - grabOffset));
        return Math.round((float) start * scroll.maxOffset() / travel);
    }
}
