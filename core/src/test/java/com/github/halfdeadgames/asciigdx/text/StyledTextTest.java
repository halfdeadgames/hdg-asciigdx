package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class StyledTextTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;
    private static final ColorNames NAMES = name -> switch (name) {
        case "red" -> OptionalInt.of(RED);
        case "blue" -> OptionalInt.of(BLUE);
        default -> OptionalInt.empty();
    };

    private static StyledText parse(String source) {
        return StyledText.parse(source, NAMES);
    }

    private static void assertForegrounds(StyledText text, Integer... expected) {
        assertEquals(expected.length, text.getVisibleLength());
        for (int index = 0; index < expected.length; index++) {
            if (expected[index] == null) {
                assertFalse(text.hasForegroundAt(index), "letter " + index);
            } else {
                assertTrue(text.hasForegroundAt(index), "letter " + index);
                assertEquals(expected[index].intValue(), text.getForegroundRgbaAt(index),
                        "letter " + index);
            }
        }
    }

    private static void assertNoStyle(StyledText text) {
        for (int index = 0; index < text.getVisibleLength(); index++) {
            assertFalse(text.hasForegroundAt(index));
            assertFalse(text.hasBackgroundAt(index));
        }
    }

    @Test
    void plainTextHasNoStyles() {
        StyledText text = parse("hello world");
        assertEquals("hello world", text.getVisibleText());
        assertEquals(11, text.getVisibleLength());
        assertNoStyle(text);
    }

    @Test
    void emptyStringGivesEmptyText() {
        StyledText text = parse("");
        assertEquals("", text.getVisibleText());
        assertEquals(0, text.getVisibleLength());
    }

    @Test
    void oneTagColorsTheLettersAfterIt() {
        StyledText text = parse("ab[color:red]cd");
        assertEquals("abcd", text.getVisibleText());
        assertForegrounds(text, null, null, RED, RED);
    }

    @Test
    void closerRestoresTheOuterValueSoTagsNest() {
        StyledText text = parse("[color:red]a[color:blue]b[/color]c[/color]d");
        assertEquals("abcd", text.getVisibleText());
        assertForegrounds(text, RED, BLUE, RED, null);
    }

    @Test
    void defaultTagSwitchesBackToThePaintAndACloserRestoresTheOuterColor() {
        StyledText text = parse("[color:red]a[color:default]b[/color]c");
        assertForegrounds(text, RED, null, RED);
    }

    @Test
    void backgroundTagsWorkLikeForegroundTags() {
        StyledText text = parse("[bg:blue]a[/bg]b[bg:default]c");
        assertEquals("abc", text.getVisibleText());
        assertTrue(text.hasBackgroundAt(0));
        assertEquals(BLUE, text.getBackgroundRgbaAt(0));
        assertFalse(text.hasBackgroundAt(1));
        assertFalse(text.hasBackgroundAt(2));
        assertNoForeground(text);
    }

    private static void assertNoForeground(StyledText text) {
        for (int index = 0; index < text.getVisibleLength(); index++) {
            assertFalse(text.hasForegroundAt(index));
        }
    }

    @Test
    void channelsAreIndependent() {
        StyledText text = parse("[color:red][bg:blue]a[/color]b");
        assertTrue(text.hasForegroundAt(0));
        assertFalse(text.hasForegroundAt(1));
        assertTrue(text.hasBackgroundAt(1));
    }

    @Test
    void hexColorsWithSixOrEightDigitsInEitherCase() {
        assertForegrounds(parse("[color:#ff8000]a"), 0xFF8000FF);
        assertForegrounds(parse("[color:#FF8000]a"), 0xFF8000FF);
        assertForegrounds(parse("[color:#ff800040]a"), 0xFF800040);
        assertForegrounds(parse("[color:#Ff80aB40]a"), 0xFF80AB40);
    }

    @Test
    void unknownTagsStayVisibleExactlyAsWritten() {
        for (String source : new String[] {"a[b]c", "[colour:red]x", "[COLOR:red]x", "[bg]x",
                "[color:]x", "[:red]x"}) {
            StyledText text = parse(source);
            assertEquals(source, text.getVisibleText(), source);
            assertNoStyle(text);
        }
    }

    @Test
    void unknownColorNameAndBadHexStayVisible() {
        for (String source : new String[] {"[color:Red]x", "[color:nope]x", "[color:#12]x",
                "[color:#gggggg]x", "[color:#ff80]x", "[color:#ff80000]x", "[color:#ff8000401]x",
                "[bg:nope]x"}) {
            StyledText text = parse(source);
            assertEquals(source, text.getVisibleText(), source);
            assertNoStyle(text);
        }
    }

    @Test
    void nameLookupIsNotUsedForHexValues() {
        ColorNames everything = name -> OptionalInt.of(RED);
        StyledText text = StyledText.parse("[color:#zzz]x", everything);
        assertEquals("[color:#zzz]x", text.getVisibleText());
    }

    @Test
    void doubleBracketWritesOneLiteralBracket() {
        StyledText text = parse("a[[b");
        assertEquals("a[b", text.getVisibleText());
        assertEquals(3, text.getVisibleLength());
    }

    @Test
    void doubleBracketBeforeTagTextDoesNotStartATag() {
        StyledText text = parse("[[color:red]x");
        assertEquals("[color:red]x", text.getVisibleText());
        assertNoStyle(text);
    }

    @Test
    void loneBracketIsPlainText() {
        StyledText text = parse("a [ b ] c");
        assertEquals("a [ b ] c", text.getVisibleText());
        assertNoStyle(text);
    }

    @Test
    void brokenOpenerDoesNotHideALaterValidTag() {
        StyledText text = parse("[x [color:red]a");
        assertEquals("[x a", text.getVisibleText());
        assertForegrounds(text, null, null, null, RED);
    }

    @Test
    void unclosedTagAtTheEndStaysVisible() {
        StyledText text = parse("abc[color:red");
        assertEquals("abc[color:red", text.getVisibleText());
        assertNoStyle(text);
    }

    @Test
    void colorStaysOpenUntilTheEndWithoutACloser() {
        assertForegrounds(parse("[color:red]ab"), RED, RED);
    }

    @Test
    void closerWithEmptyStackIsVisibleText() {
        StyledText text = parse("a[/color]b[/bg]c");
        assertEquals("a[/color]b[/bg]c", text.getVisibleText());
        assertNoStyle(text);
    }

    @Test
    void closerAfterTheStackIsEmptiedIsVisibleText() {
        StyledText text = parse("[color:red]a[/color][/color]");
        assertEquals("a[/color]", text.getVisibleText());
        assertForegrounds(text, RED, null, null, null, null, null, null, null, null);
    }

    @Test
    void effectTagsStayVisibleInStepOne() {
        StyledText text = parse("[effect:shake]a[/effect]");
        assertEquals("[effect:shake]a[/effect]", text.getVisibleText());
        assertNoStyle(text);
    }

    @Test
    void visibleLengthIsShorterThanTheRawLength() {
        String raw = "The [color:red]quick[color:default] brown fox";
        StyledText text = parse(raw);
        assertEquals("The quick brown fox", text.getVisibleText());
        assertEquals(19, text.getVisibleLength());
        assertTrue(raw.length() > text.getVisibleLength());
    }

    @Test
    void lettersAreCountedInCodePoints() {
        StyledText text = parse("a[color:red]😀b");
        assertEquals(3, text.getVisibleLength());
        assertForegrounds(text, null, RED, RED);
    }

    @Test
    void letterOutsideTheTextThrows() {
        StyledText text = parse("ab");
        assertThrows(IndexOutOfBoundsException.class, () -> text.hasForegroundAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> text.hasForegroundAt(2));
        assertThrows(IndexOutOfBoundsException.class, () -> text.getForegroundRgbaAt(2));
        assertThrows(IndexOutOfBoundsException.class, () -> text.hasBackgroundAt(2));
        assertThrows(IndexOutOfBoundsException.class, () -> text.getBackgroundRgbaAt(-1));
    }

    @Test
    void readingAColorThatTheLetterLacksThrows() {
        StyledText text = parse("ab");
        assertThrows(IllegalStateException.class, () -> text.getForegroundRgbaAt(0));
        assertThrows(IllegalStateException.class, () -> text.getBackgroundRgbaAt(0));
    }

    @Test
    void escapeThenParseGivesTheOriginalText() {
        for (String original : new String[] {"", "plain", "[", "[[", "a[b]c", "[color:red]x",
                "[[x]]", "tail[", "]["}) {
            StyledText text = StyledText.parse(StyledText.escape(original), NAMES);
            assertEquals(original, text.getVisibleText(), original);
            assertNoStyle(text);
        }
    }

    @Test
    void escapeDoublesEveryOpeningBracket() {
        assertEquals("a[[b]c[[", StyledText.escape("a[b]c["));
    }

    @Test
    void escapedTextCannotInjectATagIntoSurroundingMarkup() {
        String source = "[color:red]" + StyledText.escape("[color:blue]boo") + "[/color]";
        StyledText text = parse(source);
        assertEquals("[color:blue]boo", text.getVisibleText());
        for (int index = 0; index < text.getVisibleLength(); index++) {
            assertEquals(RED, text.getForegroundRgbaAt(index));
        }
    }

    @Test
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> StyledText.parse(null, NAMES));
        assertThrows(NullPointerException.class, () -> StyledText.parse("a", null));
        assertThrows(NullPointerException.class, () -> StyledText.escape(null));
    }

    @Test
    void defaultOverloadsReturnTheLettersColorOrTheDefault() {
        StyledText text = parse("[color:red]a[/color][bg:blue]b");

        assertEquals(RED, text.getForegroundRgbaAt(0, 0x11223344));
        assertEquals(0x11223344, text.getForegroundRgbaAt(1, 0x11223344));
        assertEquals(0x55667788, text.getBackgroundRgbaAt(0, 0x55667788));
        assertEquals(BLUE, text.getBackgroundRgbaAt(1, 0x55667788));
    }

    @Test
    void defaultOverloadsRejectABadIndex() {
        StyledText text = parse("ab");
        assertThrows(IndexOutOfBoundsException.class, () -> text.getForegroundRgbaAt(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> text.getForegroundRgbaAt(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> text.getBackgroundRgbaAt(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> text.getBackgroundRgbaAt(2, 0));
    }

    @Test
    void escapingTwiceDoublesTheBracketsAgain() {
        assertEquals("[[[[", StyledText.escape(StyledText.escape("[")));
    }

    @Test
    void tagBetweenALoneHighAndLoneLowSurrogateDoesNotJoinThem() {
        StyledText text = parse("\uD83D[color:red]\uDE00x");

        assertEquals("��x", text.getVisibleText());
        assertEquals(3, text.getVisibleLength());
        assertEquals(text.getVisibleText().codePointCount(0, text.getVisibleText().length()),
                text.getVisibleLength());
        assertForegrounds(text, null, RED, RED);
    }

    @Test
    void validSurrogatePairsAreKept() {
        StyledText text = parse("[color:red]😀x");

        assertEquals("😀x", text.getVisibleText());
        assertEquals(2, text.getVisibleLength());
    }

    @Test
    void noneKnowsNoNamesButHexStillWorks() {
        StyledText text = StyledText.parse("[color:red]a[color:#00ff00]b", ColorNames.NONE);
        assertEquals("[color:red]ab", text.getVisibleText());
        assertTrue(text.hasForegroundAt(text.getVisibleLength() - 1));
        assertEquals(0x00FF00FF, text.getForegroundRgbaAt(text.getVisibleLength() - 1));
    }
}
