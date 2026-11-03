package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;

/** The big pixel grid of the selected glyph, with shaded gap columns (lit gap ink shows as lit) and labelled guide lines. */
final class PaintGridView {

    static final int LABEL_CELLS = 5;
    private static final int MIN_PIXEL = 8;
    private static final int MAX_PIXEL = 96;
    private static final int GUIDE_THICKNESS = 2;
    private static final int GUIDE_OVERHANG = 12;

    private static final Color UNLIT_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color LIT_FILL = Color.WHITE;
    private static final Color DERIVED_LIT_FILL = UiPalette.DERIVED;
    private static final Color GAP_FILL = new Color(0.05f, 0.05f, 0.06f, 1f);
    private static final Color LINE_COLOR = new Color(0.22f, 0.24f, 0.3f, 1f);
    private static final Color GUIDE_COLOR = new Color(0.3f, 0.85f, 0.9f, 1f);

    /** Room above the grid for the two tiers of width labels. */
    static final int LABEL_ROWS_ABOVE = 3;
    private static final int WIDTH_GUIDE_THICKNESS = 2;
    private static final int BAR_THICKNESS = 2;
    private static final int BAR_DASH = 8;
    private static final Color BAR_GUIDE_COLOR = new Color(1f, 0.3f, 0.85f, 1f);
    private static final Color MARKER_FILL = new Color(0xFF9A30FF);
    private static final Color MARKER_OUTLINE = new Color(0x3A2000FF);
    private static final Color WIDTH_GUIDE_COLOR = new Color(0.55f, 0.95f, 0.4f, 1f);

    private final Painter painter;
    private String shownGroup = "";
    private PaintGuides.Shown shown = new PaintGuides.Shown(List.of(), List.of(), List.of());
    private final FontEditor editor;
    private final PaintGuides guides;
    private int left;
    private int top;
    private int pixelSize = MAX_PIXEL;

    PaintGridView(Painter painter, FontEditor editor, PaintGuides guides) {
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

    /** A pending overlay part drawn faded at its shifted position; pixels past the part's own width are outside the body. */
    record Ghost(PixelGrid pixels, int shift) { }

    void draw(int codePoint) {
        draw(codePoint, null, null, 0f);
    }

    /**
     * @param previewGlyph glyph to show instead of the slot's own (a derive preview), or null
     * @param ghost pending overlay part to show faded on top, or null
     * @param seconds running clock that drives the off-grid arrow blink
     */
    void draw(int codePoint, PixelGlyph previewGlyph, Ghost ghost, float seconds) {
        int bodyWidth = editor.bodyWidth();
        Color lit = previewGlyph != null || editor.isDerived(codePoint) ? DERIVED_LIT_FILL : LIT_FILL;
        for (int y = 0; y < editor.cellHeight(); y++) {
            for (int x = 0; x < editor.cellWidth(); x++) {
                boolean on = previewGlyph == null ? editor.isLit(codePoint, x, y) : isLit(previewGlyph, x, y);
                Color fill = on ? lit : x >= bodyWidth ? GAP_FILL : UNLIT_FILL;
                painter.fill(fill, left + x * pixelSize, top + y * pixelSize, pixelSize, pixelSize);
            }
        }
        if (ghost != null) {
            drawGhost(ghost, seconds);
        }
        for (int x = 0; x <= editor.cellWidth(); x++) {
            painter.fill(LINE_COLOR, left + x * pixelSize, top, 1, height());
        }
        for (int y = 0; y <= editor.cellHeight(); y++) {
            painter.fill(LINE_COLOR, left, top + y * pixelSize, editor.cellWidth() * pixelSize, 1);
        }
        drawMarkers(codePoint);
        selectGuides(FontRules.groupNameFor(codePoint));
        drawGuides();
        drawWidthGuides();
        for (PaintGuides.BarGuide bar : shown.bars()) {
            drawBarGuide(bar);
        }
    }

    // Guides are filtered once per group change, not per frame.
    private void selectGuides(String groupName) {
        if (!groupName.equals(shownGroup)) {
            shownGroup = groupName;
            shown = guides.forGroup(groupName, editor.cellWidth(), editor.cellHeight());
        }
    }

    // Only pixels inside the visible grid are drawn; ink wholly beyond one side gets a blinking arrow on that edge.
    private void drawGhost(Ghost ghost, float seconds) {
        PixelGrid pixels = ghost.pixels();
        int firstInk = Integer.MAX_VALUE;
        int lastInk = -1;
        int firstRow = Integer.MAX_VALUE;
        int lastRow = -1;
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < pixels.getWidth(); x++) {
                if (!pixels.isLit(x, y)) {
                    continue;
                }
                firstInk = Math.min(firstInk, x);
                lastInk = Math.max(lastInk, x);
                firstRow = Math.min(firstRow, y);
                lastRow = Math.max(lastRow, y);
                int column = x + ghost.shift();
                if (column >= 0 && column < editor.cellWidth() && y < editor.cellHeight()) {
                    boolean inside = column < pixels.getWidth();
                    painter.fill(inside ? UiPalette.GHOST : UiPalette.GHOST_OUTSIDE,
                        left + column * pixelSize, top + y * pixelSize, pixelSize, pixelSize);
                }
            }
        }
        if (lastInk < 0 || !OverlayCheck.blinkOn(seconds)) {
            return;
        }
        OverlayCheck.OffGrid side = OverlayCheck.offGrid(firstInk, lastInk, ghost.shift(), editor.cellWidth());
        if (side == OverlayCheck.OffGrid.NONE) {
            return;
        }
        int arrowTop = top + (firstRow + lastRow + 1) * pixelSize / 2 - Painter.TEXT_SIZE / 2;
        int gridRight = left + editor.cellWidth() * pixelSize;
        if (side == OverlayCheck.OffGrid.RIGHT) {
            painter.text(">", gridRight - Painter.TEXT_SIZE, arrowTop, UiPalette.WARN);
        } else {
            painter.text("<", left, arrowTop, UiPalette.WARN);
        }
    }

    private static boolean isLit(PixelGlyph glyph, int x, int y) {
        return x < glyph.pixels().getWidth() && y < glyph.pixels().getHeight() && glyph.pixels().isLit(x, y);
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

    private void drawWidthGuides() {
        int height = editor.cellHeight() * pixelSize;
        for (PaintGuides.WidthGuide guide : shown.widths()) {
            int lineX = left + guide.boundaryColumn() * pixelSize;
            painter.fill(WIDTH_GUIDE_COLOR, lineX - WIDTH_GUIDE_THICKNESS / 2, top - GUIDE_OVERHANG,
                WIDTH_GUIDE_THICKNESS, height + 2 * GUIDE_OVERHANG);
            int labelWidth = guide.label().length() * Painter.TEXT_SIZE;
            painter.text(guide.label(), lineX - labelWidth / 2,
                top - GUIDE_OVERHANG - (guide.tier() + 1) * Painter.TEXT_SIZE, WIDTH_GUIDE_COLOR);
        }
    }

    // Dashed so the lit pixels under it stay visible.
    private void drawBarGuide(PaintGuides.BarGuide bar) {
        int lineY = top + Math.round(bar.centerRow() * pixelSize);
        int width = editor.cellWidth() * pixelSize;
        int end = left + width + GUIDE_OVERHANG;
        for (int x = left - GUIDE_OVERHANG; x < end; x += 2 * BAR_DASH) {
            painter.fill(BAR_GUIDE_COLOR, x, lineY - BAR_THICKNESS / 2, Math.min(BAR_DASH, end - x), BAR_THICKNESS);
        }
        painter.text(bar.label(), left + width + GUIDE_OVERHANG + Painter.TEXT_SIZE / 2,
            lineY - Painter.TEXT_SIZE / 2, BAR_GUIDE_COLOR);
    }

    private void drawGuides() {
        int width = editor.cellWidth() * pixelSize;
        for (PaintGuides.Guide guide : shown.rows()) {
            int lineY = top + guide.boundaryRow() * pixelSize;
            painter.fill(GUIDE_COLOR, left - GUIDE_OVERHANG, lineY - GUIDE_THICKNESS / 2,
                width + 2 * GUIDE_OVERHANG, GUIDE_THICKNESS);
            painter.text(guide.label(), left + width + GUIDE_OVERHANG + Painter.TEXT_SIZE / 2,
                lineY - Painter.TEXT_SIZE / 2, GUIDE_COLOR);
        }
    }
}
