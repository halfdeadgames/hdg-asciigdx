package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.Layer;

/**
 * Far mountains, hills, and ground scrolling at different speeds under a fixed sky and status line.
 */
class ParallaxScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 22;
    private static final int PAN_CELLS = 32;
    private static final float PAN_PERIOD = 12f;

    private static final float MOUNTAIN_PARALLAX = 0.25f;
    private static final float HILL_PARALLAX = 0.5f;
    private static final float GROUND_PARALLAX = 1f;

    private static final int MOUNTAIN_BASE_ROW = 13;
    private static final int HILL_BASE_ROW = 16;
    private static final int GROUND_ROW = 18;

    private static final int SKY = 0x080A18FF;
    private static final int STAR = 0x5A6080FF;
    private static final int MOUNTAIN = 0x3A4458FF;
    private static final int MOUNTAIN_PEAK = 0x7C88A0FF;
    private static final int HILL = 0x1E4A24FF;
    private static final int HILL_CREST = 0x3C8C48FF;
    private static final int GRASS = 0x58D060FF;
    private static final int GRASS_BACKGROUND = 0x1C5A24FF;
    private static final int DIRT = 0x8A6A40FF;
    private static final int DIRT_BACKGROUND = 0x3A2A16FF;
    private static final int PATH = 0xC8B078FF;
    private static final int TREE = 0x30B040FF;
    private static final int ROCK = 0xA0A0A0FF;
    private static final int STATUS_BACKGROUND = 0x000000FF;

    private static final float MANUAL_SPEED_CELLS_PER_SECOND = 8f;
    private static final String HELP_TEXT = "<- -> pan A auto";
    private static final int HIGHLIGHT = 0x3060C0FF;

    private final GlyphSource glyphs;
    private final TerminalGrid statusGrid = new TerminalGrid(COLUMNS, 1);
    private final TerminalGrid groundGrid = wideGrid(GROUND_PARALLAX);
    private final Layer groundLayer;
    private final Vector2 pointerWorld = new Vector2();
    private final GridPoint2 hoveredCell = new GridPoint2();
    private final StringBuilder statusLine = new StringBuilder();
    private float elapsed;
    private float cameraX;
    private boolean auto = true;
    private boolean headingRight = true;
    private boolean hasPointer;
    private int pointerScreenX;
    private int pointerScreenY;
    private int highlightColumn = -1;
    private int highlightRow = -1;
    private int highlightOriginalBackground;
    private int shownCameraX = Integer.MIN_VALUE;
    private boolean shownAuto;

    ParallaxScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        this.glyphs = glyphs;
        layers.setSize(COLUMNS * glyphs.getCellWidth(), ROWS * glyphs.getCellHeight());

        drawSky();
        layers.getLayer(0).setParallax(0f);
        layers.getLayer(0).clipToStack();

        addScrolling(wideGrid(MOUNTAIN_PARALLAX), MOUNTAIN_PARALLAX);
        addScrolling(wideGrid(HILL_PARALLAX), HILL_PARALLAX);
        groundLayer = addScrolling(groundGrid, GROUND_PARALLAX);

        Layer status = layers.add(statusGrid, glyphs);
        status.setParallax(0f);
        status.clipToStack();

        applyAutoCamera();
        applyCamera();
    }

    @Override
    public String id() {
        return "parallax";
    }

    @Override
    public String title() {
        return "Parallax: camera and depth";
    }

    @Override
    public SceneGroup group() {
        return SceneGroup.GRIDS_LAYERS_AND_SPRITES;
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public void show() {
        elapsed = 0f;
        auto = true;
        headingRight = true;
        hasPointer = false;
        clearHighlight();
        super.show();
        applyAutoCamera();
        applyCamera();
    }

    @Override
    public void hide() {
        clearHighlight();
        super.hide();
    }

    @Override
    public String statusText() {
        return auto ? "auto pan" : "manual pan";
    }

    @Override
    public boolean hasLiveStatus() {
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        storePointer(screenX, screenY);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY) {
        storePointer(screenX, screenY);
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        storePointer(screenX, screenY);
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode != Input.Keys.A) {
            return false;
        }
        toggleAuto();
        return true;
    }

    @Override
    protected void update(float delta) {
        int direction = heldPanDirection();
        if (direction != 0) {
            panByHand(direction, delta);
        } else if (auto) {
            elapsed += delta;
            applyAutoCamera();
        }
        applyCamera();
        updateHighlight();
    }

    private float panPixels() {
        return PAN_CELLS * glyphs.getCellWidth();
    }

    private static int heldPanDirection() {
        int direction = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            direction++;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            direction--;
        }
        return direction;
    }

    private void panByHand(int direction, float delta) {
        auto = false;
        headingRight = direction > 0;
        float step = direction * MANUAL_SPEED_CELLS_PER_SECOND * glyphs.getCellWidth() * delta;
        cameraX = MathUtils.clamp(cameraX + step, 0f, panPixels());
    }

    /** Resumes the ping-pong from the current x, in the direction the camera was heading. */
    private void toggleAuto() {
        auto = !auto;
        if (!auto) {
            return;
        }
        float fraction = cameraX / panPixels();
        float phase = headingRight ? fraction / 2f : 1f - fraction / 2f;
        elapsed = (phase - 0.25f) * PAN_PERIOD;
    }

    private void applyAutoCamera() {
        float cycles = elapsed / PAN_PERIOD + 0.25f;
        float phase = cycles - MathUtils.floor(cycles);
        headingRight = phase < 0.5f;
        cameraX = panPixels() * pingPong(phase);
    }

    private void applyCamera() {
        layers.setCamera(cameraX, 0f);
        showStatus(Math.round(layers.getCameraX()), auto);
    }

    /**
     * Constant speed from 0 up to 1 and back over each whole cycle, so a capture always shows
     * motion.
     */
    private static float pingPong(float phase) {
        return 1f - Math.abs(2f * phase - 1f);
    }

    private void showStatus(int cameraXPixels, boolean isAuto) {
        if (cameraXPixels == shownCameraX && isAuto == shownAuto) {
            return;
        }
        shownCameraX = cameraXPixels;
        shownAuto = isAuto;
        statusLine.setLength(0);
        statusLine.append("camera x ").append(cameraXPixels).append(isAuto ? " auto" : " manual");
        GridDrawing.fillRectangle(statusGrid, 0, 0, COLUMNS, 1, ' ', WHITE, STATUS_BACKGROUND);
        GridDrawing.drawText(statusGrid, 1, 0, statusLine, GOLD, STATUS_BACKGROUND);
        GridDrawing.drawText(statusGrid, COLUMNS - HELP_TEXT.length() - 1, 0, HELP_TEXT, GRASS,
                STATUS_BACKGROUND);
    }

    private void storePointer(int screenX, int screenY) {
        hasPointer = true;
        pointerScreenX = screenX;
        pointerScreenY = screenY;
    }

    /** Recomputed every frame, since the camera moves under a still pointer. */
    private void updateHighlight() {
        if (!hasPointer) {
            return;
        }
        pointerWorld.set(pointerScreenX, pointerScreenY);
        viewport.unproject(pointerWorld);
        float stackY = viewport.getWorldHeight();
        if (!layers.isPickableAt(groundLayer, 0f, stackY, pointerWorld.x, pointerWorld.y)) {
            clearHighlight();
            return;
        }
        layers.toCell(groundLayer, 0f, stackY, pointerWorld.x, pointerWorld.y, hoveredCell);
        if (!groundGrid.contains(hoveredCell.x, hoveredCell.y)) {
            clearHighlight();
            return;
        }
        moveHighlightTo(hoveredCell.x, hoveredCell.y);
    }

    private void moveHighlightTo(int column, int row) {
        if (column == highlightColumn && row == highlightRow) {
            return;
        }
        clearHighlight();
        highlightColumn = column;
        highlightRow = row;
        highlightOriginalBackground = groundGrid.getBackground(column, row);
        groundGrid.setBackground(column, row, HIGHLIGHT);
    }

    private void clearHighlight() {
        if (highlightColumn < 0) {
            return;
        }
        groundGrid.setBackground(highlightColumn, highlightRow, highlightOriginalBackground);
        highlightColumn = -1;
        highlightRow = -1;
    }

    /** Wide enough that the camera's full pan never shows an empty edge for this factor. */
    private static TerminalGrid wideGrid(float parallax) {
        TerminalGrid wide = new TerminalGrid(COLUMNS + (int) Math.ceil(PAN_CELLS * parallax), ROWS);
        GridDrawing.fillRectangle(wide, 0, 0, wide.getColumns(), ROWS, ' ', WHITE, CLEAR);
        return wide;
    }

    private Layer addScrolling(TerminalGrid scrollingGrid, float parallax) {
        if (parallax == MOUNTAIN_PARALLAX) {
            drawMountains(scrollingGrid);
        } else if (parallax == HILL_PARALLAX) {
            drawHills(scrollingGrid);
        } else {
            drawGround(scrollingGrid);
        }
        Layer layer = layers.add(scrollingGrid, glyphs);
        layer.setParallax(parallax);
        layer.clipToStack();
        return layer;
    }

    private void drawSky() {
        GridDrawing.fillRectangle(grid, 0, 0, COLUMNS, ROWS, ' ', WHITE, SKY);
        for (int row = 0; row < MOUNTAIN_BASE_ROW - 2; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if ((column * 7 + row * 11 + column * row) % 23 == 0) {
                    grid.set(column, row, '.', STAR, SKY);
                }
            }
        }
    }

    private static void drawMountains(TerminalGrid target) {
        for (int column = 0; column < target.getColumns(); column++) {
            int height = 1 + 7 - Math.abs(column % 14 - 7);
            int top = MOUNTAIN_BASE_ROW - height;
            target.set(column, top, '▲', MOUNTAIN_PEAK, MOUNTAIN);
            GridDrawing.fillRectangle(target, column, top + 1, 1, height - 1, ' ', WHITE, MOUNTAIN);
        }
    }

    private static void drawHills(TerminalGrid target) {
        for (int column = 0; column < target.getColumns(); column++) {
            int top = HILL_BASE_ROW - Math.round(1.5f * MathUtils.sin(column * 0.35f));
            target.set(column, top, '~', HILL_CREST, HILL);
            GridDrawing.fillRectangle(target, column, top + 1, 1, ROWS - top - 1, ' ', WHITE, HILL);
        }
    }

    private static void drawGround(TerminalGrid target) {
        int width = target.getColumns();
        GridDrawing.fillRectangle(target, 0, GROUND_ROW, width, 1, '"', GRASS, GRASS_BACKGROUND);
        GridDrawing.fillRectangle(target, 0, GROUND_ROW + 1, width, ROWS - GROUND_ROW - 1, '.',
                DIRT, DIRT_BACKGROUND);
        GridDrawing.fillRectangle(target, 0, GROUND_ROW + 2, width, 1, '=', PATH, DIRT_BACKGROUND);
        for (int column = 3; column < width; column += 9) {
            target.set(column, GROUND_ROW - 1, '♣', TREE, CLEAR);
        }
        for (int column = 7; column < width; column += 13) {
            target.set(column, GROUND_ROW, 'o', ROCK, GRASS_BACKGROUND);
        }
    }
}
