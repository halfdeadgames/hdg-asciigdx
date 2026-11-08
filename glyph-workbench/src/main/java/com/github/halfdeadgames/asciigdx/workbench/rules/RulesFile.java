package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The rules and named glyph groups read from one rules file. */
public final class RulesFile {

    /** Name of the built-in group that holds every glyph. */
    static final String ALL_GROUP = "all";

    private final List<GlyphRule> rules;
    private final Map<String, GlyphSet> groups;

    RulesFile(List<GlyphRule> rules) {
        this(rules, Map.of());
    }

    RulesFile(List<GlyphRule> rules, Map<String, GlyphSet> groups) {
        this.rules = List.copyOf(rules);
        this.groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
    }

    /**
     * Reads a rules file.
     *
     * @param json the file text, lenient JSON
     * @param cellWidth glyph width in pixels, which bounds the column numbers
     * @param cellHeight glyph height in pixels, which bounds the row numbers
     * @return the rules in file order
     * @throws RulesFileException if the text is not a valid rules file
     */
    public static RulesFile parse(String json, int cellWidth, int cellHeight) {
        return RulesFileParser.parse(json, cellWidth, cellHeight);
    }

    /** A rules file with no rules and no groups. */
    public static RulesFile empty() {
        return new RulesFile(List.of());
    }

    public List<GlyphRule> rules() {
        return rules;
    }

    /**
     * Lists the defined group names, not including the built-in "all".
     *
     * @return an unmodifiable list of names in file order
     */
    public List<String> groupNames() {
        return Collections.unmodifiableList(new ArrayList<>(groups.keySet()));
    }

    /**
     * Tells whether a group is known.
     *
     * @param groupName the group name to look up
     * @return true for "all" and for every defined group
     */
    public boolean hasGroup(String groupName) {
        return groupName.equals(ALL_GROUP) || groups.containsKey(groupName);
    }

    /**
     * Tells whether a glyph belongs to a group.
     *
     * @param groupName the group name to look up
     * @param codePoint the glyph's code point
     * @return true if the group holds the glyph; false for an unknown group
     */
    public boolean isInGroup(String groupName, int codePoint) {
        if (groupName.equals(ALL_GROUP)) {
            return true;
        }
        GlyphSet group = groups.get(groupName);
        return group != null && group.contains(codePoint);
    }
}
