package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SliderTest {
    private static final int TRACK_GLYPH = '-';
    private static final int KNOB_GLYPH = '#';
    private static final LabelStyle TRACK = look(0x111111FF);
    private static final LabelStyle KNOB = look(0x222222FF);
    private static final LabelStyle TRACK_FOCUSED = look(0x333333FF);
    private static final LabelStyle KNOB_FOCUSED = look(0x444444FF);
    private static final LabelStyle TRACK_DISABLED = look(0x555555FF);
    private static final LabelStyle KNOB_DISABLED = look(0x666666FF);
    private static final SliderStyle STYLE = SliderStyle.of(TRACK, KNOB)
            .withFocused(TRACK_FOCUSED, KNOB_FOCUSED)
            .withDisabled(TRACK_DISABLED, KNOB_DISABLED)
            .withGlyphs(TRACK_GLYPH, KNOB_GLYPH);
    private static final ValueRange TEN = ValueRange.of(0f, 10f, 1f);

    private final UiSurface surface = UiSurface.ofSize(30, 16);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<Slider> changes = new ArrayList<>();

    private static LabelStyle look(int background) {
        return LabelStyle.of(0xFFFFFFFF, background);
    }

    private Slider horizontal() {
        return add(Slider.horizontal(TEN, 11, STYLE));
    }

    private Slider vertical() {
        return add(Slider.vertical(TEN, 11, STYLE));
    }

    private Slider add(Slider slider) {
        slider.addChangeListener(changes::add);
        ui.getRoot().addChild(slider);
        ui.setKeyboardFocus(slider);
        return slider;
    }

    @Test
    void horizontalKeysStepAndAreConsumed() {
        Slider slider = horizontal();

        assertTrue(ui.keyDown(Input.Keys.RIGHT));
        assertEquals(1f, slider.getValue());
        assertTrue(ui.keyDown(Input.Keys.RIGHT));
        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertEquals(1f, slider.getValue());
        assertEquals(3, changes.size());
    }

    @Test
    void repeatedKeyDownEventsMoveOneStepEach() {
        Slider slider = horizontal();

        for (int i = 0; i < 4; i++) {
            ui.keyDown(Input.Keys.RIGHT);
        }

        assertEquals(4f, slider.getValue());
        assertEquals(4, changes.size());
    }

    @Test
    void verticalKeysUseUpAndDown() {
        Slider slider = vertical();

        assertTrue(ui.keyDown(Input.Keys.UP));
        assertTrue(ui.keyDown(Input.Keys.UP));
        assertTrue(ui.keyDown(Input.Keys.DOWN));
        assertEquals(1f, slider.getValue());
    }

    @Test
    void stepDownFromAMaximumOffTheGridReachesTheHighestGridValue() {
        Slider slider = add(Slider.horizontal(ValueRange.of(0f, 10f, 3f), 11, STYLE));

        ui.keyDown(Input.Keys.END);
        ui.keyDown(Input.Keys.LEFT);

        assertEquals(9f, slider.getValue());
    }

    @Test
    void acrossAxisArrowsAreNotConsumed() {
        Slider horizontal = horizontal();
        assertFalse(ui.keyDown(Input.Keys.UP));
        assertFalse(ui.keyDown(Input.Keys.DOWN));
        assertEquals(0f, horizontal.getValue());

        ui.getRoot().removeChild(horizontal);
        Slider vertical = vertical();
        assertFalse(ui.keyDown(Input.Keys.LEFT));
        assertFalse(ui.keyDown(Input.Keys.RIGHT));
        assertEquals(0f, vertical.getValue());
        assertTrue(changes.isEmpty());
    }

    @Test
    void endsStillConsumeTheKeyWithoutAnEvent() {
        Slider slider = horizontal();

        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertTrue(changes.isEmpty());
        assertTrue(ui.keyDown(Input.Keys.END));
        assertEquals(10f, slider.getValue());
        assertTrue(ui.keyDown(Input.Keys.RIGHT));
        assertEquals(1, changes.size());
        assertTrue(ui.keyDown(Input.Keys.HOME));
        assertEquals(0f, slider.getValue());
        assertEquals(2, changes.size());
    }

    @Test
    void setValueSnapsAndNeverFiresListeners() {
        Slider slider = horizontal();

        slider.setValue(3.4f);
        assertEquals(3f, slider.getValue());
        slider.setValue(50f);
        assertEquals(10f, slider.getValue());
        slider.setValue(Float.NEGATIVE_INFINITY);
        assertEquals(0f, slider.getValue());
        assertThrows(IllegalArgumentException.class, () -> slider.setValue(Float.NaN));
        assertTrue(changes.isEmpty());
    }

    @Test
    void pressJumpsDragFollowsAndReleaseDisarms() {
        Slider slider = horizontal();

        assertTrue(ui.pointerDown(5, 0, 0, 0));
        assertEquals(5f, slider.getValue());
        assertTrue(ui.pointerDragged(8, 0, 0));
        assertEquals(8f, slider.getValue());
        assertTrue(ui.pointerDragged(-4, 0, 0));
        assertEquals(0f, slider.getValue());
        assertTrue(ui.pointerDragged(25, 0, 0));
        assertEquals(10f, slider.getValue());
        assertTrue(ui.pointerUp(25, 0, 0, 0));
        assertFalse(ui.pointerDragged(2, 0, 0));
        assertEquals(10f, slider.getValue());
        assertEquals(4, changes.size());
    }

    @Test
    void pressOnTheKnobCellFiresNoEvent() {
        Slider slider = horizontal();
        slider.setValue(5f);

        ui.pointerDown(5, 0, 0, 0);

        assertTrue(changes.isEmpty());
    }

    @Test
    void secondaryButtonIsIgnored() {
        Slider slider = horizontal();

        assertFalse(ui.pointerDown(5, 0, 0, 1));
        assertEquals(0f, slider.getValue());
    }

    @Test
    void verticalPointerHasTheMaximumAtTheTopRow() {
        Slider slider = vertical();

        ui.pointerDown(0, 0, 0, 0);
        assertEquals(10f, slider.getValue());
        ui.pointerDragged(0, 10, 0);
        assertEquals(0f, slider.getValue());
        ui.pointerDragged(0, 7, 0);
        assertEquals(3f, slider.getValue());
    }

    @Test
    void cancelDisarms() {
        Slider slider = horizontal();
        ui.pointerDown(2, 0, 0, 0);

        ui.pointerCancelled(0);
        ui.pointerDragged(9, 0, 0);

        assertEquals(2f, slider.getValue());
    }

    @Test
    void pointerMapsToTheNearestStep() {
        Slider slider = add(Slider.horizontal(ValueRange.of(0f, 100f, 25f), 5, STYLE));

        ui.pointerDown(1, 0, 0, 0);
        assertEquals(25f, slider.getValue());
        ui.pointerDragged(3, 0, 0);
        assertEquals(75f, slider.getValue());
    }

    @Test
    void disabledSliderIgnoresInputAndIsNotFocusable() {
        Slider slider = horizontal();
        slider.setDisabled(true);

        assertFalse(slider.isFocusable());
        assertFalse(ui.keyDown(Input.Keys.RIGHT));
        assertFalse(ui.pointerDown(5, 0, 0, 0));
        assertEquals(0f, slider.getValue());
        assertTrue(changes.isEmpty());
    }

    @Test
    void disablingEndsADragButStillConsumesTheRelease() {
        Slider slider = horizontal();
        ui.pointerDown(2, 0, 0, 0);

        slider.setDisabled(true);

        ui.pointerDragged(7, 0, 0);
        assertEquals(2f, slider.getValue());
        assertTrue(ui.pointerUp(7, 0, 0, 0));
        assertFalse(ui.pointerUp(7, 0, 0, 0));
    }

    @Test
    void disablingEndsADragButStillConsumesTheCancel() {
        Slider slider = horizontal();
        ui.pointerDown(2, 0, 0, 0);

        slider.setDisabled(true);

        assertTrue(ui.pointerCancelled(0));
        assertEquals(2f, slider.getValue());
    }

    @Test
    void knobCellIsTheRoundedFractionOfTheCellsMinusOne() {
        Slider slider = add(Slider.horizontal(TEN, 4, STYLE));

        slider.setValue(3f);
        assertEquals(1, slider.knobIndexFor(4));
        slider.setValue(5f);
        assertEquals(2, slider.knobIndexFor(4));
        slider.setValue(10f);
        assertEquals(3, slider.knobIndexFor(4));
        assertEquals(0, slider.knobIndexFor(1));
    }

    @Test
    void drawsTrackEverywhereAndTheKnobAtTheValueCell() {
        Slider slider = horizontal();
        slider.setValue(4f);
        slider.setPosition(2, 3);
        ui.draw();

        for (int column = 2; column < 13; column++) {
            int expected = column == 6 ? KNOB_GLYPH : TRACK_GLYPH;
            assertEquals(expected, surface.getSquareGrid().getCodePoint(column, 3));
        }
        assertEquals(TRACK.getBackgroundRgba(), surface.getSquareGrid().getBackground(2, 3));
        assertEquals(KNOB.getBackgroundRgba(), surface.getSquareGrid().getBackground(6, 3));
    }

    @Test
    void verticalKnobMovesUpWithTheValue() {
        Slider slider = vertical();
        slider.setValue(10f);
        ui.draw();
        assertEquals(KNOB_GLYPH, surface.getSquareGrid().getCodePoint(0, 0));

        slider.setValue(0f);
        ui.draw();
        assertEquals(KNOB_GLYPH, surface.getSquareGrid().getCodePoint(0, 10));
        assertEquals(TRACK_GLYPH, surface.getSquareGrid().getCodePoint(0, 0));
    }

    @Test
    void defaultGlyphsDependOnTheOrientation() {
        SliderStyle plain = SliderStyle.of(TRACK, KNOB);
        add(Slider.horizontal(TEN, 5, plain));
        Slider vertical = Slider.vertical(TEN, 5, plain);
        vertical.setPosition(0, 4);
        ui.getRoot().addChild(vertical);
        ui.draw();

        assertEquals(0x2500, surface.getSquareGrid().getCodePoint(3, 0));
        assertEquals(0x2588, surface.getSquareGrid().getCodePoint(0, 0));
        assertEquals(0x2502, surface.getSquareGrid().getCodePoint(0, 4));
        assertEquals(0x2588, surface.getSquareGrid().getCodePoint(0, 8));
    }

    @Test
    void focusedAndDisabledLooksFollowTheStateRules() {
        Slider slider = horizontal();
        ui.draw();
        assertEquals(TRACK.getBackgroundRgba(), surface.getSquareGrid().getBackground(5, 0));

        ui.setKeyboardFocus(null);
        ui.moveFocus(FocusDirection.NEXT);
        ui.draw();
        assertEquals(TRACK_FOCUSED.getBackgroundRgba(),
                surface.getSquareGrid().getBackground(5, 0));
        assertEquals(KNOB_FOCUSED.getBackgroundRgba(),
                surface.getSquareGrid().getBackground(0, 0));

        slider.setDisabled(true);
        ui.draw();
        assertEquals(TRACK_DISABLED.getBackgroundRgba(),
                surface.getSquareGrid().getBackground(5, 0));
        assertEquals(KNOB_DISABLED.getBackgroundRgba(),
                surface.getSquareGrid().getBackground(0, 0));
    }

    @Test
    void sliderSizeIsOneRowAndOneCellPerGlyph() {
        Slider horizontal = Slider.horizontal(TEN, 5, STYLE);
        Slider vertical = Slider.vertical(TEN, 3, STYLE);

        assertEquals(1, horizontal.getHeight());
        assertEquals(5, horizontal.getWidth());
        assertEquals(1, vertical.getWidth());
        assertEquals(3, vertical.getHeight());
        assertEquals(3, vertical.getPreferredLengthCells());
    }

    @Test
    void sliderDrawsOnlyInTheSquareGrid() {
        Slider slider = horizontal();
        slider.setValue(4f);
        slider.setPosition(2, 3);
        ui.draw();

        for (int textColumn = 0; textColumn < surface.getTextColumns(); textColumn++) {
            for (int row = 0; row < 16; row++) {
                assertEquals(TerminalGrid.EMPTY_CODE_POINT,
                        surface.getTextGrid().getCodePoint(textColumn, row));
            }
        }
        assertEquals(KNOB_GLYPH, surface.getSquareGrid().getCodePoint(6, 3));
    }

    @Test
    void removedListenerIsNotCalledAndInvalidArgumentsAreRejected() {
        Slider slider = horizontal();
        ChangeListener<Widget> other = source -> changes.add(null);
        slider.addChangeListener(other);
        slider.removeChangeListener(other);

        ui.keyDown(Input.Keys.RIGHT);

        assertEquals(1, changes.size());
        assertThrows(IllegalArgumentException.class, () -> Slider.horizontal(TEN, 0, STYLE));
        assertThrows(NullPointerException.class, () -> slider.addChangeListener(null));
        assertThrows(NullPointerException.class, () -> Slider.vertical(TEN, 3, null));
        assertThrows(NullPointerException.class, () -> Slider.vertical(null, 3, STYLE));
    }

    @Test
    void styleGlyphsAreExplicitOnlyAfterWithGlyphs() {
        SliderStyle plain = SliderStyle.of(TRACK, KNOB);

        assertFalse(plain.hasGlyphs());
        assertEquals(0x2500, plain.resolveTrackGlyph(BarOrientation.HORIZONTAL));
        assertEquals(0x2502, plain.resolveTrackGlyph(BarOrientation.VERTICAL));
        assertEquals(0x2588, plain.resolveKnobGlyph());
        assertTrue(STYLE.hasGlyphs());
        assertEquals(TRACK_GLYPH, STYLE.resolveTrackGlyph(BarOrientation.VERTICAL));
        assertEquals(KNOB_GLYPH, STYLE.resolveKnobGlyph());
        assertFalse(plain.equals(plain.withGlyphs(0x2500, 0x2588)));
        assertThrows(IllegalArgumentException.class, () -> plain.withGlyphs(-1, '#'));
    }
}
