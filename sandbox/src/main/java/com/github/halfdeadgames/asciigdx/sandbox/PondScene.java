package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.IntegerScaling;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.render.LayerStack;
import com.github.halfdeadgames.asciigdx.render.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.render.MapWindow;
import com.github.halfdeadgames.asciigdx.sandbox.pond.ActorLayerFiller;
import com.github.halfdeadgames.asciigdx.sandbox.pond.AutoMotion;
import com.github.halfdeadgames.asciigdx.sandbox.pond.LilyLayerFiller;
import com.github.halfdeadgames.asciigdx.sandbox.pond.Lily;
import com.github.halfdeadgames.asciigdx.sandbox.pond.LilyPusher;
import com.github.halfdeadgames.asciigdx.sandbox.pond.ManualMotion;
import com.github.halfdeadgames.asciigdx.sandbox.pond.MovingLilies;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PlayerMotion;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondCamera;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondGenerator;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondMap;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondPlayer;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PointerAim;
import com.github.halfdeadgames.asciigdx.sandbox.pond.WakeRippleEffect;
import com.github.halfdeadgames.asciigdx.sandbox.pond.WakeRipples;
import com.github.halfdeadgames.asciigdx.sandbox.pond.WaterLayerFiller;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.zone.EffectZone;

/**
 * A small roguelike over map-window layers: a player crosses an 80x44 pond while the camera follows.
 * What moves the player is a {@link PlayerMotion}: a fixed route or the keys and touch. With the route
 * the scene is a pure function of elapsed time (steps also send wake rings across the water), so the
 * {@code pondTime} setting can hold it at one moment for captures.
 */
class PondScene implements SandboxScene {
    static final String KEY_POND_TIME = "pondTime";

    private static final long SEED = 20240611L;
    private static final int WORLD_COLUMNS = 80;
    private static final int WORLD_ROWS = 44;
    private static final int WINDOW_COLUMNS = 40;
    private static final int WINDOW_ROWS = 22;
    private static final int START_COLUMN = 40;
    private static final int START_ROW = 22;
    private static final int LILIES_PER_GAME_CELL_AXIS = 2;
    private static final float REPLAY_FRAMES_PER_SECOND = 60f;
    private static final int NO_PLAYER_YET = Integer.MIN_VALUE;

    /** Builds the motion once the map exists; the pointer source is for motions that follow touch. */
    private interface MotionFactory {
        PlayerMotion create(PondMap map, PointerAim pointerAim);
    }

    private final String id;
    private final String title;
    private final PlayerMotion motion;
    private final ScalingViewport viewport;
    private final LayerStack layers = new LayerStack();
    private final Layer waterLayer;
    private final Layer lilyLayer;
    private final Layer actorLayer;
    private final WaterLayerFiller waterFiller;
    private final LilyLayerFiller lilyFiller;
    private final ActorLayerFiller actorFiller;
    private final float gameCellWidth;
    private final float gameCellHeight;
    private final float lilyCellWidth;
    private final float lilyCellHeight;
    private final PondMap map;
    private final MovingLilies movingLilies = new MovingLilies();
    private final LilyPusher lilyPusher;
    private final int cellCount;
    private final boolean clockPinned;
    private final WakeRipples wakeRipples = new WakeRipples();
    private final RectangleShape wakeArea = new RectangleShape(0, 0, 1, 1, ShapeFill.FILLED);
    private final EffectZone wakeZone;
    private final PointerAim pointerAim = new PointerAim() {
        @Override
        public boolean isDown() {
            return pointerPressedInScene && Gdx.input.isTouched();
        }

        @Override
        public void aimInto(Vector2 offset) {
            offset.set(pointerScreenX, pointerScreenY);
            viewport.unproject(offset);
            offset.y = viewport.getWorldHeight() - offset.y;
            offset.sub(playerCenterX(), playerCenterY());
        }
    };

    private float elapsed;
    private boolean waterLifted;
    private int pointerScreenX;
    private int pointerScreenY;
    private boolean pointerPressedInScene;
    private int lastStepColumn = START_COLUMN;
    private int lastStepRow = START_ROW;
    private int drawnPlayerColumn = NO_PLAYER_YET;
    private int drawnPlayerRow = NO_PLAYER_YET;
    private int shownColumn = NO_PLAYER_YET;
    private int shownRow = NO_PLAYER_YET;
    private int shownCameraX = NO_PLAYER_YET;
    private int shownCameraY = NO_PLAYER_YET;
    private String statusLine = "";

    /** The scene where the player walks a fixed route; the pond time setting holds it at one moment. */
    static PondScene autoWalk(GlyphSource glyphs, GlyphSource smallGlyphs, String pondTimeSetting) {
        return new PondScene("pond-auto", "Pond: map window, auto walk", glyphs, smallGlyphs, pondTimeSetting,
            (map, pointerAim) -> new AutoMotion(START_COLUMN, START_ROW));
    }

    /** The scene where the arrow keys or touch move the player. */
    static PondScene keyWalk(GlyphSource glyphs, GlyphSource smallGlyphs) {
        return new PondScene("pond-keys", "Pond: map window, arrow keys", glyphs, smallGlyphs, null,
            (map, pointerAim) -> new ManualMotion(new PondPlayer(START_COLUMN, START_ROW), map, pointerAim));
    }

    /** @param pondTimeSetting seconds to hold the scene at, or null or invalid to play normally */
    private PondScene(String id, String title, GlyphSource glyphs, GlyphSource smallGlyphs, String pondTimeSetting,
                      MotionFactory motionFactory) {
        this.id = id;
        this.title = title;
        gameCellWidth = glyphs.getCellWidth();
        gameCellHeight = glyphs.getCellHeight();
        float windowWidth = WINDOW_COLUMNS * gameCellWidth;
        float windowHeight = WINDOW_ROWS * gameCellHeight;
        viewport = new ScalingViewport(IntegerScaling.INSTANCE, windowWidth, windowHeight);
        layers.setSize(windowWidth, windowHeight);

        lilyCellWidth = smallGlyphs.getCellWidth();
        lilyCellHeight = smallGlyphs.getCellHeight();
        map = PondGenerator.generate(WORLD_COLUMNS, WORLD_ROWS, SEED, START_COLUMN, START_ROW);
        lilyPusher = new LilyPusher(map, movingLilies);
        motion = motionFactory.create(map, pointerAim);
        TerminalGrid waterGrid = new TerminalGrid(WINDOW_COLUMNS + 1, WINDOW_ROWS + 1);
        TerminalGrid lilyGrid = new TerminalGrid((WINDOW_COLUMNS + 1) * LILIES_PER_GAME_CELL_AXIS,
            (WINDOW_ROWS + 1) * LILIES_PER_GAME_CELL_AXIS);
        TerminalGrid actorGrid = new TerminalGrid(WINDOW_COLUMNS + 1, WINDOW_ROWS + 1);
        waterLayer = addMapWindowLayer(waterGrid, glyphs);
        lilyLayer = addMapWindowLayer(lilyGrid, smallGlyphs);
        actorLayer = addMapWindowLayer(actorGrid, glyphs);
        waterFiller = new WaterLayerFiller(map, waterLayer.getMapWindow());
        lilyFiller = new LilyLayerFiller(map, lilyLayer.getMapWindow());
        actorFiller = new ActorLayerFiller(actorLayer.getMapWindow());
        wakeZone = new EffectZone(wakeArea, new WakeRippleEffect(map, wakeRipples));
        cellCount = waterGrid.getWidth() * waterGrid.getHeight() + lilyGrid.getWidth() * lilyGrid.getHeight()
            + actorGrid.getWidth() * actorGrid.getHeight();
        clockPinned = pinClockIfRequested(pondTimeSetting);
        if (clockPinned) {
            replayLiliesUpToPinnedTime();
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public boolean recordsAnimation() {
        return motion.movesWithoutInput();
    }

    @Override
    public void show() {
        pointerPressedInScene = false;
        if (!clockPinned) {
            elapsed = 0f;
            motion.clockRestarted();
            restartLilies();
        }
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(SpriteBatch batch, float delta) {
        advanceClock(delta);
        updateWorld(clockPinned ? 0f : delta);
        SceneDrawing.clearViewportArea(viewport);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layers.draw(batch, 0, viewport.getWorldHeight());
        batch.end();
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        layers.dispose();
    }

    @Override
    public int cellCount() {
        return cellCount;
    }

    @Override
    public String statusText() {
        return statusLine;
    }

    @Override
    public boolean hasLiveStatus() {
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        return motion.keyDown(keycode);
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        pointerPressedInScene = true;
        rememberPointer(screenX, screenY);
        return motion.pointerDown();
    }

    @Override
    public boolean touchDragged(int screenX, int screenY) {
        rememberPointer(screenX, screenY);
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY) {
        pointerPressedInScene = false;
        return false;
    }

    private void rememberPointer(int screenX, int screenY) {
        pointerScreenX = screenX;
        pointerScreenY = screenY;
    }

    private Layer addMapWindowLayer(TerminalGrid mapGrid, GlyphSource source) {
        Layer layer = layers.add(mapGrid, source);
        layer.enableMapWindow();
        layer.clipToStack();
        return layer;
    }

    private void advanceClock(float delta) {
        if (!clockPinned) {
            elapsed += delta;
        }
    }

    private void restartLilies() {
        movingLilies.resetAll();
        wakeRipples.clear();
        lilyLayer.resetCellHandles();
        lilyLayer.releaseIdleCellHandles();
        settleWater();
        motion.update(elapsed);
        lastStepColumn = motion.getColumn();
        lastStepRow = motion.getRow();
    }

    /**
     * Plays the lily logic from time 0 at 60 steps per second so a pinned scene shows the lilies as
     * they would be at that time. The route is a pure function of time, so afterward the motion
     * simply jumps to the pinned time.
     */
    private void replayLiliesUpToPinnedTime() {
        lastStepColumn = START_COLUMN;
        lastStepRow = START_ROW;
        int frames = (int) (elapsed * REPLAY_FRAMES_PER_SECOND);
        for (int frame = 0; frame < frames; frame++) {
            float frameSeconds = frame / REPLAY_FRAMES_PER_SECOND;
            motion.update(frameSeconds);
            trackPlayerStep(frameSeconds);
            movingLilies.advance(1f / REPLAY_FRAMES_PER_SECOND);
        }
        motion.update(elapsed);
        trackPlayerStep(elapsed);
    }

    /**
     * Starts the lily push and a wake ring for a one-cell step; a bigger jump, such as after a restart,
     * only resyncs.
     *
     * @param stepSeconds scene clock time at which the step happened
     */
    private void trackPlayerStep(float stepSeconds) {
        int deltaColumn = motion.getColumn() - lastStepColumn;
        int deltaRow = motion.getRow() - lastStepRow;
        if (deltaColumn == 0 && deltaRow == 0) {
            return;
        }
        if (Math.abs(deltaColumn) + Math.abs(deltaRow) == 1) {
            lilyPusher.onStepStart(lastStepColumn, lastStepRow, deltaColumn, deltaRow);
            wakeRipples.start(motion.getColumn(), motion.getRow(), stepSeconds);
        }
        lastStepColumn = motion.getColumn();
        lastStepRow = motion.getRow();
    }

    /** Runs after the water fill, which does not touch handles; the ring lift is a pure function of time. */
    private void applyWakeRipples() {
        if (wakeRipples.isActiveAt(elapsed)) {
            liftWaterUnderRings();
            return;
        }
        settleWater();
    }

    private void liftWaterUnderRings() {
        waterLayer.resetCellHandles();
        int left = wakeRipples.activeLeft(elapsed);
        int top = wakeRipples.activeTop(elapsed);
        wakeArea.setPosition(left, top);
        wakeArea.setSize(wakeRipples.activeRight(elapsed) - left + 1, wakeRipples.activeBottom(elapsed) - top + 1);
        wakeZone.apply(waterLayer, elapsed);
        waterLifted = true;
    }

    private void settleWater() {
        if (!waterLifted) {
            return;
        }
        waterLayer.resetCellHandles();
        waterLayer.releaseIdleCellHandles();
        waterLifted = false;
    }

    private void updateWorld(float lilyDelta) {
        motion.update(elapsed);
        trackPlayerStep(elapsed);
        movingLilies.advance(lilyDelta);
        float remaining = motion.getSlideRemaining();
        boolean cellChanged = motion.getColumn() != drawnPlayerColumn || motion.getRow() != drawnPlayerRow;
        layers.setCamera(cameraX(remaining), cameraY(remaining));
        eraseStalePlayer();
        fillExposedCells();
        applyWakeRipples();
        applyLilyOffsets();
        drawPlayer();
        slidePlayerGlyph(remaining, cellChanged);
        showStatus();
    }

    /** The player's position in cells, trailing the logical cell by the part of the step still to slide. */
    private float fractionalColumn(float remaining) {
        return motion.getColumn() - motion.getStepColumn() * remaining;
    }

    private float fractionalRow(float remaining) {
        return motion.getRow() - motion.getStepRow() * remaining;
    }

    private float cameraX(float remaining) {
        return PondCamera.cameraFor(fractionalColumn(remaining), gameCellWidth, WINDOW_COLUMNS, WORLD_COLUMNS);
    }

    private float cameraY(float remaining) {
        return PondCamera.cameraFor(fractionalRow(remaining), gameCellHeight, WINDOW_ROWS, WORLD_ROWS);
    }

    private float playerCenterX() {
        return (fractionalColumn(motion.getSlideRemaining()) + 0.5f) * gameCellWidth - layers.getCameraX();
    }

    private float playerCenterY() {
        return (fractionalRow(motion.getSlideRemaining()) + 0.5f) * gameCellHeight - layers.getCameraY();
    }

    private void slidePlayerGlyph(float remaining, boolean cellChanged) {
        actorLayer.resetCellHandles();
        if (cellChanged) {
            actorLayer.releaseIdleCellHandles();
        }
        MapWindow actorWindow = actorLayer.getMapWindow();
        if (!actorWindow.contains(motion.getColumn(), motion.getRow())) {
            return;
        }
        actorWindow.cell(motion.getColumn(), motion.getRow()).setOffset(
            -motion.getStepColumn() * gameCellWidth * remaining,
            -motion.getStepRow() * gameCellHeight * remaining);
    }

    private void eraseStalePlayer() {
        if (drawnPlayerColumn == NO_PLAYER_YET) {
            return;
        }
        if (drawnPlayerColumn == motion.getColumn() && drawnPlayerRow == motion.getRow()) {
            return;
        }
        actorFiller.blank(drawnPlayerColumn, drawnPlayerRow);
    }

    private void fillExposedCells() {
        fillLayer(waterLayer, waterFiller);
        fillLayer(lilyLayer, lilyFiller);
        fillLayer(actorLayer, actorFiller);
    }

    private void applyLilyOffsets() {
        lilyLayer.resetCellHandles();
        MapWindow lilyWindow = lilyLayer.getMapWindow();
        for (int i = 0; i < movingLilies.size(); i++) {
            int column = movingLilies.columnAt(i);
            int row = movingLilies.rowAt(i);
            if (!lilyWindow.contains(column, row)) {
                continue;
            }
            Lily lily = movingLilies.lilyAt(i);
            lilyWindow.cell(column, row).setOffset(lily.getOffsetX() * lilyCellWidth,
                lily.getOffsetY() * lilyCellHeight);
        }
        if (movingLilies.settledInLastAdvance()) {
            lilyLayer.releaseIdleCellHandles();
        }
    }

    private static void fillLayer(Layer layer, MapCellVisitor filler) {
        layer.getMapWindow().forEachCellToFill(filler);
        layer.getMapWindow().markAllFilled();
    }

    /** Runs after the fill pass, which blanks cells scrolled into view and could erase the player. */
    private void drawPlayer() {
        drawnPlayerColumn = motion.getColumn();
        drawnPlayerRow = motion.getRow();
        actorFiller.drawPlayer(drawnPlayerColumn, drawnPlayerRow);
    }

    private void showStatus() {
        String helpText = motion.helpText();
        boolean showsCamera = helpText.isEmpty();
        int column = motion.getColumn();
        int row = motion.getRow();
        int cameraX = showsCamera ? Math.round(layers.getCameraX()) : 0;
        int cameraY = showsCamera ? Math.round(layers.getCameraY()) : 0;
        if (column == shownColumn && row == shownRow && cameraX == shownCameraX && cameraY == shownCameraY) {
            return;
        }
        shownColumn = column;
        shownRow = row;
        shownCameraX = cameraX;
        shownCameraY = cameraY;
        String suffix = showsCamera ? "camera " + cameraX + "," + cameraY : helpText;
        statusLine = "player " + column + "," + row + "  " + suffix;
    }

    private boolean pinClockIfRequested(String pondTimeSetting) {
        if (pondTimeSetting == null) {
            return false;
        }
        try {
            float seconds = Float.parseFloat(pondTimeSetting.trim());
            if (!Float.isFinite(seconds) || seconds < 0f) {
                return false;
            }
            elapsed = seconds;
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
