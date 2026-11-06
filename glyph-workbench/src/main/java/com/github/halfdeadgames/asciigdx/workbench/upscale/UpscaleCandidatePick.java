package com.github.halfdeadgames.asciigdx.workbench.upscale;

import java.util.ArrayList;
import java.util.List;

/** Which upscaled version of a glyph is selected while the user chooses between them. */
public final class UpscaleCandidatePick {

    private final List<GlyphCandidates.Candidate> candidates;
    private int selectedIndex;

    /** @param candidates at least one candidate, in the order they are listed */
    public UpscaleCandidatePick(List<GlyphCandidates.Candidate> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        this.candidates = List.copyOf(candidates);
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public GlyphCandidates.Candidate selected() {
        return candidates.get(selectedIndex);
    }

    public List<String> labels() {
        List<String> labels = new ArrayList<>(candidates.size());
        for (GlyphCandidates.Candidate candidate : candidates) {
            labels.add(candidate.label());
        }
        return labels;
    }

    /** Selects the candidate with this 1-based number; a number with no candidate changes nothing. */
    public void selectNumber(int number) {
        if (number < 1 || number > candidates.size()) {
            return;
        }
        selectedIndex = number - 1;
    }

    /** Moves the selection by {@code step} candidates, stopping at the first and the last. */
    public void move(int step) {
        selectedIndex = Math.max(0, Math.min(candidates.size() - 1, selectedIndex + step));
    }
}
