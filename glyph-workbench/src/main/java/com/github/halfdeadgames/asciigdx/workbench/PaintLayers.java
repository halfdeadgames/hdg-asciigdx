package com.github.halfdeadgames.asciigdx.workbench;

/** Which paint layers are drawn this frame, decided from what is open. Refreshed once per frame. */
final class PaintLayers {

    private boolean confirmingQuit;
    private boolean newGlyphOpen;
    private boolean newSizeOpen;
    private boolean newFontOpen;
    private boolean deriveDialogShown;
    private boolean controlsOpen;
    private boolean overlayShown;
    private boolean promptShown;

    // The dialogs that dim the whole screen when drawn. Static so input code can ask before a frame refresh.
    static boolean dialogDimsScreen(boolean confirmingQuit, boolean newGlyphOpen, boolean newSizeOpen,
                                    boolean newFontOpen, boolean deriveDims) {
        return confirmingQuit || newGlyphOpen || newSizeOpen || newFontOpen || deriveDims;
    }

    /** What is open this frame; deriveDims is true while the derive flow shows its menu or the replace confirmation. */
    record State(boolean confirmingQuit, boolean newGlyphOpen, boolean newSizeOpen, boolean newFontOpen,
                 boolean deriveDims, boolean controlsOpen, boolean rulesShowing, boolean menuOpen) {
    }

    void update(State state) {
        this.confirmingQuit = state.confirmingQuit();
        this.newGlyphOpen = state.newGlyphOpen();
        this.newSizeOpen = state.newSizeOpen();
        this.newFontOpen = state.newFontOpen();
        this.deriveDialogShown = state.deriveDims();
        this.controlsOpen = state.controlsOpen();
        // Each dialog dims the screen itself, so one under an overlay would dim the background twice.
        overlayShown = state.controlsOpen() || state.rulesShowing();
        // A dialog dims everything, so the bar is drawn beneath the dim, unless a menu is open and must stay clear.
        promptShown = dialogDimsScreen(state.confirmingQuit(), state.newGlyphOpen(), state.newSizeOpen(),
            state.newFontOpen(), state.deriveDims()) && !overlayShown && !state.menuOpen();
    }

    boolean overlayShown() {
        return overlayShown;
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

    boolean quitPromptShown() {
        return confirmingQuit && !overlayShown;
    }

    boolean newGlyphShown() {
        return newGlyphOpen && !overlayShown;
    }

    // Under the unsaved-changes prompt the dialog is hidden but kept, so cancelling returns to it.
    boolean newFontShown() {
        return newFontOpen && !overlayShown && !confirmingQuit;
    }

    boolean newSizeShown() {
        return newSizeOpen && !overlayShown;
    }

    boolean deriveDialogDrawn() {
        return !overlayShown;
    }

    // The derive help describes the highlighted operation, so it is drawn again above the dim.
    boolean redrawsDeriveHelp() {
        return !overlayShown && deriveDialogShown;
    }
}
