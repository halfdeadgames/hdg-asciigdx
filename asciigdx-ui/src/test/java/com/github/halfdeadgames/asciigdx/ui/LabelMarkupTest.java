package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.text.ColorNames;
import com.github.halfdeadgames.asciigdx.text.StyledText;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class LabelMarkupTest {
    private static final int RED = 0xFF0000FF;
    private static final int GREEN = 0x00FF00FF;
    private static final int BLUE = 0x0000FFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final LabelStyle STYLE = LabelStyle.of(WHITE, BLUE);
    private static final ColorNames NAMES =
            name -> name.equals("red") ? OptionalInt.of(RED) : OptionalInt.empty();

    private final UiSurface surface = UiSurface.ofSize(20, 3);
    private final TerminalGrid textGrid = surface.getTextGrid();

    private Label taggedLabel(String text) {
        Label label = new Label(text, STYLE);
        label.setColorNames(NAMES);
        label.pack();
        return label;
    }

    private String textRow(int row, int count) {
        StringBuilder builder = new StringBuilder();
        for (int column = 0; column < count; column++) {
            builder.appendCodePoint(textGrid.getCodePoint(column, row));
        }
        return builder.toString();
    }

    @Test
    void defaultsAreMarkupOnAndNoNames() {
        Label label = new Label("a", STYLE);
        assertTrue(label.isMarkupEnabled());
        assertSame(ColorNames.NONE, label.getColorNames());
    }

    @Test
    void getTextReturnsTheRawTextWithTags() {
        assertEquals("a[color:red]b", taggedLabel("a[color:red]b").getText());
    }

    @Test
    void preferredWidthCountsVisibleLettersOnly() {
        Label label = taggedLabel("ab[color:red]cd[/color]");
        assertEquals(2, label.getPreferredWidth());
        assertEquals(2, label.getWidth());

        Label odd = taggedLabel("[color:red]abc");
        assertEquals(2, odd.getPreferredWidth());
    }

    @Test
    void drawsPerLetterForegroundAndStyleColorElsewhere() {
        Label label = taggedLabel("a[color:red]b[/color]c[color:#00ff00]d");
        label.draw(surface);

        assertEquals("abcd", textRow(0, 4));
        assertEquals(WHITE, textGrid.getForeground(0, 0));
        assertEquals(RED, textGrid.getForeground(1, 0));
        assertEquals(WHITE, textGrid.getForeground(2, 0));
        assertEquals(GREEN, textGrid.getForeground(3, 0));
    }

    @Test
    void defaultTagUsesTheStyleForeground() {
        Label label = taggedLabel("[color:red]a[color:default]b");
        label.draw(surface);

        assertEquals(RED, textGrid.getForeground(0, 0));
        assertEquals(WHITE, textGrid.getForeground(1, 0));
    }

    @Test
    void alignmentUsesVisibleLength() {
        Label label = taggedLabel("[color:red]Hi[/color]");
        label.setSize(5, 1);
        label.setTextAlignment(HorizontalAlignment.RIGHT);
        label.draw(surface);

        assertEquals("Hi", textRow(0, 10).substring(8));
    }

    @Test
    void cutOffCountsVisibleLetters() {
        Label label = taggedLabel("[color:red]Hello[/color]");
        label.setSize(2, 1);
        label.draw(surface);

        assertEquals("Hell", textRow(0, 4));
        assertEquals(RED, textGrid.getForeground(3, 0));
    }

    @Test
    void markupOffDrawsTheTextLiterally() {
        Label label = taggedLabel("a[color:red]b");
        label.setMarkupEnabled(false);
        label.pack();
        label.draw(surface);

        assertFalse(label.isMarkupEnabled());
        assertEquals("a[color:red]b", textRow(0, 13));
        assertEquals(WHITE, textGrid.getForeground(2, 0));
        assertEquals(7, label.getPreferredWidth());
        assertEquals("a[color:red]b", label.getText());
    }

    @Test
    void turningMarkupBackOnReadsTagsAgain() {
        Label label = taggedLabel("a[color:red]b");
        label.setMarkupEnabled(false);
        label.setMarkupEnabled(true);
        label.pack();
        label.draw(surface);

        assertEquals("ab", textRow(0, 2));
        assertEquals(RED, textGrid.getForeground(1, 0));
    }

    @Test
    void changingTheTextOrNamesParsesAgain() {
        Label label = new Label("[color:red]a", STYLE);
        label.draw(surface);
        assertEquals("[color:red]a", textRow(0, 12));

        label.setColorNames(NAMES);
        label.pack();
        label.draw(surface);
        assertEquals("a", textRow(0, 1));
        assertEquals(RED, textGrid.getForeground(0, 0));

        label.setText("[color:red]xy");
        label.pack();
        label.draw(surface);
        assertEquals("xy", textRow(0, 2));
        assertEquals(RED, textGrid.getForeground(1, 0));
    }

    @Test
    void unknownTagsAreDrawnAsWritten() {
        Label label = taggedLabel("[color:nope]x");
        label.draw(surface);

        assertEquals("[color:nope]x", textRow(0, 13));
    }

    @Test
    void equalMarkupFlagInvalidatesNothingButARealChangeDoes() {
        Label label = taggedLabel("a");
        label.validate();

        label.setMarkupEnabled(true);
        assertFalse(label.isLayoutNeeded());

        label.setMarkupEnabled(false);
        assertTrue(label.isLayoutNeeded());
    }

    @Test
    void settingTheSameLookupAgainReReadsIt() {
        int[] currentRed = {RED};
        ColorNames mutable = name -> OptionalInt.of(currentRed[0]);
        Label label = new Label("[color:any]a", STYLE);
        label.setColorNames(mutable);
        label.pack();
        label.draw(surface);
        assertEquals(RED, textGrid.getForeground(0, 0));

        currentRed[0] = GREEN;
        label.setColorNames(mutable);
        label.draw(surface);

        assertEquals(GREEN, textGrid.getForeground(0, 0));
    }

    @Test
    void backgroundTagsTakeNoWidthButTheirColorIsNotYetDrawn() {
        Label label = taggedLabel("[bg:red]ab[/bg]");
        label.draw(surface);

        assertEquals("ab", textRow(0, 2));
        assertEquals(1, label.getPreferredWidth());
        assertEquals(BLUE, surface.getSquareGrid().getBackground(0, 0));
    }

    @Test
    void nullNamesAndNullTextThrow() {
        Label label = new Label("a", STYLE);
        assertThrows(NullPointerException.class, () -> label.setColorNames(null));
        assertThrows(NullPointerException.class, () -> label.setText(null));
    }

    @Test
    void escapedPlayerTextShowsLiterally() {
        Label label = taggedLabel("[color:red]" + StyledText.escape("[color:red]x"));
        label.draw(surface);

        assertEquals("[color:red]x", textRow(0, 12));
    }
}
