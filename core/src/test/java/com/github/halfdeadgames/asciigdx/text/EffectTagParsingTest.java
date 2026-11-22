package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import com.github.halfdeadgames.asciigdx.zone.EffectRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class EffectTagParsingTest {

    private static final int RED = 0xFF0000FF;
    private static final ColorNames COLORS =
            name -> name.equals("red") ? OptionalInt.of(RED) : OptionalInt.empty();

    private final List<String> applied = new ArrayList<>();
    private final CellEffect effectA = (cell, where, time) -> applied.add("a");
    private final CellEffect effectB = (cell, where, time) -> applied.add("b");
    private Map<String, String> lastSettings;

    private final EffectNames names = (name, settings) -> {
        lastSettings = settings;
        switch (name) {
            case "a":
                return Optional.of(effectA);
            case "b":
                return Optional.of(effectB);
            case "picky":
                throw new IllegalArgumentException("bad setting");
            default:
                return Optional.empty();
        }
    };

    private StyledText parse(String source) {
        return StyledText.parse(source, COLORS, names);
    }

    private List<String> effectsOfRun(StyledText text, int runIndex) {
        applied.clear();
        text.getEffectRuns().get(runIndex).getEffect().apply(null, null, 0f);
        return new ArrayList<>(applied);
    }

    private static void assertRun(StyledText text, int runIndex, int start, int end) {
        EffectRun run = text.getEffectRuns().get(runIndex);
        assertEquals(start, run.getStartLetter(), "start of run " + runIndex);
        assertEquals(end, run.getEndLetterExclusive(), "end of run " + runIndex);
    }

    @Test
    void validTagIsRemovedAndProducesRun() {
        StyledText text = parse("ab[effect:a]cd[/effect]ef");
        assertEquals("abcdef", text.getVisibleText());
        assertEquals(1, text.getEffectRuns().size());
        assertRun(text, 0, 2, 4);
        assertSame(effectA, text.getEffectRuns().get(0).getEffect());
    }

    @Test
    void settingsReachTheNamesInOrder() {
        parse("[effect:a x=1 y=two]q[/effect]");
        assertEquals(List.of("x", "y"), List.copyOf(lastSettings.keySet()));
        assertEquals("1", lastSettings.get("x"));
        assertEquals("two", lastSettings.get("y"));
    }

    @Test
    void textWithoutEffectsHasNoRuns() {
        assertTrue(parse("plain [color:red]text").getEffectRuns().isEmpty());
    }

    @Test
    void runListIsUnmodifiable() {
        StyledText text = parse("[effect:a]x[/effect]");
        assertThrows(UnsupportedOperationException.class, () -> text.getEffectRuns().clear());
    }

    @Test
    void nestedTagsGiveCombinedEffectAndSplitRuns() {
        StyledText text = parse("[effect:a]x[effect:b]y[/effect]z[/effect]w");
        assertEquals("xyzw", text.getVisibleText());
        assertEquals(3, text.getEffectRuns().size());
        assertRun(text, 0, 0, 1);
        assertRun(text, 1, 1, 2);
        assertRun(text, 2, 2, 3);
        assertEquals(List.of("a"), effectsOfRun(text, 0));
        assertEquals(List.of("a", "b"), effectsOfRun(text, 1));
        assertEquals(List.of("a"), effectsOfRun(text, 2));
    }

    @Test
    void closerEndsMostRecentEffect() {
        StyledText text = parse("[effect:a][effect:b]x[/effect]y[/effect]z");
        assertEquals(2, text.getEffectRuns().size());
        assertEquals(List.of("a", "b"), effectsOfRun(text, 0));
        assertEquals(List.of("a"), effectsOfRun(text, 1));
    }

    @Test
    void emptyRangesAreDropped() {
        StyledText text = parse("[effect:a][/effect]x[effect:b][effect:a][/effect][/effect]");
        assertEquals("x", text.getVisibleText());
        assertTrue(text.getEffectRuns().isEmpty());
    }

    @Test
    void unclosedEffectRunsToTheEnd() {
        StyledText text = parse("a[effect:a]bc");
        assertEquals(1, text.getEffectRuns().size());
        assertRun(text, 0, 1, 3);
    }

    @Test
    void unknownEffectStaysVisible() {
        StyledText text = parse("[effect:nope]x[/effect]");
        assertEquals("[effect:nope]x[/effect]", text.getVisibleText());
        assertTrue(text.getEffectRuns().isEmpty());
    }

    @Test
    void namesThatRejectSettingsLeaveTagVisible() {
        StyledText text = parse("[effect:picky z=1]x");
        assertEquals("[effect:picky z=1]x", text.getVisibleText());
        assertTrue(text.getEffectRuns().isEmpty());
    }

    @Test
    void malformedTagsStayVisible() {
        String[] sources = {"[effect:]x", "[effect:a q]x", "[effect:a =1]x", "[effect:a q=]x",
                "[effect:a q=1 q=2]x", "[effect:a  q=1]x", "[effect:a ]x", "[effect:a"};
        for (String source : sources) {
            StyledText text = parse(source);
            assertEquals(source, text.getVisibleText(), source);
            assertTrue(text.getEffectRuns().isEmpty(), source);
        }
    }

    @Test
    void closerWithNothingOpenStaysVisible() {
        StyledText text = parse("a[/effect]b[effect:a]c[/effect][/effect]");
        assertEquals("a[/effect]bc[/effect]", text.getVisibleText());
        assertEquals(1, text.getEffectRuns().size());
        assertRun(text, 0, 11, 12);
    }

    @Test
    void effectsMixWithColorTags() {
        StyledText text = parse("[color:red]a[effect:a]b[/color]c[/effect]d");
        assertEquals("abcd", text.getVisibleText());
        assertTrue(text.hasForegroundAt(0));
        assertTrue(text.hasForegroundAt(1));
        assertEquals(false, text.hasForegroundAt(2));
        assertEquals(1, text.getEffectRuns().size());
        assertRun(text, 0, 1, 3);
    }

    @Test
    void escapedEffectTagIsLiteral() {
        String escaped = StyledText.escape("[effect:a]x[/effect]");
        StyledText text = parse(escaped);
        assertEquals("[effect:a]x[/effect]", text.getVisibleText());
        assertTrue(text.getEffectRuns().isEmpty());
    }

    @Test
    void parseWithoutEffectNamesKeepsTagsVisible() {
        StyledText text = StyledText.parse("[effect:a]x[/effect]", COLORS);
        assertEquals("[effect:a]x[/effect]", text.getVisibleText());
        assertTrue(text.getEffectRuns().isEmpty());
    }

    @Test
    void noneKnowsNothing() {
        assertTrue(EffectNames.NONE.findEffect("wave", Map.of()).isEmpty());
    }

    @Test
    void realRegistryReadsSettings() {
        StyledText text = StyledText.parse(
                "[effect:wave amplitude=0.5 period=2 phase=columns:3]ab[/effect]", COLORS,
                EffectRegistry.withBuiltIns());
        assertEquals("ab", text.getVisibleText());
        assertEquals(1, text.getEffectRuns().size());
        assertRun(text, 0, 0, 2);
    }

    @Test
    void realRegistryLeavesBadTagsVisible() {
        EffectNames registry = EffectRegistry.withBuiltIns();
        String[] sources = {"[effect:wave speed=1]x", "[effect:wave amplitude=abc]x",
                "[effect:wave amplitude=-1]x", "[effect:wave phase=zigzag]x",
                "[effect:wave phase=columns:0]x", "[effect:Wave]x"};
        for (String source : sources) {
            StyledText text = StyledText.parse(source, COLORS, registry);
            assertEquals(source, text.getVisibleText(), source);
            assertTrue(text.getEffectRuns().isEmpty(), source);
        }
    }
}
