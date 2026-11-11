package com.github.halfdeadgames.asciigdx.workbench.ui;

/** A warning line near the bottom of a screen, cut to fit with "..". */
public final class NoticeLine {

    private static final int MARGIN_CELLS = 2;
    private static final int ROWS_FROM_BOTTOM = 3;
    private static final String CUT_MARK = "..";

    private NoticeLine() {
    }

    /** Draws the notice in the warning color; does nothing for an empty notice. */
    public static void draw(Painter painter, String notice, int columns, int rows) {
        if (notice.isEmpty()) {
            return;
        }
        int room = Math.max(1, columns - 2 * MARGIN_CELLS);
        painter.text(fit(notice, room), MARGIN_CELLS * Painter.TEXT_SIZE,
            (rows - ROWS_FROM_BOTTOM) * Painter.TEXT_SIZE, UiPalette.WARN);
    }

    static String fit(String notice, int room) {
        if (notice.length() <= room) {
            return notice;
        }
        if (room < CUT_MARK.length() + 1) {
            return notice.substring(0, room);
        }
        return notice.substring(0, room - CUT_MARK.length()) + CUT_MARK;
    }
}
