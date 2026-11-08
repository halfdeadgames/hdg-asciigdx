package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns rules file text into a {@link RulesFile}, checking every key and reference on the way. */
final class RulesFileParser {

    private static final String ALL_GROUP = "all";
    private static final Pattern LINE_IN_MESSAGE = Pattern.compile("line (\\d+)");
    private static final List<String> TOP_LEVEL_KEYS = List.of("groups", "rules");
    private static final List<String> RULE_KEYS =
            concat(List.of("name", "check", "appliesTo", "except"), CheckSettingsParser.SETTING_KEYS);
    private static final List<String> EXCEPTION_KEYS = List.of("group", "glyphs", "why");

    private final Map<String, GlyphSet> groups = new HashMap<>();
    private final Set<String> ruleNames = new HashSet<>();

    private final CheckSettingsParser settingsParser;

    private RulesFileParser(int cellWidth, int cellHeight) {
        settingsParser = new CheckSettingsParser(cellWidth, cellHeight);
    }

    static RulesFile parse(String json, int cellWidth, int cellHeight) {
        JsonValue root = readRoot(json);
        var parser = new RulesFileParser(cellWidth, cellHeight);
        checkKnownKeys(root, TOP_LEVEL_KEYS, "The file");
        parser.readGroups(root.get("groups"));
        return new RulesFile(parser.readRules(root.get("rules")));
    }

    private static JsonValue readRoot(String json) {
        JsonValue root;
        try {
            root = new JsonReader().parse(json);
        } catch (SerializationException syntaxError) {
            throw new RulesFileException(describeSyntaxError(syntaxError));
        }
        if (root == null || !root.isObject()) {
            throw new RulesFileException("The file must hold one object with \"groups\" and \"rules\"");
        }
        return root;
    }

    private static String describeSyntaxError(SerializationException syntaxError) {
        String detail = String.valueOf(syntaxError.getMessage());
        Matcher line = LINE_IN_MESSAGE.matcher(detail);
        if (line.find()) {
            return "Line " + line.group(1) + ": the file is not valid JSON (" + detail + ")";
        }
        return "The file is not valid JSON (" + detail + ")";
    }

    private void readGroups(JsonValue groupsValue) {
        if (groupsValue == null) {
            return;
        }
        if (!groupsValue.isObject()) {
            throw new RulesFileException("\"groups\" must be an object that names each group");
        }
        for (JsonValue group = groupsValue.child; group != null; group = group.next) {
            if (groups.containsKey(group.name)) {
                throw new RulesFileException("Group \"" + group.name + "\": defined twice");
            }
            groups.put(group.name, readGroup(group));
        }
    }

    private static GlyphSet readGroup(JsonValue group) {
        String label = "Group \"" + group.name + "\"";
        if (group.name.equals(ALL_GROUP)) {
            throw new RulesFileException(label + " cannot be defined: \"all\" is built in and means every glyph");
        }
        if (!group.isArray()) {
            throw new RulesFileException(label + " must be an array of glyph tokens");
        }
        return readGlyphTokens(group, label);
    }

    private static GlyphSet readGlyphTokens(JsonValue tokenArray, String label) {
        List<String> tokens = readStrings(tokenArray, label, "every glyph token must be a quoted string");
        try {
            return GlyphSet.parse(tokens);
        } catch (IllegalArgumentException badToken) {
            throw new RulesFileException(label + ": " + badToken.getMessage());
        }
    }

    private static List<String> readStrings(JsonValue array, String label, String notStringMessage) {
        var strings = new ArrayList<String>();
        for (JsonValue item = array.child; item != null; item = item.next) {
            if (!item.isString()) {
                throw new RulesFileException(label + ": " + notStringMessage);
            }
            strings.add(item.asString());
        }
        return strings;
    }

    private List<GlyphRule> readRules(JsonValue rulesValue) {
        var rules = new ArrayList<GlyphRule>();
        if (rulesValue == null) {
            return rules;
        }
        if (!rulesValue.isArray()) {
            throw new RulesFileException("\"rules\" must be an array of rules");
        }
        int number = 1;
        for (JsonValue rule = rulesValue.child; rule != null; rule = rule.next) {
            rules.add(readRule(rule, number));
            number++;
        }
        return rules;
    }

    private GlyphRule readRule(JsonValue rule, int number) {
        if (!rule.isObject()) {
            throw new RulesFileException("Rule " + number + " must be an object");
        }
        String label = ruleLabel(rule, number);
        checkKnownKeys(rule, RULE_KEYS, label);
        String name = readName(rule, label);
        if (!ruleNames.add(name)) {
            throw new RulesFileException(label + ": another rule already uses this name");
        }
        RuleCheck check = readCheck(rule, label);
        CheckSettings settings = settingsParser.read(rule, check, label);
        GlyphSet appliesTo = readAppliesTo(rule, label);
        return new GlyphRule(name, check, settings, appliesTo, readExceptions(rule, label));
    }

    private static String ruleLabel(JsonValue rule, int number) {
        JsonValue name = rule.get("name");
        if (name == null || !name.isString() || name.asString().isBlank()) {
            return "Rule " + number;
        }
        return "Rule \"" + name.asString() + "\"";
    }

    private static String readName(JsonValue rule, String label) {
        String name = requireString(rule, "name", label);
        if (name.isBlank()) {
            throw new RulesFileException(label + ": \"name\" cannot be blank");
        }
        return name;
    }

    private static RuleCheck readCheck(JsonValue rule, String label) {
        String fileName = requireString(rule, "check", label);
        return RuleCheck.fromFileName(fileName).orElseThrow(() -> new RulesFileException(
                label + ": unknown check \"" + fileName + "\". Known checks: " + knownCheckNames()));
    }

    private static String knownCheckNames() {
        var names = new ArrayList<String>();
        for (RuleCheck check : RuleCheck.values()) {
            names.add(check.fileName());
        }
        return String.join(", ", names);
    }

    private GlyphSet readAppliesTo(JsonValue rule, String label) {
        JsonValue appliesTo = rule.get("appliesTo");
        if (appliesTo == null) {
            throw new RulesFileException(label + ": missing \"appliesTo\"");
        }
        if (!appliesTo.isArray()) {
            throw new RulesFileException(label + ": \"appliesTo\" must be an array of group names");
        }
        if (appliesTo.child == null) {
            throw new RulesFileException(label + ": appliesTo is empty, so the rule would never check anything");
        }
        return resolveGroups(appliesTo, label + ": \"appliesTo\"");
    }

    private GlyphSet resolveGroups(JsonValue groupNames, String label) {
        var sets = new ArrayList<GlyphSet>();
        for (String groupName : readStrings(groupNames, label, "every entry must be a group name")) {
            sets.add(resolveGroup(groupName, label));
        }
        return GlyphSet.union(sets);
    }

    private GlyphSet resolveGroup(String groupName, String label) {
        if (groupName.equals(ALL_GROUP)) {
            return GlyphSet.all();
        }
        GlyphSet group = groups.get(groupName);
        if (group == null) {
            throw new RulesFileException(label + " names the group \"" + groupName
                    + "\", which is not defined. Define it in \"groups\", or use all");
        }
        return group;
    }

    private List<RuleException> readExceptions(JsonValue rule, String ruleLabel) {
        var exceptions = new ArrayList<RuleException>();
        JsonValue exceptValue = rule.get("except");
        if (exceptValue == null) {
            return exceptions;
        }
        if (!exceptValue.isArray()) {
            throw new RulesFileException(ruleLabel + ": \"except\" must be an array of entries");
        }
        int number = 1;
        for (JsonValue entry = exceptValue.child; entry != null; entry = entry.next) {
            exceptions.add(readException(entry, ruleLabel + ": except entry " + number));
            number++;
        }
        return exceptions;
    }

    private RuleException readException(JsonValue entry, String label) {
        if (!entry.isObject()) {
            throw new RulesFileException(label + " must be an object with \"group\" or \"glyphs\", and \"why\"");
        }
        checkKnownKeys(entry, EXCEPTION_KEYS, label);
        GlyphSet glyphs = readExceptionGlyphs(entry, label);
        return new RuleException(glyphs, readWhy(entry, label));
    }

    private GlyphSet readExceptionGlyphs(JsonValue entry, String label) {
        JsonValue group = entry.get("group");
        JsonValue glyphs = entry.get("glyphs");
        if ((group == null) == (glyphs == null)) {
            throw new RulesFileException(label + " needs exactly one of \"group\" or \"glyphs\"");
        }
        if (group != null) {
            return resolveGroup(requireString(entry, "group", label), label + ": \"group\"");
        }
        if (!glyphs.isArray()) {
            throw new RulesFileException(label + ": \"glyphs\" must be an array of glyph tokens");
        }
        return readGlyphTokens(glyphs, label);
    }

    private static String readWhy(JsonValue entry, String label) {
        JsonValue why = entry.get("why");
        if (why == null || !why.isString() || why.asString().isBlank()) {
            throw new RulesFileException(label + " needs a non-blank \"why\" that says why the rule is skipped");
        }
        return why.asString();
    }

    private static String requireString(JsonValue object, String key, String label) {
        JsonValue value = object.get(key);
        if (value == null) {
            throw new RulesFileException(label + ": missing \"" + key + "\"");
        }
        if (!value.isString()) {
            throw new RulesFileException(label + ": \"" + key + "\" must be text");
        }
        return value.asString();
    }

    private static void checkKnownKeys(JsonValue object, List<String> knownKeys, String label) {
        for (JsonValue entry = object.child; entry != null; entry = entry.next) {
            if (!knownKeys.contains(entry.name)) {
                throw new RulesFileException(label + ": unknown key \"" + entry.name
                        + "\". Known keys: " + String.join(", ", knownKeys));
            }
        }
    }

    private static List<String> concat(List<String> first, List<String> second) {
        var all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }
}
