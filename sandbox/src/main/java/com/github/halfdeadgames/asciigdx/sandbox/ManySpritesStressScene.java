package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.GlyphSprite;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import java.util.Optional;

/**
 * Moves thousands of free glyph sprites on one layer every frame, so the sprite mesh is rebuilt
 * with its full load. Each sprite circles its own anchor on a lattice that covers the whole grid.
 */
class ManySpritesStressScene extends GridScene {
    private static final int COLUMNS = 240;
    private static final int ROWS = 135;
    private static final long SEED = 20240613L;

    private static final int LATTICE_COLUMNS = 160;
    private static final int LATTICE_ROWS = 100;
    private static final int SPRITE_COUNT = LATTICE_COLUMNS * LATTICE_ROWS;

    private static final float ORBIT_RADIUS_PIXELS = 8f;
    private static final float ORBIT_RADIANS_PER_SECOND = 1.6f;
    private static final float PHASE_PER_SPRITE = 2.399963f;
    private static final float HUE_DEGREES_PER_SPRITE = 137.5f;
    private static final float SPRITE_SATURATION = 0.7f;
    private static final float SPRITE_VALUE = 1f;

    private final Layer layer;
    private final GlyphSprite[] sprites = new GlyphSprite[SPRITE_COUNT];
    private final float[] anchorsX = new float[SPRITE_COUNT];
    private final float[] anchorsY = new float[SPRITE_COUNT];
    private final float[] phases = new float[SPRITE_COUNT];
    private float elapsedSeconds;

    ManySpritesStressScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        float gridWidth = COLUMNS * glyphs.getCellWidth();
        float gridHeight = ROWS * glyphs.getCellHeight();
        layers.setSize(gridWidth, gridHeight);
        layer = layers.getLayer(0);
        createSprites(glyphs, gridWidth, gridHeight);
        moveSprites();
    }

    @Override
    public String id() {
        return "many-sprites-stress";
    }

    @Override
    public String title() {
        return "Many sprites: " + SPRITE_COUNT + " free glyphs move every frame";
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
        return SPRITE_COUNT + " sprites";
    }

    @Override
    protected void update(float delta) {
        elapsedSeconds += delta;
        layers.beginCellEffects(delta);
        moveSprites();
    }

    private void moveSprites() {
        float angle = ORBIT_RADIANS_PER_SECOND * elapsedSeconds;
        for (int i = 0; i < SPRITE_COUNT; i++) {
            float spriteAngle = angle + phases[i];
            sprites[i].setPosition(
                    anchorsX[i] + ORBIT_RADIUS_PIXELS * MathUtils.cos(spriteAngle),
                    anchorsY[i] + ORBIT_RADIUS_PIXELS * MathUtils.sin(spriteAngle));
        }
    }

    /**
     * Anchors sit at least one orbit radius inside the grid edges, so a sprite never leaves the
     * visible area.
     */
    private void createSprites(GlyphSource glyphs, float gridWidth, float gridHeight) {
        float marginX = ORBIT_RADIUS_PIXELS;
        float marginY = ORBIT_RADIUS_PIXELS;
        float usableWidth = gridWidth - glyphs.getCellWidth() - 2f * marginX;
        float usableHeight = gridHeight - glyphs.getCellHeight() - 2f * marginY;
        RandomXS128 random = new RandomXS128(SEED);
        Color color = new Color();
        for (int i = 0; i < SPRITE_COUNT; i++) {
            int latticeColumn = i % LATTICE_COLUMNS;
            int latticeRow = i / LATTICE_COLUMNS;
            anchorsX[i] = marginX + usableWidth * latticeColumn / (LATTICE_COLUMNS - 1);
            anchorsY[i] = marginY + usableHeight * latticeRow / (LATTICE_ROWS - 1);
            phases[i] = PHASE_PER_SPRITE * i;
            color.fromHsv((HUE_DEGREES_PER_SPRITE * i) % 360f, SPRITE_SATURATION, SPRITE_VALUE);
            color.a = 1f;
            sprites[i] = layer.addSprite(SceneDrawing.randomGlyph(random), Color.rgba8888(color),
                    anchorsX[i], anchorsY[i]);
        }
    }
}
