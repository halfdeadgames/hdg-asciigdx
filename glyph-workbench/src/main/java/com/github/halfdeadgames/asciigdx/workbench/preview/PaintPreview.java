package com.github.halfdeadgames.asciigdx.workbench.preview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntMap;
import com.github.halfdeadgames.asciigdx.workbench.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.PaintLayout;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.Random;
import java.util.Set;

/** The "PREVIEW" panel: a tab strip over a side-by-side 1x to 8x view of the selected glyph's whole cell, at real font pixel size. */
public final class PaintPreview {

    private static final int[] SCALES = PreviewFit.SCALES;
    private static final String[] SCALE_LABELS = {"1X", "2X", "4X", "8X"};
    private static final Color BACKDROP = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final String NO_CLASS_MESSAGE = "NO CLASS FOR BLANKS";
    private static final int TICKER_LINES = 2;
    private static final int TICKER_SCALE = 2;
    private static final float TICKER_PIXELS_PER_SECOND = 40f;
    private static final long TICKER_SEED = 7L;
    private static final float WHITE_SECONDS = 1f;
    private static final float FLASH_SECONDS = 0.2f;
    private static final float MAX_FRAME_SECONDS = 0.1f;

    private final Painter painter;
    private final FontEditor editor;
    private PreviewTabs.Tab activeTab = PreviewTabs.Tab.SCALES;

    private final TickerLine[] tickerLines = new TickerLine[TICKER_LINES];
    private final String[] tickerMessages = new String[TICKER_LINES];
    private final Color[] tickerMessageColors = new Color[TICKER_LINES];
    private final IntMap<PixelGrid> tickerCells = new IntMap<>();
    private final TickerGlyphDrawer tickerDrawer = new TickerGlyphDrawer();
    private Set<Integer> tickerAvailable;
    private int tickerSelected = FontEditor.NO_GLYPH - 1;
    private int tickerRevision = -1;
    private float tickerSeconds;

    public PaintPreview(Painter painter, FontEditor editor) {
        this.painter = painter;
        this.editor = editor;
    }

    private int cell() {
        return painter.cell();
    }

    private int sectionGap() {
        return painter.scale().half();
    }

    private int labelWidth() {
        return 2 * cell();
    }

    PreviewTabs.Tab activeTab() {
        return activeTab;
    }

    public void selectTab(PreviewTabs.Tab tab) {
        activeTab = tab;
    }

    public void cycleTab(boolean backward) {
        activeTab = backward ? PreviewTabs.previous(activeTab) : PreviewTabs.next(activeTab);
    }

    /** Switches tab when the clicked text cell is on a tab label. */
    public void click(CellRect rect, int column, int row) {
        PreviewTabs.Tab tab = PreviewTabs.tabAt(rect, column, row);
        if (tab == null) {
            return;
        }
        activeTab = tab;
    }

    void draw(CellRect rect, int codePoint) {
        draw(rect, codePoint, null);
    }

    /** @param shown glyph to show instead of the slot's own, or null */
    public void draw(CellRect rect, int codePoint, PixelGlyph shown) {
        Panel.draw(painter, rect, "PREVIEW", false);
        drawTabs(rect);
        int left = (rect.x() + 2) * cell();
        int rowTop = (PreviewTabs.labelRow(rect) + 1) * cell() + sectionGap();
        PixelGlyph glyph = shown != null ? shown : editor.glyph(codePoint);
        int innerWidth = PaintLayout.previewInnerWidth(rect.width(), cell());
        int scaleCount = PreviewFit.scaleCount(innerWidth,
            PaintLayout.previewBlockBudget(rect.height(), cell()),
            new GlyphSize(editor.cellWidth(), editor.cellHeight()), sectionGap(), labelWidth());
        int largestScale = PreviewFit.largestScale(scaleCount);
        if (activeTab == PreviewTabs.Tab.SCALES) {
            drawScales(left, rowTop, glyph, scaleCount);
        } else if (activeTab == PreviewTabs.Tab.TILE) {
            drawTiles(left, rowTop, glyph, innerWidth, largestScale);
        } else {
            drawTicker(rect, codePoint, left, rowTop, largestScale);
        }
    }

    /** The shortfall wording that fits {@code maxChars}, trying the most explicit form first. */
    static String shortfallMessage(int lineNumber, GlyphClass glyphClass, int need, int have, int maxChars) {
        String label = glyphClass.pluralLabel();
        String[] candidates = {
            "LINE " + lineNumber + " NEEDS " + need + " " + label + ", HAS " + have,
            "NEED " + need + " " + label + ", HAVE " + have,
            "NEED " + need + " " + label + ", HAS " + have,
            "NEED " + need + ", HAVE " + have,
        };
        for (String candidate : candidates) {
            if (candidate.length() <= maxChars) {
                return candidate;
            }
        }
        return candidates[candidates.length - 1].substring(0, Math.max(0, maxChars));
    }

    // Glyphs are clipped by skipping the pixel columns outside the content area; no GL scissor is involved.
    private void drawTicker(CellRect rect, int selected, int left, int top, int largestScale) {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_SECONDS);
        tickerSeconds += delta;
        int clipRight = (rect.x() + rect.width() - 1) * cell();
        int visibleWidth = clipRight - left;
        refreshTicker(selected, visibleWidth / cell());

        int cellHeight = editor.cellHeight();
        int bandHeight = (cell() + largestScale * cellHeight) / TICKER_LINES;
        tickerDrawer.clipLeft = left;
        tickerDrawer.clipRight = clipRight;
        tickerDrawer.featured = selected;
        tickerDrawer.featuredColor = tickerSeconds % (WHITE_SECONDS + FLASH_SECONDS) < WHITE_SECONDS
            ? Color.WHITE : UiPalette.ACCENT;
        for (int i = 0; i < TICKER_LINES; i++) {
            int bandTop = top + i * bandHeight;
            TickerLine line = tickerLines[i];
            if (line == null) {
                String message = tickerMessages[i];
                painter.text(message.length() > visibleWidth / cell() ? message.substring(0, visibleWidth / cell()) : message,
                    left, bandTop + (bandHeight - cell()) / 2, tickerMessageColors[i]);
                continue;
            }
            line.setVisibleWidth(visibleWidth);
            line.advance(delta);
            tickerDrawer.left = left;
            tickerDrawer.top = bandTop + (bandHeight - TICKER_SCALE * cellHeight) / 2;
            line.forEachVisibleGlyph(tickerDrawer);
        }
    }

    // The lines restart only when the selection or the set of glyphs with ink changes, not on every pixel edit.
    private void refreshTicker(int selected, int maxMessageChars) {
        boolean rebuild = false;
        if (editor.revision() != tickerRevision) {
            tickerRevision = editor.revision();
            tickerCells.clear();
            Set<Integer> withInk = editor.codePointsWithInk();
            if (!withInk.equals(tickerAvailable)) {
                tickerAvailable = withInk;
                rebuild = true;
            }
        }
        if (selected != tickerSelected) {
            tickerSelected = selected;
            rebuild = true;
        }
        if (rebuild) {
            rebuildTickerLines(selected, maxMessageChars);
        }
    }

    private void rebuildTickerLines(int selected, int maxMessageChars) {
        Random random = new Random(TICKER_SEED);
        GlyphClass[] classes = {GlyphClass.of(selected), GlyphClass.ANY};
        int glyphAdvance = editor.cellWidth() * TICKER_SCALE;
        for (int i = 0; i < TICKER_LINES; i++) {
            tickerLines[i] = null;
            if (classes[i] == null) {
                tickerMessages[i] = NO_CLASS_MESSAGE;
                tickerMessageColors[i] = UiPalette.DIM;
                continue;
            }
            TickerWords words = new TickerWords(tickerAvailable, classes[i], random);
            if (!words.hasEnough()) {
                tickerMessages[i] = shortfallMessage(i + 1, classes[i], words.needCount(), words.haveCount(),
                    maxMessageChars);
                tickerMessageColors[i] = UiPalette.WARN;
                continue;
            }
            int gapCells = classes[i] == GlyphClass.ANY ? 0 : 1;
            tickerLines[i] = new TickerLine(() -> words.nextWord(selected), glyphAdvance, TICKER_PIXELS_PER_SECOND,
                gapCells);
        }
    }

    private PixelGrid tickerCell(int codePoint) {
        PixelGrid cell = tickerCells.get(codePoint);
        if (cell == null) {
            PixelGlyph glyph = editor.glyph(codePoint);
            if (glyph == null) {
                return null;
            }
            cell = glyph.toCell(editor.cellWidth());
            tickerCells.put(codePoint, cell);
        }
        return cell;
    }

    private final class TickerGlyphDrawer implements TickerLine.GlyphVisitor {
        int left;
        int top;
        int clipLeft;
        int clipRight;
        int featured;
        Color featuredColor = Color.WHITE;

        @Override
        public void visit(int codePoint, float x) {
            PixelGrid cell = tickerCell(codePoint);
            if (cell == null) {
                return;
            }
            Color color = codePoint == featured ? featuredColor : Color.WHITE;
            int glyphLeft = left + Math.round(x);
            for (int py = 0; py < cell.getHeight(); py++) {
                for (int px = 0; px < cell.getWidth(); px++) {
                    if (cell.isLit(px, py)) {
                        fillWithinClip(color, glyphLeft + px * TICKER_SCALE, top + py * TICKER_SCALE);
                    }
                }
            }
        }

        private void fillWithinClip(Color color, int x, int y) {
            int from = Math.max(x, clipLeft);
            int to = Math.min(x + TICKER_SCALE, clipRight);
            if (to > from) {
                painter.fill(color, from, y, to - from, TICKER_SCALE);
            }
        }
    }

    private void drawTabs(CellRect rect) {
        int top = PreviewTabs.labelRow(rect) * cell();
        for (PreviewTabs.Tab tab : PreviewTabs.Tab.values()) {
            painter.text(PreviewTabs.label(rect, tab), PreviewTabs.labelColumn(rect, tab) * cell(), top,
                tab == activeTab ? UiPalette.ACCENT : UiPalette.DIM);
        }
    }

    // Each block is as tall as the largest kept scale (scale * count == largestScale), so switching tabs never needs a taller panel.
    private void drawTiles(int left, int top, PixelGlyph glyph, int innerWidth, int largestScale) {
        int blocks = PreviewFit.tileBlockCount(innerWidth, largestScale, editor.cellWidth(), 2 * sectionGap(),
            labelWidth());
        int blockLeft = left;
        for (int i = 0; i < blocks; i++) {
            blockLeft = drawTileBlock(blockLeft, top, glyph, SCALE_LABELS[i], SCALES[i], largestScale / SCALES[i]);
        }
    }

    private int drawTileBlock(int left, int top, PixelGlyph glyph, String label, int scale, int count) {
        int cellWidth = editor.cellWidth();
        int cellHeight = editor.cellHeight();
        int blockTop = top + cell();
        int blockSize = count * scale;
        painter.text(label, left, top, UiPalette.DIM);
        painter.fill(BACKDROP, left, blockTop, cellWidth * blockSize, cellHeight * blockSize);
        if (glyph != null) {
            PixelGrid cell = glyph.toCell(cellWidth);
            for (int row = 0; row < count; row++) {
                for (int column = 0; column < count; column++) {
                    painter.pixels(cell, left + column * cellWidth * scale, blockTop + row * cellHeight * scale,
                        scale, Color.WHITE);
                }
            }
        }
        return left + Math.max(label.length() * cell(), cellWidth * blockSize) + 2 * sectionGap();
    }

    private void drawScales(int left, int top, PixelGlyph glyph, int scaleCount) {
        int cellWidth = editor.cellWidth();
        int cellHeight = editor.cellHeight();
        int blockTop = top + cell();
        int blockLeft = left;
        for (int i = 0; i < scaleCount; i++) {
            int scale = SCALES[i];
            painter.text(SCALE_LABELS[i], blockLeft, top, UiPalette.DIM);
            painter.fill(BACKDROP, blockLeft, blockTop, cellWidth * scale, cellHeight * scale);
            if (glyph != null) {
                painter.pixels(glyph.toCell(cellWidth), blockLeft, blockTop, scale, Color.WHITE);
            }
            int labelWidth = SCALE_LABELS[i].length() * cell();
            blockLeft += Math.max(labelWidth, cellWidth * scale) + sectionGap();
        }
    }
}
