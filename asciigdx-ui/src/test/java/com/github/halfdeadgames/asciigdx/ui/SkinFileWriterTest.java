package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SkinFileWriterTest {

    private static final Path DEFAULT_SKIN_PATH = Path.of("..", "assets", "default-skin.json");
    private static final long LARGE_WHOLE_NUMBER = 9_007_199_254_740_993L;

    private record StarStyle(int color, LabelStyle look) {
    }

    private static final SkinSectionReader<StarStyle> STAR_READER = (entry, context) ->
            new StarStyle(context.readColor(entry.get("color")),
                    context.readLook(entry.get("look")));

    private static String defaultSkinText;

    @BeforeAll
    static void loadDefaultSkinText() throws IOException {
        defaultSkinText = Files.readString(DEFAULT_SKIN_PATH, StandardCharsets.UTF_8);
    }

    private static JsonValue parse(String json) {
        return new JsonReader().parse(json);
    }

    private static void assertSameTree(JsonValue expected, JsonValue actual) {
        assertEquals(expected.type(), actual.type());
        assertEquals(expected.name, actual.name);
        switch (expected.type()) {
            case object, array -> assertSameChildren(expected, actual);
            case stringValue -> assertEquals(expected.asString(), actual.asString());
            case doubleValue -> assertEquals(expected.asDouble(), actual.asDouble());
            case longValue -> assertEquals(expected.asLong(), actual.asLong());
            case booleanValue -> assertEquals(expected.asBoolean(), actual.asBoolean());
            case nullValue -> { }
        }
    }

    private static void assertSameChildren(JsonValue expected, JsonValue actual) {
        assertEquals(expected.size, actual.size);
        JsonValue expectedChild = expected.child;
        JsonValue actualChild = actual.child;
        while (expectedChild != null) {
            assertSameTree(expectedChild, actualChild);
            expectedChild = expectedChild.next;
            actualChild = actualChild.next;
        }
    }

    private static JsonValue singleValueTree(JsonValue value) {
        JsonValue section = new JsonValue(JsonValue.ValueType.object);
        section.addChild("value", value);
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("custom", section);
        return root;
    }

    private static JsonValue writeAndParseBack(JsonValue tree) {
        return parse(SkinFileWriter.write(tree));
    }

    @Test
    void defaultSkinRoundTripsToAnEqualTree() {
        JsonValue original = parse(defaultSkinText);

        assertSameTree(original, writeAndParseBack(original));
    }

    @Test
    void defaultSkinLoadsToTheSameLookups() {
        Skin original = Skin.fromJson(defaultSkinText);
        Skin rewritten = Skin.fromJson(SkinFileWriter.write(parse(defaultSkinText)));

        for (String color : new String[] {"black", "gold", "transparent", "hotPink"}) {
            assertEquals(original.getRgba(color), rewritten.getRgba(color));
        }
        assertEquals(original.getLabelStyle("title"), rewritten.getLabelStyle("title"));
        assertEquals(original.getButtonStyle("button"), rewritten.getButtonStyle("button"));
        assertEquals(original.getWindowStyle("window"), rewritten.getWindowStyle("window"));
        assertEquals(original.getTextFieldStyle("default"),
                rewritten.getTextFieldStyle("default"));
        assertEquals(original.getDialogStyle("dialog"), rewritten.getDialogStyle("dialog"));
        assertEquals(original.getSliderStyle("slider"), rewritten.getSliderStyle("slider"));
        assertEquals(original.toString(), rewritten.toString());
    }

    @Test
    void writingTwiceGivesTheSameText() {
        String once = SkinFileWriter.write(parse(defaultSkinText));
        String twice = SkinFileWriter.write(parse(once));

        assertEquals(once, twice);
        assertTrue(once.endsWith("}\n"));
    }

    @Test
    void sectionsComeOutInSchemaOrderWithCustomSectionsLast() {
        String shuffled = """
                {
                  "stars": { "first": { "color": "navy", "look": "plain" } },
                  "labels": { "plain": { "foreground": "white", "background": "navy" } },
                  "colors": { "white": "#FFFFFF", "navy": "#203070" },
                  "moons": {}
                }""";

        String written = SkinFileWriter.write(parse(shuffled));

        int colors = written.indexOf("\"colors\"");
        int labels = written.indexOf("\"labels\"");
        int stars = written.indexOf("\"stars\"");
        int moons = written.indexOf("\"moons\"");
        assertTrue(colors >= 0 && colors < labels, written);
        assertTrue(labels < stars && stars < moons, written);
        Skin skin = SkinLoader.create().withSection("stars", STAR_READER)
                .withSection("moons", STAR_READER).fromJson(written);
        assertEquals(0x203070FF, skin.getCustomStyle("stars", "first", StarStyle.class).color());
    }

    @Test
    void keysInsideAnEntryKeepTheirOrder() {
        String text = "{\"labels\": {\"b\": {\"background\": \"#000000\","
                + " \"foreground\": \"#FFFFFF\"}, \"a\": {\"foreground\": \"#FFFFFF\","
                + " \"background\": \"#000000\"}}}";

        String written = SkinFileWriter.write(parse(text));

        assertTrue(written.indexOf("\"b\"") < written.indexOf("\"a\""), written);
        assertTrue(written.indexOf("\"background\"") < written.indexOf("\"foreground\""),
                written);
    }

    @Test
    void stringsWithSpecialCharactersSurvive() {
        String[] samples = {"say \"hi\"", "back\\slash", "line\nbreak", "tab\there",
            "\u0001\u001f", "▏ █ é", "😀", "", "  padded  "};
        for (String sample : samples) {
            JsonValue tree = singleValueTree(new JsonValue(sample));

            assertSameTree(tree, writeAndParseBack(tree));
        }
    }

    @Test
    void nonAsciiCharactersAreWrittenAsIs() {
        String written = SkinFileWriter.write(singleValueTree(new JsonValue("▏😀")));

        assertTrue(written.contains("▏😀"), written);
        assertTrue(!written.contains("\\u258f"), written);
    }

    @Test
    void numbersKeepTheirKindAndValue() {
        JsonValue numbers = new JsonValue(JsonValue.ValueType.object);
        numbers.addChild("zero", new JsonValue(0L));
        numbers.addChild("one", new JsonValue(1L));
        numbers.addChild("minusOne", new JsonValue(-1L));
        numbers.addChild("large", new JsonValue(LARGE_WHOLE_NUMBER));
        numbers.addChild("quarter", new JsonValue(0.25));
        numbers.addChild("oneAndAHalf", new JsonValue(1.5));
        numbers.addChild("decimalOne", new JsonValue(1.0));
        numbers.addChild("tiny", new JsonValue(1.0e-7));
        numbers.addChild("huge", new JsonValue(1.0e20));
        JsonValue tree = new JsonValue(JsonValue.ValueType.object);
        tree.addChild("custom", numbers);

        JsonValue back = writeAndParseBack(tree).get("custom");

        assertSameTree(numbers, back);
        assertTrue(back.get("one").isLong());
        assertTrue(back.get("decimalOne").isDouble());
        assertEquals(LARGE_WHOLE_NUMBER, back.get("large").asLong());
    }

    @Test
    void wholeNumberAndDecimalTextStayDistinct() {
        String written = SkinFileWriter.write(parse("{\"custom\": {\"a\": 1, \"b\": 1.0}}"));

        assertTrue(written.contains("\"a\": 1,") || written.contains("\"a\": 1 "), written);
        assertTrue(written.contains("\"b\": 1.0"), written);
    }

    @Test
    void booleansNullsAndArraysSurvive() {
        JsonValue tree = parse("{\"custom\": {\"yes\": true, \"no\": false, \"nothing\": null,"
                + " \"list\": [1, \"two\", [3.5], {\"k\": true}], \"empty\": {}, \"none\": []}}");

        assertSameTree(tree, writeAndParseBack(tree));
    }

    @Test
    void nonObjectRootThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(new JsonValue("text")));
        assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(new JsonValue(JsonValue.ValueType.array)));
        assertThrows(IllegalArgumentException.class, () -> SkinFileWriter.write(null));
    }

    private static JsonValue twoEntryTree(int valueLength) {
        JsonValue section = new JsonValue(JsonValue.ValueType.object);
        for (String entryName : new String[] {"p", "q"}) {
            JsonValue entry = new JsonValue(JsonValue.ValueType.object);
            entry.addChild("v", new JsonValue("x".repeat(valueLength)));
            section.addChild(entryName, entry);
        }
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("custom", section);
        return root;
    }

    private static void assertNoLineLongerThan100(String written) {
        for (String line : written.split("\n")) {
            assertTrue(line.length() <= 100, line.length() + " columns: " + line);
        }
    }

    @Test
    void lineWidthLimitCountsTheTrailingCommaOfANonLastMember() {
        // The last entry line is 20 + n columns; a non-last one adds a comma.
        String at99 = SkinFileWriter.write(twoEntryTree(79));
        String at100 = SkinFileWriter.write(twoEntryTree(80));
        String at101 = SkinFileWriter.write(twoEntryTree(81));

        assertTrue(at99.contains("\"p\": { \"v\"") && at99.contains("\"q\": { \"v\""), at99);
        assertTrue(!at100.contains("\"p\": { \"v\"") && at100.contains("\"q\": { \"v\""), at100);
        assertTrue(!at101.contains("\"p\": { \"v\"") && !at101.contains("\"q\": { \"v\""),
                at101);
        assertNoLineLongerThan100(at99);
        assertNoLineLongerThan100(at100);
        assertNoLineLongerThan100(at101);
    }

    @Test
    void loneSurrogatesAreEscapedAndSurvivePairsStayRaw() {
        String lone = "a\uD800b\uDC00c\uDC00\uD800";
        String pair = "😀";
        JsonValue tree = singleValueTree(new JsonValue(lone + pair));

        String written = SkinFileWriter.write(tree);
        String throughBytes = new String(written.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(written.contains("\\ud800") && written.contains("\\udc00"), written);
        assertTrue(written.contains(pair), written);
        assertSameTree(tree, parse(throughBytes));
    }

    @Test
    void duplicateSectionNamesThrowWithTheName() {
        var builtInTwice = assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(parse("{\"labels\": {}, \"labels\": {}}")));
        var customTwice = assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(parse("{\"stars\": {}, \"stars\": {}}")));

        assertTrue(builtInTwice.getMessage().contains("labels"), builtInTwice.getMessage());
        assertTrue(customTwice.getMessage().contains("stars"), customTwice.getMessage());
    }

    @Test
    void sectionNamesDifferingOnlyByCaseThrow() {
        var againstBuiltIn = assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(parse("{\"Labels\": {}}")));
        var builtInPair = assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(parse("{\"labels\": {}, \"LABELS\": {}}")));
        var customPair = assertThrows(IllegalArgumentException.class,
                () -> SkinFileWriter.write(parse("{\"stars\": {}, \"Stars\": {}}")));

        assertTrue(againstBuiltIn.getMessage().contains("Labels"), againstBuiltIn.getMessage());
        assertTrue(builtInPair.getMessage().contains("LABELS"), builtInPair.getMessage());
        assertTrue(customPair.getMessage().contains("Stars"), customPair.getMessage());
    }

    @Test
    void memberKeysDoNotDependOnTheParentField() {
        JsonValue tree = parse("{\"custom\": {\"entry\": {\"a\": {\"b\": 1, \"c\": [1, 2]}}}}");
        JsonValue entry = tree.get("custom").get("entry");
        JsonValue nested = entry.get("a");
        nested.child.parent = null;
        nested.child.next.parent = null;
        nested.get("c").child.parent = tree;
        nested.get("c").child.next.parent = tree;
        entry.child.parent = null;
        JsonValue expected = parse("{\"custom\": {\"entry\": {\"a\": {\"b\": 1, \"c\": [1, 2]}}}}");

        assertSameTree(expected, writeAndParseBack(tree));
    }

    @Test
    void numbersJsonCannotHoldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SkinFileWriter.write(
                singleValueTree(new JsonValue(Double.NaN))));
        assertThrows(IllegalArgumentException.class, () -> SkinFileWriter.write(
                singleValueTree(new JsonValue(Double.POSITIVE_INFINITY))));
    }
}
