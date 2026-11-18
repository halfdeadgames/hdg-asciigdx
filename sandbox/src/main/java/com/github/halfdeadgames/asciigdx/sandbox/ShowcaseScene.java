package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphFilter;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.SaiksisttSize;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.GlyphSprite;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.LayerGroup;
import com.github.halfdeadgames.asciigdx.layer.Panel;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.CameraPan;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.DepthCamera;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.DropletSpray;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.EighthBar;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.FallSpin;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.GhostPath;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.LightningFlash;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.LoopFade;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.MessageLog;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.PlankBreak;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.PlayerPath;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.RainSet;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.RippleRings;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.SeeThroughWater;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.Splash;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.SplashColor;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.TitlePop;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.TypedLine;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.UnderwaterDive;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.UnderwaterSwap;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.WaterPattern;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.WaterRipple;
import com.github.halfdeadgames.asciigdx.sandbox.showcase.WaterSway;
import com.github.halfdeadgames.asciigdx.tween.Timeline;
import com.github.halfdeadgames.asciigdx.tween.Tween;
import com.github.halfdeadgames.asciigdx.tween.TweenClock;
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

    private static final float FALL_START_SECONDS = 6.0f;
    private static final float FALL_SECONDS = 1.6f;
    private static final float PAN_SECONDS = 0.8f;
    private static final float FALL_CAMERA_DEPTH = 2.5f;
    private static final Tween CAMERA_DEPTH_FALL =
            Tween.eased(FALL_START_SECONDS, FALL_SECONDS, 0f, 1f, Interpolation.pow2In);
    private static final float SPIN_SECONDS = 1.3f;
    private static final float SPIN_DEGREES = -720f;
    private static final float SHRUNK_SCALE = 0.5f;
    private static final FallSpin FALL_SPIN = new FallSpin(
            Tween.eased(FALL_START_SECONDS, SPIN_SECONDS, 0f, 1f, Interpolation.sine), SPIN_DEGREES,
            SHRUNK_SCALE);
    private static final Tween CAMERA_PAN =
            Tween.eased(FALL_START_SECONDS, PAN_SECONDS, 0f, 1f, Interpolation.sine);
    private static final float WATER_SWAY_DEGREES = -3f;
    private static final float WATER_SWAY_CYCLES_PER_SECOND = 1.25f;
    private static final WaterSway WATER_SWAY = new WaterSway(
            Tween.linear(FALL_START_SECONDS, FALL_SECONDS, 0f, 1f), WATER_SWAY_DEGREES,
            WATER_SWAY_CYCLES_PER_SECOND);

    /**
     * Sized so the water covers the inner area at its zoom of 1 / WATER_DEPTH. The pan shifts it by
     * up to about 7 columns and 2.5 rows while the zoom is still small, which needs about 45.5
     * columns and 24.6 rows on each side of center; the margin adds 4 cells per side beyond the
     * zoomed-out size.
     */
    private static final int WATER_MARGIN_CELLS = 4;
    private static final int WATER_COLUMNS =
            (int) Math.ceil(SCENE_INNER_WIDTH * WATER_DEPTH) + 2 * WATER_MARGIN_CELLS;
    private static final int WATER_ROWS =
            (int) Math.ceil(SCENE_INNER_HEIGHT * WATER_DEPTH) + 2 * WATER_MARGIN_CELLS;
    private static final int WATER_BACKGROUND_COLOR = 0x0A1E3CFF;
    private static final int WATER_GLYPH_COLOR = 0x3C78B4FF;

    /**
     * Bridge columns are numbered from the inner area's left edge (0); the grid extends this many
     * columns past each side. While the pan centers the player (6 columns right of center), the
     * right end sits zoom * (12.5 + margin - 6 * pan) cells from the screen center and must stay >=
     * 12.5 (half the inner width). Sampled over the pan and dive, the worst case needs a margin of
     * about 1.12, so 2 columns, plus 2 spare.
     */
    private static final int BRIDGE_MARGIN_COLUMNS = 4;
    private static final int BRIDGE_COLUMNS = SCENE_INNER_WIDTH + 2 * BRIDGE_MARGIN_COLUMNS;
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

    private static final float HP_DRAIN_START_SECONDS = 3.4f;
    private static final float HP_DRAIN_SECONDS = 1.0f;
    private static final float HP_AFTER_WAIL = 0.55f;

    private static final float SPLASH_SECONDS = 7.6f;
    private static final float SPRAY_SECONDS = 0.7f;
    private static final float FLASH_SECONDS = 0.25f;
    private static final int SPRAY_DROPLET_COUNT = 8;
    private static final float SPRAY_MAX_DISTANCE_CELLS = 4f;
    private static final float SPRAY_PEAK_SCALE = 1.8f;

    private static final float RIPPLE_STAGGER_SECONDS = 0.15f;
    private static final int RIPPLE_RING_COUNT = 3;
    private static final float RIPPLE_RING_LIFE_SECONDS = 0.9f;
    private static final float RIPPLE_SPEED_CELLS_PER_SECOND = 9f;
    private static final float RIPPLE_HALF_WIDTH_CELLS = 1.2f;
    private static final float RIPPLE_MAX_SCALE_BUMP = 0.6f;
    private static final float RIPPLE_COVER_MARGIN_CELLS = 1f;

    private static final float SINK_SECONDS = 0.4f;
    private static final float SURFACE_SECONDS = 0.6f;
    private static final UnderwaterDive UNDERWATER_DIVE = new UnderwaterDive(
            Tween.eased(SPLASH_SECONDS, SINK_SECONDS, 0f, 1f, Interpolation.sineOut),
            Tween.eased(SPLASH_SECONDS + SINK_SECONDS, SURFACE_SECONDS, 0f, 1f,
                    Interpolation.sine));

    private static final String TITLE_TEXT = "AsciiGDX";
    private static final float TITLE_START_SECONDS = 8.6f;
    private static final float TITLE_STAGGER_SECONDS = 0.06f;
    private static final float TITLE_POP_SECONDS = 0.3f;
    private static final float TITLE_OVERSHOOT = 3.5f;
    private static final int TITLE_FROM_COLOR = 0xFFD45CFF;
    private static final int TITLE_TO_COLOR = 0xFF6E8AFF;
    private static final float TITLE_CENTER_HEIGHT_FRACTION = 0.25f;
    private static final TitlePop TITLE_POP = new TitlePop(
            TITLE_START_SECONDS, TITLE_STAGGER_SECONDS, TITLE_POP_SECONDS, TITLE_OVERSHOOT,
            TITLE_TEXT.length(), TITLE_FROM_COLOR, TITLE_TO_COLOR);

    /** The x GridScene passes to layers.draw; the y is the viewport's world height. */
    private static final float STACK_DRAW_X = 0f;

    static final String KEY_SHOWCASE_TIME = "showcaseTime";

    private final TweenClock clock = TweenClock.looping(LOOP_SECONDS, MAX_FRAME_SECONDS);
    private final Timeline timeline = new Timeline();
    private final MessageLog log = new MessageLog(
            typedLine(0.6f, "You step onto the old rope bridge."),
            typedLine(2.0f, "Something pale drifts out of the fog."),
            typedLine(3.4f, "The ghost wails. Cold seeps in."),
            typedLine(4.8f, "You back away. The planks crack!"),
            typedLine(6.0f, "You fall!"),
            typedLine(7.6f, "SPLASH!"),
            typedLine(8.6f, "You surface, gasping."));
    private final TerminalGrid logGrid = new TerminalGrid(LOG_TEXT_WIDTH, LOG_ROWS);
    private final DepthCamera camera = new DepthCamera(HIDDEN_DISTANCE, FADE_DISTANCE);
    private final TerminalGrid waterGrid = new TerminalGrid(WATER_COLUMNS, WATER_ROWS);
    private final TerminalGrid bridgeGrid = new TerminalGrid(BRIDGE_COLUMNS, SCENE_INNER_HEIGHT);
    private final Layer waterLayer;
    private final Layer playerLayer;
    private final Layer bridgeLayer;
    private int drawnWaterStep = Integer.MIN_VALUE;
    private final Layer overlayLayer;
    private final int[] rowLine = new int[LOG_ROWS];
    private final int[] rowVisible = new int[LOG_ROWS];
    private final boolean[] rowNewest = new boolean[LOG_ROWS];
    private PlankBreak drawnPlankBreak = PlankBreak.INTACT;
    private final PlayerPath playerPath =
            new PlayerPath(PLAYER_COLUMN, BRIDGE_PLANK_ROW, PLAYER_BACKOFF_STEPS);
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
    private final LayerGroup lightningGroup;
    private final float cellWidth;
    private final float cellHeight;
    private final Panel scenePanel;
    private final float waterCenterX;
    private final float waterCenterY;
    private final float waterRestOffsetX;
    private final float waterRestOffsetY;
    private final GlyphSprite playerSprite;
    private final GlyphSprite fallingPlayerSprite;
    private final TextureGlyphSource smoothGlyphs;
    private final TextureGlyphSource crispGlyphs;
    private final TextureGlyphSource titleGlyphs;
    private final GlyphSprite[] titleLetters = new GlyphSprite[TITLE_TEXT.length()];
    private final Layer crispPlayerLayer;
    private final Layer dropletLayer;
    private Splash splash;
    private final WaterRipple waterRipple;
    private final Layer underwaterLayer;
    private final UnderwaterSwap underwaterSwap;
    private final GlyphSprite crispPlayerSprite;
    private final float fallStartX;
    private final float fallStartY;
    private final float fallEndX;
    private final float fallEndY;
    private final CameraPan cameraPan;
    private final GlyphSprite ghostSprite;
    private final LayerGroup splashGroup;
    private boolean clockPinned;
    private final Vector2 waterPixel = new Vector2();

    {
        Arrays.fill(rowLine, NO_LINE);
    }

    private static TypedLine typedLine(float startSeconds, String text) {
        return new TypedLine(startSeconds, text, LOG_CHARACTERS_PER_SECOND);
    }

    /**
     * @param showcaseTimeSetting loop time in seconds to hold the scene at, or null/invalid to play
     *        normally
     */
    ShowcaseScene(GlyphSource glyphs, GlyphSource tallGlyphs, String showcaseTimeSetting) {
        super(glyphs, COLUMNS, ROWS);
        GridDrawing.fillRectangle(grid, 0, 0, COLUMNS, ROWS, ' ', WHITE, BLACK);
        drawFrames();
        drawStats();
        cellWidth = glyphs.getCellWidth();
        cellHeight = glyphs.getCellHeight();
        scenePanel = layers.addPanelInCells(SCENE_INNER_LEFT, SCENE_INNER_TOP, SCENE_INNER_WIDTH,
                SCENE_INNER_HEIGHT, glyphs);
        smoothGlyphs =
                TextureGlyphSource.loadSaiksistt(SaiksisttSize.SQUARE_16X16, GlyphFilter.LINEAR);
        crispGlyphs =
                TextureGlyphSource.loadSaiksistt(SaiksisttSize.SQUARE_8X8, GlyphFilter.NEAREST);
        titleGlyphs =
                TextureGlyphSource.loadSaiksistt(SaiksisttSize.TALL_16X32, GlyphFilter.LINEAR);
        waterCenterX = WATER_COLUMNS * cellWidth / 2f;
        waterCenterY = WATER_ROWS * cellHeight / 2f;
        waterRestOffsetX = innerCenterX(glyphs) - waterCenterX;
        waterRestOffsetY = innerCenterY(glyphs) - waterCenterY;
        waterLayer = addWaterLayer(glyphs);
        underwaterLayer = addUnderwaterLayer(glyphs);
        playerLayer = addInnerLayer(smoothGlyphs);
        crispPlayerLayer = addCrispPlayerLayer();
        dropletLayer = addInnerLayer(glyphs);
        bridgeLayer = addBridgeLayer(glyphs);
        playerSprite = addPlayer();
        fallStartX = playerPath.columnAt(FALL_START_SECONDS) * cellWidth;
        fallStartY = playerPath.rowAt(FALL_START_SECONDS) * cellHeight;
        fallEndX = (SCENE_INNER_WIDTH - 1) * cellWidth / 2f;
        fallEndY = (SCENE_INNER_HEIGHT - 1) * cellHeight / 2f;
        fallingPlayerSprite = addFallingPlayer();
        crispPlayerSprite = addCrispPlayer();
        cameraPan = createCameraPan();
        ghostSprite = addGhost();
        addRainLayers(glyphs);
        splashGroup = createSplashGroup();
        lightningGroup = createLightningGroup(splashGroup);
        poseStackAt(SPLASH_SECONDS);
        splash = createSplash();
        waterRipple = createWaterRipple();
        underwaterSwap = createUnderwaterSwap();
        addTitleLetters(addTitleLayer());
        addLogLayer(glyphs, tallGlyphs);
        overlayLayer = addOverlay(glyphs);
        bindTimeline();
        pinAllLayersToScreen();
        pinClockIfRequested(showcaseTimeSetting);
    }

    @Override
    protected void update(float delta) {
        layers.beginCellEffects(delta);
        if (!clockPinned) {
            clock.update(delta);
        }
        float time = clock.getTimeSeconds();
        applyCamera(time);
        applyWaterSway(time);
        waterRipple.update(time);
        drawWaterIfStepChanged(time);
        drawPlankIfBreakChanged(time);
        updatePlayer(time);
        underwaterSwap.update(time);
        updateGhost(time);
        updateRain(time);
        applyLightning(time);
        splash.update(time);
        drawLog(time);
        timeline.apply(time);
    }

    private void bindTimeline() {
        bindOverlayFade();
        bindHpBar();
        bindTitleLetters();
        bindSplashColors();
        bindSpriteVisibility();
    }

    private void bindOverlayFade() {
        timeline.bind(
                LoopFade.track(FADE_IN_SECONDS, FADE_OUT_START_SECONDS, FADE_OUT_SECONDS),
                overlayLayer.getColorEffects()::setAlpha);
    }

    private void bindHpBar() {
        timeline.bind(
                Tween.eased(HP_DRAIN_START_SECONDS, HP_DRAIN_SECONDS, 1f, HP_AFTER_WAIL,
                        Interpolation.pow2Out),
                this::drawHpBar);
    }

    private void bindTitleLetters() {
        timeline.bind(TITLE_POP.scaleTrack(), titleLetters.length,
                (letter, scale) -> titleLetters[letter].setScale(scale));
        timeline.bind(TITLE_POP.alphaTrack(), titleLetters.length,
                (letter, alpha) -> titleLetters[letter].setAlpha(alpha));
    }

    private void bindSplashColors() {
        timeline.bind(SplashColor.flashTrack(SPLASH_SECONDS, FLASH_SECONDS, Interpolation.pow2Out),
                this::mixSplashLayersWithWhite);
        timeline.bind(SplashColor.saturationTrack(UNDERWATER_DIVE), this::saturateSplashLayers);
    }

    private void mixSplashLayersWithWhite(float amount) {
        splashGroup.getColorEffects().setMix(Color.WHITE, amount);
    }

    private void saturateSplashLayers(float saturation) {
        splashGroup.getColorEffects().setSaturation(saturation);
    }

    /**
     * Holds the underwater, player, crisp player, and droplet layers directly. The lightning group
     * nests inside, so splash effects reach all nine layers.
     */
    private LayerGroup createSplashGroup() {
        LayerGroup group = layers.addGroup();
        group.add(underwaterLayer);
        group.add(playerLayer);
        group.add(crispPlayerLayer);
        group.add(dropletLayer);
        return group;
    }

    private LayerGroup createLightningGroup(LayerGroup parent) {
        LayerGroup group = layers.addGroup();
        group.add(waterLayer);
        group.add(bridgeLayer);
        for (int set = 0; set < RAIN_SET_COUNT; set++) {
            group.add(rainLayers[set]);
        }
        group.setParent(parent);
        return group;
    }

    /**
     * Binds the bridge @, the falling @, and the ghost. The crisp @ stays in updatePlayer because
     * it is visible over two spans (spin end to splash, and after surfacing).
     */
    private void bindSpriteVisibility() {
        timeline.bindVisibility(0f, FALL_START_SECONDS, playerSprite::setVisible);
        timeline.bindVisibility(FALL_START_SECONDS, FALL_START_SECONDS + SPIN_SECONDS,
                fallingPlayerSprite::setVisible);
        timeline.bindVisibility(ghostPath.driftStartSeconds(), LOOP_SECONDS,
                ghostSprite::setVisible);
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
     * The zoom pivot is the grid's center and moves with the offset, so the offset that puts the
     * grid center on the inner area's center keeps it there at every zoom.
     */
    private Layer addWaterLayer(GlyphSource glyphs) {
        for (int row = 0; row < WATER_ROWS; row++) {
            GridDrawing.fillRectangle(waterGrid, 0, row, WATER_COLUMNS, 1, ' ', WATER_GLYPH_COLOR,
                    WATER_BACKGROUND_COLOR);
        }
        Layer layer = layers.add(waterGrid, glyphs);
        layer.setPivot(waterCenterX, waterCenterY);
        layer.setOffset(waterRestOffsetX, waterRestOffsetY);
        layer.setPanel(scenePanel);
        return layer;
    }

    private Layer addBridgeLayer(GlyphSource glyphs) {
        GridDrawing.fillRectangle(bridgeGrid, 0, 0, BRIDGE_COLUMNS, SCENE_INNER_HEIGHT, ' ',
                WHITE, CLEAR);
        GridDrawing.fillRectangle(bridgeGrid, 0, BRIDGE_ROPE_ROW_ABOVE, BRIDGE_COLUMNS, 1, '─',
                ROPE_COLOR, CLEAR);
        GridDrawing.fillRectangle(bridgeGrid, 0, BRIDGE_PLANK_ROW, BRIDGE_COLUMNS, 1, '║',
                PLANK_COLOR, PLANK_BACKGROUND_COLOR);
        GridDrawing.fillRectangle(bridgeGrid, 0, BRIDGE_ROPE_ROW_BELOW, BRIDGE_COLUMNS, 1, '─',
                ROPE_COLOR, CLEAR);
        return addInnerLayer(bridgeGrid, glyphs, BRIDGE_MARGIN_COLUMNS);
    }

    private Layer addInnerLayer(GlyphSource glyphs) {
        TerminalGrid emptyGrid = new TerminalGrid(SCENE_INNER_WIDTH, SCENE_INNER_HEIGHT);
        GridDrawing.fillRectangle(emptyGrid, 0, 0, SCENE_INNER_WIDTH, SCENE_INNER_HEIGHT, ' ',
                WHITE, CLEAR);
        return addInnerLayer(emptyGrid, glyphs, 0);
    }

    /**
     * Puts the pivot at the grid center and offsets the layer so grid column {@code marginColumns}
     * lines up with the inner area's left edge. Zoom then keeps the inner center fixed.
     */
    private Layer addInnerLayer(TerminalGrid layerGrid, GlyphSource glyphs, int marginColumns) {
        Layer layer = layers.add(layerGrid, glyphs);
        float gridColumns = SCENE_INNER_WIDTH + 2 * marginColumns;
        layer.setPivot(gridColumns * glyphs.getCellWidth() / 2f,
                SCENE_INNER_HEIGHT * glyphs.getCellHeight() / 2f);
        layer.setOffset(-marginColumns * glyphs.getCellWidth(), 0f);
        layer.setPanel(scenePanel);
        return layer;
    }

    /**
     * Sprite positions are the cell box's top-left corner in layer pixels, y down, so a cell's
     * origin is its column and row times the cell size.
     */
    private GlyphSprite addPlayer() {
        return bridgeLayer.addSprite('@', GOLD, bridgeLayerX(PLAYER_COLUMN),
                BRIDGE_PLANK_ROW * cellHeight);
    }

    /**
     * Converts a bridge column (0 = inner area's left edge, may be fractional) to bridge layer
     * pixels.
     */
    private float bridgeLayerX(float bridgeColumn) {
        return (bridgeColumn + BRIDGE_MARGIN_COLUMNS) * cellWidth;
    }

    private static int bridgeGridColumn(int bridgeColumn) {
        return bridgeColumn + BRIDGE_MARGIN_COLUMNS;
    }

    private GlyphSprite addFallingPlayer() {
        return playerLayer.addSprite('@', GOLD, fallStartX, fallStartY);
    }

    /**
     * The target is the player's cell center relative to the bridge pivot. The margin is symmetric,
     * so bridge numbering needs no shift.
     */
    private CameraPan createCameraPan() {
        float targetX =
                CameraPan.cellCenterFromPivot(playerPath.columnAt(FALL_START_SECONDS), cellWidth,
                        SCENE_INNER_WIDTH * cellWidth / 2f);
        float targetY =
                CameraPan.cellCenterFromPivot(playerPath.rowAt(FALL_START_SECONDS), cellHeight,
                        SCENE_INNER_HEIGHT * cellHeight / 2f);
        return new CameraPan(CAMERA_PAN, targetX, targetY);
    }

    /**
     * Sets every layer's parallax to 0, so only depth layers follow the scene panel's camera; they
     * get their parallax from their zoom each frame.
     */
    private void pinAllLayersToScreen() {
        for (int index = 0; index < layers.getLayerCount(); index++) {
            layers.getLayer(index).setParallax(0f);
        }
    }

    private void applyCamera(float time) {
        camera.setCameraDepth(FALL_CAMERA_DEPTH * CAMERA_DEPTH_FALL.getProgressAt(time));
        scenePanel.setCamera(cameraPan.cameraXAt(time), cameraPan.cameraYAt(time));
        applyDepth();
    }

    @Override
    public void dispose() {
        super.dispose();
        smoothGlyphs.dispose();
        crispGlyphs.dispose();
        titleGlyphs.dispose();
    }

    /**
     * An empty layer at the inner area's top-left, in the same spot as the crisp player layer; the
     * letters are its sprites.
     */
    private Layer addTitleLayer() {
        TerminalGrid emptyGrid = new TerminalGrid(1, 1);
        emptyGrid.set(0, 0, ' ', WHITE, CLEAR);
        Layer layer = layers.add(emptyGrid, titleGlyphs);
        layer.setPanel(scenePanel);
        return layer;
    }

    /**
     * Sprites scale about their cell center by default. The row is centered horizontally in the
     * inner area, and vertically on a quarter of its height so it stays above the @ that surfaces
     * at the center.
     */
    private void addTitleLetters(Layer titleLayer) {
        float letterWidth = titleGlyphs.getCellWidth();
        float letterHeight = titleGlyphs.getCellHeight();
        float left = (SCENE_INNER_WIDTH * cellWidth - titleLetters.length * letterWidth) / 2f;
        float top =
                SCENE_INNER_HEIGHT * cellHeight * TITLE_CENTER_HEIGHT_FRACTION - letterHeight / 2f;
        for (int letter = 0; letter < titleLetters.length; letter++) {
            GlyphSprite sprite =
                    titleLayer.addSprite(TITLE_TEXT.charAt(letter), TITLE_POP.colorAt(letter),
                            left + letter * letterWidth, top);
            titleLetters[letter] = sprite;
        }
    }

    private void updatePlayer(float time) {
        boolean fallen = time >= FALL_START_SECONDS;
        boolean spinDone = fallen && FALL_SPIN.isCrispAt(time);
        crispPlayerSprite.setVisible(spinDone && !UNDERWATER_DIVE.isUnderwaterAt(time));
        if (fallen) {
            updateFallingPlayer(time);
            return;
        }
        playerSprite.setPosition(bridgeLayerX(playerPath.columnAt(time)),
                playerPath.rowAt(time) * cellHeight);
        playerSprite.setScale(playerPath.scaleAt(time));
    }

    /**
     * Both sprites share one center; the crisp cell is smaller, so its corner shifts by half the
     * size difference.
     */
    private void updateFallingPlayer(float time) {
        float x = fallingPlayerX(time);
        float y = fallingPlayerY(time);
        fallingPlayerSprite.setPosition(x, y);
        fallingPlayerSprite.setRotation(FALL_SPIN.rotationDegreesAt(time));
        fallingPlayerSprite.setScale(FALL_SPIN.scaleAt(time));
        crispPlayerSprite.setPosition(
                x + (cellWidth - crispGlyphs.getCellWidth()) / 2f,
                y + (cellHeight - crispGlyphs.getCellHeight()) / 2f);
    }

    /** X of the top-left corner of the falling @'s cell, in player layer pixels. */
    private float fallingPlayerX(float time) {
        return cameraPan.followAt(time, fallStartX, fallEndX, camera.zoomFor(BRIDGE_DEPTH));
    }

    private float fallingPlayerY(float time) {
        return cameraPan.followAt(time, fallStartY, fallEndY, camera.zoomFor(BRIDGE_DEPTH));
    }

    /**
     * Rocks the water about the pixel under the falling @'s center, measured in the water's rest
     * placement. Without a sway the rest values are used as is. Needs the camera and water zoom of
     * this frame applied first.
     */
    private void applyWaterSway(float time) {
        float degrees = WATER_SWAY.degreesAt(time);
        placeWaterAtRest();
        if (degrees == 0f || camera.zoomFor(WATER_DEPTH) <= 0f) {
            return;
        }
        findWaterPixelUnder(playerCenterStackX(time), playerCenterStackY(time));
        waterLayer.setPivotKeepingPlacement(waterPixel.x, waterPixel.y);
        waterLayer.setRotation(degrees);
    }

    private void placeWaterAtRest() {
        waterLayer.setRotation(0f);
        waterLayer.setPivot(waterCenterX, waterCenterY);
        waterLayer.setOffset(waterRestOffsetX, waterRestOffsetY);
    }

    /**
     * Finds the water layer pixel under a point given in stack pixels (y down) and stores it in
     * waterPixel.
     */
    private void findWaterPixelUnder(float stackPixelX, float stackPixelY) {
        float stackTop = viewport.getWorldHeight();
        layers.toLayerPoint(waterLayer, STACK_DRAW_X, stackTop, STACK_DRAW_X + stackPixelX,
                stackTop - stackPixelY, waterPixel);
    }

    /**
     * Applies the camera and water sway for the given time. The layer-point query only sees the
     * stack's current state, but the ripple and the see-through water need the cell under the @ at
     * the splash when they are built. The first update replaces this pose.
     */
    private void poseStackAt(float time) {
        applyCamera(time);
        applyWaterSway(time);
    }

    private float playerCenterStackX(float time) {
        return scenePanel.getX() + fallingPlayerX(time) + cellWidth / 2f;
    }

    private float playerCenterStackY(float time) {
        return scenePanel.getY() + fallingPlayerY(time) + cellHeight / 2f;
    }

    /**
     * An empty layer in the same screen-space spot as the player layer, so sprite coordinates
     * match.
     */
    private Layer addCrispPlayerLayer() {
        TerminalGrid emptyGrid = new TerminalGrid(1, 1);
        emptyGrid.set(0, 0, ' ', WHITE, CLEAR);
        Layer layer = layers.add(emptyGrid, crispGlyphs);
        layer.setPanel(scenePanel);
        return layer;
    }

    /**
     * Inserted just below the water, so the water draws over it. Filled with the water's background
     * color so see-through water cells blend water over water instead of darkening toward the black
     * behind the stack.
     */
    private Layer addUnderwaterLayer(GlyphSource mainGlyphs) {
        int columns = (int) Math.ceil(
                SCENE_INNER_WIDTH * mainGlyphs.getCellWidth() / crispGlyphs.getCellWidth());
        int rows = (int) Math.ceil(
                SCENE_INNER_HEIGHT * mainGlyphs.getCellHeight() / crispGlyphs.getCellHeight());
        TerminalGrid underwaterGrid = new TerminalGrid(columns, rows);
        GridDrawing.fillRectangle(underwaterGrid, 0, 0, columns, rows, ' ', WHITE,
                WATER_BACKGROUND_COLOR);
        Layer layer = layers.insert(layers.indexOf(waterLayer), underwaterGrid, crispGlyphs);
        layer.setPanel(scenePanel);
        return layer;
    }

    /** The sprite sits where the crisp @ does at the splash, and scales about its own center. */
    private UnderwaterSwap createUnderwaterSwap() {
        GlyphSprite sprite = underwaterLayer.addSprite(
                '@', GOLD,
                fallEndX + (cellWidth - crispGlyphs.getCellWidth()) / 2f,
                fallEndY + (cellHeight - crispGlyphs.getCellHeight()) / 2f);
        sprite.setVisible(false);
        SeeThroughWater seeThroughWater = new SeeThroughWater(
                UNDERWATER_DIVE, waterLayer, splashWaterColumn(), splashWaterRow(),
                cellHeight / cellWidth);
        return new UnderwaterSwap(UNDERWATER_DIVE, sprite, seeThroughWater);
    }

    private GlyphSprite addCrispPlayer() {
        GlyphSprite sprite = crispPlayerLayer.addSprite('@', GOLD, 0f, 0f);
        sprite.setVisible(false);
        return sprite;
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
        bridgeGrid.set(bridgeGridColumn(BROKEN_PLANK_COLUMN), BRIDGE_PLANK_ROW, ' ', WHITE, CLEAR);
    }

    /**
     * The broken plank and its two neighbors, so the crack still shows while the player stands on
     * the middle one.
     */
    private void drawDamagedPlanks(int codePoint) {
        for (int column = BROKEN_PLANK_COLUMN - 1; column <= BROKEN_PLANK_COLUMN + 1; column++) {
            bridgeGrid.set(bridgeGridColumn(column), BRIDGE_PLANK_ROW, codePoint, PLANK_COLOR,
                    PLANK_BACKGROUND_COLOR);
        }
    }

    private GlyphSprite addGhost() {
        return bridgeLayer.addSprite(GHOST_CODE_POINT, GHOST_COLOR,
                bridgeLayerX(GHOST_START_COLUMN), BRIDGE_PLANK_ROW * cellHeight);
    }

    private void updateGhost(float time) {
        if (!ghostPath.isVisibleAt(time)) {
            return;
        }
        float row = BRIDGE_PLANK_ROW + ghostPath.wobbleRowsAt(time);
        ghostSprite.setPosition(bridgeLayerX(ghostPath.columnAt(time)), row * cellHeight);
        ghostSprite.setAlpha(ghostPath.alphaAt(time));
    }

    /**
     * Each grid is RAIN_SLIDE_CELLS wider and taller than the inner area. It starts with its extra
     * columns on the right and extra rows on top, so sliding down-left never uncovers the inner
     * area's top or right edge.
     */
    private void addRainLayers(GlyphSource glyphs) {
        for (int set = 0; set < RAIN_SET_COUNT; set++) {
            rainSets[set] = new RainSet(RAIN_CYCLE_SECONDS, RAIN_CYCLES_PER_LOOP,
                    set / (float) RAIN_SET_COUNT, -RAIN_SLIDE_CELLS, RAIN_SLIDE_CELLS,
                    RAIN_PEAK_ALPHA, RAIN_RAMP_FRACTION);
            rainGrids[set] = new TerminalGrid(RAIN_COLUMNS, RAIN_ROWS);
            rainLayers[set] = layers.add(rainGrids[set], glyphs);
            rainLayers[set].setPanel(scenePanel);
            drawnRainCycles[set] = Integer.MIN_VALUE;
        }
    }

    private void updateRain(float time) {
        for (int set = 0; set < RAIN_SET_COUNT; set++) {
            redrawRainIfCycleChanged(set, rainSets[set].cycleIndexAt(time));
            rainLayers[set].getColorEffects().setAlpha(rainSets[set].alphaAt(time));
            rainLayers[set].setOffset(
                    rainSets[set].slideColumnsAt(time) * cellWidth,
                    (rainSets[set].slideRowsAt(time) - RAIN_SLIDE_CELLS) * cellHeight);
        }
    }

    private void redrawRainIfCycleChanged(int set, int cycleIndex) {
        if (cycleIndex == drawnRainCycles[set]) {
            return;
        }
        drawnRainCycles[set] = cycleIndex;
        rainSets[set].fillDropPositions(rainRandom, RainSet.seedFor(set, cycleIndex), RAIN_COLUMNS,
                RAIN_ROWS, rainDropColumns, rainDropRows);
        GridDrawing.fillRectangle(rainGrids[set], 0, 0, RAIN_COLUMNS, RAIN_ROWS, ' ', WHITE, CLEAR);
        for (int drop = 0; drop < RAIN_DROPS_PER_SET; drop++) {
            rainGrids[set].set(rainDropColumns[drop], rainDropRows[drop], RAIN_DROP_CODE_POINT,
                    RAIN_COLOR, CLEAR);
        }
    }

    /**
     * The splash point is the center of the player's final cell; the frame, stats, and log are not
     * tinted.
     */
    private Splash createSplash() {
        DropletSpray spray = new DropletSpray(
                Tween.linear(SPLASH_SECONDS, SPRAY_SECONDS, 0f, 1f), SPRAY_DROPLET_COUNT,
                SPRAY_MAX_DISTANCE_CELLS, SPRAY_PEAK_SCALE);
        return new Splash(
                dropletLayer, fallEndX + cellWidth / 2f, fallEndY + cellHeight / 2f, cellWidth,
                cellHeight, spray);
    }

    private WaterRipple createWaterRipple() {
        RippleRings rings = new RippleRings(
                SPLASH_SECONDS, RIPPLE_STAGGER_SECONDS, RIPPLE_RING_COUNT, RIPPLE_RING_LIFE_SECONDS,
                RIPPLE_SPEED_CELLS_PER_SECOND, RIPPLE_HALF_WIDTH_CELLS);
        return new WaterRipple(
                waterLayer, splashWaterColumn(), splashWaterRow(), cellHeight / cellWidth,
                SCENE_INNER_WIDTH / 2f + RIPPLE_COVER_MARGIN_CELLS,
                SCENE_INNER_HEIGHT / 2f + RIPPLE_COVER_MARGIN_CELLS,
                rings, RIPPLE_MAX_SCALE_BUMP);
    }

    /**
     * The water cell-unit column under the @'s center at the splash; the stack must be posed at the
     * splash.
     */
    private float splashWaterColumn() {
        findWaterPixelUnder(splashStackX(), splashStackY());
        return waterPixel.x / cellWidth;
    }

    private float splashWaterRow() {
        findWaterPixelUnder(splashStackX(), splashStackY());
        return waterPixel.y / cellHeight;
    }

    private float splashStackX() {
        return scenePanel.getX() + fallEndX + cellWidth / 2f;
    }

    private float splashStackY() {
        return scenePanel.getY() + fallEndY + cellHeight / 2f;
    }

    private void applyLightning(float time) {
        lightningGroup.getColorEffects().setInvert(lightning.amountAt(time));
    }

    /** Center of the scene panel, measured from the panel's corner. */
    private static float innerCenterX(GlyphSource glyphs) {
        return SCENE_INNER_WIDTH / 2f * glyphs.getCellWidth();
    }

    private static float innerCenterY(GlyphSource glyphs) {
        return SCENE_INNER_HEIGHT / 2f * glyphs.getCellHeight();
    }

    private void applyDepth() {
        applyDepth(bridgeLayer, BRIDGE_DEPTH);
        applyDepth(waterLayer, WATER_DEPTH);
    }

    /**
     * A layer at or nearer than the camera's hidden distance has no valid zoom, so it is hidden
     * instead.
     */
    private void applyDepth(Layer layer, float depth) {
        boolean visible = camera.isVisible(depth);
        layer.setVisible(visible);
        if (!visible) {
            return;
        }
        float zoom = camera.zoomFor(depth);
        layer.setZoom(zoom);
        layer.setParallax(zoom);
        layer.getColorEffects().setAlpha(camera.alphaFor(depth));
    }

    private void drawWaterIfStepChanged(float time) {
        int step = WaterPattern.stepAt(time);
        if (step == drawnWaterStep) {
            return;
        }
        drawnWaterStep = step;
        for (int row = 0; row < WATER_ROWS; row++) {
            for (int column = 0; column < WATER_COLUMNS; column++) {
                waterGrid.set(column, row, WaterPattern.codePointAt(column, row, time),
                        WATER_GLYPH_COLOR, WATER_BACKGROUND_COLOR);
            }
        }
    }

    /**
     * The panel's corner is in main-grid pixels, and its size is in tall cells so it matches the
     * log grid.
     */
    private void addLogLayer(GlyphSource mainGlyphs, GlyphSource tallGlyphs) {
        GridDrawing.fillRectangle(logGrid, 0, 0, LOG_TEXT_WIDTH, LOG_ROWS, ' ', WHITE, CLEAR);
        Panel logPanel = layers.addPanel(
                LOG_INNER_COLUMN * mainGlyphs.getCellWidth(),
                LOG_INNER_ROW * mainGlyphs.getCellHeight(),
                LOG_TEXT_WIDTH * tallGlyphs.getCellWidth(),
                LOG_ROWS * tallGlyphs.getCellHeight());
        layers.add(logGrid, tallGlyphs).setPanel(logPanel);
    }

    private Layer addOverlay(GlyphSource glyphs) {
        TerminalGrid overlayGrid = new TerminalGrid(COLUMNS, ROWS);
        GridDrawing.fillRectangle(overlayGrid, 0, 0, COLUMNS, ROWS, ' ', WHITE, BLACK);
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
        writeLogRow(row, shown ? log.line(lineIndex).text() : "", visible,
                newest ? WHITE : LOG_NORMAL_COLOR);
    }

    private void writeLogRow(int row, String text, int visible, int color) {
        for (int column = 0; column < LOG_TEXT_WIDTH; column++) {
            int codePoint = column < visible ? text.charAt(column) : ' ';
            logGrid.set(column, row, codePoint, color, CLEAR);
        }
    }

    private void drawFrames() {
        GridDrawing.drawBox(grid, 0, 0, SCENE_FRAME_WIDTH, SCENE_FRAME_HEIGHT, FRAME_COLOR, BLACK);
        GridDrawing.drawBox(grid, STATS_LEFT, 0, STATS_WIDTH, SCENE_FRAME_HEIGHT,
                FRAME_COLOR, BLACK);
        GridDrawing.drawBox(grid, 0, LOG_TOP, COLUMNS, LOG_HEIGHT, FRAME_COLOR, BLACK);
    }

    private void drawStats() {
        GridDrawing.drawText(grid, STATS_TEXT_COLUMN, 1, "@", GOLD, BLACK);
        GridDrawing.drawText(grid, STATS_TEXT_COLUMN + 2, 1, "Wanderer", WHITE, BLACK);
        GridDrawing.drawText(grid, STATS_TEXT_COLUMN, HP_BAR_ROW, "HP ", LABEL_COLOR, BLACK);
        drawHpBar(1f);
        drawStatLine(5, "DEPTH ", "3");
        drawStatLine(6, "GOLD  ", "42");
        drawStatLine(7, "TURN  ", "118");
    }

    private void drawStatLine(int row, String label, String value) {
        GridDrawing.drawText(grid, STATS_TEXT_COLUMN, row, label, LABEL_COLOR, BLACK);
        GridDrawing.drawText(grid, STATS_TEXT_COLUMN + label.length(), row, value, WHITE, BLACK);
    }
}
