package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.GlyphSprite;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.Beat;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.DepthCamera;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.EighthBar;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.Fade;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.GhostPath;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.LightningFlash;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.LoopClock;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.MessageLog;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.PlankBreak;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.PlayerPath;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.RainSet;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.TypedLine;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.WaterPattern;

import java.util.Arrays;
import java.util.Random;

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

    private static final float HIDDEN_DISTANCE = 0.1f;
    private static final float FADE_DISTANCE = 0.5f;
    private static final float BRIDGE_DEPTH = 1f;
    private static final float WATER_DEPTH = 3.5f;

    /** Sized so the water still covers the inner area at the water's zoom of 1 / WATER_DEPTH. */
    private static final int WATER_COLUMNS = (int) Math.ceil(SCENE_INNER_WIDTH * WATER_DEPTH);
    private static final int WATER_ROWS = (int) Math.ceil(SCENE_INNER_HEIGHT * WATER_DEPTH);
    private static final int WATER_BACKGROUND_COLOR = 0x0A1E3CFF;
    private static final int WATER_GLYPH_COLOR = 0x3C78B4FF;

    private static final int BRIDGE_ROPE_ROW_ABOVE = 6;
    private static final int BRIDGE_PLANK_ROW = 7;
    private static final int BRIDGE_ROPE_ROW_BELOW = 8;
    private static final int ROPE_COLOR = 0xA0784AFF;
    private static final int PLANK_COLOR = 0x6B4423FF;
    private static final int PLANK_BACKGROUND_COLOR = 0x8B5A2BFF;

    private static final int BROKEN_PLANK_COLUMN = 18;
    private static final int CRACKED_PLANK_CODE_POINT = 0x250A;
    private static final int PLAYER_COLUMN = 15;
    private static final int PLAYER_BACKOFF_STEPS = 3;
    private static final int GHOST_CODE_POINT = 0x1F47B;
    private static final int GHOST_COLOR = 0xD8E4F0FF;
    private static final float GHOST_START_COLUMN = -1f;
    private static final float GHOST_END_COLUMN = 9f;
    private static final float LIGHTNING_STRENGTH = 0.85f;

    private static final int RAIN_SET_COUNT = 3;
    private static final int RAIN_DROPS_PER_SET = 12;
    private static final int RAIN_SLIDE_CELLS = 3;
    private static final int RAIN_COLUMNS = SCENE_INNER_WIDTH + RAIN_SLIDE_CELLS;
    private static final int RAIN_ROWS = SCENE_INNER_HEIGHT + RAIN_SLIDE_CELLS;
    private static final int RAIN_CYCLES_PER_LOOP = 12;
    private static final float RAIN_CYCLE_SECONDS = LOOP_SECONDS / RAIN_CYCLES_PER_LOOP;
    private static final float RAIN_PEAK_ALPHA = 0.6f;
    private static final float RAIN_RAMP_FRACTION = 0.25f;
    private static final int RAIN_DROP_CODE_POINT = '/';
    private static final int RAIN_COLOR = 0x8CB4E6C0;

    private static final Beat HP_DRAIN = new Beat(3.4f, 1.0f, Interpolation.pow2Out);
    private static final float HP_AFTER_WAIL = 0.55f;
    private static final float HP_LOST_AT_END = 1f - HP_AFTER_WAIL;

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
    private final DepthCamera camera = new DepthCamera(HIDDEN_DISTANCE, FADE_DISTANCE);
    private final TerminalGrid waterGrid = new TerminalGrid(WATER_COLUMNS, WATER_ROWS);
    private final TerminalGrid bridgeGrid = new TerminalGrid(SCENE_INNER_WIDTH, SCENE_INNER_HEIGHT);
    private final Layer waterLayer;
    private final Layer bridgeLayer;
    private int drawnWaterStep = Integer.MIN_VALUE;
    private final Layer overlayLayer;
    private final int[] rowLine = new int[LOG_ROWS];
    private final int[] rowVisible = new int[LOG_ROWS];
    private final boolean[] rowNewest = new boolean[LOG_ROWS];
    private PlankBreak drawnPlankBreak = PlankBreak.INTACT;
    private final PlayerPath playerPath = new PlayerPath(PLAYER_COLUMN, BRIDGE_PLANK_ROW, PLAYER_BACKOFF_STEPS);
    private final GhostPath ghostPath = new GhostPath(GHOST_START_COLUMN, GHOST_END_COLUMN);
    private final LightningFlash lightning = new LightningFlash(
        LIGHTNING_STRENGTH,
        new LightningFlash.Flicker(3.40f, 3.48f),
        new LightningFlash.Flicker(3.56f, 3.66f));
    private final TerminalGrid[] rainGrids = new TerminalGrid[RAIN_SET_COUNT];
    private final Layer[] rainLayers = new Layer[RAIN_SET_COUNT];
    private final RainSet[] rainSets = new RainSet[RAIN_SET_COUNT];
    private final int[] drawnRainCycles = new int[RAIN_SET_COUNT];
    private final int[] rainDropColumns = new int[RAIN_DROPS_PER_SET];
    private final int[] rainDropRows = new int[RAIN_DROPS_PER_SET];
    private final Random rainRandom = new Random();
    private final Layer[] lightningLayers;
    private final float cellWidth;
    private final float cellHeight;
    private final GlyphSprite playerSprite;
    private final GlyphSprite ghostSprite;
    private float lastHpFraction = 1f;
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
        waterLayer = addWaterLayer(glyphs);
        bridgeLayer = addBridgeLayer(glyphs);
        cellWidth = glyphs.getCellWidth();
        cellHeight = glyphs.getCellHeight();
        playerSprite = addPlayer();
        ghostSprite = addGhost();
        addRainLayers(glyphs);
        lightningLayers = new Layer[] {waterLayer, bridgeLayer, rainLayers[0], rainLayers[1], rainLayers[2]};
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
        applyDepth();
        drawWaterIfStepChanged(time);
        drawPlankIfBreakChanged(time);
        updatePlayer(time);
        updateGhost(time);
        updateHp(time);
        updateRain(time);
        applyLightning(time);
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

    /**
     * The zoom pivot is the grid's center and moves with the offset, so the offset that puts the grid
     * center on the inner area's center keeps it there at every zoom.
     */
    private Layer addWaterLayer(GlyphSource glyphs) {
        for (int row = 0; row < WATER_ROWS; row++) {
            GridDrawing.fill(waterGrid, 0, row, WATER_COLUMNS, 1, ' ', WATER_GLYPH_COLOR, WATER_BACKGROUND_COLOR);
        }
        Layer layer = layers.add(waterGrid, glyphs);
        float gridCenterX = WATER_COLUMNS * glyphs.getCellWidth() / 2f;
        float gridCenterY = WATER_ROWS * glyphs.getCellHeight() / 2f;
        layer.setPivot(gridCenterX, gridCenterY);
        layer.setOffset(innerCenterX(glyphs) - gridCenterX, innerCenterY(glyphs) - gridCenterY);
        clipToSceneInner(layer, glyphs);
        return layer;
    }

    private Layer addBridgeLayer(GlyphSource glyphs) {
        GridDrawing.fill(bridgeGrid, 0, 0, SCENE_INNER_WIDTH, SCENE_INNER_HEIGHT, ' ', WHITE, CLEAR);
        GridDrawing.fill(bridgeGrid, 0, BRIDGE_ROPE_ROW_ABOVE, SCENE_INNER_WIDTH, 1, '─', ROPE_COLOR, CLEAR);
        GridDrawing.fill(bridgeGrid, 0, BRIDGE_PLANK_ROW, SCENE_INNER_WIDTH, 1, '║', PLANK_COLOR, PLANK_BACKGROUND_COLOR);
        GridDrawing.fill(bridgeGrid, 0, BRIDGE_ROPE_ROW_BELOW, SCENE_INNER_WIDTH, 1, '─', ROPE_COLOR, CLEAR);
        Layer layer = layers.add(bridgeGrid, glyphs);
        layer.setPivot(SCENE_INNER_WIDTH * glyphs.getCellWidth() / 2f, SCENE_INNER_HEIGHT * glyphs.getCellHeight() / 2f);
        layer.setOffset(SCENE_INNER_LEFT * glyphs.getCellWidth(), SCENE_INNER_TOP * glyphs.getCellHeight());
        clipToSceneInner(layer, glyphs);
        return layer;
    }

    /** Sprite positions are the cell box's top-left corner in layer pixels, y down, so a cell's origin is its column and row times the cell size. */
    private GlyphSprite addPlayer() {
        return bridgeLayer.addSprite('@', GOLD, PLAYER_COLUMN * cellWidth, BRIDGE_PLANK_ROW * cellHeight);
    }

    private void updatePlayer(float time) {
        playerSprite.setPosition(playerPath.columnAt(time) * cellWidth, playerPath.rowAt(time) * cellHeight);
        playerSprite.setScale(playerPath.scaleAt(time));
    }

    private void drawPlankIfBreakChanged(float time) {
        PlankBreak state = PlankBreak.at(time);
        if (state == drawnPlankBreak) {
            return;
        }
        drawnPlankBreak = state;
        switch (state) {
            case INTACT -> drawDamagedPlanks('║');
            case CRACKED -> drawDamagedPlanks(CRACKED_PLANK_CODE_POINT);
            case BROKEN -> drawBrokenPlank();
        }
    }

    private void drawBrokenPlank() {
        drawDamagedPlanks(CRACKED_PLANK_CODE_POINT);
        bridgeGrid.set(BROKEN_PLANK_COLUMN, BRIDGE_PLANK_ROW, ' ', WHITE, CLEAR);
    }

    /** The broken plank and its two neighbors, so the crack still shows while the player stands on the middle one. */
    private void drawDamagedPlanks(int codePoint) {
        for (int column = BROKEN_PLANK_COLUMN - 1; column <= BROKEN_PLANK_COLUMN + 1; column++) {
            bridgeGrid.set(column, BRIDGE_PLANK_ROW, codePoint, PLANK_COLOR, PLANK_BACKGROUND_COLOR);
        }
    }

    private GlyphSprite addGhost() {
        GlyphSprite sprite = bridgeLayer.addSprite(GHOST_CODE_POINT, GHOST_COLOR, GHOST_START_COLUMN * cellWidth, BRIDGE_PLANK_ROW * cellHeight);
        sprite.setVisible(false);
        return sprite;
    }

    private void updateGhost(float time) {
        boolean visible = ghostPath.isVisibleAt(time);
        ghostSprite.setVisible(visible);
        if (!visible) {
            return;
        }
        float row = BRIDGE_PLANK_ROW + ghostPath.wobbleRowsAt(time);
        ghostSprite.setPosition(ghostPath.columnAt(time) * cellWidth, row * cellHeight);
        ghostSprite.setAlpha(ghostPath.alphaAt(time));
    }

    /**
     * Each grid is RAIN_SLIDE_CELLS wider and taller than the inner area. It starts with its extra columns on the
     * right and extra rows on top, so sliding down-left never uncovers the inner area's top or right edge.
     */
    private void addRainLayers(GlyphSource glyphs) {
        for (int set = 0; set < RAIN_SET_COUNT; set++) {
            rainSets[set] = new RainSet(RAIN_CYCLE_SECONDS, RAIN_CYCLES_PER_LOOP, set / (float) RAIN_SET_COUNT, -RAIN_SLIDE_CELLS, RAIN_SLIDE_CELLS, RAIN_PEAK_ALPHA, RAIN_RAMP_FRACTION);
            rainGrids[set] = new TerminalGrid(RAIN_COLUMNS, RAIN_ROWS);
            rainLayers[set] = layers.add(rainGrids[set], glyphs);
            clipToSceneInner(rainLayers[set], glyphs);
            drawnRainCycles[set] = Integer.MIN_VALUE;
        }
    }

    private void updateRain(float time) {
        for (int set = 0; set < RAIN_SET_COUNT; set++) {
            redrawRainIfCycleChanged(set, rainSets[set].cycleIndexAt(time));
            rainLayers[set].setAlpha(rainSets[set].alphaAt(time));
            rainLayers[set].setOffset(
                (SCENE_INNER_LEFT + rainSets[set].slideColumnsAt(time)) * cellWidth,
                (SCENE_INNER_TOP - RAIN_SLIDE_CELLS + rainSets[set].slideRowsAt(time)) * cellHeight);
        }
    }

    private void redrawRainIfCycleChanged(int set, int cycleIndex) {
        if (cycleIndex == drawnRainCycles[set]) {
            return;
        }
        drawnRainCycles[set] = cycleIndex;
        rainSets[set].fillDropPositions(rainRandom, RainSet.seedFor(set, cycleIndex), RAIN_COLUMNS, RAIN_ROWS, rainDropColumns, rainDropRows);
        GridDrawing.fill(rainGrids[set], 0, 0, RAIN_COLUMNS, RAIN_ROWS, ' ', WHITE, CLEAR);
        for (int drop = 0; drop < RAIN_DROPS_PER_SET; drop++) {
            rainGrids[set].set(rainDropColumns[drop], rainDropRows[drop], RAIN_DROP_CODE_POINT, RAIN_COLOR, CLEAR);
        }
    }

    private void applyLightning(float time) {
        float amount = lightning.amountAt(time);
        for (int index = 0; index < lightningLayers.length; index++) {
            lightningLayers[index].setInvert(amount);
        }
    }

    private void updateHp(float time) {
        float fraction = 1f - HP_LOST_AT_END * HP_DRAIN.progress(time);
        if (fraction == lastHpFraction) {
            return;
        }
        lastHpFraction = fraction;
        drawHpBar(fraction);
    }

    /** The clip is in stack pixels and ignores the layer's offset and zoom. */
    private static void clipToSceneInner(Layer layer, GlyphSource glyphs) {
        layer.setClip(
            SCENE_INNER_LEFT * glyphs.getCellWidth(),
            SCENE_INNER_TOP * glyphs.getCellHeight(),
            SCENE_INNER_WIDTH * glyphs.getCellWidth(),
            SCENE_INNER_HEIGHT * glyphs.getCellHeight());
    }

    private static float innerCenterX(GlyphSource glyphs) {
        return (SCENE_INNER_LEFT + SCENE_INNER_WIDTH / 2f) * glyphs.getCellWidth();
    }

    private static float innerCenterY(GlyphSource glyphs) {
        return (SCENE_INNER_TOP + SCENE_INNER_HEIGHT / 2f) * glyphs.getCellHeight();
    }

    private void applyDepth() {
        applyDepth(bridgeLayer, BRIDGE_DEPTH);
        applyDepth(waterLayer, WATER_DEPTH);
    }

    /** A layer at or nearer than the camera's hidden distance has no valid zoom, so it is hidden instead. */
    private void applyDepth(Layer layer, float depth) {
        boolean visible = camera.isVisible(depth);
        layer.setVisible(visible);
        if (!visible) {
            return;
        }
        layer.setZoom(camera.zoomFor(depth));
        layer.setAlpha(camera.alphaFor(depth));
    }

    private void drawWaterIfStepChanged(float time) {
        int step = WaterPattern.stepAt(time);
        if (step == drawnWaterStep) {
            return;
        }
        drawnWaterStep = step;
        for (int row = 0; row < WATER_ROWS; row++) {
            for (int column = 0; column < WATER_COLUMNS; column++) {
                waterGrid.set(column, row, WaterPattern.codePointAt(column, row, time), WATER_GLYPH_COLOR, WATER_BACKGROUND_COLOR);
            }
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
