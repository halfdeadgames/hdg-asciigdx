package com.github.halfdeadgames.asciigdx.workbench;

import java.util.List;

/** Routes presses, drags, and releases of the pointer to an ordered list of targets. */
final class PointerTargets {

    private final List<PointerTarget> targets;

    PointerTargets(List<PointerTarget> targets) {
        this.targets = targets;
    }

    void dispatch(int x, int y, boolean pressStarted, boolean buttonDown) {
        if (pressStarted) {
            dispatchClick(x, y);
            return;
        }
        PointerTarget captured = firstCaptured();
        if (captured == null) {
            return;
        }
        if (buttonDown) {
            captured.drag(x, y);
        } else {
            captured.release();
        }
    }

    private void dispatchClick(int x, int y) {
        for (int i = 0; i < targets.size(); i++) {
            targets.get(i).notifyPress(x, y);
        }
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).claimPress(x, y)) {
                return;
            }
        }
    }

    private PointerTarget firstCaptured() {
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).isCaptured()) {
                return targets.get(i);
            }
        }
        return null;
    }
}
