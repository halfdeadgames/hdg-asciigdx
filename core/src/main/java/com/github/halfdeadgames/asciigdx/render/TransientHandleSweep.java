package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.utils.Array;

/**
 * The per-frame pass for a layer in transient cell handle mode. It counts how long each handle has
 * been idle, releases handles idle for at least the layer's release time, and resets the rest.
 */
final class TransientHandleSweep {

    private TransientHandleSweep() {
    }

    static void run(Layer layer, float deltaSeconds) {
        float countedSeconds = countedSecondsOf(deltaSeconds);
        float releaseSeconds = layer.getTransientReleaseSeconds();
        Array<CellHandle> handles = layer.getCellHandles();
        boolean released = false;
        for (int i = handles.size - 1; i >= 0; i--) {
            CellHandle handle = handles.get(i);
            updateUnusedSeconds(handle, countedSeconds);
            if (handle.getUnusedSeconds() >= releaseSeconds) {
                layer.releaseCellHandleAt(i);
                released = true;
                continue;
            }
            handle.reset();
        }
        if (released) {
            layer.rebindCellHandles();
        }
    }

    /** Only finite positive deltas count; zero, negative, NaN, and infinite deltas add no time. */
    private static float countedSecondsOf(float deltaSeconds) {
        if (deltaSeconds > 0f && !Float.isInfinite(deltaSeconds)) {
            return deltaSeconds;
        }
        return 0f;
    }

    private static void updateUnusedSeconds(CellHandle handle, float countedSeconds) {
        if (Layer.isIdle(handle)) {
            handle.addUnusedSeconds(countedSeconds);
            return;
        }
        handle.clearUnusedSeconds();
    }
}
