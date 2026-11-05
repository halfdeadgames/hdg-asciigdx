package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintLayersTest {

    private final PaintLayers layers = new PaintLayers();

    @Test
    void nothingOpenShowsNoPrompt() {
        layers.update(nothingOpen().build());

        assertFalse(layers.promptShown());
        assertFalse(layers.menuBarBelowDim());
        assertFalse(layers.overlayShown());
        assertFalse(layers.redrawsDeriveHelp());
    }

    @Test
    void overlayHidesDialogs() {
        layers.update(nothingOpen().confirmingQuit().newGlyphOpen().newSizeOpen().newFontOpen().deriveDims()
            .controlsOpen().build());

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
        layers.update(nothingOpen().newGlyphOpen().rulesShowing().build());

        assertTrue(layers.overlayShown());
        assertFalse(layers.newGlyphShown());
    }

    @Test
    void controlsShownWhenControlsOpen() {
        layers.update(nothingOpen().controlsOpen().build());

        assertTrue(layers.controlsShown());
    }

    @Test
    void dialogPutsBarBelowDim() {
        layers.update(nothingOpen().newGlyphOpen().build());

        assertTrue(layers.promptShown());
        assertTrue(layers.menuBarBelowDim());
        assertTrue(layers.newGlyphShown());
    }

    @Test
    void deriveDimsAloneShowsPrompt() {
        layers.update(nothingOpen().deriveDims().build());

        assertTrue(layers.promptShown());
        assertTrue(layers.menuBarBelowDim());
    }

    @Test
    void openMenuOrOverlayTurnsPromptOff() {
        layers.update(nothingOpen().newGlyphOpen().menuOpen().build());
        assertFalse(layers.promptShown());
        assertFalse(layers.menuBarBelowDim());

        layers.update(nothingOpen().newGlyphOpen().controlsOpen().build());
        assertFalse(layers.promptShown());

        layers.update(nothingOpen().newGlyphOpen().rulesShowing().build());
        assertFalse(layers.promptShown());
    }

    @Test
    void promptWithMenuOpenKeepsBarOnTop() {
        layers.update(nothingOpen().confirmingQuit().menuOpen().build());

        assertFalse(layers.promptShown());
        assertFalse(layers.menuBarBelowDim());
        assertTrue(layers.quitPromptShown());
    }

    @Test
    void newFontHiddenWhileConfirmingQuit() {
        layers.update(nothingOpen().confirmingQuit().newFontOpen().build());

        assertFalse(layers.newFontShown());

        layers.update(nothingOpen().newFontOpen().build());

        assertTrue(layers.newFontShown());
    }

    @Test
    void deriveHelpRedrawnOnlyWhenDeriveDialogShownWithoutOverlay() {
        layers.update(nothingOpen().deriveDims().build());
        assertTrue(layers.redrawsDeriveHelp());
        assertTrue(layers.deriveDialogDrawn());

        layers.update(nothingOpen().build());
        assertFalse(layers.redrawsDeriveHelp());

        layers.update(nothingOpen().deriveDims().controlsOpen().build());
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

    private static StateBuilder nothingOpen() {
        return new StateBuilder();
    }

    private static final class StateBuilder {
        private boolean confirmingQuit;
        private boolean newGlyphOpen;
        private boolean newSizeOpen;
        private boolean newFontOpen;
        private boolean deriveDims;
        private boolean controlsOpen;
        private boolean rulesShowing;
        private boolean menuOpen;

        StateBuilder confirmingQuit() {
            confirmingQuit = true;
            return this;
        }

        StateBuilder newGlyphOpen() {
            newGlyphOpen = true;
            return this;
        }

        StateBuilder newSizeOpen() {
            newSizeOpen = true;
            return this;
        }

        StateBuilder newFontOpen() {
            newFontOpen = true;
            return this;
        }

        StateBuilder deriveDims() {
            deriveDims = true;
            return this;
        }

        StateBuilder controlsOpen() {
            controlsOpen = true;
            return this;
        }

        StateBuilder rulesShowing() {
            rulesShowing = true;
            return this;
        }

        StateBuilder menuOpen() {
            menuOpen = true;
            return this;
        }

        PaintLayers.State build() {
            return new PaintLayers.State(confirmingQuit, newGlyphOpen, newSizeOpen, newFontOpen, deriveDims,
                controlsOpen, rulesShowing, menuOpen);
        }
    }
}
