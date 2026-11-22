package com.github.halfdeadgames.asciigdx.text;

import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import com.github.halfdeadgames.asciigdx.zone.CellEffects;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the open effect tags while a text is parsed and cuts the letters into runs. A new run
 * starts each time the set of open effects changes; a run of several effects holds their
 * combination, outermost first.
 */
final class EffectRunBuilder {

    private final List<CellEffect> openEffects = new ArrayList<>();
    private final List<EffectRun> runs = new ArrayList<>();
    private int runStartLetter;

    boolean hasOpenEffect() {
        return !openEffects.isEmpty();
    }

    void open(CellEffect effect, int letterCount) {
        closeOpenRun(letterCount);
        openEffects.add(effect);
        runStartLetter = letterCount;
    }

    void closeLast(int letterCount) {
        closeOpenRun(letterCount);
        openEffects.remove(openEffects.size() - 1);
        runStartLetter = letterCount;
    }

    // Records the letters since the last change as a run; empty ranges are dropped.
    void closeOpenRun(int letterCount) {
        if (openEffects.isEmpty() || letterCount <= runStartLetter) {
            return;
        }
        runs.add(EffectRun.of(runStartLetter, letterCount, combineOpenEffects()));
        runStartLetter = letterCount;
    }

    List<EffectRun> getRuns() {
        return runs;
    }

    private CellEffect combineOpenEffects() {
        if (openEffects.size() == 1) {
            return openEffects.get(0);
        }
        return CellEffects.combine(openEffects.toArray(new CellEffect[0]));
    }
}
