package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import java.util.List;

/** Picks the next option of a cycle button. */
final class OptionCycle {

    private OptionCycle() {
    }

    /**
     * Returns the option after the current one, wrapping to the first. When the current value is
     * not in the list, returns the first option.
     *
     * @throws IllegalArgumentException if the list is empty
     */
    static String next(List<String> options, String current) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("A cycle needs at least one option");
        }
        int currentIndex = options.indexOf(current);
        return options.get((currentIndex + 1) % options.size());
    }
}
