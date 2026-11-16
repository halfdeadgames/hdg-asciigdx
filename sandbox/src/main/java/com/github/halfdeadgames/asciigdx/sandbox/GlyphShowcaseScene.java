package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.SaiksisttSize;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import java.util.List;

/**
 * The whole Saiksistt sheet next to examples built from its cards, chess pieces, dice, and bar
 * glyphs.
 */
final class GlyphShowcaseScene extends GridScene {

    private static final int EMPTY_SLOT_FILLER = 0x10FFFD;
    private static final int SHEET_COLUMNS = 16;
    private static final int PANEL = 18;
    private static final int COLUMNS = PANEL + 24;
    private static final int ROWS = 45;

    private static final int LABEL = 0x9090C0FF;
    private static final int CARD = 0xF0EEE0FF;
    private static final int RED = 0xC82020FF;
    private static final int INK = 0x101010FF;
    private static final int LIGHT_SQUARE = 0xC8A878FF;
    private static final int DARK_SQUARE = 0x8B5A2BFF;

    private static final int SPADE = 0x2660;
    private static final int HEART_FULL = 0x2665;
    private static final int DIAMOND = 0x2666;
    private static final int CLUB = 0x2663;
    private static final int HEART_HOLLOW = 0x2661;
    private static final int DIAMOND_HOLLOW = 0x2662;
    private static final int SPADE_HOLLOW = 0x2664;
    private static final int LIGHT_SHADE = 0x2591;
    private static final int MEDIUM_SHADE = 0x2592;
    private static final int DARK_SHADE = 0x2593;
    private static final int FULL_BLOCK = 0x2588;
    private static final int FIRST_DIE_FACE = 0x2680;

    private static final List<Integer> VERTICAL_BAR_STAGES = List.of(
            (int) ' ', 0x2581, 0x2582, 0x2583, 0x2584, 0x2585, 0x2586, 0x2587, FULL_BLOCK);
    private static final List<Integer> HORIZONTAL_BAR_STAGES = List.of(
            (int) ' ', 0x258F, 0x258E, 0x258D, 0x258C, 0x258B, 0x258A, 0x2589, FULL_BLOCK);
    private static final List<Integer> SHADE_STAGES =
            List.of((int) ' ', LIGHT_SHADE, MEDIUM_SHADE, DARK_SHADE, FULL_BLOCK);
    private static final List<Integer> HEART_STAGES = List.of(HEART_HOLLOW, HEART_FULL);

    private static final float SECONDS_PER_METER_STAGE = 0.4f;
    private static final int ANIMATED_METER_COLUMN = PANEL + 20;
    private static final int METER_STAGE_SPACING = 2;
    private static final int SHADE_STRIP_WIDTH = 20;

    private List<List<Integer>> meterStages = List.of();
    private int[] meterColors = new int[0];
    private int[] meterRows = new int[0];
    private int[] meterShownStage = new int[0];
    private float meterClock;

    private static final int OVERLAY_ROWS = 2;
    private static final int WHEEL_ROWS_PER_NOTCH = 3;
    private static final int PAGE_ROWS_FALLBACK = 1;
    private static final int TAP_SLOP_PIXELS = 24;

    private final GlyphSource glyphs;
    private final SaiksisttSize size;
    private final Layer contentLayer;

    private int windowWidth;
    private int windowHeight;
    private int scale = 1;
    private int visibleColumns = COLUMNS;
    private int visibleContentRows = ROWS;
    private int overlayRows;
    private int scrollColumn;
    private int scrollRow;

    private boolean touching;
    private boolean dragging;
    private int touchStartX;
    private int touchStartY;
    private int touchLastX;
    private int touchLastY;
    private int pendingDragX;
    private int pendingDragY;

    GlyphShowcaseScene(GlyphSource glyphs, SaiksisttSize size) {
        super(glyphs, COLUMNS, ROWS);
        this.glyphs = glyphs;
        this.size = size;
        contentLayer = layers.getLayer(0);
    }

    @Override
    public String id() {
        // The square id predates the tall size and capture baselines depend on it.
        if (size == SaiksisttSize.SQUARE_16X16) {
            return "glyph-showcase";
        }
        return "glyph-showcase-" + sizeLabel();
    }

    @Override
    public String title() {
        return "Glyph sheet showcase " + sizeLabel();
    }

    private String sizeLabel() {
        return size.getCellWidth() + "x" + size.getCellHeight();
    }

    @Override
    public void show() {
        fillGrid();
        super.show();
    }

    @Override
    public void resize(int width, int height) {
        windowWidth = width;
        windowHeight = height;
        layoutScrollView();
        super.resize(width, height);
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.UP -> scrollBy(0, -1);
            case Input.Keys.DOWN -> scrollBy(0, 1);
            case Input.Keys.LEFT -> scrollBy(-1, 0);
            case Input.Keys.RIGHT -> scrollBy(1, 0);
            case Input.Keys.PAGE_UP ->
                    scrollBy(0, -Math.max(PAGE_ROWS_FALLBACK, visibleContentRows - 1));
            case Input.Keys.PAGE_DOWN ->
                    scrollBy(0, Math.max(PAGE_ROWS_FALLBACK, visibleContentRows - 1));
            case Input.Keys.HOME -> scrollBy(-scrollColumn, -scrollRow);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountY) {
        scrollBy(0, Math.round(amountY) * WHEEL_ROWS_PER_NOTCH);
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        touching = true;
        dragging = false;
        touchStartX = screenX;
        touchStartY = screenY;
        touchLastX = screenX;
        touchLastY = screenY;
        pendingDragX = 0;
        pendingDragY = 0;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY) {
        if (!touching) {
            return false;
        }
        if (Math.abs(screenX - touchStartX) + Math.abs(screenY - touchStartY) > TAP_SLOP_PIXELS) {
            dragging = true;
        }
        int columnStepPixels = glyphs.getCellWidth() * scale;
        int rowStepPixels = glyphs.getCellHeight() * scale;
        pendingDragX += touchLastX - screenX;
        pendingDragY += touchLastY - screenY;
        int columns = ScrollView.wholeSteps(pendingDragX, columnStepPixels);
        int rows = ScrollView.wholeSteps(pendingDragY, rowStepPixels);
        pendingDragX -= columns * columnStepPixels;
        pendingDragY -= rows * rowStepPixels;
        scrollBy(columns, rows);
        touchLastX = screenX;
        touchLastY = screenY;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY) {
        if (!touching) {
            return false;
        }
        touching = false;
        return dragging;
    }

    @Override
    public String statusText() {
        return "Scroll: arrows/drag";
    }

    // The view shows whole cells at a whole-number scale; the content scrolls when it does not fit.
    private void layoutScrollView() {
        int cellWidth = glyphs.getCellWidth();
        int cellHeight = glyphs.getCellHeight();
        scale = ScrollView.chooseScale(windowWidth, windowHeight, cellWidth, cellHeight);
        visibleColumns = ScrollView.visibleCells(windowWidth, cellWidth, scale);
        int visibleRows = ScrollView.visibleCells(windowHeight, cellHeight, scale);
        int overlayCellHeight = SaiksisttSize.SQUARE_16X16.getCellHeight();
        int overlayPixels = OVERLAY_ROWS * overlayCellHeight * Math.max(1,
                Math.round(Gdx.graphics.getDensity()));
        overlayRows = Math.min(ScrollView.coveredCells(overlayPixels, cellHeight, scale),
                visibleRows - 1);
        visibleContentRows = visibleRows - overlayRows;
        float worldWidth = visibleColumns * cellWidth;
        float worldHeight = visibleRows * cellHeight;
        viewport.setWorldSize(worldWidth, worldHeight);
        layers.setSize(worldWidth, worldHeight);
        contentLayer.setClip(0f, overlayRows * cellHeight, worldWidth,
                visibleContentRows * cellHeight);
        scrollBy(0, 0);
    }

    private void scrollBy(int columns, int rows) {
        scrollColumn = ScrollView.clampScroll(scrollColumn + columns, COLUMNS, visibleColumns);
        scrollRow = ScrollView.clampScroll(scrollRow + rows, ROWS, visibleContentRows);
        contentLayer.setOffsetInCells(-scrollColumn, overlayRows - scrollRow);
    }

    private void fillGrid() {
        grid.clear();
        GlyphOrder order = GlyphOrderText.read(
                Gdx.files.classpath(size.getCharacterListPath()).readString("UTF-8"),
                SHEET_COLUMNS);
        GridDrawing.drawText(grid, 0, 0,
                "Saiksistt " + sizeLabel() + ", " + countGlyphs(order) + " glyphs", LABEL, CLEAR);
        writeSheet(order);
        writeCards(1);
        writeChess(5);
        writeMeters(15);
        writeDice(21);
        writeShades(24);
    }

    private static int countGlyphs(GlyphOrder order) {
        int count = 0;
        for (int slot = 0; slot < order.getGlyphCount(); slot++) {
            if (order.getCodePoint(slot) != EMPTY_SLOT_FILLER) {
                count++;
            }
        }
        return count;
    }

    private void writeSheet(GlyphOrder order) {
        for (int slot = 0; slot < order.getGlyphCount(); slot++) {
            int codePoint = order.getCodePoint(slot);
            if (codePoint == EMPTY_SLOT_FILLER) {
                continue;
            }
            grid.set(slot % SHEET_COLUMNS, 1 + slot / SHEET_COLUMNS, codePoint, WHITE, CLEAR);
        }
    }

    private void writeCards(int top) {
        GridDrawing.drawText(grid, PANEL, top, "Cards", LABEL, CLEAR);
        int[][] hand = {
                {'A', SPADE}, {'K', HEART_FULL}, {'Q', DIAMOND}, {'J', CLUB}, {'T', HEART_HOLLOW},
                {'9', SPADE_HOLLOW}
        };
        int column = PANEL;
        for (int[] card : hand) {
            boolean red = card[1] == HEART_FULL || card[1] == DIAMOND || card[1] == HEART_HOLLOW
                    || card[1] == DIAMOND_HOLLOW;
            int ink = red ? RED : INK;
            grid.set(column, top + 1, card[0], ink, CARD);
            grid.set(column + 1, top + 1, card[1], ink, CARD);
            column += 3;
        }
        for (int i = 0; i < 6; i++) {
            grid.set(PANEL + i * 2, top + 2, i == 2 ? DARK_SHADE : MEDIUM_SHADE,
                    i == 2 ? GOLD : CYAN, CLEAR);
        }
    }

    private void writeChess(int top) {
        GridDrawing.drawText(grid, PANEL, top, "Chess", LABEL, CLEAR);
        String blackBack = "♜♞♝♛♚♝♞♜";
        String whiteBack = "♖♘♗♕♔♗♘♖";
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                int square = (row + column) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                int piece = switch (row) {
                    case 0 -> blackBack.codePointAt(blackBack.offsetByCodePoints(0, column));
                    case 1 -> 0x265F;
                    case 6 -> 0x2659;
                    case 7 -> whiteBack.codePointAt(whiteBack.offsetByCodePoints(0, column));
                    default -> ' ';
                };
                grid.set(PANEL + column, top + 1 + row, piece, row < 2 ? INK : WHITE, square);
            }
        }
    }

    @Override
    protected void update(float delta) {
        meterClock += delta;
        int step = (int) (meterClock / SECONDS_PER_METER_STAGE);
        for (int meter = 0; meter < meterStages.size(); meter++) {
            List<Integer> stages = meterStages.get(meter);
            int stage = bouncingStage(step, stages.size());
            if (stage != meterShownStage[meter]) {
                meterShownStage[meter] = stage;
                grid.set(ANIMATED_METER_COLUMN, meterRows[meter], stages.get(stage),
                        meterColors[meter], CLEAR);
            }
        }
    }

    // Counts 0..n-1 then n-1..0, so each end shows for two steps as a short rest.
    private static int bouncingStage(int step, int stageCount) {
        int position = step % (2 * stageCount);
        return position < stageCount ? position : 2 * stageCount - 1 - position;
    }

    // Each row lists its stages from empty to full, then one cell that fills and empties again.
    private void writeMeters(int top) {
        GridDrawing.drawText(grid, PANEL, top, "Meters", LABEL, CLEAR);
        meterStages = List.of(
                VERTICAL_BAR_STAGES, HORIZONTAL_BAR_STAGES, SHADE_STAGES, HEART_STAGES);
        meterColors = new int[]{CYAN, GOLD, WHITE, RED};
        meterRows = new int[meterStages.size()];
        meterShownStage = new int[meterStages.size()];
        meterClock = 0f;
        for (int meter = 0; meter < meterStages.size(); meter++) {
            meterRows[meter] = top + 1 + meter;
            for (int stage = 0; stage < meterStages.get(meter).size(); stage++) {
                grid.set(PANEL + stage * METER_STAGE_SPACING, meterRows[meter],
                        meterStages.get(meter).get(stage),
                        meterColors[meter], CLEAR);
            }
            grid.set(ANIMATED_METER_COLUMN, meterRows[meter], meterStages.get(meter).get(0),
                    meterColors[meter], CLEAR);
        }
    }

    private void writeDice(int top) {
        GridDrawing.drawText(grid, PANEL, top, "Dice", LABEL, CLEAR);
        for (int face = 0; face < 6; face++) {
            grid.set(PANEL + face * 2, top + 1, FIRST_DIE_FACE + face, WHITE, CLEAR);
        }
    }

    // One strip stepping from empty to solid through the three shades.
    private void writeShades(int top) {
        GridDrawing.drawText(grid, PANEL, top, "Shades", LABEL, CLEAR);
        for (int column = 0; column < SHADE_STRIP_WIDTH; column++) {
            int level = Math.round(column * (SHADE_STAGES.size() - 1f) / (SHADE_STRIP_WIDTH - 1));
            grid.set(PANEL + column, top + 1, SHADE_STAGES.get(level), WHITE, CLEAR);
        }
    }
}
