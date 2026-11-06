package com.github.halfdeadgames.asciigdx.workbench.ui;

/** A filled, framed box on the cell grid: single line, or double line and brighter when focused. */
public final class Panel {

    public static final int TEXT_PADDING = 1;
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

    /** How many title characters fit in a panel this many cells wide before the title is cut. */
    public static int titleRoom(int width) {
        return width - 2 * TITLE_INSET - 2;
    }

    /** The title as drawn, with a space on each side; empty when there is none or the panel is too narrow. */
    public static String titleLabel(int width, String title) {
        if (title == null || title.isEmpty() || width < MIN_WIDTH_FOR_TITLE) {
            return "";
        }
        int room = titleRoom(width);
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
    public static void draw(Painter painter, CellRect area, String title, boolean focus) {
        int x = area.x();
        int y = area.y();
        int width = area.width();
        int height = area.height();
        int cell = painter.cell();
        painter.fill(UiPalette.PANEL, x * cell, y * cell, width * cell, height * cell);
        var color = focus ? UiPalette.FOCUS : UiPalette.FRAME;
        painter.text(topEdge(width, title, focus), x * cell, y * cell, color);
        painter.text(bottomEdge(width, focus), x * cell, (y + height - 1) * cell, color);
        String vertical = focus ? "║" : "│";
        for (int row = 1; row < height - 1; row++) {
            painter.text(vertical, x * cell, (y + row) * cell, color);
            painter.text(vertical, (x + width - 1) * cell, (y + row) * cell, color);
        }
        String label = titleLabel(width, title);
        if (!label.isEmpty()) {
            painter.fill(UiPalette.PANEL, (x + TITLE_INSET) * cell, y * cell, label.length() * cell, cell);
            painter.text(label, (x + TITLE_INSET) * cell, y * cell, focus ? UiPalette.ACCENT : UiPalette.TITLE);
        }
    }
}
