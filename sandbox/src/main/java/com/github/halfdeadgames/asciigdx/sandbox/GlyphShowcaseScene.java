package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs.Shade;

/** The whole default sheet next to examples that use its added glyphs. */
final class GlyphShowcaseScene extends GridScene {

    /** Launch setting choosing the starting font, "8" or "16"; read like the capture settings. */
    static final String KEY_SHOWCASE_FONT = "showcaseFont";

    private static final String CHARACTER_LIST =
        "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended.txt";
    private static final String CHARACTER_LIST_16 =
        "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended-16.txt";
    private static final int SHEET_COLUMNS = 16;
    private static final int PANEL = 18;
    private static final int COLUMNS = PANEL + 24;
    private static final int ROWS = 39;

    private static final int LABEL = 0x9090C0FF;
    private static final int CARD = 0xF0EEE0FF;
    private static final int RED = 0xC82020FF;
    private static final int INK = 0x101010FF;
    private static final int LIGHT_SQUARE = 0xC8A878FF;
    private static final int DARK_SQUARE = 0x8B5A2BFF;

    private static final int TEN = 0xE0C0;
    private static final int CARD_BACK = 0xE0C1;
    private static final int CARD_BACK_SELECTED = 0xE0C2;
    private static final int SHIELD_STAGES_START = 0xE0A0;
    private static final int SWORD_STAGES_START = 0xE0A4;
    private static final int SHIELD_EMPTY = 0xE0A9;
    private static final int SHIELD_FULL = 0xE0AA;
    private static final int OUTLINED_DICE_START = 0xE0B0;
    private static final int HEART_FULL = 0x2764;
    private static final int HEART_HOLLOW = 0xE0D0;
    private static final int HEART_FILL_START = 0xE0D1;
    private static final int HEART_SWEEP_START = 0xE0D8;

    private static final float SECONDS_PER_METER_STAGE = 0.4f;
    private static final int ANIMATED_METER_COLUMN = PANEL + 20;

    private static final int HOURGLASS_START = 0xE0E0;
    private static final int HOURGLASS_STAGE_COUNT = 5;
    private static final int SAND = 0xD2B48CFF;
    private static final float SECONDS_PER_DRAIN_STAGE = 0.8f;
    private static final float SECONDS_PER_FLIP_FRAME = 0.09f;

    private record TimelineFrame(int codePoint, float seconds) {
    }

    // Drains stage by stage, holds when empty on top, flips quickly, then starts over.
    private static final TimelineFrame[] HOURGLASS_TIMELINE = {
        new TimelineFrame(HOURGLASS_START, SECONDS_PER_DRAIN_STAGE),
        new TimelineFrame(HOURGLASS_START + 1, SECONDS_PER_DRAIN_STAGE),
        new TimelineFrame(HOURGLASS_START + 2, SECONDS_PER_DRAIN_STAGE),
        new TimelineFrame(HOURGLASS_START + 3, SECONDS_PER_DRAIN_STAGE),
        new TimelineFrame(HOURGLASS_START + 4, 1.2f),
        new TimelineFrame(HOURGLASS_START + 5, SECONDS_PER_FLIP_FRAME),
        new TimelineFrame(HOURGLASS_START + 6, SECONDS_PER_FLIP_FRAME),
        new TimelineFrame(HOURGLASS_START + 7, SECONDS_PER_FLIP_FRAME),
    };

    private int hourglassRow;
    private int hourglassShownCodePoint;
    private int[][] meterStages = new int[0][];
    private int[] meterColors = new int[0];
    private int[] meterRows = new int[0];
    private int[] meterShownStage = new int[0];
    private float meterClock;

    private static final int OVERLAY_ROWS = 2;
    private static final int WHEEL_ROWS_PER_NOTCH = 3;
    private static final int PAGE_ROWS_FALLBACK = 1;
    private static final int TAP_SLOP_PIXELS = 24;

    private final GlyphSource smallGlyphs;
    private final Vector2 touchPoint = new Vector2();
    private TextureGlyphSource largeGlyphs;
    private Layer contentLayer;
    private boolean largeFontActive;
    private boolean largeFontPending;

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

    /** @param showcaseFontSetting launch setting {@link #KEY_SHOWCASE_FONT}: "8", "16", or null for the default */
    GlyphShowcaseScene(GlyphSource glyphs, String showcaseFontSetting) {
        super(glyphs, COLUMNS, ROWS);
        smallGlyphs = glyphs;
        largeFontPending = parseLargeFont(showcaseFontSetting);
    }

    private static boolean parseLargeFont(String setting) {
        if (setting == null || setting.trim().isEmpty() || setting.trim().equals("8")) {
            return false;
        }
        if (setting.trim().equals("16")) {
            return true;
        }
        throw new IllegalArgumentException(
            "Invalid value for '" + KEY_SHOWCASE_FONT + "': '" + setting + "' must be 8 or 16");
    }

    @Override
    public String id() {
        return "glyph-showcase";
    }

    @Override
    public String title() {
        return "Glyph sheet showcase";
    }

    @Override
    public void show() {
        if (largeFontPending) {
            largeFontPending = false;
            useFont(true);
        }
        fillGrid();
        super.show();
    }

    @Override
    public void resize(int width, int height) {
        windowWidth = width;
        windowHeight = height;
        if (largeFontActive) {
            layoutLargeView();
        }
        super.resize(width, height);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F) {
            toggleFont();
            return true;
        }
        if (!largeFontActive) {
            return false;
        }
        switch (keycode) {
            case Input.Keys.UP -> scrollBy(0, -1);
            case Input.Keys.DOWN -> scrollBy(0, 1);
            case Input.Keys.LEFT -> scrollBy(-1, 0);
            case Input.Keys.RIGHT -> scrollBy(1, 0);
            case Input.Keys.PAGE_UP -> scrollBy(0, -Math.max(PAGE_ROWS_FALLBACK, visibleContentRows - 1));
            case Input.Keys.PAGE_DOWN -> scrollBy(0, Math.max(PAGE_ROWS_FALLBACK, visibleContentRows - 1));
            case Input.Keys.HOME -> scrollBy(-scrollColumn, -scrollRow);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountY) {
        if (!largeFontActive) {
            return false;
        }
        scrollBy(0, Math.round(amountY) * WHEEL_ROWS_PER_NOTCH);
        return true;
    }

    // Touch devices have no F key, so a tap on the title row switches fonts; a drag scrolls instead.
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
        if (largeFontActive) {
            int stepPixels = largeGlyphs.getCellWidth() * scale;
            pendingDragX += touchLastX - screenX;
            pendingDragY += touchLastY - screenY;
            int columns = ScrollView.wholeSteps(pendingDragX, stepPixels);
            int rows = ScrollView.wholeSteps(pendingDragY, stepPixels);
            pendingDragX -= columns * stepPixels;
            pendingDragY -= rows * stepPixels;
            scrollBy(columns, rows);
        }
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
        if (dragging || !isOnTitleRow(touchStartX, touchStartY)) {
            return dragging;
        }
        toggleFont();
        return true;
    }

    private boolean isOnTitleRow(int screenX, int screenY) {
        viewport.unproject(touchPoint.set(screenX, screenY));
        float cellWidth = activeGlyphs().getCellWidth();
        float cellHeight = activeGlyphs().getCellHeight();
        float pixelsBelowTop = viewport.getWorldHeight() - touchPoint.y - overlayRows * cellHeight;
        if (touchPoint.x < 0f || touchPoint.y > viewport.getWorldHeight() || pixelsBelowTop < 0f) {
            return false;
        }
        int column = (int) (touchPoint.x / cellWidth) + scrollColumn;
        int row = (int) (pixelsBelowTop / cellHeight) + scrollRow;
        return row == 0 && column < COLUMNS;
    }

    @Override
    public String statusText() {
        return largeFontActive ? "Font 16x16 (F/arrows/drag)" : "Font 8x8 (F)";
    }

    private GlyphSource activeGlyphs() {
        return largeFontActive ? largeGlyphs : smallGlyphs;
    }

    private void toggleFont() {
        useFont(!largeFontActive);
        scrollColumn = 0;
        scrollRow = 0;
        fillGrid();
        resize(windowWidth, windowHeight);
    }

    // The 16x16 view shows whole cells at a whole-number scale; the content scrolls when it does not fit.
    private void layoutLargeView() {
        int cellWidth = largeGlyphs.getCellWidth();
        int cellHeight = largeGlyphs.getCellHeight();
        scale = ScrollView.chooseScale(windowWidth, windowHeight, cellWidth);
        visibleColumns = ScrollView.visibleCells(windowWidth, cellWidth, scale);
        int visibleRows = ScrollView.visibleCells(windowHeight, cellHeight, scale);
        int overlayPixels = OVERLAY_ROWS * smallGlyphs.getCellHeight() * Math.max(1, Math.round(Gdx.graphics.getDensity()));
        overlayRows = Math.min(ScrollView.coveredCells(overlayPixels, cellHeight, scale), visibleRows - 1);
        visibleContentRows = visibleRows - overlayRows;
        float worldWidth = visibleColumns * cellWidth;
        float worldHeight = visibleRows * cellHeight;
        viewport.setWorldSize(worldWidth, worldHeight);
        layers.setSize(worldWidth, worldHeight);
        contentLayer.setClip(0f, overlayRows * cellHeight, worldWidth, visibleContentRows * cellHeight);
        scrollBy(0, 0);
    }

    private void scrollBy(int columns, int rows) {
        scrollColumn = ScrollView.clampScroll(scrollColumn + columns, COLUMNS, visibleColumns);
        scrollRow = ScrollView.clampScroll(scrollRow + rows, ROWS, visibleContentRows);
        contentLayer.setCellOffset(-scrollColumn, overlayRows - scrollRow);
    }

    // The large source is loaded once on first use and kept until dispose, so toggling never leaks textures.
    private void useFont(boolean large) {
        if (large && largeGlyphs == null) {
            largeGlyphs = TextureGlyphSource.loadDungeonMode16();
        }
        largeFontActive = large;
        GlyphSource glyphs = activeGlyphs();
        layers.remove(layers.get(0));
        contentLayer = layers.add(grid, glyphs);
        if (!large) {
            overlayRows = 0;
            scrollColumn = 0;
            scrollRow = 0;
        }
        viewport.setWorldSize(COLUMNS * glyphs.getCellWidth(), ROWS * glyphs.getCellHeight());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (largeGlyphs != null) {
            largeGlyphs.dispose();
            largeGlyphs = null;
        }
    }

    private void fillGrid() {
        grid.clear();
        String characterList = largeFontActive ? CHARACTER_LIST_16 : CHARACTER_LIST;
        GlyphOrder order = GlyphOrderText.read(Gdx.files.classpath(characterList).readString("UTF-8"), SHEET_COLUMNS);
        String fontName = largeFontActive ? "16x16" : "8x8";
        writeText(0, 0, "dungeon.mode extended " + fontName + ", " + order.size() + " glyphs", LABEL, CLEAR);
        for (int slot = 0; slot < order.size(); slot++) {
            grid.set(slot % SHEET_COLUMNS, 1 + slot / SHEET_COLUMNS, order.codePointAt(slot), WHITE, CLEAR);
        }
        writeCards(1);
        writeChess(5);
        writeMeters(15);
        writeDice(22);
        writeShadeStrips(25);
        writeSphere(32);
    }

    private void writeCards(int top) {
        writeText(PANEL, top, "Cards", LABEL, CLEAR);
        int[][] hand = {{'A', 0x2660}, {'K', 0x2665}, {'Q', 0x2666}, {'J', 0x2663}, {TEN, 0x2661}, {'9', 0x2664}};
        int column = PANEL;
        for (int[] card : hand) {
            boolean red = card[1] == 0x2665 || card[1] == 0x2666 || card[1] == 0x2661 || card[1] == 0x2662;
            int ink = red ? RED : INK;
            grid.set(column, top + 1, card[0], ink, CARD);
            grid.set(column + 1, top + 1, card[1], ink, CARD);
            column += 3;
        }
        for (int i = 0; i < 6; i++) {
            grid.set(PANEL + i * 2, top + 2, i == 2 ? CARD_BACK_SELECTED : CARD_BACK, i == 2 ? GOLD : CYAN, CLEAR);
        }
    }

    private void writeChess(int top) {
        writeText(PANEL, top, "Chess", LABEL, CLEAR);
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
        for (int meter = 0; meter < meterStages.length; meter++) {
            int[] stages = meterStages[meter];
            int stage = bouncingStage(step, stages.length);
            if (stage != meterShownStage[meter]) {
                meterShownStage[meter] = stage;
                grid.set(ANIMATED_METER_COLUMN, meterRows[meter], stages[stage], meterColors[meter], CLEAR);
            }
        }
        int hourglass = hourglassCodePointAt(meterClock);
        if (hourglass != hourglassShownCodePoint) {
            hourglassShownCodePoint = hourglass;
            grid.set(ANIMATED_METER_COLUMN, hourglassRow, hourglass, SAND, CLEAR);
        }
    }

    private static int hourglassCodePointAt(float seconds) {
        float loopSeconds = 0f;
        for (TimelineFrame frame : HOURGLASS_TIMELINE) {
            loopSeconds += frame.seconds();
        }
        float remaining = seconds % loopSeconds;
        for (TimelineFrame frame : HOURGLASS_TIMELINE) {
            if (remaining < frame.seconds()) {
                return frame.codePoint();
            }
            remaining -= frame.seconds();
        }
        return HOURGLASS_TIMELINE[0].codePoint();
    }

    // Counts 0..n-1 then n-1..0, so each end shows for two steps as a short rest.
    private static int bouncingStage(int step, int stageCount) {
        int position = step % (2 * stageCount);
        return position < stageCount ? position : 2 * stageCount - 1 - position;
    }

    // Each row lists its stages from empty to full, then one cell that fills and empties again.
    private void writeMeters(int top) {
        writeText(PANEL, top, "Meters", LABEL, CLEAR);
        meterStages = new int[][] {
            {SHIELD_EMPTY, SHIELD_STAGES_START, SHIELD_STAGES_START + 1, SHIELD_STAGES_START + 2, SHIELD_FULL},
            {SWORD_STAGES_START, SWORD_STAGES_START + 1, SWORD_STAGES_START + 2, SWORD_STAGES_START + 3,
                SWORD_STAGES_START + 4},
            {HEART_HOLLOW, HEART_FILL_START, HEART_FILL_START + 1, HEART_FILL_START + 2, HEART_FULL},
            {HEART_HOLLOW, HEART_SWEEP_START, HEART_SWEEP_START + 1, HEART_SWEEP_START + 2, HEART_SWEEP_START + 3,
                HEART_SWEEP_START + 4, HEART_SWEEP_START + 5, HEART_SWEEP_START + 6, HEART_FULL},
        };
        meterColors = new int[] {CYAN, GOLD, RED, RED};
        meterRows = new int[meterStages.length];
        meterShownStage = new int[meterStages.length];
        meterClock = 0f;
        for (int meter = 0; meter < meterStages.length; meter++) {
            meterRows[meter] = top + 1 + meter;
            for (int stage = 0; stage < meterStages[meter].length; stage++) {
                grid.set(PANEL + stage * 2, meterRows[meter], meterStages[meter][stage], meterColors[meter], CLEAR);
            }
            grid.set(ANIMATED_METER_COLUMN, meterRows[meter], meterStages[meter][0], meterColors[meter], CLEAR);
        }
        hourglassRow = top + 1 + meterStages.length;
        for (int stage = 0; stage < HOURGLASS_STAGE_COUNT; stage++) {
            grid.set(PANEL + stage * 2, hourglassRow, HOURGLASS_START + stage, SAND, CLEAR);
        }
        hourglassShownCodePoint = HOURGLASS_START;
        grid.set(ANIMATED_METER_COLUMN, hourglassRow, HOURGLASS_START, SAND, CLEAR);
    }

    private void writeDice(int top) {
        writeText(PANEL, top, "Dice", LABEL, CLEAR);
        for (int face = 0; face < 6; face++) {
            grid.set(PANEL + face * 2, top + 1, 0x2680 + face, WHITE, CLEAR);
            grid.set(PANEL + 12 + face * 2, top + 1, OUTLINED_DICE_START + face, WHITE, CLEAR);
        }
    }

    // One strip per shade family, stepping from empty to solid.
    private void writeShadeStrips(int top) {
        writeText(PANEL, top, "Shades", LABEL, CLEAR);
        Shade[][] families = {
            {Shade.BAYER_25, Shade.BAYER_50, Shade.BAYER_75},
            {Shade.HATCH_25, Shade.HATCH_50, Shade.HATCH_75},
            {Shade.FLIPPED_HATCH_25, Shade.FLIPPED_HATCH_50, Shade.FLIPPED_HATCH_75},
        };
        int width = 20;
        for (int family = 0; family < families.length; family++) {
            for (int column = 0; column < width; column++) {
                int level = Math.round(column * 4f / (width - 1));
                int codePoint = switch (level) {
                    case 0 -> ' ';
                    case 4 -> QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.ALL);
                    default -> QuadrantGlyphs.codePoint(families[family][level - 1], QuadrantGlyphs.ALL);
                };
                grid.set(PANEL + column, top + 1 + family * 2, codePoint, WHITE, CLEAR);
            }
        }
    }

    // A lit sphere at twice the grid resolution: each quadrant is on or off, and each cell
    // takes the Bayer shade nearest to the mean brightness of its lit quadrants.
    private void writeSphere(int top) {
        writeText(PANEL, top, "Sub-cell sphere", LABEL, CLEAR);
        int columns = 10;
        int rows = 5;
        Shade[] byLevel = {Shade.BAYER_25, Shade.BAYER_50, Shade.BAYER_75, Shade.SOLID};
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int mask = 0;
                float total = 0f;
                for (int quadrant = 0; quadrant < 4; quadrant++) {
                    float brightness = sphereBrightness(
                        column * 2 + quadrant % 2 + 0.5f, row * 2 + quadrant / 2 + 0.5f, columns * 2f, rows * 2f);
                    if (brightness > 0.06f) {
                        mask |= 1 << quadrant;
                        total += brightness;
                    }
                }
                if (mask == 0) {
                    continue;
                }
                float mean = total / Integer.bitCount(mask);
                int level = Math.max(0, Math.min(3, Math.round(mean * 4f) - 1));
                grid.set(PANEL + column, top + 1 + row, QuadrantGlyphs.codePoint(byLevel[level], mask), GOLD, CLEAR);
            }
        }
    }

    private static float sphereBrightness(float x, float y, float width, float height) {
        float dx = (x - width / 2f) / (height * 0.45f);
        float dy = (y - height / 2f) / (height * 0.45f);
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= 1f) {
            return 0f;
        }
        float nz = (float) Math.sqrt(1f - distanceSquared);
        return Math.max(0f, Math.min(1f, -0.5f * dx - 0.6f * dy + 0.62f * nz));
    }
}
