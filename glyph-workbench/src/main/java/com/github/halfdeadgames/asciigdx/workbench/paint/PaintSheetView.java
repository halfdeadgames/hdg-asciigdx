package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollBarHit;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;

/** The scrollable sheet of all slots on the left of the paint screen. */
final class PaintSheetView {

    static final int COLUMNS = 16;
    static final int GAP = 1;
    private static final int OUTLINE = 2;

    private static final Color CELL_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color EMPTY_OUTLINE = new Color(0.4f, 0.42f, 0.55f, 1f);
    private static final Color GLYPH_COLOR = Color.WHITE;
    private static final Color DERIVED_COLOR = UiPalette.DERIVED;
    private static final Color CURSOR_COLOR = Color.YELLOW;
    private static final Color SOURCE_CURSOR_COLOR = UiPalette.ORANGE;

    private final Painter painter;
    private final FontEditor editor;

    private final int frameLeft;
    private final int topRows;
    private int rows;
    private SheetGridLayout layout;
    private int layoutCell;
    private final ScrollState scroll;
    // Reuses the row scroller with glyph columns as its rows.
    private final ScrollState columnScroll = new ScrollState(COLUMNS);
    private boolean columnsChanged;
    private boolean draggingColumnBar;
    private int columnGrabOffset;
    private final StringBuilder barGlyph = new StringBuilder(1);
    private int visibleColumns;
    private int frameCellsWide;
    private int minimumFrameCells;
    private String title;
    private String shortTitle;
    private int frameCellsHigh;
    private int barLeft;
    private int barCells;

    PaintSheetView(Painter painter, FontEditor editor, int frameLeft, int topRows) {
        this.painter = painter;
        this.editor = editor;
        this.frameLeft = frameLeft;
        this.topRows = topRows;
        rows = editor.slotCount() / COLUMNS;
        scroll = new ScrollState(rows);
        setVisibleColumns(COLUMNS);
    }

    private int cellPixels() {
        return painter.cellPixels();
    }

    private int frameTop() {
        return topRows * cellPixels();
    }

    // Whole-number zoom that makes a glyph's longer side at least one text cell.
    private int pixelScale() {
        return pixelScaleFor(editor.cellWidth(), editor.cellHeight(), cellPixels());
    }

    private static int pixelScaleFor(int glyphWidth, int glyphHeight, int cellPixels) {
        return Math.max(1,
                Math.min(ceilDiv(cellPixels, glyphWidth), ceilDiv(cellPixels, glyphHeight)));
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    /** Pixel width of one slot on the sheet at the given cell size. */
    static int slotWidthFor(int glyphWidth, int glyphHeight, int cellPixels) {
        return glyphWidth * pixelScaleFor(glyphWidth, glyphHeight, cellPixels);
    }

    /** Pixel width of one slot on the sheet. */
    int slotWidth() {
        return slotWidthFor(editor.cellWidth(), editor.cellHeight(), cellPixels());
    }

    /**
     * Shows a window of glyph columns; the frame shrinks to fit them. Slot numbers do not change.
     */
    void setVisibleColumns(int columns) {
        int clamped = Math.max(1, Math.min(COLUMNS, columns));
        if (clamped == visibleColumns && layoutCell == cellPixels()) {
            return;
        }
        visibleColumns = clamped;
        layoutCell = cellPixels();
        columnScroll.setVisibleRows(visibleColumns);
        columnsChanged = true;
        frameCellsWide = Math.max(minimumFrameCells,
                PaintColumns.sheetCells(visibleColumns, slotWidth(), GAP, cellPixels()));
        int inset =
                PaintColumns.panelInset(frameCellsWide, visibleColumns * (slotWidth() + GAP) + GAP,
                        cellPixels());
        layout = new SheetGridLayout(frameLeft + cellPixels() + inset, frameTop() + cellPixels(),
                new GlyphSize(slotWidth(), editor.cellHeight() * pixelScale()), GAP, COLUMNS,
                visibleColumns, rows);
        barLeft = frameLeft + (frameCellsWide - 2) * cellPixels();
    }

    /** Keeps the frame at least this many cells wide; the glyph grid stays centered in it. */
    void setMinimumFrameCells(int cells) {
        if (cells == minimumFrameCells) {
            return;
        }
        minimumFrameCells = cells;
        layoutCell = -1;
        setVisibleColumns(visibleColumns);
    }

    /**
     * Replaces the default "SHEET" frame title; {@code shortTitle} is shown when the frame is too
     * narrow for it.
     */
    void setTitle(String title, String shortTitle) {
        this.title = title;
        this.shortTitle = shortTitle;
    }

    /** Width of the whole frame in cells. */
    int frameCells() {
        return frameCellsWide;
    }

    private String frameTitle() {
        if (title == null) {
            return PaintColumns.SHEET_TITLE;
        }
        return title.length() <= Panel.titleRoom(frameCellsWide) ? title : shortTitle;
    }

    /**
     * Picks up slots the editor added: the row count, scroll limits, and grid layout follow it.
     */
    void refreshSlots() {
        rows = editor.slotCount() / COLUMNS;
        scroll.setTotalRows(rows);
        layoutCell = -1;
        setVisibleColumns(visibleColumns);
    }

    ScrollState scroll() {
        return scroll;
    }

    ScrollState columnScroll() {
        return columnScroll;
    }

    int frameRight() {
        return frameLeft + frameCellsWide * cellPixels();
    }

    /** Scrolls both ways, the least amount that shows the slot. */
    void keepVisible(int slot) {
        scroll.keepVisible(slot / COLUMNS);
        columnScroll.keepVisible(slot % COLUMNS);
    }

    private boolean columnBarShown() {
        return columnScroll.needed();
    }

    /**
     * Fits the frame to the window height, down to the bottom row when the key bar is hidden.
     * Keeps the cursor visible when asked or when the number of visible rows changed.
     */
    void fitToHeight(int worldHeight, boolean statusBarShown, int cursorSlot,
            boolean revealCursor) {
        frameCellsHigh = PaintLayout.sheetRows(worldHeight, cellPixels(), statusBarShown, topRows);
        barCells = Math.max(1, frameCellsHigh - 2 - (columnBarShown() ? 1 : 0));
        int before = scroll.visibleRows();
        scroll.setVisibleRows(layout.rowsThatFit(barCells * cellPixels()));
        if (revealCursor || before != scroll.visibleRows()) {
            scroll.keepVisible(cursorSlot / COLUMNS);
        }
        if (revealCursor || columnsChanged) {
            columnScroll.keepVisible(cursorSlot % COLUMNS);
            columnsChanged = false;
        }
    }

    boolean contains(int pixelX, int pixelY) {
        return pixelX >= frameLeft && pixelX < frameRight()
                && pixelY >= frameTop() && pixelY < frameTop() + frameCellsHigh * cellPixels();
    }

    /** Slot under a pixel, or -1. */
    int slotAt(int pixelX, int pixelY) {
        return layout.slotAt(pixelX, pixelY, scroll.offset(), scroll.visibleRows(),
                columnScroll.offset());
    }

    /** Handles a click on the scroll bar arrows or track. */
    void clickBar(int pixelX, int pixelY) {
        int barTop = layout.top();
        if (!scroll.needed() || pixelX < barLeft || pixelX >= barLeft + cellPixels()
                || pixelY < barTop || pixelY >= barTop + barCells * cellPixels()) {
            return;
        }
        int barCell = (pixelY - barTop) / cellPixels();
        ScrollBarHit.click(scroll, ScrollBarHit.part(scroll, barCell, barCells));
    }

    private int columnBarTop() {
        return frameTop() + (frameCellsHigh - 2) * cellPixels();
    }

    private int columnBarCells() {
        return frameCellsWide - 3;
    }

    private int columnBarCellAt(int pixelX, int pixelY) {
        int barTop = columnBarTop();
        if (!columnBarShown() || pixelY < barTop || pixelY >= barTop + cellPixels()
                || pixelX < frameLeft + cellPixels()
                || pixelX >= frameLeft + cellPixels() + columnBarCells() * cellPixels()) {
            return -1;
        }
        return (pixelX - frameLeft - cellPixels()) / cellPixels();
    }

    /**
     * Handles a press on the glyph column bar; true when the press landed on it. A press on the
     * thumb starts a drag.
     */
    boolean pressColumnBar(int pixelX, int pixelY) {
        int barCell = columnBarCellAt(pixelX, pixelY);
        if (barCell < 0) {
            return false;
        }
        ScrollBarHit.Part part = ScrollBarHit.part(columnScroll, barCell, columnBarCells());
        if (part == ScrollBarHit.Part.THUMB) {
            draggingColumnBar = true;
            columnGrabOffset = ScrollBarHit.grabOffset(columnScroll, barCell, columnBarCells());
        } else {
            ScrollBarHit.click(columnScroll, part);
        }
        return true;
    }

    boolean isDraggingColumnBar() {
        return draggingColumnBar;
    }

    /** Moves the thumb with the pointer; the pointer may leave the bar sideways. */
    void dragColumnBar(int pixelX) {
        int barCell = Math.floorDiv(pixelX - frameLeft - cellPixels(), cellPixels());
        columnScroll.scrollTo(ScrollBarHit.offsetForDrag(columnScroll, barCell, columnBarCells(),
                columnGrabOffset));
    }

    void endColumnBarDrag() {
        draggingColumnBar = false;
    }

    void draw(int cursorSlot) {
        draw(cursorSlot, -1);
    }

    /** @param sourceSlot slot of the derive source cursor, or -1 for none */
    void draw(int cursorSlot, int sourceSlot) {
        Panel.draw(painter,
                new CellRect(frameLeft / cellPixels(), topRows, frameCellsWide, frameCellsHigh),
                frameTitle(), false);
        drawBar();
        if (columnBarShown()) {
            drawColumnBar();
        }
        drawSlots();
        drawCursor(cursorSlot);
        drawSourceCursor(sourceSlot);
    }

    private void drawSlots() {
        int firstSlot = scroll.offset() * COLUMNS;
        int endSlot =
                Math.min(editor.slotCount(), (scroll.offset() + scroll.visibleRows()) * COLUMNS);
        for (int slot = firstSlot; slot < endSlot; slot++) {
            int codePoint = editor.codePointAtSlot(slot);
            if (!columnScroll.isVisible(slot % COLUMNS) || codePoint == FontEditor.NO_GLYPH) {
                continue;
            }
            drawSlot(slot, codePoint);
        }
    }

    private void drawSlot(int slot, int codePoint) {
        int width = layout.slotWidth();
        int height = layout.slotHeight();
        int hairline = painter.scale().scaled(1);
        int x = layout.slotLeft(slot, columnScroll.offset());
        int top = layout.slotTop(slot, scroll.offset());
        painter.fill(CELL_FILL, x, top, width, height);
        PixelGlyph glyph = editor.glyph(codePoint);
        if (glyph == null) {
            outline(EMPTY_OUTLINE, x, top, width, height, hairline);
        } else {
            painter.pixels(glyph.pixels(), x, top, pixelScale(),
                    editor.isDerived(codePoint) ? DERIVED_COLOR : GLYPH_COLOR);
        }
    }

    private void drawCursor(int cursorSlot) {
        if (!isSlotVisible(cursorSlot)) {
            return;
        }
        outline(CURSOR_COLOR, layout.slotLeft(cursorSlot, columnScroll.offset()),
                layout.slotTop(cursorSlot, scroll.offset()), layout.slotWidth(),
                layout.slotHeight(),
                painter.scale().scaled(OUTLINE));
    }

    private void drawSourceCursor(int sourceSlot) {
        if (sourceSlot < 0 || !isSlotVisible(sourceSlot)) {
            return;
        }
        int outline = painter.scale().scaled(OUTLINE);
        int inset = outline + painter.scale().scaled(1);
        outline(SOURCE_CURSOR_COLOR, layout.slotLeft(sourceSlot, columnScroll.offset()) + inset,
                layout.slotTop(sourceSlot, scroll.offset()) + inset,
                layout.slotWidth() - 2 * inset, layout.slotHeight() - 2 * inset, outline);
    }

    private boolean isSlotVisible(int slot) {
        return scroll.isVisible(slot / COLUMNS) && columnScroll.isVisible(slot % COLUMNS);
    }

    private void outline(Color color, int x, int top, int width, int height, int thickness) {
        painter.fill(color, x, top, width, thickness);
        painter.fill(color, x, top + height - thickness, width, thickness);
        painter.fill(color, x, top, thickness, height);
        painter.fill(color, x + width - thickness, top, thickness, height);
    }

    private void drawColumnBar() {
        Color color = UiPalette.FRAME;
        int cells = columnBarCells();
        int barTop = columnBarTop();
        int barLeftEdge = frameLeft + cellPixels();
        int trackCells = cells - 2;
        painter.text("◄", barLeftEdge, barTop, color);
        painter.text("►", barLeftEdge + (cells - 1) * cellPixels(), barTop, color);
        int thumbStart = columnScroll.thumbStart(trackCells);
        int thumbEnd = thumbStart + columnScroll.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            barGlyph.setLength(0);
            boolean thumb = i >= thumbStart && i < thumbEnd;
            barGlyph.append(thumb ? '█' : '░');
            painter.text(barGlyph, barLeftEdge + (i + 1) * cellPixels(), barTop,
                    thumb ? UiPalette.FOCUS : color);
        }
    }

    private void drawBar() {
        ScrollBar.drawBarAtPixels(painter, barLeft, layout.top(), barCells,
                scroll.totalRows(), scroll.visibleRows(), scroll.offset(), scroll.needed());
    }
}
