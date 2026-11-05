package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;
import java.util.Objects;

/** The big pixel grid of the selected glyph, with shaded gap columns (lit gap ink shows as lit) and labelled guide lines. */
final class PaintGridView {

    static final int LABEL_CELLS = 5;
    private static final int MIN_PIXEL = 2;
    private static final int MIN_PIXEL_FOR_MARKERS = 6;
    private static final int LABEL_GAP = 4;
    private static final int MAX_PIXEL = 96;
    private static final int GUIDE_THICKNESS = 2;
    private static final int LEADER_THICKNESS = 1;

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

    private static final Color HIGHLIGHT_COLOR = new Color(1f, 0.95f, 0.15f, 1f);

    private final Painter painter;
    private PaintGuides.Listed highlighted;
    private String shownGroup = "";
    private PaintGuides.Shown shown = new PaintGuides.Shown(List.of(), List.of(), List.of());
    private final FontEditor editor;
    private PaintGuides guides;
    private int left;
    private int top;
    private int pixelSize = MAX_PIXEL;
    private int areaWidth;
    private int areaHeight;
    private boolean layoutDirty = true;
    private final PaintGridLabels labels = new PaintGridLabels();
    private int ghostFirstInk;
    private int ghostLastInk;
    private int ghostFirstRow;
    private int ghostLastRow;

    PaintGridView(Painter painter, FontEditor editor, PaintGuides guides) {
        this.painter = painter;
        this.editor = editor;
        this.guides = guides;
    }

    // The thickness and gap constants are tuned at the default scale.
    private int scaled(int pixelsAtDefault) {
        return painter.scale().scaled(pixelsAtDefault);
    }

    private int overhang() {
        return painter.scale().half();
    }

    private int highlightThickness() {
        return painter.scale().px(2);
    }

    /** Switches to other guides; the next draw filters them again. */
    void setGuides(PaintGuides newGuides) {
        guides = newGuides;
        shownGroup = "";
    }

    /** Picks the largest pixel size that fits the area, leaving room on the right for guide labels. */
    void fitTo(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        int usableWidth = areaWidth - LABEL_CELLS * painter.cell();
        int fit = Math.min(usableWidth / editor.cellWidth(), areaHeight / editor.cellHeight());
        pixelSize = Math.max(MIN_PIXEL, Math.min(MAX_PIXEL, fit));
        left = areaLeft;
        top = areaTop;
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        layoutDirty = true;
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
        drawPixels(codePoint, previewGlyph);
        if (ghost != null) {
            drawGhost(ghost, seconds);
        }
        drawGridLines();
        drawMarkers(codePoint);
        selectGuides(FontRules.groupNameFor(codePoint));
        if (layoutDirty) {
            layoutLabels();
        }
        drawGuides();
        drawWidthGuides();
        drawBarGuides();
        drawHighlight();
    }

    private void drawPixels(int codePoint, PixelGlyph previewGlyph) {
        int bodyWidth = editor.bodyWidth();
        Color lit = previewGlyph != null || editor.isDerived(codePoint) ? DERIVED_LIT_FILL : LIT_FILL;
        for (int y = 0; y < editor.cellHeight(); y++) {
            for (int x = 0; x < editor.cellWidth(); x++) {
                boolean on = previewGlyph == null ? editor.isLit(codePoint, x, y) : isLit(previewGlyph, x, y);
                Color fill = on ? lit : x >= bodyWidth ? GAP_FILL : UNLIT_FILL;
                painter.fill(fill, left + x * pixelSize, top + y * pixelSize, pixelSize, pixelSize);
            }
        }
    }

    private void drawGridLines() {
        for (int x = 0; x <= editor.cellWidth(); x++) {
            painter.fill(LINE_COLOR, left + x * pixelSize, top, 1, height());
        }
        for (int y = 0; y <= editor.cellHeight(); y++) {
            painter.fill(LINE_COLOR, left, top + y * pixelSize, editor.cellWidth() * pixelSize, 1);
        }
    }

    private void drawBarGuides() {
        List<PaintGuides.BarGuide> bars = shown.bars();
        for (int i = 0; i < bars.size(); i++) {
            int slot = shown.rows().size() + i;
            drawBarGuide(bars.get(i), labels.rowLabelY(slot),
                slot == labels.highlightRowSlot() ? HIGHLIGHT_COLOR : BAR_GUIDE_COLOR);
        }
    }

    // Guides are filtered once per group change, not per frame.
    private void selectGuides(String groupName) {
        if (!groupName.equals(shownGroup)) {
            shownGroup = groupName;
            shown = guides.forGroup(groupName, editor.cellWidth(), editor.cellHeight());
            layoutDirty = true;
        }
    }

    // Only pixels inside the visible grid are drawn; ink wholly beyond one side gets a blinking arrow on that edge.
    private void drawGhost(Ghost ghost, float seconds) {
        drawGhostPixels(ghost);
        measureGhostInk(ghost.pixels());
        drawOffGridArrow(ghost, seconds);
    }

    private void drawGhostPixels(Ghost ghost) {
        PixelGrid pixels = ghost.pixels();
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < pixels.getWidth(); x++) {
                if (!pixels.isLit(x, y)) {
                    continue;
                }
                int column = x + ghost.shift();
                if (column >= 0 && column < editor.cellWidth() && y < editor.cellHeight()) {
                    boolean inside = column < pixels.getWidth();
                    painter.fill(inside ? UiPalette.GHOST : UiPalette.GHOST_OUTSIDE,
                        left + column * pixelSize, top + y * pixelSize, pixelSize, pixelSize);
                }
            }
        }
    }

    private void measureGhostInk(PixelGrid pixels) {
        ghostFirstInk = Integer.MAX_VALUE;
        ghostLastInk = -1;
        ghostFirstRow = Integer.MAX_VALUE;
        ghostLastRow = -1;
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < pixels.getWidth(); x++) {
                if (!pixels.isLit(x, y)) {
                    continue;
                }
                ghostFirstInk = Math.min(ghostFirstInk, x);
                ghostLastInk = Math.max(ghostLastInk, x);
                ghostFirstRow = Math.min(ghostFirstRow, y);
                ghostLastRow = Math.max(ghostLastRow, y);
            }
        }
    }

    private void drawOffGridArrow(Ghost ghost, float seconds) {
        if (ghostLastInk < 0 || !OverlayCheck.blinkOn(seconds)) {
            return;
        }
        OverlayCheck.OffGrid side =
            OverlayCheck.offGrid(ghostFirstInk, ghostLastInk, ghost.shift(), editor.cellWidth());
        if (side == OverlayCheck.OffGrid.NONE) {
            return;
        }
        int arrowTop = top + (ghostFirstRow + ghostLastRow + 1) * pixelSize / 2 - painter.cell() / 2;
        int gridRight = left + editor.cellWidth() * pixelSize;
        if (side == OverlayCheck.OffGrid.RIGHT) {
            painter.text(">", gridRight - painter.cell(), arrowTop, UiPalette.WARN);
        } else {
            painter.text("<", left, arrowTop, UiPalette.WARN);
        }
    }

    private static boolean isLit(PixelGlyph glyph, int x, int y) {
        return x < glyph.pixels().getWidth() && y < glyph.pixels().getHeight() && glyph.pixels().isLit(x, y);
    }

    // Orange with a dark outline reads on both white and dark pixels.
    private void drawMarkers(int codePoint) {
        if (pixelSize < MIN_PIXEL_FOR_MARKERS) {
            return;
        }
        int marker = Math.max(scaled(2), pixelSize / 4);
        int inset = (pixelSize - marker) / 2;
        int outline = scaled(1);
        for (int y = 0; y < editor.cellHeight(); y++) {
            for (int x = 0; x < editor.cellWidth(); x++) {
                if (editor.wasLitWhenSaved(codePoint, x, y)) {
                    int markerLeft = left + x * pixelSize + inset;
                    int markerTop = top + y * pixelSize + inset;
                    painter.fill(MARKER_OUTLINE, markerLeft - outline, markerTop - outline,
                        marker + 2 * outline, marker + 2 * outline);
                    painter.fill(MARKER_FILL, markerLeft, markerTop, marker, marker);
                }
            }
        }
    }

    private void layoutLabels() {
        layoutDirty = false;
        PaintGuides.Listed mark = highlightVisible() ? highlighted : null;
        labels.layout(shown, mark, new PaintGridLabels.Frame(left, top, pixelSize, areaWidth, areaHeight,
            painter.cell(), scaled(LABEL_GAP)));
    }

    // Guides beyond the cell are not drawn, so they take no label slot either.
    private boolean highlightVisible() {
        PaintGuides.Listed guide = highlighted;
        if (guide == null) {
            return false;
        }
        return switch (guide.kind()) {
            case ROW -> guide.position() <= editor.cellHeight();
            case COLUMN -> guide.position() <= editor.cellWidth();
            case BAR -> guide.position() + guide.rowCount() <= editor.cellHeight();
        };
    }

    private void drawWidthGuides() {
        int height = editor.cellHeight() * pixelSize;
        int overhang = overhang();
        int thickness = scaled(WIDTH_GUIDE_THICKNESS);
        List<PaintGuides.WidthGuide> widths = shown.widths();
        for (int i = 0; i < widths.size(); i++) {
            PaintGuides.WidthGuide guide = widths.get(i);
            int lineX = left + guide.boundaryColumn() * pixelSize;
            painter.fill(WIDTH_GUIDE_COLOR, lineX - thickness / 2, top - overhang,
                thickness, height + 2 * overhang);
            drawWidthLabel(guide.label(), i == labels.highlightWidthSlot() ? HIGHLIGHT_COLOR : WIDTH_GUIDE_COLOR,
                lineX, labels.widthLabelX(i), guide.tier());
        }
    }

    private void drawWidthLabel(String text, Color color, int lineX, int labelX, int tier) {
        int cell = painter.cell();
        int labelWidth = text.length() * cell;
        int labelY = top - overhang() - (tier + 1) * cell;
        painter.text(text, labelX, labelY, color);
        if (labelX != lineX - labelWidth / 2) {
            // Underline the moved label out to the guide, then drop a stem down to the guide's end.
            int underlineY = labelY + cell;
            int leader = scaled(LEADER_THICKNESS);
            painter.fill(color, Math.min(lineX, labelX), underlineY,
                Math.max(lineX, labelX + labelWidth) - Math.min(lineX, labelX), leader);
            painter.fill(color, lineX, underlineY, leader, top - overhang() - underlineY);
        }
    }

    // Draws the label of a right-hand guide at labelY; a label moved off its guide gets a thin elbow leader.
    private void drawRowLabel(String text, Color color, int lineY, int labelY) {
        int cell = painter.cell();
        int guideEnd = left + editor.cellWidth() * pixelSize + overhang();
        int labelX = guideEnd + cell / 2;
        painter.text(text, labelX, labelY, color);
        int labelCenter = labelY + cell / 2;
        if (labelCenter == lineY) {
            return;
        }
        int leader = scaled(LEADER_THICKNESS);
        int elbowX = guideEnd + cell / 4;
        painter.fill(color, guideEnd, lineY, elbowX - guideEnd + leader, leader);
        painter.fill(color, elbowX, Math.min(lineY, labelCenter), leader, Math.abs(labelCenter - lineY));
        painter.fill(color, elbowX, labelCenter, labelX - elbowX, leader);
    }

    // Dashed so the lit pixels under it stay visible.
    private void drawBarGuide(PaintGuides.BarGuide bar, int labelY, Color labelColor) {
        int lineY = top + Math.round(bar.centerRow() * pixelSize);
        drawDashedLine(BAR_GUIDE_COLOR, lineY, scaled(BAR_THICKNESS));
        drawRowLabel(bar.label(), labelColor, lineY, labelY);
    }

    private void drawDashedLine(Color color, int lineY, int thickness) {
        int end = left + editor.cellWidth() * pixelSize + overhang();
        int dash = scaled(BAR_DASH);
        for (int x = left - overhang(); x < end; x += 2 * dash) {
            painter.fill(color, x, lineY - thickness / 2, Math.min(dash, end - x), thickness);
        }
    }

    /** Guide to draw thick and bright whatever its groups; null for none. Guides beyond the cell are not drawn. */
    void setHighlight(PaintGuides.Listed guide) {
        if (!Objects.equals(highlighted, guide)) {
            layoutDirty = true;
        }
        highlighted = guide;
    }

    private void drawHighlight() {
        if (!highlightVisible()) {
            return;
        }
        PaintGuides.Listed guide = highlighted;
        int thickness = highlightThickness();
        switch (guide.kind()) {
            case ROW -> drawRowHighlight(guide, thickness);
            case COLUMN -> drawColumnHighlight(guide, thickness);
            case BAR -> drawBarHighlight(guide, thickness);
        }
    }

    // A highlight that is also a shown guide had its label drawn in the highlight colour with that guide.
    private boolean highlightNeedsOwnRowLabel() {
        return labels.highlightRowSlot() >= shown.rows().size() + shown.bars().size();
    }

    private void drawRowHighlight(PaintGuides.Listed guide, int thickness) {
        int overhang = overhang();
        int lineY = top + guide.position() * pixelSize;
        painter.fill(HIGHLIGHT_COLOR, left - overhang, lineY - thickness / 2,
            editor.cellWidth() * pixelSize + 2 * overhang, thickness);
        if (highlightNeedsOwnRowLabel()) {
            drawRowLabel(guide.label(), HIGHLIGHT_COLOR, lineY, labels.rowLabelY(labels.highlightRowSlot()));
        }
    }

    private void drawColumnHighlight(PaintGuides.Listed guide, int thickness) {
        int lineX = left + guide.position() * pixelSize;
        painter.fill(HIGHLIGHT_COLOR, lineX - thickness / 2, top - overhang(),
            thickness, height() + 2 * overhang());
        if (labels.highlightWidthSlot() >= shown.widths().size()) {
            drawWidthLabel(guide.label(), HIGHLIGHT_COLOR, lineX,
                labels.widthLabelX(labels.highlightWidthSlot()), 0);
        }
    }

    private void drawBarHighlight(PaintGuides.Listed guide, int thickness) {
        int lineY = top + Math.round(guide.centerRow() * pixelSize);
        drawDashedLine(HIGHLIGHT_COLOR, lineY, thickness);
        if (highlightNeedsOwnRowLabel()) {
            drawRowLabel(guide.label(), HIGHLIGHT_COLOR, lineY, labels.rowLabelY(labels.highlightRowSlot()));
        }
    }

    private void drawGuides() {
        int width = editor.cellWidth() * pixelSize;
        int overhang = overhang();
        int thickness = scaled(GUIDE_THICKNESS);
        List<PaintGuides.Guide> rows = shown.rows();
        for (int i = 0; i < rows.size(); i++) {
            PaintGuides.Guide guide = rows.get(i);
            int lineY = top + guide.boundaryRow() * pixelSize;
            painter.fill(GUIDE_COLOR, left - overhang, lineY - thickness / 2,
                width + 2 * overhang, thickness);
            drawRowLabel(guide.label(), i == labels.highlightRowSlot() ? HIGHLIGHT_COLOR : GUIDE_COLOR,
                lineY, labels.rowLabelY(i));
        }
    }
}
