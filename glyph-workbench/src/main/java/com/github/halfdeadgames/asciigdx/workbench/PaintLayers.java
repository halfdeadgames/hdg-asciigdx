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

    /** deriveDims is true while the derive flow shows its menu or the replace confirmation. */
    void update(boolean confirmingQuit, boolean newGlyphOpen, boolean newSizeOpen, boolean newFontOpen,
                boolean deriveDims, boolean controlsOpen, boolean rulesShowing, boolean menuOpen) {
        this.confirmingQuit = confirmingQuit;
        this.newGlyphOpen = newGlyphOpen;
        this.newSizeOpen = newSizeOpen;
        this.newFontOpen = newFontOpen;
        this.deriveDialogShown = deriveDims;
        this.controlsOpen = controlsOpen;
        // Each dialog dims the screen itself, so one under an overlay would dim the background twice.
        overlayShown = controlsOpen || rulesShowing;
        // A dialog dims everything, so the bar is drawn beneath the dim, unless a menu is open and must stay clear.
        promptShown = dialogDimsScreen(confirmingQuit, newGlyphOpen, newSizeOpen, newFontOpen, deriveDims)
            && !overlayShown && !menuOpen;
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
