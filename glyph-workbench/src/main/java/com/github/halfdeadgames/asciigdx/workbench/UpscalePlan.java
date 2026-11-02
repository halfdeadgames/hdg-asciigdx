package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Decides which size a font is upscaled to and from which sizes. Work goes up one step at a time:
 * the target is the largest existing size plus 8, and a source must be a smaller existing size
 * that divides the target evenly (16 from 8, 24 from 8, 32 from 16 or 8).
 */
final class UpscalePlan {

    static final int STEP = 8;

    /**
     * One font's next upscale.
     *
     * @param sources existing sizes that divide {@code target} evenly, ascending; empty when none does
     */
    record Choice(String folderName, int largest, int target, List<Integer> sources) {

        boolean hasSource() {
            return !sources.isEmpty();
        }

        /** The source with the largest size, which supplies the sheet. */
        int primarySource() {
            return sources.get(sources.size() - 1);
        }

        int factorFrom(int source) {
            return target / source;
        }

        /** Menu text such as {@code SAIKSISTT 8 -> 16}. */
        String label() {
            return folderName.toUpperCase(java.util.Locale.ROOT) + " " + largest + " -> " + target;
        }
    }

    private UpscalePlan() {
    }

    /** Sizes below {@code target} that divide it evenly, ascending. */
    static List<Integer> evenSources(int target, Collection<Integer> existingSizes) {
        List<Integer> sources = new ArrayList<>();
        for (int size : new TreeSet<>(existingSizes)) {
            if (size > 0 && size < target && target % size == 0) {
                sources.add(size);
            }
        }
        return sources;
    }

    static Choice choiceFor(String folderName, Collection<Integer> existingSizes) {
        int largest = new TreeSet<>(existingSizes).last();
        int target = largest + STEP;
        return new Choice(folderName, largest, target, evenSources(target, existingSizes));
    }

    /** One choice per font folder, in the order the folders first appear in {@code fonts}. */
    static List<Choice> plan(List<FontChoice> fonts) {
        Map<String, List<Integer>> sizesByFolder = new LinkedHashMap<>();
        for (FontChoice font : fonts) {
            sizesByFolder.computeIfAbsent(font.folderName(), name -> new ArrayList<>()).add(font.size());
        }
        List<Choice> choices = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : sizesByFolder.entrySet()) {
            choices.add(choiceFor(entry.getKey(), entry.getValue()));
        }
        return choices;
    }
}
