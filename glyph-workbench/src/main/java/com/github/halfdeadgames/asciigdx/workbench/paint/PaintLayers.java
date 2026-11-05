package com.github.halfdeadgames.asciigdx.workbench.paint;

/** Which paint layers are drawn this frame, decided from what is open. Refreshed once per frame. */
final class PaintLayers {

    private boolean confirmingUnsaved;
    private boolean newGlyphOpen;
    private boolean newSizeOpen;
    private boolean upscaleSourceOpen;
    private boolean newFontOpen;
    private boolean deriveDialogShown;
    private boolean controlsOpen;
    private boolean coveringPanelShown;
    private boolean promptShown;

    // The dialogs that dim the whole screen when drawn. Static so input code can ask before a frame refresh.
    static boolean dialogDimsScreen(boolean confirmingUnsaved, boolean newGlyphOpen, boolean newSizeOpen,
                                    boolean upscaleSourceOpen, boolean newFontOpen, boolean deriveDims) {
        return confirmingUnsaved || newGlyphOpen || newSizeOpen || upscaleSourceOpen || newFontOpen || deriveDims;
    }

    /** What is open this frame; deriveDims is true while the derive flow shows its menu or the replace confirmation. */
    record State(boolean confirmingUnsaved, boolean newGlyphOpen, boolean newSizeOpen, boolean upscaleSourceOpen,
                 boolean newFontOpen, boolean deriveDims, boolean controlsOpen, boolean rulesShowing,
                 boolean menuOpen) {
    }

    void update(State state) {
        this.confirmingUnsaved = state.confirmingUnsaved();
        this.newGlyphOpen = state.newGlyphOpen();
        this.newSizeOpen = state.newSizeOpen();
        this.upscaleSourceOpen = state.upscaleSourceOpen();
        this.newFontOpen = state.newFontOpen();
        this.deriveDialogShown = state.deriveDims();
        this.controlsOpen = state.controlsOpen();
        // Each dialog dims the screen itself, so one under a covering panel would dim the background twice.
        coveringPanelShown = state.controlsOpen() || state.rulesShowing();
        // A dialog dims everything, so the bar is drawn beneath the dim, unless a menu is open and must stay clear.
        promptShown = dialogDimsScreen(state.confirmingUnsaved(), state.newGlyphOpen(), state.newSizeOpen(),
            state.upscaleSourceOpen(), state.newFontOpen(), state.deriveDims()) && !coveringPanelShown && !state.menuOpen();
    }

    boolean coveringPanelShown() {
        return coveringPanelShown;
    }

    boolean promptShown() {
        return promptShown;
    }

    boolean menuBarBelowDim() {
        return promptShown;
    }

    boolean controlsShown() {
        return controlsOpen;
    }

    boolean unsavedPromptShown() {
        return confirmingUnsaved && !coveringPanelShown;
    }

    boolean newGlyphShown() {
        return newGlyphOpen && !coveringPanelShown;
    }

    // Under the unsaved-changes prompt the dialog is hidden but kept, so cancelling returns to it.
    boolean newFontShown() {
        return newFontOpen && !coveringPanelShown && !confirmingUnsaved;
    }

    boolean newSizeShown() {
        return newSizeOpen && !coveringPanelShown;
    }

    boolean upscaleSourceShown() {
        return upscaleSourceOpen && !coveringPanelShown;
    }

    boolean deriveDialogAllowed() {
        return !coveringPanelShown;
    }

    // The derive help describes the highlighted operation, so it is drawn again above the dim.
    boolean redrawsDeriveHelp() {
        return !coveringPanelShown && deriveDialogShown;
    }
}
