package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.math.MathUtils;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.CellHandle;
import com.github.halfdeadgames.asciigdx.render.Layer;

/**
 * Proves temporary cell overrides look the same on flat and lifted cells. A word sits in the grid;
 * its first half stays flat and its second half bobs through lifted cells. One six-second cycle of
 * one-second phases applies identical overrides to every letter:
 * 0 plain, 1 gold foreground mix rising, 2 white hit flash fading, 3 dark red background mix,
 * 4 temporary '*' glyph, 5 a green grid write hidden by a gold mix that clears halfway.
 */
final class OverrideDemo {
    static final int PHASE_COUNT = 6;
    static final float PHASE_SECONDS = 1f;
    static final float CYCLE_SECONDS = PHASE_COUNT * PHASE_SECONDS;

    private static final String LABEL = "overrides: flat | lifted";
    private static final String WORD = "OVERRIDE";
    private static final int LABEL_COLUMN = 4;
    private static final int LABEL_ROW = 20;
    private static final int WORD_COLUMN = 16;
    private static final int WORD_ROW = 21;
    private static final int FIRST_LIFTED_LETTER = 4;

    private static final int LABEL_COLOR = 0xA0A0A0FF;
    private static final int LETTER_COLOR = 0xA0C0FFFF;
    private static final int GAME_WRITE_COLOR = 0x40FF40FF;
    private static final int BACKDROP = 0x202030FF;
    private static final int GOLD_MIX = 0xFFD45CFF;
    private static final int WHITE_MIX = 0xFFFFFFFF;
    private static final int DARK_RED_MIX = 0x8B0000FF;
    private static final int TEMPORARY_GLYPH = '*';

    private static final float BACKGROUND_MIX_AMOUNT = 0.8f;
    private static final float BOB_PIXELS = 3f;
    private static final float BOB_RADIANS_PER_SECOND = 3f;
    private static final float BOB_PHASE_PER_LETTER = 0.9f;
    private static final float HIDE_SECONDS = 0.5f;

    private final TerminalGrid grid;
    private final CellHandle[] handles = new CellHandle[WORD.length()];
    private float clock;
    private boolean gameWriteActive;

    OverrideDemo(TerminalGrid grid, Layer layer) {
        this.grid = grid;
        for (int i = 0; i < handles.length; i++) {
            handles[i] = layer.cell(WORD_COLUMN + i, WORD_ROW);
        }
        draw();
    }

    /** Writes the label and word into the grid; call again after anything overwrites the area. */
    void draw() {
        for (int i = 0; i < LABEL.length(); i++) {
            grid.set(LABEL_COLUMN + i, LABEL_ROW, LABEL.charAt(i), LABEL_COLOR, BACKDROP);
        }
        for (int i = 0; i < WORD.length(); i++) {
            grid.set(WORD_COLUMN + i, WORD_ROW, WORD.charAt(i), letterColor(), BACKDROP);
        }
    }

    void update(float step) {
        clock = (clock + step) % CYCLE_SECONDS;
        updateBob();
        int phase = (int) (clock / PHASE_SECONDS);
        float progress = clock / PHASE_SECONDS - phase;
        updateGameWrite(phase);
        applyPhase(phase, progress);
    }

    private void updateBob() {
        for (int i = FIRST_LIFTED_LETTER; i < handles.length; i++) {
            float angle = clock * BOB_RADIANS_PER_SECOND + i * BOB_PHASE_PER_LETTER;
            handles[i].setOffset(0f, MathUtils.sin(angle) * BOB_PIXELS);
        }
    }

    private void updateGameWrite(int phase) {
        boolean shouldBeWritten = phase == PHASE_COUNT - 1;
        if (shouldBeWritten == gameWriteActive) {
            return;
        }
        gameWriteActive = shouldBeWritten;
        paintLetters(letterColor());
    }

    private int letterColor() {
        return gameWriteActive ? GAME_WRITE_COLOR : LETTER_COLOR;
    }

    private void paintLetters(int foreground) {
        for (int i = 0; i < handles.length; i++) {
            grid.setForeground(WORD_COLUMN + i, WORD_ROW, foreground);
        }
    }

    private void applyPhase(int phase, float progress) {
        switch (phase) {
            case 1 -> applyMixes(GOLD_MIX, progress, 0, 0f, CellHandle.NO_CODE_POINT);
            case 2 -> applyMixes(WHITE_MIX, 1f - progress, WHITE_MIX, 1f - progress, CellHandle.NO_CODE_POINT);
            case 3 -> applyMixes(0, 0f, DARK_RED_MIX, BACKGROUND_MIX_AMOUNT, CellHandle.NO_CODE_POINT);
            case 4 -> applyMixes(0, 0f, 0, 0f, TEMPORARY_GLYPH);
            case 5 -> applyMixes(GOLD_MIX, progress * PHASE_SECONDS < HIDE_SECONDS ? 1f : 0f, 0, 0f,
                CellHandle.NO_CODE_POINT);
            default -> clearAll();
        }
    }

    private void applyMixes(int foreground, float foregroundAmount, int background, float backgroundAmount,
            int codePoint) {
        for (int i = 0; i < handles.length; i++) {
            handles[i].setMix(foreground, foregroundAmount, background, backgroundAmount);
            handles[i].setCodePoint(codePoint);
        }
    }

    private void clearAll() {
        for (int i = 0; i < handles.length; i++) {
            handles[i].clearOverrides();
        }
    }
}
