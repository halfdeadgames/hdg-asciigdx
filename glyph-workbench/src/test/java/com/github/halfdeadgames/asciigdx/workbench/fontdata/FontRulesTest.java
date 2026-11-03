package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void parsesRealSaiksisttRules() throws IOException {
        String repoRoot = System.getProperty("workbench.repoRoot");
        assertNotNull(repoRoot, "workbench.repoRoot must be set by the build");
        FontRules rules = FontRules.parse(Files.readString(Path.of(repoRoot, "fonts/saiksistt/rules.md"), StandardCharsets.UTF_8));
        assertEquals("Saiksistt", rules.title());
        assertTrue(rules.motto().startsWith("Saiksistt is the Blackfoot word"));
        assertTrue(rules.motto().endsWith("readable at every size."));
        assertNotNull(rules.group(FontRules.CORE));
        assertNotNull(rules.group(FontRules.UPPERCASE));
        assertNotNull(rules.group(FontRules.LOWERCASE));
        assertNotNull(rules.group(FontRules.DIGITS));
        assertNotNull(rules.group(FontRules.PUNCTUATION));
        assertNotNull(rules.group("At 16 and above"));
        FontRules.Group core = rules.group(FontRules.CORE);
        assertEquals(6, core.bullets().stream().filter(b -> !b.sub()).count());
        assertEquals(6, core.bullets().stream().filter(FontRules.Bullet::sub).count());
        assertFalse(core.bullets().get(0).sub());
        assertTrue(core.bullets().get(2).sub());
        assertEquals("Row 0: accents, the dots of i and j, and lowercase ascenders.", core.bullets().get(2).text());
        assertEquals("Lowercase", rules.groupFor('g').name());
    }
}
