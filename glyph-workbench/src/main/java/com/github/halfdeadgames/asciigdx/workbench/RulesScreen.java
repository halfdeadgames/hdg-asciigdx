package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;

/**
 * The F1 rules page as a slide-in overlay: loads the rules file of the font directory, owns the
 * open state and slide animation, and takes over the keyboard and wheel while it is open.
 */
final class RulesScreen {

    private static final String TAG = "RulesScreen";
    private static final float SLIDE_SECONDS = 0.15f;
    private static final float DIM_ALPHA = 0.7f;
    private static final int WHEEL_ROWS = 3;
    private static final Color PAGE_FILL = new Color(0.08f, 0.09f, 0.13f, 1f);
    private static final Color FRAME_COLOR = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color HEADING_COLOR = new Color(0.8f, 0.6f, 1f, 1f);

    private final Painter painter;
    private final RulesPanel panel;
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private final Color dim = new Color();
    private boolean open;
    private float slide;

    /** @param fontDir absolute path of the font folder holding rules.md, or null for none */
    RulesScreen(Painter painter, String fontDir) {
        this.painter = painter;
        panel = new RulesPanel(painter, FRAME_COLOR, PAGE_FILL, TEXT_COLOR, HEADING_COLOR);
        loadRules(fontDir);
        if (Boolean.getBoolean("workbench.rulesOpen")) {
            open = true;
            slide = 1f;
        }
    }

    private void loadRules(String fontDir) {
        if (fontDir == null) {
            panel.setMissing("NO RULES FILE: NO FONT DIRECTORY SET");
            return;
        }
        FileHandle file = Gdx.files.absolute(fontDir).child(FontRules.FILE_NAME);
        if (!file.exists()) {
            panel.setMissing("NO RULES FILE: " + file.path());
            return;
        }
        try {
            panel.setRules(FontRules.parse(file.readString("UTF-8")));
        } catch (GdxRuntimeException e) {
            panel.setMissing("NO RULES FILE: " + file.path());
            Gdx.app.error(TAG, "Cannot read " + file.path() + ": " + e.getMessage());
        }
    }

    boolean isOpen() {
        return open;
    }

    /** True while the page is open or still sliding out. */
    boolean isShowing() {
        return open || slide > 0f;
    }

    /** Opens the page from the top; F1 and the Help menu both come here. */
    void open() {
        open = true;
        panel.resetScroll();
    }

    /**
     * Advances the slide and handles F1, Esc, and scrolling. Returns true when the overlay used this
     * frame's input, so the caller must skip its own input handling (and must not quit on Esc).
     * The wheel ticks are read, not cleared; the caller clears them.
     */
    boolean handleInput(float delta, int wheelTicks) {
        float step = delta / SLIDE_SECONDS;
        slide = MathUtils.clamp(slide + (open ? step : -step), 0f, 1f);
        if (!open) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
                open();
                return true;
            }
            return false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            open = false;
            repeatUp.update(false, delta);
            repeatDown.update(false, delta);
            return true;
        }
        int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        for (int i = 0; i < ups; i++) {
            panel.scrollBy(-1);
        }
        int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
        for (int i = 0; i < downs; i++) {
            panel.scrollBy(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            panel.pageUp();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            panel.pageDown();
        }
        panel.scrollBy(wheelTicks * WHEEL_ROWS);
        return true;
    }

    /** Draws the dimmed backdrop and the sliding page over everything drawn before. */
    void draw(int worldWidth, int worldHeight, int codePoint) {
        if (slide <= 0f) {
            return;
        }
        dim.set(0f, 0f, 0f, DIM_ALPHA * slide);
        painter.fill(dim, 0, 0, worldWidth, worldHeight);
        int cell = painter.cell();
        int cellsWide = RulesPanel.cellsWideFor(worldWidth, cell);
        float eased = 1f - (1f - slide) * (1f - slide);
        int left = worldWidth - Math.round(cellsWide * cell * eased);
        panel.draw(left, 0, cellsWide, Math.max(3, worldHeight / cell), worldHeight, codePoint);
    }
}
