package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENT: shared state of the boundary rings: the ring is solid red and can be shown or
 * hidden.
 */
public final class WidgetBoundary {
    private static final int RED_RGBA = 0xFF0000FF;

    private static final List<BoundaryFrame> FRAMES = new ArrayList<>();
    private static boolean shown = true;

    private WidgetBoundary() {
    }

    static void register(BoundaryFrame frame) {
        FRAMES.add(frame);
    }

    /** Forgets the frames of an example that was dropped. Call before building a new tree. */
    public static void forgetFrames() {
        FRAMES.clear();
    }

    /** Shows the rings when hidden and hides them when shown. */
    public static void toggleShown() {
        shown = !shown;
        requestRingRedraw();
    }

    public static boolean isShown() {
        return shown;
    }

    static int currentRgba() {
        return RED_RGBA;
    }

    private static void requestRingRedraw() {
        for (int i = 0; i < FRAMES.size(); i++) {
            FRAMES.get(i).requestRedraw();
        }
    }
}
