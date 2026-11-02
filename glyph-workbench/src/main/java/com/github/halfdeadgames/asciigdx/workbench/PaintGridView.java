package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import java.util.List;

/** The big pixel grid of the selected glyph, with shaded gap columns and labelled guide lines. */
final class PaintGridView {

    static final int LABEL_CELLS = 5;
    private static final int MIN_PIXEL = 8;
    private static final int MAX_PIXEL = 48;
    private static final int GUIDE_THICKNESS = 2;
    private static final int GUIDE_OVERHANG = 12;

    private static final Color UNLIT_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color LIT_FILL = Color.WHITE;
    private static final Color DERIVED_LIT_FILL = new Color(0.75f, 0.6f, 1f, 1f);
    private static final Color GAP_FILL = new Color(0.05f, 0.05f, 0.06f, 1f);
    private static final Color LINE_COLOR = new Color(0.22f, 0.24f, 0.3f, 1f);
    private static final Color GUIDE_COLOR = new Color(0.3f, 0.85f, 0.9f, 1f);

    /** Room above the grid for the two tiers of width labels. */
    static final int LABEL_ROWS_ABOVE = 3;
    private static final int WIDTH_GUIDE_THICKNESS = 2;
    private static final Color MARKER_FILL = new Color(0xFF9A30FF);
    private static final Color MARKER_OUTLINE = new Color(0x3A2000FF);
    private static final Color WIDTH_GUIDE_COLOR = new Color(0.55f, 0.95f, 0.4f, 1f);

    private final Painter painter;
    private String widthGuideGroup = "";
    private List<PaintGuides.WidthGuide> widthGuides = List.of();
    private final FontEditor editor;
    private final List<PaintGuides.Guide> guides;
    private int left;
    private int top;
    private int pixelSize = MAX_PIXEL;

    PaintGridView(Painter painter, FontEditor editor, List<PaintGuides.Guide> guides) {
        this.painter = painter;
        this.editor = editor;
        this.guides = guides;
    }

    /** Picks the largest pixel size that fits the area, leaving room on the right for guide labels. */
    void fitTo(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        int usableWidth = areaWidth - LABEL_CELLS * Painter.TEXT_SIZE;
        int fit = Math.min(usableWidth / editor.cellWidth(), areaHeight / editor.cellHeight());
        pixelSize = Math.max(MIN_PIXEL, Math.min(MAX_PIXEL, fit));
        left = areaLeft;
        top = areaTop;
    }

    int height() {
        return pixelSize * editor.cellHeight();
    }

    /** Column under a pixel; outside the grid it is below 0 or at least the cell width. */
    int columnAt(int pixelX) {
        return Math.floorDiv(pixelX - left, pixelSize);
    }

    int rowAt(int pixelY) {
        return Math.floorDiv(pixelY - top, pixelSize);
    }

    void draw(int codePoint) {
        int drawnWidth = editor.drawnWidth(codePoint);
        Color lit = editor.isDerived(codePoint) ? DERIVED_LIT_FILL : LIT_FILL;
        for (int y = 0; y < editor.cellHeight(); y++) {
            for (int x = 0; x < editor.cellWidth(); x++) {
                Color fill = x >= drawnWidth ? GAP_FILL : editor.isLit(codePoint, x, y) ? lit : UNLIT_FILL;
                painter.fill(fill, left + x * pixelSize, top + y * pixelSize, pixelSize, pixelSize);
            }
        }
        for (int x = 0; x <= editor.cellWidth(); x++) {
            painter.fill(LINE_COLOR, left + x * pixelSize, top, 1, height());
        }
        for (int y = 0; y <= editor.cellHeight(); y++) {
            painter.fill(LINE_COLOR, left, top + y * pixelSize, editor.cellWidth() * pixelSize, 1);
        }
        drawMarkers(codePoint);
        drawGuides();
        drawWidthGuides(FontRules.groupNameFor(codePoint));
    }

    // Orange with a dark outline reads on both white and dark pixels.
    private void drawMarkers(int codePoint) {
        int marker = Math.max(2, pixelSize / 4);
        int inset = (pixelSize - marker) / 2;
        for (int y = 0; y < editor.cellHeight(); y++) {
            for (int x = 0; x < editor.cellWidth(); x++) {
                if (editor.wasLitWhenSaved(codePoint, x, y)) {
                    int markerLeft = left + x * pixelSize + inset;
                    int markerTop = top + y * pixelSize + inset;
                    painter.fill(MARKER_OUTLINE, markerLeft - 1, markerTop - 1, marker + 2, marker + 2);
                    painter.fill(MARKER_FILL, markerLeft, markerTop, marker, marker);
                }
            }
        }
    }

    private void drawWidthGuides(String groupName) {
        if (!groupName.equals(widthGuideGroup)) {
            widthGuideGroup = groupName;
            widthGuides = PaintGuides.saiksisttWidths(groupName, editor.cellWidth());
        }
        int height = editor.cellHeight() * pixelSize;
        for (PaintGuides.WidthGuide guide : widthGuides) {
            int lineX = left + guide.boundaryColumn() * pixelSize;
            painter.fill(WIDTH_GUIDE_COLOR, lineX - WIDTH_GUIDE_THICKNESS / 2, top - GUIDE_OVERHANG,
                WIDTH_GUIDE_THICKNESS, height + 2 * GUIDE_OVERHANG);
            int labelWidth = guide.label().length() * Painter.TEXT_SIZE;
            painter.text(guide.label(), lineX - labelWidth / 2,
                top - GUIDE_OVERHANG - (guide.tier() + 1) * Painter.TEXT_SIZE, WIDTH_GUIDE_COLOR);
        }
    }

    private void drawGuides() {
        int width = editor.cellWidth() * pixelSize;
        for (PaintGuides.Guide guide : guides) {
            if (guide.boundaryRow() > editor.cellHeight()) {
                continue;
            }
            int lineY = top + guide.boundaryRow() * pixelSize;
            painter.fill(GUIDE_COLOR, left - GUIDE_OVERHANG, lineY - GUIDE_THICKNESS / 2,
                width + 2 * GUIDE_OVERHANG, GUIDE_THICKNESS);
            painter.text(guide.label(), left + width + GUIDE_OVERHANG + Painter.TEXT_SIZE / 2,
                lineY - Painter.TEXT_SIZE / 2, GUIDE_COLOR);
        }
    }
}
