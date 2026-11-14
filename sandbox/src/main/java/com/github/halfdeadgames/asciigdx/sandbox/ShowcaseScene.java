package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.EighthBar;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.Fade;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.LoopClock;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.MessageLog;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.TypedLine;

import java.util.Arrays;

/** A small roguelike screen assembled in steps: scene frame, stats panel, and message log. */
class ShowcaseScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 26;

    private static final int SCENE_FRAME_WIDTH = 27;
    private static final int SCENE_FRAME_HEIGHT = 16;
    /** Inner area of the scene frame in grid cells; later steps clip their layers to it. */
    static final int SCENE_INNER_LEFT = 1;
    static final int SCENE_INNER_TOP = 1;
    static final int SCENE_INNER_WIDTH = 25;
    static final int SCENE_INNER_HEIGHT = 14;

    private static final int STATS_LEFT = 27;
    private static final int STATS_WIDTH = 13;
    private static final int STATS_TEXT_COLUMN = 28;
    private static final int HP_BAR_COLUMN = STATS_TEXT_COLUMN + 3;
    private static final int HP_BAR_ROW = 3;
    private static final int HP_BAR_CELLS = 8;

    private static final int LOG_TOP = 16;
    private static final int LOG_HEIGHT = 10;

    /** Main-grid cells inside the log frame; the log text uses tall cells, two main rows each. */
    private static final int LOG_INNER_COLUMN = 1;
    private static final int LOG_INNER_ROW = LOG_TOP + 1;
    private static final int LOG_ROWS = (LOG_HEIGHT - 2) / 2;
    private static final int LOG_TEXT_WIDTH = COLUMNS - 2;
    private static final int NO_LINE = -1;
    private static final float LOG_CHARACTERS_PER_SECOND = 30f;

    private static final float LOOP_SECONDS = 11.5f;
    private static final float MAX_FRAME_SECONDS = 0.1f;
    private static final float FADE_IN_SECONDS = 0.5f;
    private static final float FADE_OUT_START_SECONDS = 9.5f;
    private static final float FADE_OUT_SECONDS = 0.5f;

    private static final int LOG_NORMAL_COLOR = 0xB0B0B0FF;
    private static final int FRAME_COLOR = 0x5A6E82FF;
    private static final int LABEL_COLOR = 0xA0A0A0FF;
    private static final int HP_COLOR = 0xD04040FF;

    static final String KEY_SHOWCASE_TIME = "showcaseTime";

    private final LoopClock clock = new LoopClock(LOOP_SECONDS, MAX_FRAME_SECONDS);
    private final Fade fade = new Fade(FADE_IN_SECONDS, FADE_OUT_START_SECONDS, FADE_OUT_SECONDS);
    private final MessageLog log = new MessageLog(
        typedLine(0.6f, "You step onto the old rope bridge."),
        typedLine(2.0f, "Something pale drifts out of the fog."),
        typedLine(3.4f, "The ghost wails. Cold seeps in."),
        typedLine(4.8f, "You back away. The planks crack!"),
        typedLine(6.0f, "You fall!"),
        typedLine(7.6f, "SPLASH!"));
    private final TerminalGrid logGrid = new TerminalGrid(LOG_TEXT_WIDTH, LOG_ROWS);
    private final Layer overlayLayer;
    private final int[] rowLine = new int[LOG_ROWS];
    private final int[] rowVisible = new int[LOG_ROWS];
    private final boolean[] rowNewest = new boolean[LOG_ROWS];
    private boolean clockPinned;

    {
        Arrays.fill(rowLine, NO_LINE);
    }

    private static TypedLine typedLine(float startSeconds, String text) {
        return new TypedLine(startSeconds, text, LOG_CHARACTERS_PER_SECOND);
    }

    /** @param showcaseTimeSetting loop time in seconds to hold the scene at, or null/invalid to play normally */
    ShowcaseScene(GlyphSource glyphs, GlyphSource tallGlyphs, String showcaseTimeSetting) {
        super(glyphs, COLUMNS, ROWS);
        GridDrawing.fill(grid, 0, 0, COLUMNS, ROWS, ' ', WHITE, BLACK);
        drawFrames();
        drawStats();
        addLogLayer(glyphs, tallGlyphs);
        overlayLayer = addOverlay(glyphs);
        pinClockIfRequested(showcaseTimeSetting);
    }

    @Override
    protected void update(float delta) {
        if (!clockPinned) {
            clock.advance(delta);
        }
        float time = clock.time();
        drawLog(time);
        overlayLayer.setAlpha(fade.alphaAt(time));
    }

    @Override
    public String id() {
        return "showcase-water";
    }

    @Override
    public String title() {
        return "Showcase: falling into water";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    /** Writes the 8 bar cells for the given share of full health, 0 to 1. */
    void drawHpBar(float fraction) {
        for (int cell = 0; cell < HP_BAR_CELLS; cell++) {
            int codePoint = EighthBar.codePointAt(cell, HP_BAR_CELLS, fraction);
            grid.set(HP_BAR_COLUMN + cell, HP_BAR_ROW, codePoint, HP_COLOR, BLACK);
        }
    }

    /** Offset is in main-grid pixels, so it is computed from the main glyph source, not the tall one. */
    private void addLogLayer(GlyphSource mainGlyphs, GlyphSource tallGlyphs) {
        GridDrawing.fill(logGrid, 0, 0, LOG_TEXT_WIDTH, LOG_ROWS, ' ', WHITE, CLEAR);
        Layer logLayer = layers.add(logGrid, tallGlyphs);
        logLayer.setOffset(
            LOG_INNER_COLUMN * mainGlyphs.getCellWidth(),
            LOG_INNER_ROW * mainGlyphs.getCellHeight());
    }

    private Layer addOverlay(GlyphSource glyphs) {
        TerminalGrid overlayGrid = new TerminalGrid(COLUMNS, ROWS);
        GridDrawing.fill(overlayGrid, 0, 0, COLUMNS, ROWS, ' ', WHITE, BLACK);
        return layers.add(overlayGrid, glyphs);
    }

    private void pinClockIfRequested(String showcaseTimeSetting) {
        if (showcaseTimeSetting == null) {
            return;
        }
        try {
            float seconds = Float.parseFloat(showcaseTimeSetting.trim());
            if (!Float.isFinite(seconds)) {
                return;
            }
            clock.setTime(seconds);
            clockPinned = true;
        } catch (NumberFormatException ignored) {
            // an invalid value means: play normally
        }
    }

    private void drawLog(float time) {
        int firstShown = log.firstShownLine(time, LOG_ROWS);
        int newestStarted = log.startedCount(time) - 1;
        for (int row = 0; row < LOG_ROWS; row++) {
            drawLogRow(row, time, firstShown + row, newestStarted);
        }
    }

    private void drawLogRow(int row, float time, int lineIndex, int newestStarted) {
        boolean shown = lineIndex < log.lineCount() && log.line(lineIndex).hasStarted(time);
        int shownLine = shown ? lineIndex : NO_LINE;
        int visible = shown ? log.line(lineIndex).visibleCharacters(time) : 0;
        boolean newest = shown && lineIndex == newestStarted;
        if (shownLine == rowLine[row] && visible == rowVisible[row] && newest == rowNewest[row]) {
            return;
        }
        rowLine[row] = shownLine;
        rowVisible[row] = visible;
        rowNewest[row] = newest;
        writeLogRow(row, shown ? log.line(lineIndex).text() : "", visible, newest ? WHITE : LOG_NORMAL_COLOR);
    }

    private void writeLogRow(int row, String text, int visible, int color) {
        for (int column = 0; column < LOG_TEXT_WIDTH; column++) {
            int codePoint = column < visible ? text.charAt(column) : ' ';
            logGrid.set(column, row, codePoint, color, CLEAR);
        }
    }

    private void drawFrames() {
        GridDrawing.box(grid, 0, 0, SCENE_FRAME_WIDTH, SCENE_FRAME_HEIGHT, FRAME_COLOR, BLACK);
        GridDrawing.box(grid, STATS_LEFT, 0, STATS_WIDTH, SCENE_FRAME_HEIGHT, FRAME_COLOR, BLACK);
        GridDrawing.box(grid, 0, LOG_TOP, COLUMNS, LOG_HEIGHT, FRAME_COLOR, BLACK);
    }

    private void drawStats() {
        GridDrawing.text(grid, STATS_TEXT_COLUMN, 1, "@", GOLD, BLACK);
        GridDrawing.text(grid, STATS_TEXT_COLUMN + 2, 1, "Wanderer", WHITE, BLACK);
        GridDrawing.text(grid, STATS_TEXT_COLUMN, HP_BAR_ROW, "HP ", LABEL_COLOR, BLACK);
        drawHpBar(1f);
        drawStatLine(5, "DEPTH ", "3");
        drawStatLine(6, "GOLD  ", "42");
        drawStatLine(7, "TURN  ", "118");
    }

    private void drawStatLine(int row, String label, String value) {
        GridDrawing.text(grid, STATS_TEXT_COLUMN, row, label, LABEL_COLOR, BLACK);
        GridDrawing.text(grid, STATS_TEXT_COLUMN + label.length(), row, value, WHITE, BLACK);
    }
}
