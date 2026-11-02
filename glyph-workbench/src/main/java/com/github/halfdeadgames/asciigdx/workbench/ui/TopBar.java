package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.Painter;
import java.util.ArrayList;
import java.util.List;

/** The top strip: a breadcrumb such as GLYPH WORKBENCH ► SAIKSISTT ► PAINT 8, and an optional right-aligned status. */
public final class TopBar {

    public enum Kind { TITLE, TEXT, ACCENT, SEPARATOR }

    /** A piece of text at a cell column. */
    public record Segment(String text, int x, Kind kind) {
    }

    private static final int CELL = Painter.TEXT_SIZE;
    private static final String SEPARATOR = "►";

    private TopBar() {
    }

    /** The first crumb is the title colour, the last the accent colour, the rest plain; separators are dim. */
    public static List<Segment> layout(List<String> crumbs) {
        List<Segment> segments = new ArrayList<>();
        int x = 1;
        for (int i = 0; i < crumbs.size(); i++) {
            Kind kind = i == 0 ? Kind.TITLE : i == crumbs.size() - 1 ? Kind.ACCENT : Kind.TEXT;
            segments.add(new Segment(crumbs.get(i), x, kind));
            x += crumbs.get(i).length();
            if (i < crumbs.size() - 1) {
                segments.add(new Segment(SEPARATOR, x + 1, Kind.SEPARATOR));
                x += 3;
            }
        }
        return segments;
    }

    /** Column of a right-aligned text with one blank cell before the edge. */
    public static int rightX(int columns, String text) {
        return Math.max(0, columns - text.length() - 1);
    }

    public static void draw(Painter painter, int columns, int row, List<String> crumbs, String status, Color statusColor) {
        painter.fill(UiPalette.PANEL, 0, row * CELL, columns * CELL, CELL);
        for (Segment segment : layout(crumbs)) {
            Color color = switch (segment.kind()) {
                case TITLE -> UiPalette.TITLE;
                case TEXT -> UiPalette.TEXT;
                case ACCENT -> UiPalette.ACCENT;
                case SEPARATOR -> UiPalette.DIM;
            };
            painter.text(segment.text(), segment.x() * CELL, row * CELL, color);
        }
        if (status != null && !status.isEmpty()) {
            painter.text(status, rightX(columns, status) * CELL, row * CELL, statusColor);
        }
    }
}
