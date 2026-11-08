package com.github.halfdeadgames.asciigdx.workbench.guides;

import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.DIGIT;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.LOWER;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.RULES;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.UPPER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuideGroupsTest {

    private static final RulesFile OVERLAPPING = RulesFile.parse("""
        {"groups": {"letters": ["A-Z", "a-z"], "capitals": ["A-Z"]}}
        """, 8, 8);

    @Test
    void allShowsOnEveryGlyph() {
        assertTrue(GuideGroups.showsOnGlyph(Set.of("all"), UPPER, RULES));
        assertTrue(GuideGroups.showsOnGlyph(Set.of("all"), DIGIT, RULES));
    }

    @Test
    void noGroupsShowsOnEveryGlyph() {
        assertTrue(GuideGroups.showsOnGlyph(Set.of(), UPPER, RULES));
    }

    @Test
    void rulesWithoutGroupsShowEveryGuideOnEveryGlyph() {
        assertTrue(GuideGroups.showsOnGlyph(Set.of("uppercase"), LOWER, RulesFile.empty()));
        assertTrue(GuideGroups.showsOnGlyph(Set.of("uppercase"), LOWER, RulesFile.parse("{}", 8, 8)));
    }

    @Test
    void glyphInTheGroupShows() {
        assertTrue(GuideGroups.showsOnGlyph(Set.of("uppercase"), UPPER, RULES));
    }

    @Test
    void glyphNotInTheGroupDoesNotShow() {
        assertFalse(GuideGroups.showsOnGlyph(Set.of("uppercase"), LOWER, RULES));
    }

    @Test
    void glyphInOneOfSeveralGroupsShows() {
        Set<String> groups = new LinkedHashSet<>(Set.of("uppercase", "digits"));
        assertTrue(GuideGroups.showsOnGlyph(groups, DIGIT, RULES));
        assertFalse(GuideGroups.showsOnGlyph(groups, LOWER, RULES));
    }

    @Test
    void glyphInTwoOverlappingGroupsShows() {
        assertTrue(GuideGroups.showsOnGlyph(Set.of("letters"), UPPER, OVERLAPPING));
        assertTrue(GuideGroups.showsOnGlyph(Set.of("capitals"), UPPER, OVERLAPPING));
        assertTrue(GuideGroups.showsOnGlyph(Set.of("letters", "capitals"), UPPER, OVERLAPPING));
        assertFalse(GuideGroups.showsOnGlyph(Set.of("capitals"), LOWER, OVERLAPPING));
    }

    @Test
    void unknownGroupShowsOnEveryGlyph() {
        assertTrue(GuideGroups.showsOnGlyph(Set.of("symbols"), UPPER, RULES));
        assertTrue(GuideGroups.showsOnGlyph(Set.of("symbols"), DIGIT, RULES));
        assertTrue(GuideGroups.showsOnGlyph(Set.of("lowercase", "symbols"), UPPER, RULES));
    }

    @Test
    void undefinedGroupIsDetected() {
        assertTrue(GuideGroups.namesUndefinedGroup(Set.of("symbols"), RULES));
        assertFalse(GuideGroups.namesUndefinedGroup(Set.of("uppercase", "digits"), RULES));
        assertFalse(GuideGroups.namesUndefinedGroup(Set.of("all"), RULES));
    }
}
