package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiScale;

/** Chooses the UI scale of the paint screen: the automatic factor, or a requested override cut down to fit. */
final class PaintUiScale {

    private final int editorCellWidth;
    private final int editorCellHeight;
    private Integer overrideFactor;
    private int autoFactor = UiScale.DEFAULT_FACTOR;
    private UiScale scale = UiScale.DEFAULT;

    PaintUiScale(int editorCellWidth, int editorCellHeight, Integer initialOverride) {
        this.editorCellWidth = editorCellWidth;
        this.editorCellHeight = editorCellHeight;
        this.overrideFactor = initialOverride;
    }

    void update(int width, int height) {
        autoFactor = PaintScale.bestFactor(width, height, PaintSheetView.GAP, editorCellWidth, editorCellHeight);
        int factor = chosenFactor(width, height);
        if (factor != scale.factor()) {
            scale = new UiScale(factor);
        }
    }

    void step(int delta, int width, int height) {
        overrideFactor = PaintScale.stepped(scale.factor(), delta);
        update(width, height);
    }

    void useAuto(int width, int height) {
        overrideFactor = null;
        update(width, height);
    }

    UiScale scale() {
        return scale;
    }

    int factor() {
        return scale.factor();
    }

    boolean isAuto() {
        return overrideFactor == null;
    }

    String label() {
        return PaintScale.label(factor(), isAuto());
    }

    boolean overrideTooBig() {
        return overrideFactor != null && factor() < overrideFactor;
    }

    Integer overrideFactor() {
        return overrideFactor;
    }

    // The requested override is kept as asked; only what is applied is cut down to fit the window.
    private int chosenFactor(int width, int height) {
        if (overrideFactor == null) {
            return autoFactor;
        }
        return PaintScale.fittingFactor(overrideFactor, width, height, PaintSheetView.GAP,
            new GlyphSize(editorCellWidth, editorCellHeight));
    }
}
