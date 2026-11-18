package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.sandbox.zones.SpellEffectCycle;
import com.github.halfdeadgames.asciigdx.sandbox.zones.ZonesBackdrop;
import com.github.halfdeadgames.asciigdx.shape.CircleShape;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.tween.TweenClock;
import com.github.halfdeadgames.asciigdx.zone.CellEffects;
import com.github.halfdeadgames.asciigdx.zone.EffectZone;
import com.github.halfdeadgames.asciigdx.zone.Phase;

/**
 * Effect zones over a map-like floor: a filled circle "area spell" that moves, a caption with a
 * wave running along it, a permanent outline circle drawn through the same shape code, and an
 * optional full-screen zone for a performance check.
 *
 * <p>Keys: arrow keys move the spell. E cycles the spell's effect: combined, wave, bounce, shake,
 * pulse, spin, flash, rainbow. F toggles the full-screen zone; the status line shows the layer's
 * frame handle count (H). T swaps between a square and a tall font, rebuilding the circles for the
 * new cell shape. On touch screens, a touch or drag above the bottom quarter moves the spell, and
 * the bottom quarter is split into thirds for E, F, and T.
 *
 * <p>The scene clock loops, so captures can hold it still with the {@code zonesTime} setting.
 */
class ZonesScene extends GridScene {
    static final String KEY_ZONES_TIME = "zonesTime";
    static final String KEY_ZONES_START = "zonesStart";
    private static final String EFFECT_OPTION_PREFIX = "effect=";

    private static final int COLUMNS = 40;
    private static final int ROWS = 22;
    // below 7 for a rounder outline; see CircleShape
    private static final float SPELL_RADIUS = 6.6f;
    private static final float SPELL_START_X = 11.5f;
    private static final float SPELL_START_Y = 9.5f;
    private static final float OUTLINE_CENTER_X = 30.5f;
    private static final float OUTLINE_CENTER_Y = 9.5f;
    private static final float MOVE_CELLS_PER_SECOND = 12f;
    private static final float CAPPED_DELTA = 0.1f;
    private static final float BUTTON_STRIP_START = 0.75f;
    private static final int FULL_SCREEN_SEED = 7;

    private final GlyphSource squareGlyphs;
    private final GlyphSource tallGlyphs;
    private final Layer layer;
    private final TweenClock clock =
            TweenClock.looping(SpellEffectCycle.LOOP_SECONDS, CAPPED_DELTA);
    private final SpellEffectCycle spellEffects = new SpellEffectCycle();
    private final EffectZone spellZone;
    private final EffectZone captionZone;
    private final EffectZone fullScreenZone;
    private final StringBuilder status = new StringBuilder();
    private final Vector2 touchPoint = new Vector2();
    private CircleShape spellShape;
    private boolean tall;
    private boolean fullScreenOn;
    private boolean clockPinned;

    /**
     * @param squareGlyphs font the scene opens with
     * @param tallGlyphs font T swaps to; its cells are taller than wide
     * @param zonesTimeSetting loop time in seconds to hold the scene at, or null or invalid to play
     *     normally
     * @param zonesStartSetting comma-separated start options for captures: {@code tall},
     *     {@code full}, {@code effect=<name>}; null or unknown tokens are ignored
     */
    ZonesScene(GlyphSource squareGlyphs, GlyphSource tallGlyphs, String zonesTimeSetting,
            String zonesStartSetting) {
        super(squareGlyphs, COLUMNS, ROWS);
        this.squareGlyphs = squareGlyphs;
        this.tallGlyphs = tallGlyphs;
        layer = layers.getLayer(0);
        layers.setSize(COLUMNS * squareGlyphs.getCellWidth(), ROWS * squareGlyphs.getCellHeight());
        spellShape = createCircle(SPELL_START_X, SPELL_START_Y, ShapeFill.FILLED, squareGlyphs);
        spellZone = new EffectZone(spellShape, spellEffects.current());
        captionZone = new EffectZone(
                RectangleShape.forSquareCells(ZonesBackdrop.CAPTION_COLUMN,
                        ZonesBackdrop.CAPTION_ROW,
                        ZonesBackdrop.CAPTION.length(), 1, ShapeFill.FILLED),
                CellEffects.wave(0.4f, 1.2f, Phase.alongColumns(10f)));
        fullScreenZone = new EffectZone(
                RectangleShape.forSquareCells(0, 0, COLUMNS, ROWS, ShapeFill.FILLED),
                CellEffects.wave(0.2f, 1f, Phase.scattered(FULL_SCREEN_SEED)));
        redrawBackdrop();
        pinClockIfRequested(zonesTimeSetting);
        applyStartOptions(zonesStartSetting);
    }

    @Override
    public String id() {
        return "zones";
    }

    @Override
    public String title() {
        return "Zones: area effects";
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
    public boolean hasLiveStatus() {
        return true;
    }

    @Override
    public String statusText() {
        status.setLength(0);
        status.append("E:").append(spellEffects.currentName())
                .append(" F:").append(fullScreenOn ? "on" : "off")
                .append(" T:").append(tall ? "tall" : "sq")
                .append(" H:").append(layer.getFrameCellHandleCount());
        return status.toString();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.E) {
            cycleSpellEffect();
            return true;
        }
        if (keycode == Input.Keys.F) {
            toggleFullScreenZone();
            return true;
        }
        if (keycode == Input.Keys.T) {
            swapFont();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        if (!isInButtonStrip(screenY)) {
            moveSpellToScreen(screenX, screenY);
            return true;
        }
        int third = Gdx.graphics.getWidth() / 3;
        if (screenX < third) {
            cycleSpellEffect();
        } else if (screenX < third * 2) {
            toggleFullScreenZone();
        } else {
            swapFont();
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY) {
        if (isInButtonStrip(screenY)) {
            return false;
        }
        moveSpellToScreen(screenX, screenY);
        return true;
    }

    @Override
    protected void update(float delta) {
        if (!clockPinned) {
            clock.update(delta);
        }
        moveSpellFromArrowKeys(delta);
        layers.beginCellEffects(delta);
        applyZones(clock.getTimeSeconds());
    }

    private void applyZones(float time) {
        if (fullScreenOn) {
            fullScreenZone.apply(layer, time);
        }
        spellZone.apply(layer, time);
        captionZone.apply(layer, time);
    }

    private void moveSpellFromArrowKeys(float delta) {
        float horizontal = axis(Input.Keys.RIGHT, Input.Keys.LEFT);
        float vertical = axis(Input.Keys.DOWN, Input.Keys.UP);
        if (horizontal == 0f && vertical == 0f) {
            return;
        }
        float distance = MOVE_CELLS_PER_SECOND * Math.min(delta, CAPPED_DELTA);
        moveSpellTo(spellShape.getCenterX() + horizontal * distance,
                spellShape.getCenterY() + vertical * distance);
    }

    private static float axis(int positiveKey, int negativeKey) {
        float value = 0f;
        if (Gdx.input.isKeyPressed(positiveKey)) {
            value += 1f;
        }
        if (Gdx.input.isKeyPressed(negativeKey)) {
            value -= 1f;
        }
        return value;
    }

    private void moveSpellToScreen(int screenX, int screenY) {
        touchPoint.set(screenX, screenY);
        viewport.unproject(touchPoint);
        float cellWidth = layer.getGlyphSource().getCellWidth();
        float cellHeight = layer.getGlyphSource().getCellHeight();
        moveSpellTo(touchPoint.x / cellWidth,
                (viewport.getWorldHeight() - touchPoint.y) / cellHeight);
    }

    private void moveSpellTo(float centerX, float centerY) {
        spellShape.setCenter(MathUtils.clamp(centerX, 0f, COLUMNS),
                MathUtils.clamp(centerY, 0f, ROWS));
    }

    private boolean isInButtonStrip(int screenY) {
        return screenY > Gdx.graphics.getHeight() * BUTTON_STRIP_START;
    }

    private void cycleSpellEffect() {
        spellEffects.advance();
        showCurrentSpellEffect();
    }

    private void showCurrentSpellEffect() {
        spellZone.setEffect(spellEffects.current());
    }

    private void applyStartOptions(String startSetting) {
        if (startSetting == null) {
            return;
        }
        String[] tokens = startSetting.split(",");
        for (int i = 0; i < tokens.length; i++) {
            applyStartOption(tokens[i].trim());
        }
    }

    private void applyStartOption(String token) {
        if (token.equalsIgnoreCase("tall")) {
            if (!tall) {
                swapFont();
            }
        } else if (token.equalsIgnoreCase("full")) {
            if (!fullScreenOn) {
                toggleFullScreenZone();
            }
        } else if (token.regionMatches(true, 0, EFFECT_OPTION_PREFIX, 0,
                EFFECT_OPTION_PREFIX.length())) {
            spellEffects.select(token.substring(EFFECT_OPTION_PREFIX.length()).trim());
            showCurrentSpellEffect();
        }
    }

    private void toggleFullScreenZone() {
        fullScreenOn = !fullScreenOn;
    }

    /**
     * Swapping is rare, so allocating new shapes here is fine. The grid keeps its size; the world
     * size follows the new cell size.
     */
    private void swapFont() {
        tall = !tall;
        GlyphSource glyphs = tall ? tallGlyphs : squareGlyphs;
        layer.setGlyphSource(glyphs);
        layers.setSize(COLUMNS * glyphs.getCellWidth(), ROWS * glyphs.getCellHeight());
        viewport.setWorldSize(COLUMNS * glyphs.getCellWidth(), ROWS * glyphs.getCellHeight());
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        spellShape = createCircle(spellShape.getCenterX(), spellShape.getCenterY(),
                ShapeFill.FILLED, glyphs);
        spellZone.setShape(spellShape);
        redrawBackdrop();
    }

    private void redrawBackdrop() {
        ZonesBackdrop.drawFloor(grid);
        ZonesBackdrop.drawCaption(grid);
        ZonesBackdrop.drawOutline(grid,
                createCircle(OUTLINE_CENTER_X, OUTLINE_CENTER_Y, ShapeFill.OUTLINE,
                        layer.getGlyphSource()));
    }

    private static CircleShape createCircle(float centerX, float centerY, ShapeFill fill,
            GlyphSource glyphs) {
        float aspect = (float) glyphs.getCellHeight() / glyphs.getCellWidth();
        return new CircleShape(centerX, centerY, SPELL_RADIUS, fill, aspect);
    }

    private void pinClockIfRequested(String zonesTimeSetting) {
        if (zonesTimeSetting == null) {
            return;
        }
        try {
            float seconds = Float.parseFloat(zonesTimeSetting.trim());
            if (!Float.isFinite(seconds)) {
                return;
            }
            clock.setTime(seconds);
            clockPinned = true;
        } catch (NumberFormatException ignored) {
            // not a number: play normally
        }
    }
}
