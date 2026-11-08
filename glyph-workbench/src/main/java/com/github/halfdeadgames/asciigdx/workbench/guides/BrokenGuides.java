package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.ArrayList;
import java.util.List;

/** Finds the guides that name a group the rules file does not define. */
public final class BrokenGuides {

    /** A broken guide with the group names it uses that the rules file lacks. */
    public record Broken(String label, List<String> undefinedNames) {

        /** The warning text, such as {@code Guide CAP: unknown group symbols, extra}. */
        public String message() {
            return "Guide " + label + ": unknown group " + String.join(", ", undefinedNames);
        }
    }

    private BrokenGuides() {
    }

    /** The broken guides in guide-list order: rows, columns, bars, then column bars. None when the rules define no groups. */
    public static List<Broken> find(PaintGuides guides, RulesFile rules) {
        List<Broken> broken = new ArrayList<>();
        for (PaintGuides.Listed guide : guides.allGuides()) {
            List<String> undefined = GuideGroups.undefinedNames(GuideGroups.namesIn(guide.groups()), rules);
            if (!undefined.isEmpty()) {
                broken.add(new Broken(guide.label(), List.copyOf(undefined)));
            }
        }
        return broken;
    }
}
