package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FontRulesTest {

    private static final String SAMPLE = """
        # Demo

        First line of motto
        second line.

        Not the motto.

        ## Core
        - One
          - One A
        - Two

        ## Uppercase
        - Wide

        ## Extras
        - Other
        """;

    @Test
    void readsTitleAndMottoFirstParagraphOnly() {
        FontRules rules = FontRules.parse(SAMPLE);
        assertEquals("Demo", rules.title());
        assertEquals("First line of motto second line.", rules.motto());
    }

    @Test
    void keepsGroupsInOrderIncludingUnknownHeadings() {
        FontRules rules = FontRules.parse(SAMPLE);
        assertEquals(3, rules.groups().size());
        assertEquals("Core", rules.groups().get(0).name());
        assertEquals("Extras", rules.groups().get(2).name());
    }

    @Test
    void subBulletsAreMarkedAndKeepOrder() {
        FontRules.Group core = FontRules.parse(SAMPLE).group(FontRules.CORE);
        assertEquals(3, core.bullets().size());
        assertEquals(new FontRules.Bullet("One", false), core.bullets().get(0));
        assertEquals(new FontRules.Bullet("One A", true), core.bullets().get(1));
        assertEquals(new FontRules.Bullet("Two", false), core.bullets().get(2));
    }

    @Test
    void groupLookupIgnoresCase() {
        assertNotNull(FontRules.parse(SAMPLE).group("CORE"));
        assertNull(FontRules.parse(SAMPLE).group("Missing"));
    }

    @Test
    void picksGroupByCodePoint() {
        assertEquals(FontRules.UPPERCASE, FontRules.groupNameFor('A'));
        assertEquals(FontRules.LOWERCASE, FontRules.groupNameFor('a'));
        assertEquals(FontRules.DIGITS, FontRules.groupNameFor('7'));
        assertEquals(FontRules.PUNCTUATION, FontRules.groupNameFor(','));
    }

    @Test
    void missingGroupGivesNull() {
        FontRules rules = FontRules.parse(SAMPLE);
        assertEquals("Uppercase", rules.groupFor('Q').name());
        assertNull(rules.groupFor('q'));
        assertNull(rules.groupFor('.'));
    }

    @Test
    void emptyTextGivesEmptyRules() {
        FontRules rules = FontRules.parse("");
        assertEquals("", rules.title());
        assertEquals("", rules.motto());
        assertTrue(rules.groups().isEmpty());
    }
}
