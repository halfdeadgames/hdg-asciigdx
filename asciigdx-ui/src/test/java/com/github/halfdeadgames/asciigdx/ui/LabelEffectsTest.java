package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.text.EffectNames;
import com.github.halfdeadgames.asciigdx.text.EffectRun;
import com.github.halfdeadgames.asciigdx.zone.CellEffects;
import com.github.halfdeadgames.asciigdx.zone.EffectRegistry;
import com.github.halfdeadgames.asciigdx.zone.Phase;
import java.util.List;
import org.junit.jupiter.api.Test;

class LabelEffectsTest {
    private static final LabelStyle STYLE = LabelStyle.of(0xFFFFFFFF, 0x0000FFFF);
    private static final String WAVE_TEXT = "ab[effect:wave]cd[/effect]e";

    private final UiSurface surface = UiSurface.ofSize(20, 3);
    private final TerminalGrid textGrid = surface.getTextGrid();

    private static Label labelWithEffects(String text) {
        Label label = new Label(text, STYLE);
        label.setEffectNames(EffectRegistry.withBuiltIns());
        label.pack();
        return label;
    }

    private static Label sizedLabel(String text, int widthInCells,
            HorizontalAlignment alignment) {
        Label label = labelWithEffects(text);
        label.setSize(widthInCells, 1);
        label.setTextAlignment(alignment);
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
    void effectsDefaultToNoneAndTagsStayVisible() {
        Label label = new Label(WAVE_TEXT, STYLE);
        label.pack();
        label.draw(surface);

        assertSame(EffectNames.NONE, label.getEffectNames());
        assertTrue(label.getEffectRuns().isEmpty());
        assertEquals("ab[effect:wave]cd[/effect]e", textRow(0, WAVE_TEXT.length()));
    }

    @Test
    void knownTagIsRemovedAndBecomesARun() {
        Label label = labelWithEffects(WAVE_TEXT);
        label.draw(surface);

        List<EffectRun> runs = label.getEffectRuns();
        assertEquals("abcde", textRow(0, 5));
        assertEquals(1, runs.size());
        assertEquals(2, runs.get(0).getStartLetter());
        assertEquals(4, runs.get(0).getEndLetterExclusive());
        assertEquals(3, label.getPreferredWidth());
    }

    @Test
    void changingEffectsParsesTheTextAgain() {
        Label label = labelWithEffects(WAVE_TEXT);
        assertEquals(1, label.getEffectRuns().size());

        label.setEffectNames(EffectNames.NONE);
        assertTrue(label.getEffectRuns().isEmpty());
        assertEquals((WAVE_TEXT.length() + 1) / 2, label.getPreferredWidth());

        label.setEffectNames(EffectRegistry.withBuiltIns());
        assertEquals(1, label.getEffectRuns().size());
    }

    @Test
    void setEffectsRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> new Label("a", STYLE).setEffectNames(null));
    }

    @Test
    void runsAreEmptyWhileMarkupIsOff() {
        Label label = labelWithEffects(WAVE_TEXT);
        label.setMarkupEnabled(false);
        assertTrue(label.getEffectRuns().isEmpty());

        label.setMarkupEnabled(true);
        assertEquals(1, label.getEffectRuns().size());
    }

    @Test
    void leftAlignedLettersStartAtColumnZero() {
        Label label = sizedLabel("abcd", 10, HorizontalAlignment.LEFT);

        assertEquals(0, label.getTextColumnOfLetter(0));
        assertEquals(3, label.getTextColumnOfLetter(3));
    }

    @Test
    void centeredLettersStartAfterHalfTheSpareColumns() {
        Label label = sizedLabel("abcd", 10, HorizontalAlignment.CENTER);

        assertEquals(8, label.getTextColumnOfLetter(0));
        assertEquals(11, label.getTextColumnOfLetter(3));
    }

    @Test
    void rightAlignedLettersEndAtTheRightEdge() {
        Label label = sizedLabel("abcd", 10, HorizontalAlignment.RIGHT);

        assertEquals(16, label.getTextColumnOfLetter(0));
        assertEquals(19, label.getTextColumnOfLetter(3));
    }

    @Test
    void tagsDoNotShiftTheLetterColumns() {
        Label label = sizedLabel(WAVE_TEXT, 10, HorizontalAlignment.RIGHT);

        assertEquals(15, label.getTextColumnOfLetter(0));
        assertEquals(17, label.getTextColumnOfLetter(2));
    }

    @Test
    void letterColumnMatchesWhereTheLetterIsDrawn() {
        Label label = sizedLabel("abcd", 10, HorizontalAlignment.CENTER);
        label.setPosition(3, 1);
        label.draw(surface);

        int column = 2 * label.getX() + label.getTextColumnOfLetter(2);
        assertEquals('c', textGrid.getCodePoint(column, label.getY()));
    }

    @Test
    void cutOffAndMissingLettersHaveNoColumn() {
        Label label = sizedLabel("abcdef", 2, HorizontalAlignment.CENTER);

        assertEquals(3, label.getTextColumnOfLetter(3));
        assertEquals(Label.NOT_SHOWN,label.getTextColumnOfLetter(4));
        assertEquals(Label.NOT_SHOWN,label.getTextColumnOfLetter(6));
        assertEquals(Label.NOT_SHOWN,label.getTextColumnOfLetter(-1));
    }

    @Test
    void skinEffectsHaveTheBuiltInsAndAreTheSameInstanceEveryCall() {
        Skin skin = Skin.fromJson("{}");

        assertSame(skin.getEffectRegistry(), skin.getEffectRegistry());
        for (String name : List.of("wave", "bounce", "shake", "pulse", "spin", "flash",
                "rainbow", "glyphcycle")) {
            assertTrue(skin.getEffectRegistry().findEffect(name, java.util.Map.of()).isPresent(), name);
        }
    }

    @Test
    void skinsDoNotShareTheirEffects() {
        Skin first = Skin.fromJson("{}");
        Skin second = Skin.fromJson("{}");

        first.getEffectRegistry().register("tilt", settings -> CellEffects.spin(10f, Phase.sync()));
        assertTrue(second.getEffectRegistry().findEffect("tilt", java.util.Map.of()).isEmpty());
    }

    @Test
    void effectRegisteredOnTheSkinWorksInALabel() {
        Skin skin = Skin.fromJson("{}");
        skin.getEffectRegistry().register("tilt", settings -> CellEffects.spin(10f, Phase.sync()));
        Label label = new Label("a[effect:tilt]bc[/effect]", STYLE);
        label.setEffectNames(skin.getEffectRegistry());

        List<EffectRun> runs = label.getEffectRuns();
        assertEquals(1, runs.size());
        assertEquals(1, runs.get(0).getStartLetter());
        assertEquals(3, runs.get(0).getEndLetterExclusive());
    }
}
