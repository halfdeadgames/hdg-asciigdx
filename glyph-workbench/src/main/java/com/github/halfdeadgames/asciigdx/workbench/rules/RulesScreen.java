package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.HashSet;
import java.util.List;

/**
 * The F1 rules page as a slide-in overlay: shows the rules and notes it is given, owns the
 * open state and slide animation, and takes over the keyboard and wheel while it is open.
 */
public final class RulesScreen {

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

    public RulesScreen(Painter painter) {
        this.painter = painter;
        panel = new RulesPanel(painter, FRAME_COLOR, PAGE_FILL, TEXT_COLOR, HEADING_COLOR, UiPalette.WARN);
        if (Boolean.getBoolean("workbench.rulesOpen")) {
            open = true;
            slide = 1f;
        }
    }

    /**
     * Replaces what the page shows.
     *
     * @param rules what reading the rules file gave
     * @param notesMarkdownOrNull the font's notes, or null if it has none
     */
    public void show(RulesLoadResult rules, String notesMarkdownOrNull) {
        panel.show(rules, notesMarkdownOrNull);
    }

    /**
     * Sets which rules the current glyph breaks, so the page can flag them.
     *
     * @param warnings the glyph's current rule warnings; each key is a rule name
     */
    public void showBrokenRules(List<RuleWarning> warnings) {
        var names = new HashSet<String>();
        for (RuleWarning warning : warnings) {
            names.add(warning.key());
        }
        panel.setBrokenRuleNames(names);
    }

    public boolean isOpen() {
        return open;
    }

    /** True while the page is open or still sliding out. */
    public boolean isShowing() {
        return open || slide > 0f;
    }

    /** Opens the page from the top; F1 and the Help menu both come here. */
    public void open() {
        open = true;
        panel.resetScroll();
    }

    /**
     * Advances the slide and handles F1, Esc, and scrolling. Returns true when the overlay used this
     * frame's input, so the caller must skip its own input handling (and must not quit on Esc).
     * The wheel ticks are read, not cleared; the caller clears them.
     */
    public boolean handleInput(float delta, int wheelTicks) {
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
    public void draw(int worldWidth, int worldHeight, int codePoint) {
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
