package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkinSchemaTest {

    private static final Path DEFAULT_SKIN_PATH = Path.of("..", "assets", "default-skin.json");
    private static final String SUBJECT = "subject";
    private static final String UNKNOWN_KEY = "zzzUnknown";
    private static final List<String> FILE_ORDER = List.of("colors", "labels", "buttons",
            "menuBars", "checkBoxes", "radioButtons", "textFields", "progressBars", "sliders",
            "scrollPanes", "windows", "dialogs");

    @Test
    void sectionNamesAreInFileOrder() {
        assertEquals(FILE_ORDER, SkinSchema.getSectionNames());
    }

    @Test
    void everySectionHasKeysExceptColors() {
        assertTrue(SkinSchema.getKeys("colors").isEmpty());
        for (String section : FILE_ORDER.subList(1, FILE_ORDER.size())) {
            assertFalse(SkinSchema.getKeys(section).isEmpty(), section);
        }
    }

    @Test
    void unknownSectionHasNoSchema() {
        assertThrows(IllegalArgumentException.class, () -> SkinSchema.getKeys("fonts"));
    }

    @Test
    void everyKeyHasATypeAndADescription() {
        for (String section : FILE_ORDER) {
            for (SkinKeyDefinition key : collectKeys(SkinSchema.getKeys(section))) {
                assertNotNull(key.type(), section + "." + key.name());
                assertFalse(key.description().isBlank(), section + "." + key.name());
                assertFalse(key.defaultDescription().isBlank(), section + "." + key.name());
            }
        }
    }

    @Test
    void keyOrderIsTheOrderOfTheKnownKeysList() {
        assertEquals(List.of("foreground", "background"), SkinSchema.LABEL.getKeyNames());
        assertEquals(List.of("normal", "pointerOver", "pressed", "disabled", "focused",
                "horizontalPadding"), SkinSchema.BUTTON.getKeyNames());
        assertEquals(List.of("window", "message", "buttons", "textField", "dimColor",
                "dimAmount"), SkinSchema.getEntrySchema("dialogs").getKeyNames());
        assertEquals(List.of("trackGlyph", "knobGlyph", "track", "knob"),
                SkinSchema.getEntrySchema("sliders").getKeyNames());
    }

    @Test
    void everyKeyInTheDefaultSkinIsInTheSchema() throws IOException {
        String json = Files.readString(DEFAULT_SKIN_PATH, StandardCharsets.UTF_8);
        JsonValue root = new JsonReader().parse(json);

        for (JsonValue section = root.child; section != null; section = section.next) {
            assertTrue(FILE_ORDER.contains(section.name), "section " + section.name);
            if (section.name.equals("colors")) {
                continue;
            }
            for (JsonValue entry = section.child; entry != null; entry = entry.next) {
                assertKeysInSchema(entry, SkinSchema.getEntrySchema(section.name),
                        section.name + "." + entry.name);
            }
        }
    }

    @Test
    void everySchemaKeyIsAcceptedByTheReader() {
        for (String section : FILE_ORDER.subList(1, FILE_ORDER.size())) {
            SkinObjectSchema schema = SkinSchema.getEntrySchema(section);
            for (SkinKeyDefinition key : schema.keys()) {
                String json = skinWithSubject(section, sampleObject(schema, key.name()));

                assertDoesNotThrow(() -> Skin.fromJson(json), section + "." + key.name());
            }
        }
    }

    @Test
    void everyNestedSchemaKeyIsAcceptedByTheReader() {
        for (String section : FILE_ORDER.subList(1, FILE_ORDER.size())) {
            SkinObjectSchema schema = SkinSchema.getEntrySchema(section);
            for (SkinKeyDefinition key : schema.keys()) {
                if (key.type() instanceof SkinKeyType.Group group) {
                    assertNestedKeysAccepted(section, schema, key, group.schema());
                }
            }
        }
    }

    @Test
    void everyRequiredKeyIsReportedWhenMissing() {
        for (String section : FILE_ORDER.subList(1, FILE_ORDER.size())) {
            SkinObjectSchema schema = SkinSchema.getEntrySchema(section);
            for (SkinKeyDefinition key : schema.keys()) {
                if (!key.required()) {
                    continue;
                }
                String json = skinWithSubject(section, sampleObjectWithout(schema, key.name()));

                var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
                assertTrue(thrown.getMessage().contains("missing \"" + key.name() + "\""),
                        thrown.getMessage());
            }
        }
    }

    @Test
    void unknownKeysAreRejectedWithTheSchemaKeyList() {
        for (String section : FILE_ORDER.subList(1, FILE_ORDER.size())) {
            SkinObjectSchema schema = SkinSchema.getEntrySchema(section);
            String entry = withExtraKey(sampleObject(schema, null), UNKNOWN_KEY);
            String json = skinWithSubject(section, entry);

            var thrown = assertThrows(SkinFileException.class, () -> Skin.fromJson(json));
            assertTrue(thrown.getMessage().contains("unknown key \"" + UNKNOWN_KEY + "\""),
                    thrown.getMessage());
            assertTrue(thrown.getMessage().contains("Known keys: "
                    + String.join(", ", schema.getKeyNames())), thrown.getMessage());
        }
    }

    private static List<SkinKeyDefinition> collectKeys(List<SkinKeyDefinition> keys) {
        var all = new ArrayList<SkinKeyDefinition>();
        for (SkinKeyDefinition key : keys) {
            all.add(key);
            if (key.type() instanceof SkinKeyType.Group group) {
                all.addAll(collectKeys(group.schema().keys()));
            }
        }
        return all;
    }

    private static void assertKeysInSchema(JsonValue object, SkinObjectSchema schema,
            String path) {
        for (JsonValue member = object.child; member != null; member = member.next) {
            SkinKeyDefinition key = findKey(schema, member.name);
            assertNotNull(key, path + "." + member.name + " is not in the schema");
            assertNestedKeysInSchema(member, key.type(), path + "." + member.name);
        }
    }

    private static void assertNestedKeysInSchema(JsonValue value, SkinKeyType type,
            String path) {
        if (type instanceof SkinKeyType.Group group) {
            assertKeysInSchema(value, group.schema(), path);
        }
        if (type instanceof SkinKeyType.StyleReference reference && value.isObject()) {
            assertKeysInSchema(value, getInlineSchema(reference.kind()), path);
        }
    }

    private static SkinObjectSchema getInlineSchema(SkinStyleKind kind) {
        return kind == SkinStyleKind.LABEL ? SkinSchema.LABEL : SkinSchema.BUTTON;
    }

    private static SkinKeyDefinition findKey(SkinObjectSchema schema, String name) {
        for (SkinKeyDefinition key : schema.keys()) {
            if (key.name().equals(name)) {
                return key;
            }
        }
        return null;
    }

    private static void assertNestedKeysAccepted(String section, SkinObjectSchema outer,
            SkinKeyDefinition groupKey, SkinObjectSchema inner) {
        for (SkinKeyDefinition nestedKey : inner.keys()) {
            String nested = sampleObject(inner, nestedKey.name());
            String entry = sampleObject(outer, groupKey.name()).replace(
                    "\"" + groupKey.name() + "\": " + sampleObject(inner, null),
                    "\"" + groupKey.name() + "\": " + nested);
            String json = skinWithSubject(section, entry);

            assertDoesNotThrow(() -> Skin.fromJson(json),
                    section + "." + groupKey.name() + "." + nestedKey.name());
        }
    }

    private static String withExtraKey(String nonEmptyObjectJson, String extraKey) {
        return nonEmptyObjectJson.substring(0, nonEmptyObjectJson.length() - 1) + ", \""
                + extraKey + "\": 1}";
    }

    private static String sampleObject(SkinObjectSchema schema, String extraKey) {
        var members = new ArrayList<String>();
        for (SkinKeyDefinition key : schema.keys()) {
            if (key.required() || isSelectedKey(schema, key.name(), extraKey)) {
                members.add("\"" + key.name() + "\": " + sampleValue(key.type()));
            }
        }
        return "{" + String.join(", ", members) + "}";
    }

    private static String sampleObjectWithout(SkinObjectSchema schema, String omittedKey) {
        var members = new ArrayList<String>();
        for (SkinKeyDefinition key : schema.keys()) {
            if (key.required() && !key.name().equals(omittedKey)) {
                members.add("\"" + key.name() + "\": " + sampleValue(key.type()));
            }
        }
        return "{" + String.join(", ", members) + "}";
    }

    private static boolean isSelectedKey(SkinObjectSchema schema, String name, String extraKey) {
        if (extraKey == null) {
            return false;
        }
        if (name.equals(extraKey)) {
            return true;
        }
        for (SkinObjectSchema.KeyPair pair : schema.togetherPairs()) {
            boolean isPartner = (pair.first().equals(extraKey) && pair.second().equals(name))
                    || (pair.second().equals(extraKey) && pair.first().equals(name));
            if (isPartner) {
                return true;
            }
        }
        return false;
    }

    private static String sampleValue(SkinKeyType type) {
        if (type instanceof SkinKeyType.Color) {
            return "\"c\"";
        }
        if (type instanceof SkinKeyType.Glyph) {
            return "\"x\"";
        }
        if (type instanceof SkinKeyType.Mark) {
            return "\"m\"";
        }
        if (type instanceof SkinKeyType.WholeNumber wholeNumber) {
            return String.valueOf(wholeNumber.minimum() + 1);
        }
        if (type instanceof SkinKeyType.DecimalNumber decimalNumber) {
            return String.valueOf(decimalNumber.minimum());
        }
        if (type instanceof SkinKeyType.Flag) {
            return "true";
        }
        if (type instanceof SkinKeyType.Choice choice) {
            return "\"" + choice.options().get(0) + "\"";
        }
        if (type instanceof SkinKeyType.StyleReference reference) {
            return "\"" + getSampleStyleName(reference.kind()) + "\"";
        }
        if (type instanceof SkinKeyType.Group group) {
            return sampleObject(group.schema(), null);
        }
        throw new IllegalStateException("Unhandled key type " + type);
    }

    private static String getSampleStyleName(SkinStyleKind kind) {
        return switch (kind) {
            case LABEL -> "l";
            case BUTTON -> "b";
            case WINDOW -> "w";
            case TEXT_FIELD -> "t";
        };
    }

    private static String skinWithSubject(String section, String subjectJson) {
        Map<String, List<String>> entries = new LinkedHashMap<>();
        entries.put("colors", new ArrayList<>(List.of("\"c\": \"#000000\"")));
        entries.put("labels", new ArrayList<>(List.of(
                "\"l\": {\"foreground\": \"c\", \"background\": \"c\"}")));
        entries.put("buttons", new ArrayList<>(List.of("\"b\": {\"normal\": \"l\"}")));
        entries.put("textFields", new ArrayList<>(List.of("\"t\": {\"normal\": \"l\"}")));
        entries.put("windows", new ArrayList<>(List.of("\"w\": {\"box\": \"single\", "
                + "\"border\": \"l\", \"title\": \"l\", \"background\": \"c\"}")));
        entries.computeIfAbsent(section, name -> new ArrayList<>())
                .add("\"" + SUBJECT + "\": " + subjectJson);
        var sections = new ArrayList<String>();
        for (Map.Entry<String, List<String>> sectionEntries : entries.entrySet()) {
            sections.add("\"" + sectionEntries.getKey() + "\": {"
                    + String.join(", ", sectionEntries.getValue()) + "}");
        }
        return "{" + String.join(", ", sections) + "}";
    }
}
