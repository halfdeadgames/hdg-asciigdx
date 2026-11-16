package com.github.halfdeadgames.asciigdx.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.color.HueColor;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.CellHandle;
import com.github.halfdeadgames.asciigdx.render.HeadlessLayers;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.render.LayerStack;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import org.junit.jupiter.api.Test;

class CellEffectsTest {

    private static final float DELTA = 1e-4f;
    private static final float CELL_WIDTH = 8f;
    private static final float TALL_CELL_HEIGHT = 16f;

    private final LayerStack stack = HeadlessLayers.createStack();
    private final Layer layer = HeadlessLayers.add(stack, new TerminalGrid(10, 10));
    private final ZoneCell where = tallCellAt(0, 0, 0, 0f);

    private static ZoneCell tallCellAt(int column, int row, int zoneColumn, float distance) {
        ZoneCell cell = new ZoneCell();
        cell.set(column, row, zoneColumn, 0, distance, 0f);
        cell.setCellSize(CELL_WIDTH, TALL_CELL_HEIGHT);
        return cell;
    }

    private CellHandle freshHandle(int column, int row) {
        CellHandle handle = layer.cell(column, row);
        handle.reset();
        return handle;
    }

    @Test
    void waveAtQuarterPeriodIsAmplitudeInCellHeights() {
        CellHandle handle = freshHandle(0, 0);

        CellEffects.wave(0.25f, 2f, Phase.sync()).apply(handle, where, 0.5f);

        assertEquals(0.25f * TALL_CELL_HEIGHT, handle.getOffsetY(), DELTA);
        assertEquals(0f, handle.getOffsetX());
    }

    @Test
    void waveAtStartAndHalfPeriodIsCentered() {
        CellEffect wave = CellEffects.wave(1f, 2f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        wave.apply(handle, where, 0f);
        assertEquals(0f, handle.getOffsetY(), DELTA);
        wave.apply(handle, where, 1f);
        assertEquals(0f, handle.getOffsetY(), DELTA);
    }

    @Test
    void waveUsesThePhaseOfTheCell() {
        CellEffect wave = CellEffects.wave(1f, 4f, Phase.alongColumns(4f));
        CellHandle handle = freshHandle(0, 0);

        wave.apply(handle, tallCellAt(0, 0, 1, 0f), 1f);

        assertEquals(0f, handle.getOffsetY(), DELTA);
    }

    @Test
    void bounceNeverMovesDownAndPeaksAtHalfPeriod() {
        CellEffect bounce = CellEffects.bounce(2f, 1f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        for (int i = 0; i <= 40; i++) {
            handle.resetTransform();
            bounce.apply(handle, where, i * 0.05f);
            assertTrue(handle.getOffsetY() <= 0f, "moved down at step " + i);
            assertTrue(handle.getOffsetY() >= -2f * TALL_CELL_HEIGHT - DELTA);
        }
        handle.resetTransform();
        bounce.apply(handle, where, 0.5f);
        assertEquals(-2f * TALL_CELL_HEIGHT, handle.getOffsetY(), DELTA);
    }

    @Test
    void shakeIsConstantWithinAStepAndChangesAcrossSteps() {
        CellEffect shake = CellEffects.shake(0.5f, 10f, 7);
        CellHandle first = freshHandle(0, 0);
        shake.apply(first, where, 0.31f);
        float firstX = first.getOffsetX();
        float firstY = first.getOffsetY();

        CellHandle repeat = freshHandle(0, 0);
        shake.apply(repeat, where, 0.39f);
        assertEquals(firstX, repeat.getOffsetX());
        assertEquals(firstY, repeat.getOffsetY());

        boolean changed = false;
        for (int step = 4; step < 30 && !changed; step++) {
            CellHandle later = freshHandle(0, 0);
            shake.apply(later, where, step * 0.1f + 0.05f);
            changed = later.getOffsetX() != firstX || later.getOffsetY() != firstY;
        }
        assertTrue(changed);
    }

    @Test
    void shakeStaysWithinBoundsPerAxisUsingCellSizes() {
        CellEffect shake = CellEffects.shake(0.25f, 20f, 3);
        CellHandle handle = freshHandle(0, 0);
        boolean sawHorizontal = false;
        boolean sawVertical = false;

        for (int step = 0; step < 200; step++) {
            handle.resetTransform();
            shake.apply(handle, tallCellAt(step % 7, step % 5, 0, 0f), step / 20f + 0.01f);
            assertTrue(Math.abs(handle.getOffsetX()) <= 0.25f * CELL_WIDTH);
            assertTrue(Math.abs(handle.getOffsetY()) <= 0.25f * TALL_CELL_HEIGHT);
            sawHorizontal |= Math.abs(handle.getOffsetX()) > 0.25f * CELL_WIDTH * 0.5f;
            sawVertical |= Math.abs(handle.getOffsetY()) > 0.25f * TALL_CELL_HEIGHT * 0.5f;
        }
        assertTrue(sawHorizontal);
        assertTrue(sawVertical);
    }

    @Test
    void shakeIsDeterministicPerSeedAndDiffersAcrossSeeds() {
        CellHandle a = freshHandle(0, 0);
        CellHandle b = freshHandle(1, 0);
        CellHandle c = freshHandle(2, 0);

        CellEffects.shake(1f, 5f, 11).apply(a, where, 0.5f);
        CellEffects.shake(1f, 5f, 11).apply(b, where, 0.5f);
        CellEffects.shake(1f, 5f, 12).apply(c, where, 0.5f);

        assertEquals(a.getOffsetX(), b.getOffsetX());
        assertEquals(a.getOffsetY(), b.getOffsetY());
        assertNotEquals(a.getOffsetX(), c.getOffsetX());
    }

    @Test
    void shakeDiffersBetweenCells() {
        CellEffect shake = CellEffects.shake(1f, 5f, 11);
        CellHandle a = freshHandle(0, 0);
        CellHandle b = freshHandle(1, 0);

        shake.apply(a, tallCellAt(0, 0, 0, 0f), 0.5f);
        shake.apply(b, tallCellAt(1, 0, 1, 0f), 0.5f);

        assertNotEquals(a.getOffsetX(), b.getOffsetX());
    }

    @Test
    void pulseScalesBothAxesAroundOne() {
        CellEffect pulse = CellEffects.pulse(0.5f, 4f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        pulse.apply(handle, where, 1f);
        assertEquals(1.5f, handle.getScaleX(), DELTA);
        assertEquals(1.5f, handle.getScaleY(), DELTA);

        handle.resetTransform();
        pulse.apply(handle, where, 3f);
        assertEquals(0.5f, handle.getScaleX(), DELTA);

        handle.resetTransform();
        pulse.apply(handle, where, 0f);
        assertEquals(1f, handle.getScaleX(), DELTA);
    }

    @Test
    void spinRotatesBySpeedTimesTimeMinusPhaseTurns() {
        CellHandle handle = freshHandle(0, 0);

        CellEffects.spin(90f, Phase.sync()).apply(handle, where, 2f);
        assertEquals(180f, handle.getRotation(), DELTA);

        handle.resetTransform();
        CellEffects.spin(90f, Phase.alongColumns(4f)).apply(handle, tallCellAt(0, 0, 2, 0f), 2f);
        assertEquals(0f, handle.getRotation(), DELTA);

        handle.resetTransform();
        CellEffects.spin(-90f, Phase.sync()).apply(handle, where, 1f);
        assertEquals(-90f, handle.getRotation(), DELTA);
    }

    @Test
    void flashMixesTheColorBetweenZeroAndMaxAmount() {
        CellEffect flash = CellEffects.flash(0xFF8800FF, 0.8f, 4f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        flash.apply(handle, where, 0f);
        assertEquals(0xFF8800FF, handle.getForegroundMixColor());
        assertEquals(0.4f, handle.getForegroundMixAmount(), DELTA);

        flash.apply(handle, where, 1f);
        assertEquals(0.8f, handle.getForegroundMixAmount(), DELTA);

        flash.apply(handle, where, 3f);
        assertEquals(0f, handle.getForegroundMixAmount(), DELTA);
    }

    @Test
    void rainbowMixesTheHueAtTheCellsCyclePosition() {
        CellEffect rainbow = CellEffects.rainbow(0.6f, 6f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        rainbow.apply(handle, where, 2f);

        assertEquals(HueColor.fromHue(1f / 3f), handle.getForegroundMixColor());
        assertEquals(0.6f, handle.getForegroundMixAmount(), DELTA);
    }

    @Test
    void rainbowPhaseShiftsTheHue() {
        CellEffect rainbow = CellEffects.rainbow(1f, 1f, Phase.alongColumns(2f));
        CellHandle handle = freshHandle(0, 0);

        rainbow.apply(handle, tallCellAt(0, 0, 1, 0f), 0.5f);

        assertEquals(HueColor.fromHue(0f), handle.getForegroundMixColor());
    }

    @Test
    void waveMovesOnlyVerticallyByCellHeight() {
        CellHandle handle = freshHandle(0, 0);

        CellEffects.wave(1f, 4f, Phase.sync()).apply(handle, where, 1f);

        assertEquals(TALL_CELL_HEIGHT, handle.getOffsetY(), DELTA);
        assertEquals(0f, handle.getOffsetX());
    }

    @Test
    void shakeHorizontalOffsetUsesCellWidthNotHeight() {
        float amplitude = 1f;
        CellEffect shake = CellEffects.shake(amplitude, 1f, 5);
        CellHandle handle = freshHandle(0, 0);
        boolean exceedsQuarterWidth = false;

        for (int column = 0; column < 50; column++) {
            handle.resetTransform();
            shake.apply(handle, tallCellAt(column, 0, 0, 0f), 0.5f);
            assertTrue(Math.abs(handle.getOffsetX()) <= amplitude * CELL_WIDTH);
            exceedsQuarterWidth |= Math.abs(handle.getOffsetX()) > amplitude * CELL_WIDTH / 4f;
        }
        assertTrue(exceedsQuarterWidth);
    }

    @Test
    void waveAtHugeTimeMatchesTheSameMomentEarly() {
        CellEffect wave = CellEffects.wave(1f, 1f, Phase.sync());
        CellHandle early = freshHandle(0, 0);
        CellHandle late = freshHandle(1, 0);

        wave.apply(early, where, 0.25f);
        wave.apply(late, where, 36000.25f);

        assertEquals(early.getOffsetY(), late.getOffsetY(), 1e-3f * TALL_CELL_HEIGHT);
    }

    @Test
    void spinAtHugeTimeStaysPrecise() {
        CellEffect spin = CellEffects.spin(90f, Phase.sync());
        CellHandle early = freshHandle(0, 0);
        CellHandle late = freshHandle(1, 0);

        spin.apply(early, where, 0.25f);
        spin.apply(late, where, 36000.25f);

        assertEquals(early.getRotation(), late.getRotation(), 1e-3f);
    }

    @Test
    void rainbowSkipsCellsWithNonFiniteTime() {
        CellEffect rainbow = CellEffects.rainbow(1f, 1f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        rainbow.apply(handle, where, Float.NaN);
        rainbow.apply(handle, where, Float.POSITIVE_INFINITY);

        assertEquals(0, handle.getForegroundMixColor());
        assertEquals(0f, handle.getForegroundMixAmount());
    }

    @Test
    void combineRunsEveryEffectInOrderAndLastColorWins() {
        CellEffect combined = CellEffects.combine(
                CellEffects.wave(1f, 4f, Phase.sync()),
                CellEffects.pulse(1f, 4f, Phase.sync()),
                CellEffects.spin(10f, Phase.sync()),
                (cell, cellWhere, time) -> cell.setForegroundMix(0x111111FF, 0.1f),
                (cell, cellWhere, time) -> cell.setForegroundMix(0x222222FF, 0.2f));
        CellHandle handle = freshHandle(0, 0);

        combined.apply(handle, where, 1f);

        assertEquals(TALL_CELL_HEIGHT, handle.getOffsetY(), DELTA);
        assertEquals(2f, handle.getScaleX(), DELTA);
        assertEquals(10f, handle.getRotation(), DELTA);
        assertEquals(0x222222FF, handle.getForegroundMixColor());
    }

    @Test
    void combineCopiesTheArrayAtCreation() {
        CellEffect[] effects = {CellEffects.spin(10f, Phase.sync())};
        CellEffect combined = CellEffects.combine(effects);
        effects[0] = CellEffects.spin(100f, Phase.sync());
        CellHandle handle = freshHandle(0, 0);

        combined.apply(handle, where, 1f);

        assertEquals(10f, handle.getRotation(), DELTA);
    }

    @Test
    void combinedEffectComposesWithAnotherZoneAndUsesTheLayersCellSize() {
        RectangleShape shape = new RectangleShape(2, 2, 1, 1, ShapeFill.FILLED);
        EffectZone waveZone = new EffectZone(shape, CellEffects.combine(
                CellEffects.wave(0.5f, 4f, Phase.sync())));
        EffectZone nudgeZone = new EffectZone(shape, (cell, cellWhere, time) -> cell.offsetBy(0f, 1f));

        stack.beginCellEffects(0f);
        waveZone.apply(layer, 1f);
        nudgeZone.apply(layer, 1f);

        float layerCellHeight = layer.getGlyphSource().getCellHeight();
        assertEquals(0.5f * layerCellHeight + 1f, layer.frameCell(2, 2).getOffsetY(), DELTA);
    }

    @Test
    void zoneFillsCellSizeFromTheLayersGlyphSource() {
        float[] seen = new float[2];
        RectangleShape shape = new RectangleShape(0, 0, 1, 1, ShapeFill.FILLED);

        new EffectZone(shape, (cell, cellWhere, time) -> {
            seen[0] = cellWhere.getCellWidth();
            seen[1] = cellWhere.getCellHeight();
        }).apply(layer, 0f);

        assertEquals(layer.getGlyphSource().getCellWidth(), seen[0]);
        assertEquals(layer.getGlyphSource().getCellHeight(), seen[1]);
    }

    @Test
    void invalidArgumentsThrow() {
        Phase sync = Phase.sync();
        float nan = Float.NaN;
        float infinity = Float.POSITIVE_INFINITY;

        assertThrows(IllegalArgumentException.class, () -> CellEffects.wave(-1f, 1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.wave(nan, 1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.wave(1f, 0f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.wave(1f, infinity, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.bounce(1f, -1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.bounce(-1f, 1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.shake(-1f, 1f, 0));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.shake(1f, 0f, 0));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.pulse(-0.1f, 1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.pulse(0.1f, nan, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.spin(nan, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.spin(infinity, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.flash(0, 1.5f, 1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.flash(0, 1f, 0f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.rainbow(-1f, 1f, sync));
        assertThrows(IllegalArgumentException.class, () -> CellEffects.rainbow(1f, 0f, sync));
    }

    @Test
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> CellEffects.wave(1f, 1f, null));
        assertThrows(NullPointerException.class, () -> CellEffects.bounce(1f, 1f, null));
        assertThrows(NullPointerException.class, () -> CellEffects.pulse(1f, 1f, null));
        assertThrows(NullPointerException.class, () -> CellEffects.spin(1f, null));
        assertThrows(NullPointerException.class, () -> CellEffects.flash(0, 1f, 1f, null));
        assertThrows(NullPointerException.class, () -> CellEffects.rainbow(1f, 1f, null));
        assertThrows(NullPointerException.class, () -> CellEffects.combine((CellEffect[]) null));
        assertThrows(NullPointerException.class,
                () -> CellEffects.combine(CellEffects.spin(1f, Phase.sync()), null));
    }
}
