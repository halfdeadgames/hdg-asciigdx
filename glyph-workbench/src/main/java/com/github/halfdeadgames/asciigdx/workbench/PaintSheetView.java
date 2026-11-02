package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;

/** The scrollable sheet of all slots on the left of the paint screen. */
final class PaintSheetView {

    static final int COLUMNS = 16;
    private static final int TEXT = Painter.TEXT_SIZE;
    private static final int BAR_WIDTH = TEXT;
    private static final int TARGET_CELL_PIXELS = 24;
    private static final int OUTLINE = 2;
    private static final int MARK_SIZE = 6;

    private static final Color CELL_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color EMPTY_OUTLINE = new Color(0.4f, 0.42f, 0.55f, 1f);
    private static final Color GLYPH_COLOR = Color.WHITE;
    private static final Color DERIVED_COLOR = new Color(0.75f, 0.6f, 1f, 1f);
    private static final Color CURSOR_COLOR = Color.YELLOW;
    private static final Color FRAME_COLOR = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color LABEL_COLOR = new Color(0.8f, 0.6f, 1f, 1f);
    private static final Color BAR_COLOR = new Color(0.75f, 0.8f, 0.95f, 1f);
    private static final Color BAR_DIM_COLOR = new Color(0.25f, 0.27f, 0.34f, 1f);

    private final Painter painter;
    private final FontEditor editor;
    private final int frameLeft;
    private final int frameTop;
    private final int pixelScale;
    private final int rows;
    private final SheetGridLayout layout;
    private final ScrollState scroll;
    private final StringBuilder barGlyph = new StringBuilder(1);
    private Color[] marks;
    private int frameCellsWide;
    private int frameCellsHigh;
    private int barLeft;
    private int barCells;

    PaintSheetView(Painter painter, FontEditor editor, int frameLeft, int frameTop) {
        this.painter = painter;
        this.editor = editor;
        this.frameLeft = frameLeft;
        this.frameTop = frameTop;
        pixelScale = Math.max(1, (TARGET_CELL_PIXELS + editor.cellWidth() - 1) / editor.cellWidth());
        rows = editor.slotCount() / COLUMNS;
        int cellSize = Math.max(editor.cellWidth(), editor.cellHeight()) * pixelScale;
        layout = new SheetGridLayout(frameLeft + TEXT, frameTop + TEXT, cellSize, 1, COLUMNS, rows);
        scroll = new ScrollState(rows);
        int innerWidth = Painter.roundUpToCells(layout.panelWidth() + BAR_WIDTH);
        frameCellsWide = innerWidth / TEXT + 2;
        barLeft = layout.left() + innerWidth - BAR_WIDTH;
    }

    ScrollState scroll() {
        return scroll;
    }

    int frameRight() {
        return frameLeft + frameCellsWide * TEXT;
    }

    /** Fits the frame to the window height; keeps the cursor row visible when asked or when the size changed. */
    void fitToHeight(int worldHeight, int margin, int cursorSlot, boolean revealCursor) {
        frameCellsHigh = Math.max(3, (worldHeight - frameTop - margin) / TEXT);
        int innerHeight = (frameCellsHigh - 2) * TEXT;
        barCells = innerHeight / TEXT;
        int before = scroll.visibleRows();
        scroll.setVisibleRows(layout.rowsThatFit(innerHeight));
        if (revealCursor || before != scroll.visibleRows()) {
            scroll.keepVisible(cursorSlot / COLUMNS);
        }
    }

    boolean contains(int pixelX, int pixelY) {
        return pixelX >= frameLeft && pixelX < frameRight()
            && pixelY >= frameTop && pixelY < frameTop + frameCellsHigh * TEXT;
    }

    /** Slot under a pixel, or -1. */
    int slotAt(int pixelX, int pixelY) {
        return layout.cellAt(pixelX, pixelY, scroll.offset(), scroll.visibleRows());
    }

    /** Handles a click on the scroll bar arrows or track. */
    void clickBar(int pixelX, int pixelY) {
        int barTop = layout.top();
        if (!scroll.needed() || pixelX < barLeft || pixelX >= barLeft + BAR_WIDTH
            || pixelY < barTop || pixelY >= barTop + barCells * TEXT) {
            return;
        }
        int cell = (pixelY - barTop) / TEXT;
        if (cell == 0) {
            scroll.scrollBy(-1);
        } else if (cell == barCells - 1) {
            scroll.scrollBy(1);
        } else {
            scroll.clickTrack(cell - 1, barCells - 2);
        }
    }

    /** Small corner squares drawn on slots; one color (or null for none) per slot, or null for no marks. */
    void setMarks(Color[] marks) {
        this.marks = marks;
    }

    void draw(int cursorSlot) {
        painter.text("GLYPH SHEET", frameLeft, frameTop - TEXT, LABEL_COLOR);
        painter.frame(FRAME_COLOR, frameLeft, frameTop, frameCellsWide, frameCellsHigh);
        drawBar();
        int size = layout.cellSize();
        int firstSlot = scroll.offset() * COLUMNS;
        int endSlot = Math.min(editor.slotCount(), (scroll.offset() + scroll.visibleRows()) * COLUMNS);
        for (int slot = firstSlot; slot < endSlot; slot++) {
            int codePoint = editor.codePointAtSlot(slot);
            if (codePoint == FontEditor.NO_GLYPH) {
                continue;
            }
            int x = layout.cellLeft(slot);
            int top = layout.cellTop(slot, scroll.offset());
            painter.fill(CELL_FILL, x, top, size, size);
            PixelGlyph glyph = editor.glyph(codePoint);
            if (glyph == null) {
                outline(EMPTY_OUTLINE, x, top, size, 1);
            } else {
                painter.pixels(glyph.pixels(), x, top, pixelScale, editor.isDerived(codePoint) ? DERIVED_COLOR : GLYPH_COLOR);
            }
            if (marks != null && marks[slot] != null) {
                painter.fill(marks[slot], x + size - MARK_SIZE - 1, top + 1, MARK_SIZE, MARK_SIZE);
            }
        }
        if (scroll.isVisible(cursorSlot / COLUMNS)) {
            outline(CURSOR_COLOR, layout.cellLeft(cursorSlot), layout.cellTop(cursorSlot, scroll.offset()), size, OUTLINE);
        }
    }

    private void outline(Color color, int x, int top, int size, int thickness) {
        painter.fill(color, x, top, size, thickness);
        painter.fill(color, x, top + size - thickness, size, thickness);
        painter.fill(color, x, top, thickness, size);
        painter.fill(color, x + size - thickness, top, thickness, size);
    }

    private void drawBar() {
        boolean active = scroll.needed();
        Color color = active ? BAR_COLOR : BAR_DIM_COLOR;
        int barTop = layout.top();
        painter.text("▲", barLeft, barTop, color);
        painter.text("▼", barLeft, barTop + (barCells - 1) * TEXT, color);
        int trackCells = barCells - 2;
        int thumbStart = scroll.thumbStart(trackCells);
        int thumbEnd = thumbStart + scroll.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            barGlyph.setLength(0);
            barGlyph.append(active && i >= thumbStart && i < thumbEnd ? '█' : '░');
            painter.text(barGlyph, barLeft, barTop + (i + 1) * TEXT, color);
        }
    }
}
