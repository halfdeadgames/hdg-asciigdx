package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs.Shade;

/** The whole default sheet next to examples that use its added glyphs. */
final class GlyphShowcaseScene extends GridScene {

    private static final String CHARACTER_LIST =
        "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended.txt";
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

    GlyphShowcaseScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
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
        grid.clear();
        GlyphOrder order = GlyphOrderText.read(Gdx.files.classpath(CHARACTER_LIST).readString("UTF-8"), SHEET_COLUMNS);
        writeText(0, 0, "dungeon.mode extended, " + order.size() + " glyphs", LABEL, CLEAR);
        for (int slot = 0; slot < order.size(); slot++) {
            grid.set(slot % SHEET_COLUMNS, 1 + slot / SHEET_COLUMNS, order.codePointAt(slot), WHITE, CLEAR);
        }
        writeCards(1);
        writeChess(5);
        writeMeters(15);
        writeDice(22);
        writeShadeStrips(25);
        writeSphere(32);
        super.show();
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
            {0x26C9, SHIELD_STAGES_START, SHIELD_STAGES_START + 1, SHIELD_STAGES_START + 2, 0x26CA},
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
