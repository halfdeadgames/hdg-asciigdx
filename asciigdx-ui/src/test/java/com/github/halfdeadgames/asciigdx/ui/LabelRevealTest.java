package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.text.TextReveal;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class LabelRevealTest {
    private static final int RED = 0xFF0000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0x0000FFFF;
    private static final LabelStyle STYLE = LabelStyle.of(WHITE, BLUE);

    private final UiSurface surface = UiSurface.ofSize(20, 3);
    private final TerminalGrid textGrid = surface.getTextGrid();

    private String textRow(int count) {
        StringBuilder builder = new StringBuilder();
        for (int column = 0; column < count; column++) {
            int codePoint = textGrid.getCodePoint(column, 0);
            builder.appendCodePoint(codePoint == 0 || codePoint == ' ' ? '.' : codePoint);
        }
        return builder.toString();
    }

    @Test
    void newLabelShowsEverything() {
        Label label = new Label("hello", STYLE);
        label.draw(surface);

        assertEquals(5, label.getShownLetterCount());
        assertEquals("hello", textRow(5));
    }

    @Test
    void limitDrawsOnlyThePrefix() {
        Label label = new Label("hello", STYLE);
        label.setShownLetterCount(2);
        label.draw(surface);

        assertEquals(2, label.getShownLetterCount());
        assertEquals("he...", textRow(5));
    }

    @Test
    void limitAboveLengthShowsEverythingAndZeroShowsNothing() {
        Label label = new Label("hi", STYLE);
        label.setShownLetterCount(99);
        assertEquals(2, label.getShownLetterCount());

        label.setShownLetterCount(0);
        label.draw(surface);
        assertEquals(0, label.getShownLetterCount());
        assertEquals("..", textRow(2));
    }

    @Test
    void limitCountsVisibleLettersNotTags() {
        Label label = new Label("a[color:#ff0000]bc[wait:1]d", STYLE);
        label.setShownLetterCount(2);
        label.draw(surface);

        assertEquals("ab..", textRow(4));
        assertEquals(4, new Label("a[color:#ff0000]bc[wait:1]d", STYLE).getShownLetterCount());
    }

    @Test
    void sizeAndAlignmentStayComputedFromTheFullText() {
        Label label = new Label("abcd", STYLE);
        label.setSize(4, 1);
        label.setTextAlignment(HorizontalAlignment.RIGHT);
        label.setShownLetterCount(1);
        label.draw(surface);

        assertEquals(2, label.getPreferredWidth());
        assertEquals(4, label.getWidth());
        assertEquals("....a...", textRow(8));
    }

    @Test
    void hiddenLettersReportNotShown() {
        Label label = new Label("abcd", STYLE);
        label.setShownLetterCount(2);

        assertEquals(0, label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(1, label.getAbsoluteTextColumnOfLetter(1));
        assertEquals(Label.NOT_SHOWN, label.getAbsoluteTextColumnOfLetter(2));
        assertEquals(Label.NOT_SHOWN, label.getAbsoluteTextColumnOfLetter(3));
    }

    @Test
    void setTextWithDifferentTextResetsToShowAll() {
        Label label = new Label("abc", STYLE);
        label.setShownLetterCount(1);
        label.setText("xyz");
        label.draw(surface);

        assertEquals(3, label.getShownLetterCount());
        assertEquals("xyz", textRow(3));
    }

    @Test
    void setTextWithEqualTextKeepsTheLimit() {
        Label label = new Label("abc", STYLE);
        label.setShownLetterCount(1);
        label.setText("abc");

        assertEquals(1, label.getShownLetterCount());
    }

    @Test
    void createRevealMatchesNamedColorsAndWaitTags() {
        Label label = new Label("a[color:red]bc[/color][wait:1]d", STYLE);
        label.setColorNames(name -> name.equals("red") ? OptionalInt.of(RED) : OptionalInt.empty());
        TextReveal reveal = label.createReveal(4f);

        assertEquals(4, reveal.getShownLetterCount(2f));
        assertEquals(3, reveal.getShownLetterCount(1.75f));
        assertEquals(2f, reveal.getTotalSeconds(), 0.0001f);
    }

    @Test
    void createRevealCountsLettersAfterEffectTagsAreRemoved() {
        Label label = new Label("a[effect:wave]bc[/effect]d", STYLE);
        label.setEffectNames((name, settings) -> Optional.of((cell, where, time) -> { }));
        TextReveal reveal = label.createReveal(4f);

        assertEquals(1f, reveal.getTotalSeconds(), 0.0001f);
        assertEquals(4, reveal.getShownLetterCount(1f));
    }

    @Test
    void createRevealWithMarkupOffCountsEveryCharacterAndNoPauses() {
        Label label = new Label("[wait:1]ab", STYLE);
        label.setMarkupEnabled(false);
        TextReveal reveal = label.createReveal(4f);

        assertEquals(10, label.getPreferredWidth() * 2);
        assertEquals(10f / 4f, reveal.getTotalSeconds(), 0.0001f);
        assertEquals(10, reveal.getShownLetterCount(2.5f));
    }

    @Test
    void showAllLettersUndoesTheLimit() {
        Label label = new Label("abc", STYLE);
        label.setShownLetterCount(1);
        label.showAllLetters();
        label.draw(surface);

        assertEquals("abc", textRow(3));
    }

    @Test
    void negativeLimitThrows() {
        Label label = new Label("abc", STYLE);

        assertThrows(IllegalArgumentException.class, () -> label.setShownLetterCount(-1));
    }
}
