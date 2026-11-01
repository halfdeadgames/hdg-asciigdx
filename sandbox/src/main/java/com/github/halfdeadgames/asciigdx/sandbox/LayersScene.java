package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.Layer;

/**
 * A clipped, shaking map under a sliding popup, a hopping marker, fading rain that jumps by whole cells,
 * a drifting banner, and a frame that always stays on top.
 */
class LayersScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 22;
    private static final int FRAME_THICKNESS = 2;

    private static final int POPUP_COLUMNS = 18;
    private static final int POPUP_ROWS = 6;
    private static final int POPUP_REST_COLUMN = 19;
    private static final int POPUP_REST_ROW = 4;

    private static final int MAX_SHIFT = 6;
    private static final int AT_COLUMN = 33;
    private static final int AT_ROW = 8;

    private static final String BANNER_TEXT = "~ slow drift ~";
    private static final int BANNER_COLUMN = 19;
    private static final int BANNER_ROW = 16;
    private static final int BANNER_TRAVEL_CELLS = 3;
    private static final float BANNER_SPEED = 2f;

    private static final int BOSS_ROW = 13;
    private static final int BOSS_FIRST_COLUMN = 20;
    private static final int BOSS_SPAN = 4;

    private static final float POPUP_CYCLE = 7f;
    private static final float POPUP_SLIDE = 1.2f;
    private static final float POPUP_HOLD = 3f;
    private static final float FADE_PERIOD = 4f;
    private static final float TINT_PERIOD = 9f;
    private static final float BOSS_STEP_PERIOD = 0.8f;
    private static final float REACTION_DURATION = 0.35f;
    private static final float SHAKE_AMPLITUDE = 3.3f;

    private static final int WALL = 0x8C8C9CFF;
    private static final int WALL_BACKGROUND = 0x2A2A36FF;
    private static final int FLOOR = 0x5A5A4AFF;
    private static final int FLOOR_BACKGROUND = 0x14140FFF;
    private static final int HIDDEN_BACKGROUND = 0x3A1020FF;
    private static final int POPUP_BACKGROUND = 0x10204AFF;
    private static final int POPUP_FRAME = 0xA0C0FFFF;
    private static final int BANNER_BACKGROUND = 0x202030FF;
    private static final int FRAME_OUTER = 0x4A3414FF;
    private static final int FRAME_INNER = 0x2A1C0AFF;
    private static final int FRAME_LINE = 0xC09040FF;
    private static final int RED = 0xFF4040FF;
    private static final int BOSS_RED = 0xFF2020FF;
    private static final int SCROLL = 0xE0C890FF;

    private static final String LETTERS = "MPARBF";
    private static final int MAP = 0;
    private static final int POPUP = 1;
    private static final int AT = 2;
    private static final int RAIN = 3;
    private static final int BANNER = 4;
    private static final int FRAME = 5;

    private final GlyphSource glyphs;
    private final Layer[] layerById = new Layer[LETTERS.length()];
    private final TerminalGrid atGrid = new TerminalGrid(2, 1);
    private final RandomXS128 rainRandom = new RandomXS128();
    private final StringBuilder status = new StringBuilder();
    private float elapsed;
    private boolean rainVisible = true;
    private int bossColumn = -1;
    private int bossStep = -1;
    private boolean startled;
    private int rainCycle = -1;

    LayersScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        this.glyphs = glyphs;
        layers.setSize(COLUMNS * glyphs.getCellWidth(), ROWS * glyphs.getCellHeight());

        layerById[MAP] = layers.get(0);
        layerById[MAP].clipToStack();
        drawMap();

        TerminalGrid popupGrid = new TerminalGrid(POPUP_COLUMNS, POPUP_ROWS);
        drawPopup(popupGrid);
        layerById[POPUP] = layers.add(popupGrid, glyphs);
        layerById[POPUP].clipToStack();

        atGrid.set(0, 0, '@', WHITE, CLEAR);
        atGrid.set(1, 0, ' ', WHITE, CLEAR);
        layerById[AT] = layers.add(atGrid, glyphs);

        TerminalGrid rainGrid = new TerminalGrid(COLUMNS + MAX_SHIFT, ROWS + MAX_SHIFT);
        drawRain(rainGrid);
        layerById[RAIN] = layers.add(rainGrid, glyphs);
        layerById[RAIN].clipToStack();

        TerminalGrid bannerGrid = new TerminalGrid(BANNER_TEXT.length(), 1);
        writeText(bannerGrid, 0, 0, BANNER_TEXT, GOLD, BANNER_BACKGROUND);
        layerById[BANNER] = layers.add(bannerGrid, glyphs);
        layerById[BANNER].clipToStack();

        TerminalGrid frameGrid = new TerminalGrid(COLUMNS, ROWS);
        drawFrame(frameGrid);
        layerById[FRAME] = layers.add(frameGrid, glyphs);

        applyAnimation();
    }

    @Override
    public String id() {
        return "layers";
    }

    @Override
    public String title() {
        return "Layers: clip, shake, order";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public void show() {
        elapsed = 0f;
        rainCycle = -1;
        super.show();
        applyAnimation();
    }

    @Override
    public String statusText() {
        status.setLength(0);
        status.append("order ");
        for (int i = 0; i < layers.size(); i++) {
            status.append(LETTERS.charAt(idAt(i)));
        }
        status.append(" rain ").append(rainVisible ? "on" : "off")
            .append(" snap ").append(layers.isPixelSnapping() ? "on" : "off");
        return status.toString();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.R) {
            rotateOrder();
            return true;
        }
        if (keycode == Input.Keys.V) {
            toggleRain();
            return true;
        }
        if (keycode == Input.Keys.P) {
            layers.setPixelSnapping(!layers.isPixelSnapping());
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        int third = Gdx.graphics.getWidth() / 3;
        if (screenX < third) {
            rotateOrder();
        } else if (screenX < third * 2) {
            toggleRain();
        } else {
            layers.setPixelSnapping(!layers.isPixelSnapping());
        }
        return true;
    }

    @Override
    protected void update(float delta) {
        elapsed += delta;
        applyAnimation();
    }

    private int idAt(int stackIndex) {
        Layer layer = layers.get(stackIndex);
        for (int i = 0; i < layerById.length; i++) {
            if (layerById[i] == layer) {
                return i;
            }
        }
        return MAP;
    }

    /** Cycles popup, marker, rain, and banner through the middle depths; the map stays bottom and the frame top. */
    private void rotateOrder() {
        layers.get(1).moveTo(layers.size() - 2);
    }

    private void toggleRain() {
        rainVisible = !rainVisible;
        layerById[RAIN].setVisible(rainVisible);
    }

    private void applyAnimation() {
        int step = (int) (elapsed / BOSS_STEP_PERIOD);
        float reaction = (elapsed - step * BOSS_STEP_PERIOD) / REACTION_DURATION;
        if (step == 0 || reaction >= 1f) {
            reaction = -1f;
        }
        moveBoss(step);
        applyMapShake(reaction);
        applyHop(reaction);
        applyPopup();
        applyRain();
        applyBanner();
    }

    private void moveBoss(int step) {
        if (step == bossStep) {
            return;
        }
        bossStep = step;
        if (bossColumn >= 0) {
            grid.set(bossColumn, BOSS_ROW, '.', FLOOR, FLOOR_BACKGROUND);
        }
        int phase = step % (2 * (BOSS_SPAN - 1));
        bossColumn = BOSS_FIRST_COLUMN + (phase < BOSS_SPAN ? phase : 2 * (BOSS_SPAN - 1) - phase);
        grid.set(bossColumn, BOSS_ROW, 'D', BOSS_RED, FLOOR_BACKGROUND);
    }

    /** Reaction is progress through the post-step effect from 0 to 1, or negative when idle. */
    private void applyMapShake(float reaction) {
        float shakeX = 0f;
        float shakeY = 0f;
        if (reaction >= 0f) {
            float seconds = reaction * REACTION_DURATION;
            float falloff = 1f - reaction;
            shakeX = SHAKE_AMPLITUDE * falloff * MathUtils.sin(seconds * 70f);
            shakeY = SHAKE_AMPLITUDE * 0.6f * falloff * MathUtils.sin(seconds * 53f + 1f);
        }
        layerById[MAP].setOffset(shakeX, shakeY);
    }

    private void applyHop(float reaction) {
        boolean hopping = reaction >= 0f;
        if (hopping != startled) {
            startled = hopping;
            if (hopping) {
                atGrid.set(1, 0, '!', RED, CLEAR);
            } else {
                atGrid.set(1, 0, ' ', WHITE, CLEAR);
            }
        }
        float lift = hopping ? 4f * reaction * (1f - reaction) * glyphs.getCellHeight() : 0f;
        layerById[AT].setOffset(AT_COLUMN * glyphs.getCellWidth(), AT_ROW * glyphs.getCellHeight() - lift);
    }

    private void applyPopup() {
        float cycleTime = elapsed % POPUP_CYCLE;
        float restX = POPUP_REST_COLUMN * glyphs.getCellWidth();
        float hiddenX = COLUMNS * glyphs.getCellWidth();
        float progress;
        if (cycleTime < POPUP_SLIDE) {
            progress = cycleTime / POPUP_SLIDE;
        } else if (cycleTime < POPUP_SLIDE + POPUP_HOLD) {
            progress = 1f;
        } else if (cycleTime < POPUP_SLIDE * 2f + POPUP_HOLD) {
            progress = 1f - (cycleTime - POPUP_SLIDE - POPUP_HOLD) / POPUP_SLIDE;
        } else {
            progress = 0f;
        }
        float eased = progress * progress * (3f - 2f * progress);
        layerById[POPUP].setOffset(MathUtils.lerp(hiddenX, restX, eased),
            POPUP_REST_ROW * glyphs.getCellHeight());
    }

    private void applyRain() {
        Layer rain = layerById[RAIN];
        int cycle = (int) (elapsed / FADE_PERIOD);
        if (cycle != rainCycle) {
            rainCycle = cycle;
            rainRandom.setSeed(cycle);
            int shiftColumns = rainRandom.nextInt(MAX_SHIFT + 1);
            int shiftRows = rainRandom.nextInt(MAX_SHIFT + 1);
            rain.setCellOffset(-shiftColumns, -shiftRows);
        }
        float mix = 0.5f - 0.5f * MathUtils.cos(MathUtils.PI2 * elapsed / TINT_PERIOD);
        float red = MathUtils.lerp(0.6f, 1f, mix);
        float green = MathUtils.lerp(0.8f, 0.7f, mix);
        float blue = MathUtils.lerp(1f, 0.4f, mix);
        rain.setTint(red, green, blue, 1f);
        rain.setAlpha(0.5f - 0.5f * MathUtils.cos(MathUtils.PI2 * elapsed / FADE_PERIOD));
    }

    private void applyBanner() {
        float travel = BANNER_TRAVEL_CELLS * glyphs.getCellWidth();
        float along = (elapsed * BANNER_SPEED) % (2f * travel);
        float drift = along < travel ? along : 2f * travel - along;
        layerById[BANNER].setOffset(BANNER_COLUMN * glyphs.getCellWidth() + drift,
            BANNER_ROW * glyphs.getCellHeight());
    }

    private void drawMap() {
        int last = FRAME_THICKNESS;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                boolean hidden = column < last || row < last || column >= COLUMNS - last || row >= ROWS - last;
                if (hidden) {
                    drawHiddenCell(column, row);
                    continue;
                }
                boolean border = row == last || row == ROWS - 1 - last
                    || column == last || column == COLUMNS - 1 - last;
                boolean divider = column == 16 && row != 8 && row != 9;
                boolean ledge = row == 11 && column > 16 && column != 30 && column != 31;
                if (border || divider || ledge) {
                    grid.set(column, row, '#', WALL, WALL_BACKGROUND);
                } else {
                    grid.set(column, row, '.', FLOOR, FLOOR_BACKGROUND);
                }
            }
        }
        grid.set(4, 5, '$', GOLD, FLOOR_BACKGROUND);
        grid.set(10, 7, '!', 0xFF60FFFF, FLOOR_BACKGROUND);
        grid.set(12, 16, '?', SCROLL, FLOOR_BACKGROUND);
        grid.set(30, 15, '$', GOLD, FLOOR_BACKGROUND);
        grid.set(35, 7, '+', 0xC08040FF, FLOOR_BACKGROUND);
        grid.set(25, 17, '%', 0x60E060FF, FLOOR_BACKGROUND);
    }

    /** Bright content in the cells the frame covers, so only a shake lets slivers of it peek out. */
    private void drawHiddenCell(int column, int row) {
        int hash = (column * 5 + row * 3 + column * row) % 7;
        if (hash == 0) {
            grid.set(column, row, '*', 0xFF80FFFF, HIDDEN_BACKGROUND);
        } else if (hash == 3) {
            grid.set(column, row, '$', GOLD, HIDDEN_BACKGROUND);
        } else if (hash == 5) {
            grid.set(column, row, '%', 0x60FF60FF, HIDDEN_BACKGROUND);
        } else {
            grid.set(column, row, '=', 0xFF8040FF, HIDDEN_BACKGROUND);
        }
    }

    private static void drawFrame(TerminalGrid frame) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int depth = Math.min(Math.min(column, row), Math.min(COLUMNS - 1 - column, ROWS - 1 - row));
                if (depth >= FRAME_THICKNESS) {
                    frame.set(column, row, ' ', WHITE, CLEAR);
                } else if (depth == 0) {
                    frame.set(column, row, ' ', WHITE, FRAME_OUTER);
                } else {
                    frame.set(column, row, frameGlyph(column, row), FRAME_LINE, FRAME_INNER);
                }
            }
        }
        writeText(frame, 2, ROWS - 1, "R order  V rain  P snap", GOLD, FRAME_OUTER);
    }

    private static int frameGlyph(int column, int row) {
        boolean left = column == 1;
        boolean right = column == COLUMNS - 2;
        boolean top = row == 1;
        boolean bottom = row == ROWS - 2;
        if (top && left) {
            return '┌';
        }
        if (top && right) {
            return '┐';
        }
        if (bottom && left) {
            return '└';
        }
        if (bottom && right) {
            return '┘';
        }
        return top || bottom ? '─' : '│';
    }

    private static void drawPopup(TerminalGrid popup) {
        for (int row = 0; row < POPUP_ROWS; row++) {
            for (int column = 0; column < POPUP_COLUMNS; column++) {
                popup.set(column, row, ' ', WHITE, POPUP_BACKGROUND);
            }
        }
        for (int column = 1; column < POPUP_COLUMNS - 1; column++) {
            popup.set(column, 0, '─', POPUP_FRAME, POPUP_BACKGROUND);
            popup.set(column, POPUP_ROWS - 1, '─', POPUP_FRAME, POPUP_BACKGROUND);
        }
        for (int row = 1; row < POPUP_ROWS - 1; row++) {
            popup.set(0, row, '│', POPUP_FRAME, POPUP_BACKGROUND);
            popup.set(POPUP_COLUMNS - 1, row, '│', POPUP_FRAME, POPUP_BACKGROUND);
        }
        popup.set(0, 0, '┌', POPUP_FRAME, POPUP_BACKGROUND);
        popup.set(POPUP_COLUMNS - 1, 0, '┐', POPUP_FRAME, POPUP_BACKGROUND);
        popup.set(0, POPUP_ROWS - 1, '└', POPUP_FRAME, POPUP_BACKGROUND);
        popup.set(POPUP_COLUMNS - 1, POPUP_ROWS - 1, '┘', POPUP_FRAME, POPUP_BACKGROUND);
        writeText(popup, 2, 1, "Inventory", GOLD, POPUP_BACKGROUND);
        writeText(popup, 2, 2, "$ Gold      x2", WHITE, POPUP_BACKGROUND);
        writeText(popup, 2, 3, "! Potion    x1", WHITE, POPUP_BACKGROUND);
        writeText(popup, 2, 4, "[Esc] close", 0x9090B0FF, POPUP_BACKGROUND);
    }

    private static void drawRain(TerminalGrid rain) {
        for (int row = 0; row < rain.getHeight(); row++) {
            for (int column = 0; column < rain.getWidth(); column++) {
                int hash = (column * 7 + row * 13 + column * row) % 11;
                if (hash == 0) {
                    rain.set(column, row, '/', 0xA0C8FFFF, CLEAR);
                } else if (hash == 5 && (column + row) % 2 == 0) {
                    rain.set(column, row, ':', 0xC0D8FFFF, CLEAR);
                }
            }
        }
    }
}
