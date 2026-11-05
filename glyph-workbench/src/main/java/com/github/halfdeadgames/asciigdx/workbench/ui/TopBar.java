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

    private static final String SEPARATOR = "►";
    private static final int DEFAULT_START = 1;

    private TopBar() {
    }

    /** The first crumb is the title colour, the last the accent colour, the rest plain; separators are dim. */
    public static List<Segment> layout(List<String> crumbs) {
        List<Segment> segments = new ArrayList<>();
        int x = DEFAULT_START;
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

    /** Drops the left-most crumbs until the rest fit the cells; the last crumb always stays. */
    public static List<String> fit(List<String> crumbs, int availableCells) {
        int start = 0;
        while (start < crumbs.size() - 1 && width(crumbs, start) > availableCells) {
            start++;
        }
        return crumbs.subList(start, crumbs.size());
    }

    private static int width(List<String> crumbs, int start) {
        int width = DEFAULT_START + 3 * (crumbs.size() - 1 - start);
        for (int i = start; i < crumbs.size(); i++) {
            width += crumbs.get(i).length();
        }
        return width;
    }

    /** Column of a right-aligned text with one blank cell before the edge. */
    public static int rightX(int columns, String text) {
        return Math.max(0, columns - text.length() - 1);
    }

    /** Also draws a plain tag just left of the status; the crumbs shorten to leave room for both. */
    public static void draw(Painter painter, int columns, int row, List<String> crumbs, TopBarStatus barStatus) {
        String tag = barStatus.tag();
        String status = barStatus.text();
        Color statusColor = barStatus.color();
        int cell = painter.cell();
        painter.fill(UiPalette.PANEL, 0, row * cell, columns * cell, cell);
        boolean hasStatus = isPresent(status);
        boolean hasTag = isPresent(tag);
        int available = columns - 1 - rightWidth(hasTag ? tag : "", hasStatus ? status : "");
        for (Segment segment : layout(fit(crumbs, available))) {
            painter.text(segment.text(), segment.x() * cell, row * cell, colorOf(segment.kind()));
        }
        if (hasTag) {
            painter.text(tag, tagX(columns, tag, hasStatus ? status : "") * cell, row * cell, UiPalette.TEXT);
        }
        if (hasStatus) {
            painter.text(status, rightX(columns, status) * cell, row * cell, statusColor);
        }
    }

    private static boolean isPresent(String text) {
        return text != null && !text.isEmpty();
    }

    private static Color colorOf(Kind kind) {
        return switch (kind) {
            case TITLE -> UiPalette.TITLE;
            case TEXT -> UiPalette.TEXT;
            case ACCENT -> UiPalette.ACCENT;
            case SEPARATOR -> UiPalette.DIM;
        };
    }

    /** Cells taken at the right edge by a tag and a status, each followed by one blank cell. */
    static int rightWidth(String tag, String status) {
        return (tag.isEmpty() ? 0 : tag.length() + 1) + (status.isEmpty() ? 0 : status.length() + 1);
    }

    /** Column of a tag that sits left of the status, with one blank cell between them. */
    static int tagX(int columns, String tag, String status) {
        return Math.max(0, columns - rightWidth(tag, status));
    }
}
