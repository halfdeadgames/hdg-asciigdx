package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.ZoomMode;

/**
 * A dungeon map that zooms and rotates about the hero inside a clipped window, under a frame and
 * help row that stay put. Runs a looping zoom-and-spin tour on its own until a manual key or tap
 * takes over. Where the map is visible, the cell under the mouse or last touch is highlighted.
 */
class LayerTransformScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 22;
    private static final int FRAME_THICKNESS = 2;
    private static final int HERO_COLUMN = 20;
    private static final int HERO_ROW = 11;

    private static final float ZOOM_STEP = 0.25f;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 8f;
    private static final float TURN_STEP_DEGREES = -15f;
    private static final float FULL_TURN_DEGREES = 360f;

    private static final float CYCLE_SECONDS = 16f;
    private static final float[] KEY_TIMES = {0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 16f};
    private static final float[] KEY_ZOOMS = {1f, 1f, 1.5f, 1.5f, 2f, 2f, 3f, 3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f};
    private static final float[] KEY_ROTATIONS = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, -90f, -90f, 0f, -360f};

    static final String KEY_HOVER = "hover";

    private static final String HELP_TEXT = "+- zoom QE turn W mode A auto 0 reset";

    private static final int WALL = 0x8C8C9CFF;
    private static final int WALL_BACKGROUND = 0x2A2A36FF;
    private static final int FLOOR = 0x5A5A4AFF;
    private static final int FLOOR_BACKGROUND = 0x14140FFF;
    private static final int FRAME_OUTER = 0x4A3414FF;
    private static final int FRAME_LINE = 0xC09040FF;
    private static final int HERO = 0xFFFFFFFF;
    private static final int POTION = 0xFF60FFFF;
    private static final int SCROLL = 0xE0C890FF;
    private static final int DRAGON = 0xFF2020FF;
    private static final int HIGHLIGHT = 0x3060C0FF;

    private final float cellWidth;
    private final float cellHeight;
    private final Layer mapLayer;
    private final StringBuilder status = new StringBuilder();
    private final float[] hoverFractions;
    private final Vector2 pointerWorld = new Vector2();
    private final GridPoint2 hoveredCell = new GridPoint2();
    private float elapsed;
    private boolean auto = true;
    private boolean hasRealPointer;
    private int pointerScreenX;
    private int pointerScreenY;
    private int highlightColumn = -1;
    private int highlightRow = -1;
    private int highlightOriginalBackground;

    /** @param hoverSetting fake pointer as "fx,fy" window fractions from the top-left, or null for none */
    LayerTransformScene(GlyphSource glyphs, String hoverSetting) {
        super(glyphs, COLUMNS, ROWS);
        hoverFractions = parseHoverFractions(hoverSetting);
        cellWidth = glyphs.getCellWidth();
        cellHeight = glyphs.getCellHeight();
        layers.setSize(COLUMNS * cellWidth, ROWS * cellHeight);

        mapLayer = layers.getLayer(0);
        drawMap();
        mapLayer.setPivotCell(HERO_COLUMN, HERO_ROW);
        mapLayer.setClip(FRAME_THICKNESS * cellWidth, FRAME_THICKNESS * cellHeight,
            (COLUMNS - 2 * FRAME_THICKNESS) * cellWidth, (ROWS - 2 * FRAME_THICKNESS) * cellHeight);

        TerminalGrid frameGrid = new TerminalGrid(COLUMNS, ROWS);
        drawFrame(frameGrid);
        layers.add(frameGrid, glyphs);

        applyAutoKeyframe();
    }

    @Override
    public String id() {
        return "layer-transform";
    }

    @Override
    public String title() {
        return "Layers: zoom and rotation";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public boolean hasLiveStatus() {
        return true;
    }

    @Override
    public void show() {
        elapsed = 0f;
        auto = true;
        hasRealPointer = false;
        clearHighlight();
        super.show();
        applyAutoKeyframe();
    }

    @Override
    public void hide() {
        clearHighlight();
        super.hide();
    }

    @Override
    public String statusText() {
        status.setLength(0);
        status.append('z');
        appendTwoDecimals(mapLayer.getZoom());
        status.append('>');
        appendZoom(mapLayer.getEffectiveZoom());
        status.append(' ').append(Math.round(wrapDegrees(mapLayer.getRotation()))).append('°')
            .append(' ').append(mapLayer.getZoomMode() == ZoomMode.SMOOTH ? 'S' : 'W');
        if (auto) {
            status.append(" auto");
        }
        return status.toString();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        storeRealPointer(screenX, screenY);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY) {
        storeRealPointer(screenX, screenY);
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.PLUS, Input.Keys.EQUALS -> zoomBy(ZOOM_STEP);
            case Input.Keys.MINUS -> zoomBy(-ZOOM_STEP);
            case Input.Keys.Q -> turnBy(TURN_STEP_DEGREES);
            case Input.Keys.E -> turnBy(-TURN_STEP_DEGREES);
            case Input.Keys.W -> toggleZoomMode();
            case Input.Keys.A -> auto = !auto;
            case Input.Keys.NUM_0 -> reset();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        storeRealPointer(screenX, screenY);
        int fifth = Gdx.graphics.getWidth() / 5;
        if (screenX < fifth) {
            zoomBy(-ZOOM_STEP);
        } else if (screenX < fifth * 2) {
            zoomBy(ZOOM_STEP);
        } else if (screenX < fifth * 3) {
            turnBy(TURN_STEP_DEGREES);
        } else if (screenX < fifth * 4) {
            toggleZoomMode();
        } else {
            auto = !auto;
        }
        return true;
    }

    @Override
    protected void update(float delta) {
        elapsed += delta;
        if (auto) {
            applyAutoKeyframe();
        }
        updateHighlight();
    }

    private void storeRealPointer(int screenX, int screenY) {
        hasRealPointer = true;
        pointerScreenX = screenX;
        pointerScreenY = screenY;
    }

    private static float[] parseHoverFractions(String setting) {
        if (setting == null || setting.isEmpty()) {
            return null;
        }
        String[] parts = setting.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new float[] {Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim())};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean hasPointer() {
        return hasRealPointer || hoverFractions != null;
    }

    private void updateHighlight() {
        if (!hasPointer()) {
            clearHighlight();
            return;
        }
        if (hasRealPointer) {
            pointerWorld.set(pointerScreenX, pointerScreenY);
        } else {
            pointerWorld.set(hoverFractions[0] * Gdx.graphics.getWidth(), hoverFractions[1] * Gdx.graphics.getHeight());
        }
        viewport.unproject(pointerWorld);
        float stackY = viewport.getWorldHeight();
        if (!layers.isPickableAt(mapLayer, 0f, stackY, pointerWorld.x, pointerWorld.y)) {
            clearHighlight();
            return;
        }
        layers.toCell(mapLayer, 0f, stackY, pointerWorld.x, pointerWorld.y, hoveredCell);
        if (!grid.contains(hoveredCell.x, hoveredCell.y)) {
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
        highlightOriginalBackground = grid.getBackground(column, row);
        grid.setBackground(column, row, HIGHLIGHT);
    }

    private void clearHighlight() {
        if (highlightColumn < 0) {
            return;
        }
        grid.setBackground(highlightColumn, highlightRow, highlightOriginalBackground);
        highlightColumn = -1;
        highlightRow = -1;
    }

    private void zoomBy(float step) {
        auto = false;
        mapLayer.setZoom(MathUtils.clamp(mapLayer.getZoom() + step, MIN_ZOOM, MAX_ZOOM));
    }

    private void turnBy(float degrees) {
        auto = false;
        mapLayer.setRotation(wrapDegrees(mapLayer.getRotation() + degrees));
    }

    /** Switching the mode leaves auto alone, so the two modes can be compared mid-tour. */
    private void toggleZoomMode() {
        mapLayer.setZoomMode(mapLayer.getZoomMode() == ZoomMode.SMOOTH ? ZoomMode.WHOLE : ZoomMode.SMOOTH);
    }

    private void reset() {
        auto = false;
        mapLayer.setZoom(1f);
        mapLayer.setRotation(0f);
    }

    private void applyAutoKeyframe() {
        float cycleTime = elapsed % CYCLE_SECONDS;
        int next = 1;
        while (next < KEY_TIMES.length - 1 && cycleTime >= KEY_TIMES[next]) {
            next++;
        }
        int previous = next - 1;
        float progress = (cycleTime - KEY_TIMES[previous]) / (KEY_TIMES[next] - KEY_TIMES[previous]);
        float eased = Interpolation.smooth.apply(progress);
        mapLayer.setZoom(MathUtils.lerp(KEY_ZOOMS[previous], KEY_ZOOMS[next], eased));
        mapLayer.setRotation(MathUtils.lerp(KEY_ROTATIONS[previous], KEY_ROTATIONS[next], eased));
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % FULL_TURN_DEGREES;
        return wrapped < 0f ? wrapped + FULL_TURN_DEGREES : wrapped;
    }

    private void appendTwoDecimals(float value) {
        int hundredths = Math.round(value * 100f);
        int fraction = hundredths % 100;
        status.append(hundredths / 100).append('.');
        if (fraction < 10) {
            status.append('0');
        }
        status.append(fraction);
    }

    private void appendZoom(float value) {
        if (value == (int) value) {
            status.append((int) value);
            return;
        }
        appendTwoDecimals(value);
    }

    private void drawMap() {
        GridDrawing.fillRectangle(grid, 0, 0, COLUMNS, ROWS, '#', WALL, WALL_BACKGROUND);
        GridDrawing.fillRectangle(grid, 3, 3, COLUMNS - 6, ROWS - 6, '.', FLOOR, FLOOR_BACKGROUND);
        drawVerticalWall(13, 10, 11);
        drawVerticalWall(27, 6, 7);
        drawVerticalWall(27, 15, 15);
        for (int column = 28; column <= 36; column++) {
            if (column != 33) {
                grid.set(column, 11, '#', WALL, WALL_BACKGROUND);
            }
        }
        grid.set(6, 5, '$', GOLD, FLOOR_BACKGROUND);
        grid.set(9, 15, '!', POTION, FLOOR_BACKGROUND);
        grid.set(17, 6, '?', SCROLL, FLOOR_BACKGROUND);
        grid.set(22, 16, '$', GOLD, FLOOR_BACKGROUND);
        grid.set(34, 5, '!', POTION, FLOOR_BACKGROUND);
        grid.set(32, 14, 'D', DRAGON, FLOOR_BACKGROUND);
        grid.set(HERO_COLUMN, HERO_ROW, '@', HERO, FLOOR_BACKGROUND);
    }

    /** Draws a wall down the column across the floor rows, leaving a doorway from firstGap to lastGap. */
    private void drawVerticalWall(int column, int firstGap, int lastGap) {
        for (int row = 3; row < ROWS - 3; row++) {
            if (row < firstGap || row > lastGap) {
                grid.set(column, row, '#', WALL, WALL_BACKGROUND);
            }
        }
    }

    private static void drawFrame(TerminalGrid frame) {
        GridDrawing.fillRectangle(frame, 0, 0, COLUMNS, ROWS, ' ', WHITE, FRAME_OUTER);
        GridDrawing.fillRectangle(frame, FRAME_THICKNESS, FRAME_THICKNESS,
            COLUMNS - 2 * FRAME_THICKNESS, ROWS - 2 * FRAME_THICKNESS, ' ', WHITE, CLEAR);
        GridDrawing.drawBox(frame, 1, 1, COLUMNS - 2, ROWS - 2, FRAME_LINE, FRAME_OUTER);
        GridDrawing.drawText(frame, (COLUMNS - HELP_TEXT.length()) / 2, ROWS - 1, HELP_TEXT, GOLD, FRAME_OUTER);
    }
}
