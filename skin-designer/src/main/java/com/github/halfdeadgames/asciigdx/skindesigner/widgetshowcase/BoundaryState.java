package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENT: shared state of the boundary rings: the ring is solid red and can be shown or
 * hidden. The frames and the popup overlay register here so a toggle redraws them.
 */
public final class BoundaryState {
    private static final int RED_RGBA = 0xFF0000FF;

    private final List<Widget> frames = new ArrayList<>();
    private Widget overlay;
    private boolean shown = true;

    void addFrame(Widget frame) {
        frames.add(frame);
    }

    void setOverlay(Widget overlay) {
        this.overlay = overlay;
    }

    /** Forgets the frames of an example that was dropped. Call before building a new tree. */
    public void forgetFrames() {
        frames.clear();
    }

    /** Shows the rings when hidden and hides them when shown. */
    public void toggleShown() {
        shown = !shown;
        requestRingRedraw();
    }

    public boolean isShown() {
        return shown;
    }

    int getRgba() {
        return RED_RGBA;
    }

    private void requestRingRedraw() {
        for (int i = 0; i < frames.size(); i++) {
            frames.get(i).requestRedraw();
        }
        if (overlay != null) {
            overlay.requestRedraw();
        }
    }
}
