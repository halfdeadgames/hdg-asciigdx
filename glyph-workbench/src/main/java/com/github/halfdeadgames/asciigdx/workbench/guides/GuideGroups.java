package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** What the group names of a guide mean for a glyph, given the groups a rules file defines. */
final class GuideGroups {

    static final String ALL = "all";
    static final Set<String> EVERY_GLYPH = Set.of(ALL);

    private GuideGroups() {
    }

    /**
     * Whether a guide with these groups shows on a glyph: it is for {@code all} glyphs or names no
     * group, the rules file defines no groups, a group it names is undefined, or the glyph is in
     * one of its groups.
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

    /**
     * The group names a guide uses that the rules file does not define, in the guide's order. Empty
     * when the rules file defines no groups, because then every guide applies to all glyphs and no
     * name counts as broken.
     */
    static List<String> undefinedNames(Set<String> groups, RulesFile rules) {
        List<String> undefined = new ArrayList<>();
        if (rules.groupNames().isEmpty()) {
            return undefined;
        }
        for (String group : groups) {
            if (!rules.hasGroup(group)) {
                undefined.add(group);
            }
        }
        return undefined;
    }

    /**
     * Whether a guide is broken: it names a group that a rules file with groups does not define.
     */
    static boolean isBroken(Set<String> groups, RulesFile rules) {
        return !undefinedNames(groups, rules).isEmpty();
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

    /** The group names written in a groups text, in order; empty for {@code all}. */
    static Set<String> namesIn(String groupsText) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : groupsText.split(",")) {
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        if (names.isEmpty() || containsAll(names)) {
            return new LinkedHashSet<>();
        }
        return names;
    }

    private static boolean containsAll(Set<String> names) {
        for (String name : names) {
            if (name.equalsIgnoreCase(ALL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The groups after switching one name on or off. A guide for {@code all} gets just that name;
     * switching off the last name goes back to {@code all}. The names are then stored in the rules
     * file's group order, with names the file does not define after them in their existing order.
     */
    static Set<String> toggled(Set<String> groups, String name, List<String> definedGroups) {
        if (groups.isEmpty() || groups.contains(ALL)) {
            return new LinkedHashSet<>(Set.of(name));
        }
        Set<String> edited = new LinkedHashSet<>(groups);
        if (!edited.remove(name)) {
            edited.add(name);
        }
        return edited.isEmpty() ? EVERY_GLYPH : inRulesFileOrder(edited, definedGroups);
    }

    private static Set<String> inRulesFileOrder(Set<String> names, List<String> definedGroups) {
        Set<String> ordered = new LinkedHashSet<>();
        for (String defined : definedGroups) {
            if (names.contains(defined)) {
                ordered.add(defined);
            }
        }
        ordered.addAll(names);
        return ordered;
    }

    /**
     * The line the message area shows for a guide, such as {@code CAP: ALL GLYPHS} or
     * {@code BAR: UPPERCASE, DIGITS}.
     */
    static String summary(String label, String groupsText) {
        Set<String> names = namesIn(groupsText);
        String shown =
                names.isEmpty() ? "ALL GLYPHS" : String.join(", ", names).toUpperCase(Locale.ROOT);
        return label + ": " + shown;
    }
}
