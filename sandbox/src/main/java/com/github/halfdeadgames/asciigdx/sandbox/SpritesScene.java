package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.render.GlyphSprite;
import com.github.halfdeadgames.asciigdx.render.Layer;

/**
 * Off-grid sprites on one map layer: an orbiting ring, a slowly spinning scaled marker, a mirrored
 * glyph, a half-transparent glyph over bright cells, and a stream of short-lived sprites that
 * keeps adding and removing so leaks show up in the mesh count. Monster glyphs in the map grid
 * bump and wiggle through lifted cells while their backgrounds stay put.
 *
 * <p>Keys: G toggles gray. F flashes white. P toggles pixel snapping. A toggles layer alpha.
 * R resets every lifted cell; bumps resume on their next cycle. M slides the layer left and right,
 * and the sprites and lifted cells move with it. K shrinks the map grid by ten columns, so
 * monsters there lose their handles; they get fresh ones when the grid grows back.
 *
 * <p>The word at the bottom runs the {@link OverrideDemo} cycle: flat and lifted letters must
 * look identical in every phase.
 */
class SpritesScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 22;

    private static final int RING_SIZE = 8;
    private static final float RING_RADIUS_ROWS = 6f;
    private static final float RING_DEGREES_PER_SECOND = 45f;
    private static final float SPIN_DEGREES_PER_SECOND = 30f;

    private static final int CHURN_CAPACITY = 40;
    private static final float CHURN_SPAWN_PERIOD = 0.05f;
    private static final float CHURN_RISE_ROWS_PER_SECOND = 6f;
    private static final int CHURN_COLUMN_STRIDE = 7;
    private static final int CHURN_COLUMN_SPAN = 36;

    private static final int SHRINK_COLUMNS = 10;
    private static final float SLIDE_CELLS = 4f;
    private static final float SLIDE_RADIANS_PER_SECOND = 1.2f;

    private static final float FLASH_DURATION = 0.4f;
    private static final float FLASH_STRENGTH = 0.8f;
    private static final float HALF_ALPHA = 0.5f;
    private static final float CAPPED_DELTA = 0.1f;

    private static final int FLOOR_A = 0x1C2A1CFF;
    private static final int FLOOR_B = 0x142014FF;
    private static final int FLOOR_GLYPH = 0x4C7A4CFF;
    private static final int STRIPE_BACKGROUND = 0xE0E0E0FF;
    private static final int STRIPE_GLYPH = 0x303030FF;
    private static final int[] RING_COLORS = {
        0xFF4040FF, 0xFF9040FF, 0xFFE040FF, 0x60FF60FF, 0x40FFD0FF, 0x4090FFFF, 0xA060FFFF, 0xFF60C0FF
    };
    private static final int[] CHURN_COLORS = {0xFFD700FF, 0x00FFFFFF, 0xFF80FFFF, 0x90FF90FF};

    private static final int STRIPE_FIRST_COLUMN = 26;
    private static final int STRIPE_LAST_COLUMN = 32;
    private static final int STRIPE_FIRST_ROW = 15;
    private static final int STRIPE_LAST_ROW = 18;

    private final Layer mapLayer;
    private final float cellWidth;
    private final float cellHeight;
    private final GlyphSprite[] ringSprites = new GlyphSprite[RING_SIZE];
    private final GlyphSprite[] churnSprites = new GlyphSprite[CHURN_CAPACITY];
    private final StringBuilder status = new StringBuilder();
    private final LiftedMonster[] monsters = {
        new LiftedMonster(8, 10, 'g', 0xFF5050FF, 1, 0, false, 0f),
        new LiftedMonster(12, 5, 'o', 0x60C0FFFF, 0, 1, false, 0.4f),
        new LiftedMonster(20, 18, 's', 0xFFA040FF, -1, 0, false, 0.8f),
        new LiftedMonster(16, 12, 'k', 0xFFFF60FF, 0, 0, true, 0.6f),
        new LiftedMonster(34, 8, 'D', 0xFF40FFFF, -1, 0, false, 0.2f),
        new LiftedMonster(36, 13, 'r', 0xC0FF60FF, 0, -1, false, 1f)
    };
    private final OverrideDemo overrideDemo;
    private GlyphSprite spinner;
    private float monsterClock;
    private float slideClock;
    private boolean sliding;
    private boolean shrunk;
    private int churnOldest;
    private int churnCount;
    private int churnSpawned;
    private float churnClock;
    private float ringAngle;
    private float flashRemaining;
    private boolean gray;
    private boolean halfAlpha;

    private final boolean startsFaded;

    /**
     * @param startsFaded opens at layer alpha 0.5, so captures measure the faded drawing path
     */
    SpritesScene(GlyphSource glyphs, boolean startsFaded) {
        super(glyphs, COLUMNS, ROWS);
        this.startsFaded = startsFaded;
        cellWidth = glyphs.getCellWidth();
        cellHeight = glyphs.getCellHeight();
        layers.setSize(COLUMNS * cellWidth, ROWS * cellHeight);
        mapLayer = layers.get(0);
        drawMap();
        placeMonsters();
        overrideDemo = new OverrideDemo(grid, mapLayer);
        createRing();
        createFixedSprites();
        updateRing();
        if (startsFaded) {
            toggleHalfAlpha();
        }
    }

    @Override
    public String id() {
        return startsFaded ? "sprites-fade" : "sprites";
    }

    @Override
    public String title() {
        return startsFaded ? "Sprites: faded layer" : "Sprites: off-grid glyphs";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public String statusText() {
        status.setLength(0);
        // Labeled by key (G gray, P snap, A alpha, M slide, K shrink): the overlay is 40 columns wide.
        status.append("G:").append(gray ? "on" : "off")
            .append(" P:").append(layers.isPixelSnapping() ? "on" : "off")
            .append(" A:").append(halfAlpha ? "0.5" : "1")
            .append(" M:").append(sliding ? "on" : "off")
            .append(" K:").append(shrunk ? "on" : "off");
        return status.toString();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.G) {
            toggleGray();
            return true;
        }
        if (keycode == Input.Keys.F) {
            startFlash();
            return true;
        }
        if (keycode == Input.Keys.P) {
            toggleSnapping();
            return true;
        }
        if (keycode == Input.Keys.A) {
            toggleHalfAlpha();
            return true;
        }
        if (keycode == Input.Keys.R) {
            resetLiftedCells();
            return true;
        }
        if (keycode == Input.Keys.M) {
            toggleSlide();
            return true;
        }
        if (keycode == Input.Keys.K) {
            toggleShrink();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        int third = Gdx.graphics.getWidth() / 3;
        if (screenX < third) {
            toggleGray();
        } else if (screenX < third * 2) {
            startFlash();
        } else {
            toggleSnapping();
        }
        return true;
    }

    @Override
    protected void update(float delta) {
        float step = Math.min(delta, CAPPED_DELTA);
        ringAngle += RING_DEGREES_PER_SECOND * step;
        updateRing();
        spinner.rotateBy(SPIN_DEGREES_PER_SECOND * step);
        updateChurn(step);
        updateFlash(step);
        updateMonsters(step);
        updateSlide(step);
        overrideDemo.update(step);
    }

    private void updateMonsters(float step) {
        monsterClock += step;
        for (int i = 0; i < monsters.length; i++) {
            monsters[i].update(mapLayer, grid.getWidth(), grid.getHeight(), monsterClock, cellWidth, cellHeight);
        }
    }

    private void updateSlide(float step) {
        if (!sliding) {
            return;
        }
        slideClock += step;
        mapLayer.setOffset(MathUtils.sin(slideClock * SLIDE_RADIANS_PER_SECOND) * SLIDE_CELLS * cellWidth, 0f);
    }

    private void resetLiftedCells() {
        mapLayer.resetCellHandles();
        for (int i = 0; i < monsters.length; i++) {
            monsters[i].suspendUntilNextCycle();
        }
    }

    private void toggleSlide() {
        sliding = !sliding;
        if (!sliding) {
            mapLayer.setOffset(0f, 0f);
        }
    }

    /** Resizing allocates, which is fine for a key press. The grown grid starts empty, so the map is redrawn. */
    private void toggleShrink() {
        shrunk = !shrunk;
        grid.resize(shrunk ? COLUMNS - SHRINK_COLUMNS : COLUMNS, ROWS);
        drawMap();
        placeMonsters();
        overrideDemo.draw();
    }

    private void placeMonsters() {
        for (int i = 0; i < monsters.length; i++) {
            LiftedMonster monster = monsters[i];
            monster.placeIn(grid, floorBackground(monster.getColumn(), monster.getRow()));
        }
    }

    private void toggleGray() {
        gray = !gray;
        mapLayer.setSaturation(gray ? 0f : 1f);
    }

    private void startFlash() {
        flashRemaining = FLASH_DURATION;
    }

    private void toggleSnapping() {
        layers.setPixelSnapping(!layers.isPixelSnapping());
    }

    private void toggleHalfAlpha() {
        halfAlpha = !halfAlpha;
        mapLayer.setAlpha(halfAlpha ? HALF_ALPHA : 1f);
    }

    private void updateFlash(float step) {
        if (flashRemaining <= 0f) {
            return;
        }
        flashRemaining = Math.max(0f, flashRemaining - step);
        mapLayer.setMix(1f, 1f, 1f, FLASH_STRENGTH * flashRemaining / FLASH_DURATION);
    }

    private void createRing() {
        for (int i = 0; i < RING_SIZE; i++) {
            ringSprites[i] = mapLayer.addSprite('O', RING_COLORS[i], 0f, 0f);
        }
    }

    private void updateRing() {
        float centerX = COLUMNS * cellWidth * 0.5f - cellWidth * 0.5f;
        float centerY = ROWS * cellHeight * 0.5f - cellHeight * 0.5f;
        float radius = RING_RADIUS_ROWS * cellHeight;
        for (int i = 0; i < RING_SIZE; i++) {
            float angle = ringAngle + i * (360f / RING_SIZE);
            ringSprites[i].setPosition(centerX + radius * MathUtils.cosDeg(angle),
                centerY - radius * MathUtils.sinDeg(angle));
        }
    }

    private void createFixedSprites() {
        spinner = mapLayer.addSprite('@', WHITE, 5f * cellWidth, 3f * cellHeight);
        spinner.setScale(2f);

        GlyphSprite mirrored = mapLayer.addSprite('P', GOLD, 33f * cellWidth, 4f * cellHeight);
        mirrored.setScale(-1f, 1f);

        GlyphSprite translucent = mapLayer.addSprite('@', 0xFF2020FF, 28f * cellWidth, 16f * cellHeight);
        translucent.setAlpha(HALF_ALPHA);
    }

    private void updateChurn(float step) {
        churnClock += step;
        while (churnClock >= CHURN_SPAWN_PERIOD) {
            churnClock -= CHURN_SPAWN_PERIOD;
            spawnChurnSprite();
        }
        float rise = CHURN_RISE_ROWS_PER_SECOND * cellHeight * step;
        for (int i = 0; i < churnCount; i++) {
            churnSprites[(churnOldest + i) % CHURN_CAPACITY].moveBy(0f, -rise);
        }
    }

    private void spawnChurnSprite() {
        if (churnCount == CHURN_CAPACITY) {
            mapLayer.removeSprite(churnSprites[churnOldest]);
            churnOldest = (churnOldest + 1) % CHURN_CAPACITY;
            churnCount--;
        }
        int column = 2 + churnSpawned * CHURN_COLUMN_STRIDE % CHURN_COLUMN_SPAN;
        int color = CHURN_COLORS[churnSpawned % CHURN_COLORS.length];
        GlyphSprite sprite = mapLayer.addSprite('*', color, column * cellWidth, (ROWS - 1) * cellHeight);
        churnSprites[(churnOldest + churnCount) % CHURN_CAPACITY] = sprite;
        churnCount++;
        churnSpawned++;
    }

    private void drawMap() {
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int column = 0; column < grid.getWidth(); column++) {
                if (isStripe(column, row)) {
                    grid.set(column, row, '=', STRIPE_GLYPH, STRIPE_BACKGROUND);
                    continue;
                }
                boolean even = (column + row) % 2 == 0;
                grid.set(column, row, even ? '.' : ',', FLOOR_GLYPH, floorBackground(column, row));
            }
        }
    }

    private static int floorBackground(int column, int row) {
        return (column + row) % 2 == 0 ? FLOOR_A : FLOOR_B;
    }

    private static boolean isStripe(int column, int row) {
        return column >= STRIPE_FIRST_COLUMN && column <= STRIPE_LAST_COLUMN
            && row >= STRIPE_FIRST_ROW && row <= STRIPE_LAST_ROW;
    }
}
