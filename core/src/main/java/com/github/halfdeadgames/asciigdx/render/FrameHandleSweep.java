package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.utils.Array;

/**
 * The per-frame pass over a layer's frame handles. It counts how long each handle has been idle,
 * releases handles idle for at least the release time, and resets the rest. Lasting handles are
 * never touched. The caller rebinds the renderer afterward with
 * {@link CellHandleSet#rebindRendererIfChanged()}.
 */
final class FrameHandleSweep {

    private FrameHandleSweep() {
    }

    static void runOnFrameHandles(CellHandleSet handles, float releaseSeconds, float deltaSeconds) {
        Array<CellHandle> frameHandles = handles.frameHandles();
        float countedSeconds = countedSecondsOf(deltaSeconds);
        for (int i = frameHandles.size - 1; i >= 0; i--) {
            CellHandle handle = frameHandles.get(i);
            updateUnusedSeconds(handle, countedSeconds);
            if (handle.getUnusedSeconds() >= releaseSeconds) {
                handles.releaseFrameAt(i);
                continue;
            }
            handle.reset();
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
        if (CellHandleSet.isIdle(handle)) {
            handle.addUnusedSeconds(countedSeconds);
            return;
        }
        handle.clearUnusedSeconds();
    }
}
