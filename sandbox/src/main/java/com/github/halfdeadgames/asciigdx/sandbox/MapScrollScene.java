package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.MapCellVisitor;
import com.github.halfdeadgames.asciigdx.layer.MapWindow;
import java.util.Optional;

/**
 * Pans a camera over a map five screens wide and tall, refilling the cells that scroll into view
 * every frame, as when a player walks through a level.
 */
class MapScrollScene extends GridScene {
    private static final int VIEW_COLUMNS = 240;
    private static final int VIEW_ROWS = 135;
    private static final int MAP_COLUMNS = VIEW_COLUMNS * 5;
    private static final int MAP_ROWS = VIEW_ROWS * 5;
    // One extra column and row so the sub-cell remainder never exposes an edge.
    private static final int WINDOW_COLUMNS = VIEW_COLUMNS + 1;
    private static final int WINDOW_ROWS = VIEW_ROWS + 1;
    private static final long SEED = 20240611L;
    private static final int DARK_BACKGROUND_MASK = 0x3F3F3F00;
    private static final int OPAQUE_ALPHA = 0xFF;

    private static final float HORIZONTAL_PERIOD_SECONDS = 40f;
    private static final float VERTICAL_PERIOD_SECONDS = 27f;
    // Keeps the camera at least this many cells inside every map edge.
    private static final int EDGE_MARGIN_CELLS = 2;

    private final int[] mapGlyphs = new int[MAP_COLUMNS * MAP_ROWS];
    private final int[] mapForegrounds = new int[MAP_COLUMNS * MAP_ROWS];
    private final int[] mapBackgrounds = new int[MAP_COLUMNS * MAP_ROWS];
    private final float cameraCenterX;
    private final float cameraCenterY;
    private final float cameraAmplitudeX;
    private final float cameraAmplitudeY;
    private final MapWindow mapWindow;
    private final MapCellVisitor mapFiller = this::fillMapCell;
    private float elapsedSeconds;

    MapScrollScene(GlyphSource glyphs) {
        super(glyphs, VIEW_COLUMNS, VIEW_ROWS);
        generateMap();

        layers.remove(layers.getLayer(0));
        layers.setSize(VIEW_COLUMNS * glyphs.getCellWidth(), VIEW_ROWS * glyphs.getCellHeight());
        Layer windowLayer = layers.add(new TerminalGrid(WINDOW_COLUMNS, WINDOW_ROWS), glyphs);
        windowLayer.enableMapWindow();
        windowLayer.clipToStack();
        mapWindow = windowLayer.getMapWindow();

        float cameraRangeX = (MAP_COLUMNS - WINDOW_COLUMNS - 2 * EDGE_MARGIN_CELLS)
                * glyphs.getCellWidth();
        float cameraRangeY = (MAP_ROWS - WINDOW_ROWS - 2 * EDGE_MARGIN_CELLS)
                * glyphs.getCellHeight();
        cameraAmplitudeX = cameraRangeX / 2f;
        cameraAmplitudeY = cameraRangeY / 2f;
        cameraCenterX = EDGE_MARGIN_CELLS * glyphs.getCellWidth() + cameraAmplitudeX;
        cameraCenterY = EDGE_MARGIN_CELLS * glyphs.getCellHeight() + cameraAmplitudeY;
        moveCameraAndFill();
    }

    @Override
    public String id() {
        return "map-scroll";
    }

    @Override
    public String title() {
        return "Map scrolling: moving camera";
    }

    @Override
    public SceneGroup group() {
        return SceneGroup.BENCHMARKS;
    }

    @Override
    public Optional<WindowSize> preferredWindowSize() {
        return Optional.of(WindowSize.FULL_HD);
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public String statusText() {
        return MAP_COLUMNS + "x" + MAP_ROWS + " map";
    }

    @Override
    protected void update(float delta) {
        elapsedSeconds += delta;
        moveCameraAndFill();
    }

    private void moveCameraAndFill() {
        float horizontalPhase = MathUtils.PI2 * elapsedSeconds / HORIZONTAL_PERIOD_SECONDS;
        float verticalPhase = MathUtils.PI2 * elapsedSeconds / VERTICAL_PERIOD_SECONDS;
        layers.setCamera(cameraCenterX + cameraAmplitudeX * MathUtils.sin(horizontalPhase),
                cameraCenterY + cameraAmplitudeY * MathUtils.sin(verticalPhase));
        mapWindow.forEachCellToFill(mapFiller);
        mapWindow.markAllFilled();
    }

    private void fillMapCell(int mapColumn, int mapRow) {
        int index = mapRow * MAP_COLUMNS + mapColumn;
        mapWindow.set(mapColumn, mapRow, mapGlyphs[index], mapForegrounds[index],
                mapBackgrounds[index]);
    }

    private void generateMap() {
        RandomXS128 random = new RandomXS128(SEED);
        for (int index = 0; index < mapGlyphs.length; index++) {
            mapGlyphs[index] = SceneDrawing.randomGlyph(random);
            mapForegrounds[index] = SceneDrawing.randomOpaqueColor(random);
            mapBackgrounds[index] = (SceneDrawing.randomOpaqueColor(random) & DARK_BACKGROUND_MASK)
                    | OPAQUE_ALPHA;
        }
    }
}
