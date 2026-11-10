package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.derive.OverlayCheck;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGridLabels;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import com.github.halfdeadgames.asciigdx.workbench.ui.Blink;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;
import java.util.Objects;

/** The big pixel grid of the selected glyph, with labelled guide lines. */
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
    private static final Color LINE_COLOR = new Color(0.22f, 0.24f, 0.3f, 1f);
    private static final Color GUIDE_COLOR = new Color(0.3f, 0.85f, 0.9f, 1f);

    private static final int WIDTH_GUIDE_THICKNESS = 2;
    private static final int BAR_THICKNESS = 2;
    private static final int BAR_DASH = 8;
    private static final Color BAR_GUIDE_COLOR = new Color(1f, 0.3f, 0.85f, 1f);
    private static final Color MARKER_FILL = new Color(0xFF9A30FF);
    private static final Color MARKER_OUTLINE = new Color(0x3A2000FF);
    private static final Color WIDTH_GUIDE_COLOR = new Color(0.55f, 0.95f, 0.4f, 1f);

    private static final Color HIGHLIGHT_COLOR = new Color(1f, 0.95f, 0.15f, 1f);

    private static final float BAND_TINT_ALPHA = 0.14f;
    private static final Color BAR_TINT_COLOR = new Color(
        BAR_GUIDE_COLOR.r, BAR_GUIDE_COLOR.g, BAR_GUIDE_COLOR.b, BAND_TINT_ALPHA);
    private static final Color HIGHLIGHT_TINT_COLOR = new Color(
        HIGHLIGHT_COLOR.r, HIGHLIGHT_COLOR.g, HIGHLIGHT_COLOR.b, BAND_TINT_ALPHA);

    private final Painter painter;
    private PaintGuides.Listed highlighted;
    private static final int NO_GLYPH = -1;
    private int shownCodePoint = NO_GLYPH;
    private RulesFile rules = RulesFile.empty();
    private PaintGuides.Shown shown = new PaintGuides.Shown(List.of(), List.of(), List.of());
    private final FontEditor editor;
    private PaintGuides guides;
    private int left;
    private int top;
    private int pixelSize = MAX_PIXEL;
    private int areaWidth;
    private int areaHeight;
    private boolean layoutDirty = true;
    private boolean blinkOn = true;
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
        shownCodePoint = NO_GLYPH;
    }

    /** Switches to other rules, whose groups decide which guides show; the next draw filters them again. */
    void setRules(RulesFile newRules) {
        rules = newRules;
        shownCodePoint = NO_GLYPH;
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

    /** A pending overlay part drawn faded at its shifted position; pixels past the part's own width are outside the cell. */
    record Ghost(PixelGrid pixels, int shift, int down) { }

    void draw(int codePoint) {
        draw(codePoint, null, null, 0f);
    }

    /**
     * @param previewGlyph glyph to show instead of the slot's own (a derive preview), or null
     * @param ghost pending overlay part to show faded on top, or null
     * @param seconds running clock that drives the off-grid arrow blink
     */
    void draw(int codePoint, PixelGlyph previewGlyph, Ghost ghost, float seconds) {
        blinkOn = Blink.isOnAt(seconds);
        drawPixels(codePoint, previewGlyph);
        if (ghost != null) {
            drawGhost(ghost, seconds);
        }
        selectGuides(codePoint);
        if (layoutDirty) {
            layoutLabels();
        }
        drawBandTints();
        drawGridLines();
        drawMarkers(codePoint);
        drawGuides();
        drawWidthGuides();
        drawBarGuides();
        drawColumnBarGuides();
        drawHighlight();
    }

    private void drawPixels(int codePoint, PixelGlyph previewGlyph) {
        Color lit = previewGlyph != null || editor.isDerived(codePoint) ? DERIVED_LIT_FILL : LIT_FILL;
        for (int y = 0; y < editor.cellHeight(); y++) {
            for (int x = 0; x < editor.cellWidth(); x++) {
                boolean on = previewGlyph == null ? editor.isLit(codePoint, x, y) : isLit(previewGlyph, x, y);
                Color fill = on ? lit : UNLIT_FILL;
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

    private void drawBandTints() {
        drawShownBarTints();
        drawShownColumnBarTints();
        drawSelectedBandTint();
    }

    private void drawShownBarTints() {
        List<PaintGuides.BarGuide> bars = shown.bars();
        boolean selectedVisible = highlightVisible();
        for (int i = 0; i < bars.size(); i++) {
            PaintGuides.BarGuide bar = bars.get(i);
            if (selectedVisible && GuideBands.isSelected(highlighted, PaintGuides.ListedKind.BAR,
                bar.label(), bar.firstRow(), bar.rowCount())) {
                continue;
            }
            if (isBlinkedOut(shown.isBarBroken(i), false)) {
                continue;
            }
            tintRows(BAR_TINT_COLOR, bar.firstRow(), bar.rowCount());
        }
    }

    // A broken guide alternates; the selected one stays solid so it can be edited.
    private boolean isBlinkedOut(boolean broken, boolean selected) {
        return broken && !selected && !blinkOn;
    }

    private void drawShownColumnBarTints() {
        List<PaintGuides.ColumnBarGuide> columnBars = shown.columnBars();
        boolean selectedVisible = highlightVisible();
        for (int i = 0; i < columnBars.size(); i++) {
            PaintGuides.ColumnBarGuide bar = columnBars.get(i);
            if (selectedVisible && GuideBands.isSelected(highlighted, PaintGuides.ListedKind.COLUMN_BAR,
                bar.label(), bar.firstColumn(), bar.columnCount())) {
                continue;
            }
            if (isBlinkedOut(shown.isColumnBarBroken(i), false)) {
                continue;
            }
            tintColumns(BAR_TINT_COLOR, bar.firstColumn(), bar.columnCount());
        }
    }

    private void drawSelectedBandTint() {
        if (!highlightVisible()) {
            return;
        }
        if (highlighted.kind() == PaintGuides.ListedKind.BAR) {
            tintRows(HIGHLIGHT_TINT_COLOR, highlighted.position(), highlighted.rowCount());
        } else if (highlighted.kind() == PaintGuides.ListedKind.COLUMN_BAR) {
            tintColumns(HIGHLIGHT_TINT_COLOR, highlighted.position(), highlighted.rowCount());
        }
    }

    private void tintRows(Color tint, int firstRow, int rowCount) {
        painter.fill(tint, left, GuideBands.bandStart(top, firstRow, pixelSize),
            editor.cellWidth() * pixelSize, GuideBands.bandLength(rowCount, pixelSize));
    }

    private void tintColumns(Color tint, int firstColumn, int columnCount) {
        painter.fill(tint, GuideBands.bandStart(left, firstColumn, pixelSize), top,
            GuideBands.bandLength(columnCount, pixelSize), height());
    }

    private void drawBarGuides() {
        List<PaintGuides.BarGuide> bars = shown.bars();
        for (int i = 0; i < bars.size(); i++) {
            int slot = shown.rows().size() + i;
            if (isBlinkedOut(shown.isBarBroken(i), slot == labels.highlightRowSlot())) {
                continue;
            }
            drawBarGuide(bars.get(i), labels.rowLabelY(slot),
                slot == labels.highlightRowSlot() ? HIGHLIGHT_COLOR : BAR_GUIDE_COLOR);
        }
    }

    // Guides are filtered once per glyph, guides, or rules change, not per frame.
    private void selectGuides(int codePoint) {
        if (codePoint == shownCodePoint) {
            return;
        }
        shownCodePoint = codePoint;
        shown = guides.forGlyph(codePoint, rules, editor.cellWidth(), editor.cellHeight());
        layoutDirty = true;
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
                int row = y + ghost.down();
                if (column >= 0 && column < editor.cellWidth() && row >= 0 && row < editor.cellHeight()) {
                    boolean inside = column < pixels.getWidth() && row < pixels.getHeight();
                    painter.fill(inside ? UiPalette.GHOST : UiPalette.GHOST_OUTSIDE,
                        left + column * pixelSize, top + row * pixelSize, pixelSize, pixelSize);
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
            side = OverlayCheck.offGridRows(ghostFirstRow, ghostLastRow, ghost.down(), editor.cellHeight());
        }
        if (side == OverlayCheck.OffGrid.NONE) {
            return;
        }
        int gridRight = left + editor.cellWidth() * pixelSize;
        int gridBottom = top + editor.cellHeight() * pixelSize;
        int arrowTop = clamp(top + (ghostFirstRow + ghostLastRow + 1 + 2 * ghost.down()) * pixelSize / 2
            - painter.cell() / 2, top, gridBottom - painter.cell());
        int arrowLeft = clamp(left + (ghostFirstInk + ghostLastInk + 1 + 2 * ghost.shift()) * pixelSize / 2
            - painter.cell() / 2, left, gridRight - painter.cell());
        switch (side) {
            case RIGHT -> painter.text(">", gridRight - painter.cell(), arrowTop, UiPalette.WARN);
            case LEFT -> painter.text("<", left, arrowTop, UiPalette.WARN);
            case UP -> painter.text("^", arrowLeft, top, UiPalette.WARN);
            case DOWN -> painter.text("v", arrowLeft, gridBottom - painter.cell(), UiPalette.WARN);
            default -> { }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
            case COLUMN_BAR -> guide.position() + guide.rowCount() <= editor.cellWidth();
        };
    }

    private void drawWidthGuides() {
        int height = editor.cellHeight() * pixelSize;
        int overhang = overhang();
        int thickness = scaled(WIDTH_GUIDE_THICKNESS);
        List<PaintGuides.WidthGuide> widths = shown.widths();
        for (int i = 0; i < widths.size(); i++) {
            PaintGuides.WidthGuide guide = widths.get(i);
            if (isBlinkedOut(shown.isWidthBroken(i), i == labels.highlightWidthSlot())) {
                continue;
            }
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
        drawDashes(color, left - overhang(), left + editor.cellWidth() * pixelSize + overhang(),
            lineY - thickness / 2, thickness, false);
    }

    private void drawDashedColumn(Color color, int lineX, int thickness) {
        drawDashes(color, top - overhang(), top + height() + overhang(), lineX - thickness / 2, thickness, true);
    }

    // Dashes run from start to end along the line; crossStart is the line's near edge on the other axis.
    private void drawDashes(Color color, int start, int end, int crossStart, int thickness, boolean vertical) {
        int dash = scaled(BAR_DASH);
        for (int at = start; at < end; at += 2 * dash) {
            int length = Math.min(dash, end - at);
            if (vertical) {
                painter.fill(color, crossStart, at, thickness, length);
            } else {
                painter.fill(color, at, crossStart, length, thickness);
            }
        }
    }

    private void drawColumnBarGuides() {
        List<PaintGuides.ColumnBarGuide> columnBars = shown.columnBars();
        for (int i = 0; i < columnBars.size(); i++) {
            int slot = shown.widths().size() + i;
            if (isBlinkedOut(shown.isColumnBarBroken(i), slot == labels.highlightWidthSlot())) {
                continue;
            }
            drawColumnBarGuide(columnBars.get(i), labels.widthLabelX(slot),
                slot == labels.highlightWidthSlot() ? HIGHLIGHT_COLOR : BAR_GUIDE_COLOR);
        }
    }

    // Dashed so the lit pixels under it stay visible.
    private void drawColumnBarGuide(PaintGuides.ColumnBarGuide bar, int labelX, Color labelColor) {
        int lineX = left + Math.round(bar.centerColumn() * pixelSize);
        drawDashedColumn(BAR_GUIDE_COLOR, lineX, scaled(BAR_THICKNESS));
        drawWidthLabel(bar.label(), labelColor, lineX, labelX, bar.tier());
    }

    private void drawColumnBarHighlight(PaintGuides.Listed guide, int thickness) {
        int lineX = left + Math.round(guide.centerColumn() * pixelSize);
        drawDashedColumn(HIGHLIGHT_COLOR, lineX, thickness);
        if (highlightNeedsOwnWidthLabel()) {
            drawWidthLabel(guide.label(), HIGHLIGHT_COLOR, lineX,
                labels.widthLabelX(labels.highlightWidthSlot()), 0);
        }
    }

    // A highlight that is also a shown guide had its label drawn in the highlight colour with that guide.
    private boolean highlightNeedsOwnWidthLabel() {
        return labels.highlightWidthSlot() >= shown.widths().size() + shown.columnBars().size();
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
            case COLUMN_BAR -> drawColumnBarHighlight(guide, thickness);
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
        if (highlightNeedsOwnWidthLabel()) {
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
            if (isBlinkedOut(shown.isRowBroken(i), i == labels.highlightRowSlot())) {
                continue;
            }
            int lineY = top + guide.boundaryRow() * pixelSize;
            painter.fill(GUIDE_COLOR, left - overhang, lineY - thickness / 2,
                width + 2 * overhang, thickness);
            drawRowLabel(guide.label(), i == labels.highlightRowSlot() ? HIGHLIGHT_COLOR : GUIDE_COLOR,
                lineY, labels.rowLabelY(i));
        }
    }
}
