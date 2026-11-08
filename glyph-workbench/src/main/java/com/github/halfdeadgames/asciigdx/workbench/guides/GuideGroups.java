package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.LinkedHashSet;
import java.util.Set;

/** What the group names of a guide mean for a glyph, given the groups a rules file defines. */
final class GuideGroups {

    static final String ALL = "all";
    static final Set<String> EVERY_GLYPH = Set.of(ALL);

    private GuideGroups() {
    }

    /**
     * Whether a guide with these groups shows on a glyph: it is for {@code all} glyphs or names no group, the rules
     * file defines no groups, a group it names is undefined, or the glyph is in one of its groups.
     */
    static boolean showsOnGlyph(Set<String> groups, int codePoint, RulesFile rules) {
        if (groups.isEmpty() || groups.contains(ALL)) {
            return true;
        }
        if (rules.groupNames().isEmpty() || namesUndefinedGroup(groups, rules)) {
            return true;
        }
        return isInAnyGroup(groups, codePoint, rules);
    }

    /** Whether a guide names a group the rules file does not define. */
    static boolean namesUndefinedGroup(Set<String> groups, RulesFile rules) {
        for (String group : groups) {
            if (!rules.hasGroup(group)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInAnyGroup(Set<String> groups, int codePoint, RulesFile rules) {
        for (String group : groups) {
            if (rules.isInGroup(group, codePoint)) {
                return true;
            }
        }
        return false;
    }

    /** The groups as the guides file writes them: names joined by commas, or {@code all}. */
    static String text(Set<String> groups) {
        if (groups.isEmpty()) {
            return ALL;
        }
        return String.join(",", groups);
    }

    /** The groups with {@code all} replaced by the four names the keys 1-4 toggle. */
    static Set<String> spelledOut(Set<String> groups) {
        if (groups.contains(ALL)) {
            return new LinkedHashSet<>(PaintGuides.KEY_TOGGLE_GROUPS);
        }
        return groups;
    }

    /** The groups in key order followed by other names; exactly the four key names become {@code all}. */
    static Set<String> collapsed(Set<String> groups) {
        if (groups.size() == PaintGuides.KEY_TOGGLE_GROUPS.size()
            && groups.containsAll(PaintGuides.KEY_TOGGLE_GROUPS)) {
            return EVERY_GLYPH;
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (String group : PaintGuides.KEY_TOGGLE_GROUPS) {
            if (groups.contains(group)) {
                ordered.add(group);
            }
        }
        ordered.addAll(groups);
        return ordered;
    }
}
