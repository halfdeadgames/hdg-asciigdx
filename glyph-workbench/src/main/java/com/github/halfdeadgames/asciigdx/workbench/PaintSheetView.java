package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;

/** The scrollable sheet of all slots on the left of the paint screen. */
final class PaintSheetView {

    static final int COLUMNS = 16;
    static final int GAP = 1;
    private static final int OUTLINE = 2;
    private static final int MARK_SIZE = 6;

    private static final Color CELL_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color EMPTY_OUTLINE = new Color(0.4f, 0.42f, 0.55f, 1f);
    private static final Color GLYPH_COLOR = Color.WHITE;
    private static final Color DERIVED_COLOR = UiPalette.DERIVED;
    private static final Color CURSOR_COLOR = Color.YELLOW;
    private static final Color SOURCE_CURSOR_COLOR = UiPalette.ORANGE;
    private static final Color FRAME_COLOR = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color LABEL_COLOR = new Color(0.8f, 0.6f, 1f, 1f);
    private static final Color BAR_COLOR = new Color(0.75f, 0.8f, 0.95f, 1f);
    private static final Color BAR_DIM_COLOR = new Color(0.25f, 0.27f, 0.34f, 1f);

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
    private Color[] marks;
    private int visibleColumns;
    private int frameCellsWide;
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

    private int cell() {
        return painter.cell();
    }

    private int frameTop() {
        return topRows * cell();
    }

    // Whole-number zoom that makes a glyph's longer side at least one text cell.
    private int pixelScale() {
        return pixelScaleFor(editor.cellWidth(), editor.cellHeight(), cell());
    }

    private static int pixelScaleFor(int glyphWidth, int glyphHeight, int cellPixels) {
        return Math.max(1, Math.min(ceilDiv(cellPixels, glyphWidth), ceilDiv(cellPixels, glyphHeight)));
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
        return slotWidthFor(editor.cellWidth(), editor.cellHeight(), cell());
    }

    /** Shows a window of glyph columns; the frame shrinks to fit them. Slot numbers do not change. */
    void setVisibleColumns(int columns) {
        int clamped = Math.max(1, Math.min(COLUMNS, columns));
        if (clamped == visibleColumns && layoutCell == cell()) {
            return;
        }
        visibleColumns = clamped;
        layoutCell = cell();
        columnScroll.setVisibleRows(visibleColumns);
        columnsChanged = true;
        frameCellsWide = PaintColumns.sheetCells(visibleColumns, slotWidth(), GAP, cell());
        int inset = PaintColumns.panelInset(frameCellsWide, visibleColumns * (slotWidth() + GAP) + GAP, cell());
        layout = new SheetGridLayout(frameLeft + cell() + inset, frameTop() + cell(),
            slotWidth(), editor.cellHeight() * pixelScale(), GAP, COLUMNS, visibleColumns, rows);
        barLeft = frameLeft + (frameCellsWide - 2) * cell();
    }

    /**
     * Picks up slots the editor added: the row count, scroll limits, and grid layout follow it. Marks are
     * indexed by slot, so they are dropped and must be set again.
     */
    void refreshSlots() {
        rows = editor.slotCount() / COLUMNS;
        scroll.setTotalRows(rows);
        marks = null;
        layoutCell = -1;
        setVisibleColumns(visibleColumns);
    }

    ScrollState scroll() {
        return scroll;
    }

    /** First visible glyph column. */
    int firstColumn() {
        return columnScroll.offset();
    }

    ScrollState columnScroll() {
        return columnScroll;
    }

    int frameRight() {
        return frameLeft + frameCellsWide * cell();
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
     * Fits the frame to the window height, down to the bottom row when the key bar is hidden. Keeps the cursor
     * visible when asked or when the number of visible rows changed.
     */
    void fitToHeight(int worldHeight, boolean statusBarShown, int cursorSlot, boolean revealCursor) {
        frameCellsHigh = PaintLayout.sheetRows(worldHeight, cell(), statusBarShown, topRows);
        barCells = Math.max(1, frameCellsHigh - 2 - (columnBarShown() ? 1 : 0));
        int before = scroll.visibleRows();
        scroll.setVisibleRows(layout.rowsThatFit(barCells * cell()));
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
            && pixelY >= frameTop() && pixelY < frameTop() + frameCellsHigh * cell();
    }

    /** Slot under a pixel, or -1. */
    int slotAt(int pixelX, int pixelY) {
        return layout.cellAt(pixelX, pixelY, scroll.offset(), scroll.visibleRows(), columnScroll.offset());
    }

    /** Handles a click on the scroll bar arrows or track. */
    void clickBar(int pixelX, int pixelY) {
        int barTop = layout.top();
        if (!scroll.needed() || pixelX < barLeft || pixelX >= barLeft + cell()
            || pixelY < barTop || pixelY >= barTop + barCells * cell()) {
            return;
        }
        int cell = (pixelY - barTop) / cell();
        if (cell == 0) {
            scroll.scrollBy(-1);
        } else if (cell == barCells - 1) {
            scroll.scrollBy(1);
        } else {
            scroll.clickTrack(cell - 1, barCells - 2);
        }
    }

    private int columnBarTop() {
        return frameTop() + (frameCellsHigh - 2) * cell();
    }

    private int columnBarCells() {
        return frameCellsWide - 3;
    }

    private int columnBarCellAt(int pixelX, int pixelY) {
        int barTop = columnBarTop();
        if (!columnBarShown() || pixelY < barTop || pixelY >= barTop + cell()
            || pixelX < frameLeft + cell() || pixelX >= frameLeft + cell() + columnBarCells() * cell()) {
            return -1;
        }
        return (pixelX - frameLeft - cell()) / cell();
    }

    /** Handles a press on the glyph column bar; true when the press landed on it. A press on the thumb starts a drag. */
    boolean pressColumnBar(int pixelX, int pixelY) {
        int cell = columnBarCellAt(pixelX, pixelY);
        if (cell < 0) {
            return false;
        }
        ScrollBarHit.Part part = ScrollBarHit.part(columnScroll, cell, columnBarCells());
        if (part == ScrollBarHit.Part.THUMB) {
            draggingColumnBar = true;
            columnGrabOffset = ScrollBarHit.grabOffset(columnScroll, cell, columnBarCells());
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
        int cell = Math.floorDiv(pixelX - frameLeft - cell(), cell());
        columnScroll.scrollTo(ScrollBarHit.offsetForDrag(columnScroll, cell, columnBarCells(), columnGrabOffset));
    }

    void endColumnBarDrag() {
        draggingColumnBar = false;
    }

    /** Small corner squares drawn on slots; one color (or null for none) per slot, or null for no marks. */
    void setMarks(Color[] marks) {
        this.marks = marks;
    }

    void draw(int cursorSlot) {
        draw(cursorSlot, -1);
    }

    /** @param sourceSlot slot of the derive source cursor, or -1 for none */
    void draw(int cursorSlot, int sourceSlot) {
        Panel.draw(painter, frameLeft / cell(), topRows, frameCellsWide, frameCellsHigh,
            PaintColumns.sheetTitle(frameCellsWide), false);
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
        int endSlot = Math.min(editor.slotCount(), (scroll.offset() + scroll.visibleRows()) * COLUMNS);
        for (int slot = firstSlot; slot < endSlot; slot++) {
            int codePoint = editor.codePointAtSlot(slot);
            if (!columnScroll.isVisible(slot % COLUMNS) || codePoint == FontEditor.NO_GLYPH) {
                continue;
            }
            drawSlot(slot, codePoint);
        }
    }

    private void drawSlot(int slot, int codePoint) {
        int width = layout.cellWidth();
        int height = layout.cellHeight();
        int hairline = painter.scale().scaled(1);
        int x = layout.cellLeft(slot, columnScroll.offset());
        int top = layout.cellTop(slot, scroll.offset());
        painter.fill(CELL_FILL, x, top, width, height);
        PixelGlyph glyph = editor.glyph(codePoint);
        if (glyph == null) {
            outline(EMPTY_OUTLINE, x, top, width, height, hairline);
        } else {
            painter.pixels(glyph.pixels(), x, top, pixelScale(), editor.isDerived(codePoint) ? DERIVED_COLOR : GLYPH_COLOR);
        }
        if (marks != null && marks[slot] != null) {
            int markSize = painter.scale().scaled(MARK_SIZE);
            painter.fill(marks[slot], x + width - markSize - hairline, top + hairline, markSize, markSize);
        }
    }

    private void drawCursor(int cursorSlot) {
        if (!isSlotVisible(cursorSlot)) {
            return;
        }
        outline(CURSOR_COLOR, layout.cellLeft(cursorSlot, columnScroll.offset()),
            layout.cellTop(cursorSlot, scroll.offset()), layout.cellWidth(), layout.cellHeight(),
            painter.scale().scaled(OUTLINE));
    }

    private void drawSourceCursor(int sourceSlot) {
        if (sourceSlot < 0 || !isSlotVisible(sourceSlot)) {
            return;
        }
        int outline = painter.scale().scaled(OUTLINE);
        int inset = outline + painter.scale().scaled(1);
        outline(SOURCE_CURSOR_COLOR, layout.cellLeft(sourceSlot, columnScroll.offset()) + inset,
            layout.cellTop(sourceSlot, scroll.offset()) + inset,
            layout.cellWidth() - 2 * inset, layout.cellHeight() - 2 * inset, outline);
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
        int barLeftEdge = frameLeft + cell();
        int trackCells = cells - 2;
        painter.text("◄", barLeftEdge, barTop, color);
        painter.text("►", barLeftEdge + (cells - 1) * cell(), barTop, color);
        int thumbStart = columnScroll.thumbStart(trackCells);
        int thumbEnd = thumbStart + columnScroll.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            barGlyph.setLength(0);
            boolean thumb = i >= thumbStart && i < thumbEnd;
            barGlyph.append(thumb ? '█' : '░');
            painter.text(barGlyph, barLeftEdge + (i + 1) * cell(), barTop, thumb ? UiPalette.FOCUS : color);
        }
    }

    private void drawBar() {
        boolean active = scroll.needed();
        Color color = UiPalette.FRAME;
        int barTop = layout.top();
        painter.text("▲", barLeft, barTop, color);
        painter.text("▼", barLeft, barTop + (barCells - 1) * cell(), color);
        int trackCells = barCells - 2;
        int thumbStart = scroll.thumbStart(trackCells);
        int thumbEnd = thumbStart + scroll.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            barGlyph.setLength(0);
            boolean thumb = active && i >= thumbStart && i < thumbEnd;
            barGlyph.append(thumb ? '█' : '░');
            painter.text(barGlyph, barLeft, barTop + (i + 1) * cell(), thumb ? UiPalette.FOCUS : color);
        }
    }
}
