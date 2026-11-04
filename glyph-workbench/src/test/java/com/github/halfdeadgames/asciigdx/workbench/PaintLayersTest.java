package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintLayersTest {

    private final PaintLayers layers = new PaintLayers();

    @Test
    void nothingOpenShowsNoPrompt() {
        layers.update(false, false, false, false, false, false, false, false);

        assertFalse(layers.promptShown());
        assertFalse(layers.menuBarBelowDim());
        assertFalse(layers.overlayShown());
        assertFalse(layers.redrawsDeriveHelp());
    }

    @Test
    void overlayHidesDialogs() {
        layers.update(true, true, true, true, true, true, false, false);

        assertTrue(layers.overlayShown());
        assertFalse(layers.quitPromptShown());
        assertFalse(layers.newGlyphShown());
        assertFalse(layers.newFontShown());
        assertFalse(layers.newSizeShown());
        assertFalse(layers.deriveDialogDrawn());
        assertFalse(layers.promptShown());
    }

    @Test
    void rulesCountAsOverlay() {
        layers.update(false, true, false, false, false, false, true, false);

        assertTrue(layers.overlayShown());
        assertFalse(layers.newGlyphShown());
    }

    @Test
    void controlsShownWhenControlsOpen() {
        layers.update(false, false, false, false, false, true, false, false);

        assertTrue(layers.controlsShown());
    }

    @Test
    void dialogPutsBarBelowDim() {
        layers.update(false, true, false, false, false, false, false, false);

        assertTrue(layers.promptShown());
        assertTrue(layers.menuBarBelowDim());
        assertTrue(layers.newGlyphShown());
    }

    @Test
    void deriveDimsAloneShowsPrompt() {
        layers.update(false, false, false, false, true, false, false, false);

        assertTrue(layers.promptShown());
        assertTrue(layers.menuBarBelowDim());
    }

    @Test
    void openMenuOrOverlayTurnsPromptOff() {
        layers.update(false, true, false, false, false, false, false, true);
        assertFalse(layers.promptShown());
        assertFalse(layers.menuBarBelowDim());

        layers.update(false, true, false, false, false, true, false, false);
        assertFalse(layers.promptShown());

        layers.update(false, true, false, false, false, false, true, false);
        assertFalse(layers.promptShown());
    }

    @Test
    void promptWithMenuOpenKeepsBarOnTop() {
        layers.update(true, false, false, false, false, false, false, true);

        assertFalse(layers.promptShown());
        assertFalse(layers.menuBarBelowDim());
        assertTrue(layers.quitPromptShown());
    }

    @Test
    void newFontHiddenWhileConfirmingQuit() {
        layers.update(true, false, false, true, false, false, false, false);

        assertFalse(layers.newFontShown());

        layers.update(false, false, false, true, false, false, false, false);

        assertTrue(layers.newFontShown());
    }

    @Test
    void deriveHelpRedrawnOnlyWhenDeriveDialogShownWithoutOverlay() {
        layers.update(false, false, false, false, true, false, false, false);
        assertTrue(layers.redrawsDeriveHelp());
        assertTrue(layers.deriveDialogDrawn());

        layers.update(false, false, false, false, false, false, false, false);
        assertFalse(layers.redrawsDeriveHelp());

        layers.update(false, false, false, false, true, true, false, false);
        assertFalse(layers.redrawsDeriveHelp());
    }

    @Test
    void dialogDimsScreenMatchesAnyDimmingDialog() {
        assertFalse(PaintLayers.dialogDimsScreen(false, false, false, false, false));
        assertTrue(PaintLayers.dialogDimsScreen(true, false, false, false, false));
        assertTrue(PaintLayers.dialogDimsScreen(false, true, false, false, false));
        assertTrue(PaintLayers.dialogDimsScreen(false, false, true, false, false));
        assertTrue(PaintLayers.dialogDimsScreen(false, false, false, true, false));
        assertTrue(PaintLayers.dialogDimsScreen(false, false, false, false, true));
    }
}
