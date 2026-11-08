package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RulesFileTest {

    private static final int CELL = 16;

    private static final String README_EXAMPLE = """
            {
              groups: {
                capitals:   [ "A-Z" ],
                lowercase:  [ "a-z" ],
                digits:     [ "0-9" ],
                descenders: [ "g", "j", "p", "q", "y", ",", ";" ],
                tiling:     [ "U+2500-U+259F" ]
              },
              rules: [
                {
                  name: "Blank ring",
                  check: ring,
                  size: 1,
                  appliesTo: [ all ],
                  except: [ { group: tiling, why: "joins its neighbours" } ]
                },
                {
                  name: "Centered across",
                  check: centeredX,
                  appliesTo: [ capitals, lowercase, digits ]
                },
                {
                  name: "Nothing below the baseline",
                  check: noInkRows,
                  rows: [ "12-15" ],
                  appliesTo: [ capitals, lowercase, digits ],
                  except: [ { group: descenders, why: "tails drop into rows 12-14" } ]
                }
              ]
            }
            """;

    private static String errorOf(String json) {
        return assertThrows(RulesFileException.class, () -> RulesFile.parse(json, CELL, CELL)).getMessage();
    }

    private static String errorOfRule(String ruleBody) {
        return errorOf("{ groups: { caps: [ \"A-Z\" ] }, rules: [ { " + ruleBody + " } ] }");
    }

    private static CheckSettings settingsOf(String ruleBody) {
        return onlyRule("{ rules: [ { name: r, appliesTo: [ all ], " + ruleBody + " } ] }").settings();
    }

    private static GlyphRule onlyRule(String json) {
        var rules = RulesFile.parse(json, CELL, CELL).rules();
        assertEquals(1, rules.size());
        return rules.get(0);
    }

    @Test
    void readmeExampleParses() {
        var rules = RulesFile.parse(README_EXAMPLE, CELL, CELL).rules();
        assertEquals(3, rules.size());
        assertEquals("Blank ring", rules.get(0).name());
        assertEquals(RuleCheck.RING, rules.get(0).check());
        assertEquals(RuleCheck.CENTERED_X, rules.get(1).check());
        assertEquals(RuleCheck.NO_INK_ROWS, rules.get(2).check());
        assertEquals("joins its neighbours", rules.get(0).exceptions().get(0).why());
    }

    @Test
    void groupNamesFollowFileOrder() {
        var names = java.util.List.of("zulu", "alpha", "mike", "bravo", "yankee", "charlie", "x1", "b2", "q9", "k");
        var body = new StringBuilder();
        for (String name : names) {
            body.append(name).append(": [ \"A\" ], ");
        }
        var file = RulesFile.parse("{ groups: { " + body + " } }", CELL, CELL);
        assertEquals(names, file.groupNames());
    }

    @Test
    void allIsBuiltInAndNotListed() {
        var file = RulesFile.parse("{ groups: { caps: [ \"A-Z\" ] } }", CELL, CELL);
        assertEquals(java.util.List.of("caps"), file.groupNames());
        assertTrue(file.hasGroup("all"));
        assertTrue(file.isInGroup("all", 'a'));
        assertTrue(file.isInGroup("all", 0x2588));
    }

    @Test
    void definedGroupMatchesOnlyItsGlyphs() {
        var file = RulesFile.parse("{ groups: { caps: [ \"A-Z\" ] } }", CELL, CELL);
        assertTrue(file.hasGroup("caps"));
        assertTrue(file.isInGroup("caps", 'Q'));
        assertFalse(file.isInGroup("caps", 'q'));
    }

    @Test
    void unknownGroupIsFalseForBothQueries() {
        var file = RulesFile.parse("{ groups: { caps: [ \"A-Z\" ] } }", CELL, CELL);
        assertFalse(file.hasGroup("nope"));
        assertFalse(file.isInGroup("nope", 'A'));
    }

    @Test
    void fileWithoutGroupsHasNoGroupNames() {
        assertTrue(RulesFile.parse("{}", CELL, CELL).groupNames().isEmpty());
    }

    @Test
    void groupNamesCannotBeModified() {
        var names = RulesFile.parse("{ groups: { caps: [ \"A-Z\" ] } }", CELL, CELL).groupNames();
        assertThrows(UnsupportedOperationException.class, () -> names.add("extra"));
        assertThrows(UnsupportedOperationException.class, () -> names.remove(0));
    }

    @Test
    void emptyObjectHasNoRules() {
        assertTrue(RulesFile.parse("{}", CELL, CELL).rules().isEmpty());
    }

    @Test
    void readmeExampleSettings() {
        var rules = RulesFile.parse(README_EXAMPLE, CELL, CELL).rules();
        assertEquals(1, rules.get(0).settings().ringSize());
        assertEquals(bits(12, 13, 14, 15), rules.get(2).settings().rows());
        assertTrue(rules.get(2).settings().columns().isEmpty());
        assertEquals(CheckSettings.NONE, rules.get(1).settings());
    }

    private static java.util.BitSet bits(int... lines) {
        var set = new java.util.BitSet();
        for (int line : lines) {
            set.set(line);
        }
        return set;
    }

    @Test
    void rowsMixNumbersAndRanges() {
        assertEquals(bits(0, 5, 6, 7), settingsOf("check: noInkRows, rows: [ 0, \"5-7\" ]").rows());
    }

    @Test
    void quotedSingleNumberIsAccepted() {
        assertEquals(bits(4, 12), settingsOf("check: noInkRows, rows: [ \"4\", \"12\" ]").rows());
    }

    @Test
    void quotedSingleNumberOutsideCellIsRejected() {
        assertEquals("Rule \"r\": row 16 is outside the 16x16 cell (rows 0-15)",
                errorOfRule("name: r, check: noInkRows, rows: [ \"16\" ], appliesTo: [ all ]"));
    }

    @Test
    void columnsAreRead() {
        assertEquals(bits(0, 15), settingsOf("check: noInkColumns, columns: [ 0, 15 ]").columns());
    }

    @Test
    void wholeNumberWrittenWithDecimalPointIsAccepted() {
        assertEquals(bits(3), settingsOf("check: noInkRows, rows: [ 3.0 ]").rows());
    }

    @Test
    void settingsCannotBeChangedFromOutside() {
        var settings = settingsOf("check: noInkRows, rows: [ 3 ]");
        settings.rows().set(9);
        assertEquals(bits(3), settings.rows());
    }

    @Test
    void duplicateGroupIsRejected() {
        assertEquals("Group \"caps\": defined twice",
                errorOf("{ groups: { caps: [ \"A\" ], caps: [ \"B\" ] } }"));
    }

    @Test
    void emptyAppliesToIsRejected() {
        assertEquals("Rule \"r\": appliesTo is empty, so the rule would never check anything",
                errorOfRule("name: r, check: centeredX, appliesTo: []"));
    }

    @Test
    void settingThatTheCheckDoesNotUseIsRejected() {
        assertEquals("Rule \"r\": check noInkRows does not use \"size\"",
                errorOfRule("name: r, check: noInkRows, rows: [ 1 ], size: 1, appliesTo: [ all ]"));
    }

    @Test
    void settingOnCheckWithoutSettingsIsRejected() {
        assertEquals("Rule \"r\": check centeredX does not use \"rows\"",
                errorOfRule("name: r, check: centeredX, rows: [ 1 ], appliesTo: [ all ]"));
    }

    @Test
    void missingSettingIsRejected() {
        assertEquals("Rule \"r\": check noInkRows needs \"rows\"",
                errorOfRule("name: r, check: noInkRows, appliesTo: [ all ]"));
        assertEquals("Rule \"r\": check noInkColumns needs \"columns\"",
                errorOfRule("name: r, check: noInkColumns, appliesTo: [ all ]"));
        assertEquals("Rule \"r\": check ring needs \"size\"",
                errorOfRule("name: r, check: ring, appliesTo: [ all ]"));
    }

    @Test
    void emptyRowsAreRejected() {
        assertEquals("Rule \"r\": \"rows\" must be a non-empty array of numbers or ranges",
                errorOfRule("name: r, check: noInkRows, rows: [], appliesTo: [ all ]"));
    }

    @Test
    void rowOutsideCellIsRejected() {
        assertEquals("Rule \"r\": row 16 is outside the 16x16 cell (rows 0-15)",
                errorOfRule("name: r, check: noInkRows, rows: [ 16 ], appliesTo: [ all ]"));
    }

    @Test
    void rangeEndOutsideCellIsRejected() {
        assertEquals("Rule \"r\": column 20 is outside the 16x16 cell (columns 0-15)",
                errorOfRule("name: r, check: noInkColumns, columns: [ \"14-20\" ], appliesTo: [ all ]"));
    }

    @Test
    void negativeRowIsRejected() {
        assertEquals("Rule \"r\": row -1 is outside the 16x16 cell (rows 0-15)",
                errorOfRule("name: r, check: noInkRows, rows: [ -1 ], appliesTo: [ all ]"));
    }

    @Test
    void backwardsRangeIsRejected() {
        assertEquals("Rule \"r\": row range \"15-12\" goes backwards: write the lower number first",
                errorOfRule("name: r, check: noInkRows, rows: [ \"15-12\" ], appliesTo: [ all ]"));
    }

    @Test
    void badRowEntriesSayWhatIsAllowed() {
        String allowed = " is not a row entry: use a number like 12 or a range like \"12-15\"";
        assertEquals("Rule \"r\": \"abc\"" + allowed,
                errorOfRule("name: r, check: noInkRows, rows: [ \"abc\" ], appliesTo: [ all ]"));
        assertEquals("Rule \"r\": 1.5" + allowed,
                errorOfRule("name: r, check: noInkRows, rows: [ 1.5 ], appliesTo: [ all ]"));
    }

    @Test
    void ringSizeOutsideRangeIsRejected() {
        String message = "Rule \"r\": \"size\" must be a whole number from 1 to 7 in a 16x16 cell, "
                + "so the middle stays free";
        assertEquals(message, errorOfRule("name: r, check: ring, size: 8, appliesTo: [ all ]"));
        assertEquals(message, errorOfRule("name: r, check: ring, size: 0, appliesTo: [ all ]"));
        assertEquals(message, errorOfRule("name: r, check: ring, size: 1.5, appliesTo: [ all ]"));
        assertEquals(message, errorOfRule("name: r, check: ring, size: big, appliesTo: [ all ]"));
    }

    @Test
    void largestRingSizeIsAccepted() {
        assertEquals(7, settingsOf("check: ring, size: 7").ringSize());
    }

    @Test
    void boundsFollowTheCellSize() {
        var json = "{ rules: [ { name: r, check: noInkRows, rows: [ 8 ], appliesTo: [ all ] } ] }";
        var error = assertThrows(RulesFileException.class, () -> RulesFile.parse(json, 16, 8));
        assertEquals("Rule \"r\": row 8 is outside the 16x8 cell (rows 0-7)", error.getMessage());
    }

    @Test
    void commentsAreAllowed() {
        var rule = onlyRule("""
                // leading comment
                { rules: [ /* inline */ { name: "r", check: centeredX, appliesTo: [ all ] } ] }
                """);
        assertEquals("r", rule.name());
    }

    @Test
    void appliesToAllCoversEveryGlyph() {
        var rule = onlyRule("{ rules: [ { name: r, check: centeredX, appliesTo: [ all ] } ] }");
        assertTrue(rule.appliesTo('A'));
        assertTrue(rule.appliesTo(0x2588));
    }

    @Test
    void appliesToJoinsGroups() {
        var rule = onlyRule("{ groups: { a: [\"A\"], b: [\"b\"] }, rules: "
                + "[ { name: r, check: centeredX, appliesTo: [ a, b ] } ] }");
        assertTrue(rule.appliesTo('A'));
        assertTrue(rule.appliesTo('b'));
        assertFalse(rule.appliesTo('c'));
    }

    @Test
    void exceptionByGroupExcludesItsGlyphs() {
        var rule = onlyRule("{ groups: { caps: [\"A-Z\"], odd: [\"Q\"] }, rules: [ { name: r, check: centeredX, "
                + "appliesTo: [ caps ], except: [ { group: odd, why: \"tail\" } ] } ] }");
        assertTrue(rule.appliesTo('A'));
        assertFalse(rule.appliesTo('Q'));
    }

    @Test
    void exceptionByGlyphsExcludesThem() {
        var rule = onlyRule("{ rules: [ { name: r, check: centeredX, appliesTo: [ all ], "
                + "except: [ { glyphs: [ \"U+00B7\" ], why: \"narrow\" } ] } ] }");
        assertFalse(rule.appliesTo(0xB7));
        assertTrue(rule.appliesTo('A'));
    }

    @Test
    void appliesToNamesKeepFileOrderAndAll() {
        var rule = onlyRule("{ groups: { a: [\"A\"], b: [\"b\"] }, rules: "
                + "[ { name: r, check: centeredX, appliesTo: [ b, all, a ] } ] }");
        assertEquals(java.util.List.of("b", "all", "a"), rule.appliesToNames());
    }

    @Test
    void exceptionByGroupKeepsGroupNameAsSource() {
        var rule = onlyRule("{ groups: { odd: [\"Q\"] }, rules: [ { name: r, check: centeredX, "
                + "appliesTo: [ all ], except: [ { group: odd, why: \"tail\" } ] } ] }");
        assertEquals("odd", rule.exceptions().get(0).source());
    }

    @Test
    void exceptionByGlyphsKeepsTokensAsWritten() {
        var rule = onlyRule("{ rules: [ { name: r, check: centeredX, appliesTo: [ all ], except: ["
                + "{ glyphs: [ \"U+00B7\" ], why: \"narrow\" }, { glyphs: [ \"g\", \"j\" ], why: \"tails\" } ] } ] }");
        assertEquals("U+00B7", rule.exceptions().get(0).source());
        assertEquals("g, j", rule.exceptions().get(1).source());
    }

    @Test
    void syntaxErrorIsRewrapped() {
        assertTrue(errorOf("{ rules: [ { name: \"x\" ").contains("not valid JSON"));
    }

    @Test
    void syntaxErrorNamesItsLine() {
        String message = errorOf("{\n  groups: {\n    capitals: [ \"A-Z\" ] ]\n  }\n}");
        assertTrue(message.startsWith("Line 3: "), message);
    }

    @Test
    void nonObjectFileIsRejected() {
        assertEquals("The file must hold one object with \"groups\" and \"rules\"", errorOf("[ 1 ]"));
    }

    @Test
    void unknownTopLevelKeyIsRejected() {
        assertEquals("The file: unknown key \"grops\". Known keys: groups, rules", errorOf("{ grops: {} }"));
    }

    @Test
    void groupNamedAllIsRejected() {
        assertEquals("Group \"all\" cannot be defined: \"all\" is built in and means every glyph",
                errorOf("{ groups: { all: [ \"A\" ] } }"));
    }

    @Test
    void groupThatIsNotAnArrayIsRejected() {
        assertEquals("Group \"caps\" must be an array of glyph tokens", errorOf("{ groups: { caps: \"A-Z\" } }"));
    }

    @Test
    void badTokenNamesTheGroup() {
        assertEquals("Group \"tiling\": \"ZZ\" is not a glyph: use one character like \"A\", a code like "
                + "\"U+0041\", or a range like \"A-Z\"", errorOf("{ groups: { tiling: [ \"ZZ\" ] } }"));
    }

    @Test
    void unknownRuleKeyListsKnownKeys() {
        assertEquals("Rule \"Blank ring\": unknown key \"apliesTo\". Known keys: name, check, appliesTo, except, "
                + "rows, columns, size", errorOfRule("name: \"Blank ring\", check: ring, apliesTo: [ all ]"));
    }

    @Test
    void missingNameIsReportedByNumber() {
        assertEquals("Rule 1: missing \"name\"", errorOfRule("check: ring, appliesTo: [ all ]"));
    }

    @Test
    void missingCheckIsRejected() {
        assertEquals("Rule \"r\": missing \"check\"", errorOfRule("name: r, appliesTo: [ all ]"));
    }

    @Test
    void missingAppliesToIsRejected() {
        assertEquals("Rule \"r\": missing \"appliesTo\"", errorOfRule("name: r, check: centeredX"));
    }

    @Test
    void duplicateNameIsRejected() {
        var json = "{ rules: [ { name: r, check: centeredX, appliesTo: [ all ] }, "
                + "{ name: r, check: centeredX, appliesTo: [ all ] } ] }";
        assertEquals("Rule \"r\": another rule already uses this name", errorOf(json));
    }

    @Test
    void unknownCheckListsKnownChecks() {
        assertEquals("Rule \"r\": unknown check \"circle\". Known checks: noInkRows, noInkColumns, ring, "
                + "centeredX, centeredBoth", errorOfRule("name: r, check: circle, appliesTo: [ all ]"));
    }

    @Test
    void undefinedGroupInAppliesToIsRejected() {
        assertEquals("Rule \"r\": \"appliesTo\" names the group \"nope\", which is not defined. "
                + "Define it in \"groups\", or use all", errorOfRule("name: r, check: centeredX, appliesTo: [ nope ]"));
    }

    @Test
    void exceptionNeedsGroupOrGlyphs() {
        assertEquals("Rule \"r\": except entry 1 needs exactly one of \"group\" or \"glyphs\"",
                errorOfRule("name: r, check: centeredX, appliesTo: [ all ], except: [ { why: \"x\" } ]"));
    }

    @Test
    void exceptionWithBothGroupAndGlyphsIsRejected() {
        assertEquals("Rule \"r\": except entry 1 needs exactly one of \"group\" or \"glyphs\"",
                errorOfRule("name: r, check: centeredX, appliesTo: [ all ], "
                        + "except: [ { group: caps, glyphs: [ \"A\" ], why: \"x\" } ]"));
    }

    @Test
    void exceptionNeedsNonBlankWhy() {
        assertEquals("Rule \"r\": except entry 1 needs a non-blank \"why\" that says why the rule is skipped",
                errorOfRule("name: r, check: centeredX, appliesTo: [ all ], except: [ { group: caps, why: \" \" } ]"));
    }

    @Test
    void exceptionWithUnknownKeyIsRejected() {
        assertEquals("Rule \"r\": except entry 1: unknown key \"reason\". Known keys: group, glyphs, why",
                errorOfRule("name: r, check: centeredX, appliesTo: [ all ], except: [ { group: caps, reason: \"x\" } ]"));
    }

    @Test
    void exceptionWithUndefinedGroupIsRejected() {
        assertEquals("Rule \"r\": except entry 1: \"group\" names the group \"nope\", which is not defined. "
                + "Define it in \"groups\", or use all",
                errorOfRule("name: r, check: centeredX, appliesTo: [ all ], except: [ { group: nope, why: \"x\" } ]"));
    }
}
