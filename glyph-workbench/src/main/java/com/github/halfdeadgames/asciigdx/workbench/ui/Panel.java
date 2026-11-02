package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.github.halfdeadgames.asciigdx.workbench.Painter;

/** A filled, framed box on the cell grid: single line, or double line and brighter when focused. */
public final class Panel {

    private static final int CELL = Painter.TEXT_SIZE;
    private static final int TITLE_INSET = 2;
    private static final int MIN_WIDTH_FOR_TITLE = 7;

    private Panel() {
    }

    /** The top edge, {@code width} cells, with " TITLE " inset after two cells; long titles are shortened. */
    public static String topEdge(int width, String title, boolean focus) {
        char horizontal = focus ? '═' : '─';
        StringBuilder edge = new StringBuilder(width);
        edge.append(focus ? '╔' : '┌');
        for (int i = 2; i < width; i++) {
            edge.append(horizontal);
        }
        edge.append(focus ? '╗' : '┐');
        String label = titleLabel(width, title);
        if (!label.isEmpty()) {
            edge.replace(TITLE_INSET, TITLE_INSET + label.length(), label);
        }
        return edge.toString();
    }

    /** The title as drawn, with a space on each side; empty when there is none or the panel is too narrow. */
    public static String titleLabel(int width, String title) {
        if (title == null || title.isEmpty() || width < MIN_WIDTH_FOR_TITLE) {
            return "";
        }
        int room = width - 2 * TITLE_INSET - 2;
        String shown = title.length() > room ? title.substring(0, room) : title;
        return " " + shown + " ";
    }

    public static String bottomEdge(int width, boolean focus) {
        char horizontal = focus ? '═' : '─';
        StringBuilder edge = new StringBuilder(width);
        edge.append(focus ? '╚' : '└');
        for (int i = 2; i < width; i++) {
            edge.append(horizontal);
        }
        edge.append(focus ? '╝' : '┘');
        return edge.toString();
    }

    /** Draws at a cell position; width and height are in cells, at least 2. */
    public static void draw(Painter painter, int x, int y, int width, int height, String title, boolean focus) {
        painter.fill(UiPalette.PANEL, x * CELL, y * CELL, width * CELL, height * CELL);
        var color = focus ? UiPalette.FOCUS : UiPalette.FRAME;
        painter.text(topEdge(width, title, focus), x * CELL, y * CELL, color);
        painter.text(bottomEdge(width, focus), x * CELL, (y + height - 1) * CELL, color);
        String vertical = focus ? "║" : "│";
        for (int row = 1; row < height - 1; row++) {
            painter.text(vertical, x * CELL, (y + row) * CELL, color);
            painter.text(vertical, (x + width - 1) * CELL, (y + row) * CELL, color);
        }
        String label = titleLabel(width, title);
        if (!label.isEmpty()) {
            painter.fill(UiPalette.PANEL, (x + TITLE_INSET) * CELL, y * CELL, label.length() * CELL, CELL);
            painter.text(label, (x + TITLE_INSET) * CELL, y * CELL, focus ? UiPalette.ACCENT : UiPalette.TITLE);
        }
    }
}
