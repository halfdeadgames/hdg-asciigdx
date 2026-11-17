package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.fit.IntegerScaling;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.CameraFraming;
import com.github.halfdeadgames.asciigdx.layer.FrameAlignment;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import com.github.halfdeadgames.asciigdx.layer.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.layer.MapWindow;
import com.github.halfdeadgames.asciigdx.sandbox.pond.ActorLayerFiller;
import com.github.halfdeadgames.asciigdx.sandbox.pond.AutoMotion;
import com.github.halfdeadgames.asciigdx.sandbox.pond.AutoWalk;
import com.github.halfdeadgames.asciigdx.sandbox.pond.Lily;
import com.github.halfdeadgames.asciigdx.sandbox.pond.LilyFlashes;
import com.github.halfdeadgames.asciigdx.sandbox.pond.LilyLayerFiller;
import com.github.halfdeadgames.asciigdx.sandbox.pond.LilyPusher;
import com.github.halfdeadgames.asciigdx.sandbox.pond.ManualMotion;
import com.github.halfdeadgames.asciigdx.sandbox.pond.MovingLilies;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PlayerMotion;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PointerAim;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondCell;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondGenerator;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondMap;
import com.github.halfdeadgames.asciigdx.sandbox.pond.PondPlayer;
import com.github.halfdeadgames.asciigdx.sandbox.pond.WakeRippleEffect;
import com.github.halfdeadgames.asciigdx.sandbox.pond.WakeRipples;
import com.github.halfdeadgames.asciigdx.sandbox.pond.WaterLayerFiller;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.zone.EffectZone;

/**
 * A small roguelike over map-window layers: a player crosses an 80x44 pond while the camera
 * follows. What moves the player is a {@link PlayerMotion}: a fixed route or the keys and touch.
 * With the route the scene is a pure function of elapsed time (steps also send wake rings across
 * the water), so the {@code pondTime} setting can hold it at one moment for captures.
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
    private static final int NO_PLAYER_YET = Integer.MIN_VALUE;

    /**
     * Builds the motion once the map exists; the pointer source is for motions that follow touch.
     */
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
    private final LilyFlashes lilyFlashes;
    private final int cellCount;
    private final boolean clockPinned;
    private final WakeRipples wakeRipples = new WakeRipples();
    private final RectangleShape wakeArea =
            RectangleShape.forSquareCells(0, 0, 1, 1, ShapeFill.FILLED);
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

    private final Vector2 pointerWorld = new Vector2();
    private final GridPoint2 pointerMapCell = new GridPoint2();

    private float elapsed;
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
    private int shownPointerColumn = NO_PLAYER_YET;
    private int shownPointerRow = NO_PLAYER_YET;
    private String statusLine = "";

    /**
     * The scene where the player walks a fixed route; the pond time setting holds it at one moment.
     */
    static PondScene autoWalk(GlyphSource glyphs, GlyphSource smallGlyphs, String pondTimeSetting) {
        return new PondScene("pond-auto", "Pond: map window, auto walk", glyphs, smallGlyphs,
                pondTimeSetting,
                (map, pointerAim) -> new AutoMotion(START_COLUMN, START_ROW));
    }

    /** The scene where the arrow keys or touch move the player. */
    static PondScene keyWalk(GlyphSource glyphs, GlyphSource smallGlyphs) {
        return new PondScene("pond-keys", "Pond: map window, arrow keys", glyphs, smallGlyphs, null,
                (map, pointerAim) -> new ManualMotion(new PondPlayer(START_COLUMN, START_ROW), map,
                        pointerAim));
    }

    /** @param pondTimeSetting seconds to hold the scene at, or null or invalid to play normally */
    private PondScene(String id, String title, GlyphSource glyphs, GlyphSource smallGlyphs,
            String pondTimeSetting,
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
        lilyFlashes = new LilyFlashes(map, lilyLayer);
        wakeZone = new EffectZone(wakeArea, new WakeRippleEffect(map, wakeRipples));
        cellCount = waterGrid.getColumns() * waterGrid.getRows()
                + lilyGrid.getColumns() * lilyGrid.getRows()
                + actorGrid.getColumns() * actorGrid.getRows();
        clockPinned = pinClockIfRequested(pondTimeSetting);
        if (clockPinned) {
            replayStepsUpToPinnedTime();
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
    public boolean mouseMoved(int screenX, int screenY) {
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
        lilyFlashes.clear();
        map.clearVisited();
        lilyLayer.getMapWindow().markAllNeedFill();
        motion.update(elapsed);
        lastStepColumn = motion.getColumn();
        lastStepRow = motion.getRow();
        actorLayer.getMapWindow().clearSlides();
    }

    /**
     * Visits each route step from time 0 up to the pinned time, so a pinned scene holds the same
     * lily events, wake rings, and flashes a live run would have at that time. Every effect of a
     * step starts at the step's own start time, so one visit per step is enough; the first frame
     * then updates the motion to the pinned time itself.
     */
    private void replayStepsUpToPinnedTime() {
        lastStepColumn = START_COLUMN;
        lastStepRow = START_ROW;
        int lastStep = (int) (elapsed / AutoWalk.STEP_SECONDS);
        for (int step = 0; step <= lastStep; step++) {
            float stepSeconds = step * AutoWalk.STEP_SECONDS;
            motion.update(stepSeconds);
            trackPlayerStep();
            movingLilies.advanceTo(stepSeconds);
        }
    }

    /**
     * Starts the glyph slide, the lily push, a wake ring, and a lily flash for a one-cell step, all
     * at the step's start time; a bigger jump, such as after a restart, clears all slides and only
     * resyncs.
     */
    private void trackPlayerStep() {
        int deltaColumn = motion.getColumn() - lastStepColumn;
        int deltaRow = motion.getRow() - lastStepRow;
        if (deltaColumn == 0 && deltaRow == 0) {
            return;
        }
        markVisited(motion.getColumn(), motion.getRow());
        if (Math.abs(deltaColumn) + Math.abs(deltaRow) != 1) {
            actorLayer.getMapWindow().clearSlides();
        } else {
            float stepSeconds = motion.getStepStartSeconds();
            actorLayer.getMapWindow().startSlide(lastStepColumn, lastStepRow,
                    motion.getColumn(), motion.getRow(), stepSeconds,
                    AutoWalk.STEP_SECONDS, motion.getStepEasing());
            lilyPusher.onStepStart(lastStepColumn, lastStepRow, deltaColumn, deltaRow,
                    stepSeconds);
            wakeRipples.start(motion.getColumn(), motion.getRow(), stepSeconds);
            lilyFlashes.start(motion.getColumn(), motion.getRow(), stepSeconds);
        }
        lastStepColumn = motion.getColumn();
        lastStepRow = motion.getRow();
    }

    /**
     * Remembers the visit in the game cell and flags its lilies for the next fill, which draws
     * them paler.
     */
    private void markVisited(int gameColumn, int gameRow) {
        PondCell gameCell = map.cell(gameColumn, gameRow);
        if (gameCell.isVisited()) {
            return;
        }
        gameCell.markVisited();
        lilyLayer.getMapWindow().markAreaNeedsFill(gameColumn * LILIES_PER_GAME_CELL_AXIS,
                gameRow * LILIES_PER_GAME_CELL_AXIS, LILIES_PER_GAME_CELL_AXIS,
                LILIES_PER_GAME_CELL_AXIS);
    }

    /** The ring lift is a pure function of time; handles were reset at the start of the frame. */
    private void applyWakeRipples() {
        if (!wakeRipples.isActiveAt(elapsed)) {
            return;
        }
        int left = wakeRipples.activeLeft(elapsed);
        int top = wakeRipples.activeTop(elapsed);
        wakeArea.setPosition(left, top);
        wakeArea.setSize(wakeRipples.activeRight(elapsed) - left + 1,
                wakeRipples.activeBottom(elapsed) - top + 1);
        wakeZone.apply(waterLayer, elapsed);
    }

    private void updateWorld(float lilyDelta) {
        layers.beginCellEffects(lilyDelta);
        motion.update(elapsed);
        trackPlayerStep();
        movingLilies.advanceTo(elapsed);
        layers.setCamera(cameraX(), cameraY());
        eraseStalePlayer();
        fillExposedCells();
        applyWakeRipples();
        applyLilyOffsets();
        lilyFlashes.apply(elapsed);
        drawPlayer();
        actorLayer.getMapWindow().applySlides(elapsed);
        showStatus();
    }

    /** Returns the drawn player center X in map pixels, including any slide in progress. */
    private float playerMapCenterX() {
        return (motion.getColumn() + 0.5f) * gameCellWidth + actorLayer.getMapWindow()
                .getSlideOffsetX(motion.getColumn(), motion.getRow(), elapsed);
    }

    private float playerMapCenterY() {
        return (motion.getRow() + 0.5f) * gameCellHeight + actorLayer.getMapWindow()
                .getSlideOffsetY(motion.getColumn(), motion.getRow(), elapsed);
    }

    private float cameraX() {
        return CameraFraming.centerOn(playerMapCenterX(), WINDOW_COLUMNS * gameCellWidth,
                WORLD_COLUMNS * gameCellWidth, FrameAlignment.START);
    }

    private float cameraY() {
        return CameraFraming.centerOn(playerMapCenterY(), WINDOW_ROWS * gameCellHeight,
                WORLD_ROWS * gameCellHeight, FrameAlignment.START);
    }

    private float playerCenterX() {
        return playerMapCenterX() - layers.getCameraX();
    }

    private float playerCenterY() {
        return playerMapCenterY() - layers.getCameraY();
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
        MapWindow lilyWindow = lilyLayer.getMapWindow();
        for (int i = 0; i < movingLilies.size(); i++) {
            int column = movingLilies.columnAt(i);
            int row = movingLilies.rowAt(i);
            if (!lilyWindow.contains(column, row)) {
                continue;
            }
            Lily lily = movingLilies.lilyAt(i);
            lilyWindow.obtainFrameCellHandle(column, row)
                    .setOffset(lily.getOffsetX() * lilyCellWidth,
                            lily.getOffsetY() * lilyCellHeight);
        }
    }

    private static void fillLayer(Layer layer, MapCellVisitor filler) {
        layer.getMapWindow().forEachCellToFill(filler);
        layer.getMapWindow().markAllFilled();
    }

    /**
     * Runs after the fill pass, which blanks cells scrolled into view and could erase the player.
     */
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
        findPointerMapCell();
        if (column == shownColumn && row == shownRow && cameraX == shownCameraX
                && cameraY == shownCameraY && pointerMapCell.x == shownPointerColumn
                && pointerMapCell.y == shownPointerRow) {
            return;
        }
        shownColumn = column;
        shownRow = row;
        shownCameraX = cameraX;
        shownCameraY = cameraY;
        shownPointerColumn = pointerMapCell.x;
        shownPointerRow = pointerMapCell.y;
        String suffix = showsCamera ? "camera " + cameraX + "," + cameraY : helpText;
        statusLine = "@" + column + "," + row + " ptr " + pointerMapCell.x + ","
                + pointerMapCell.y + "  " + suffix;
    }

    private void findPointerMapCell() {
        pointerWorld.set(pointerScreenX, pointerScreenY);
        viewport.unproject(pointerWorld);
        layers.toMapCell(waterLayer, 0, viewport.getWorldHeight(), pointerWorld.x, pointerWorld.y,
                pointerMapCell);
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
