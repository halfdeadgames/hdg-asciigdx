package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class MapWindowSlideTest {

    private static final int COLUMNS = 10;
    private static final int ROWS = 6;
    private static final int CELL_WIDTH = 8;
    private static final int CELL_HEIGHT = 16;
    private static final float DELTA = 0.001f;

    private final LayerStack stack =
            TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final Layer layer = stack.add(
            new TerminalGrid(COLUMNS, ROWS), TestGlyphSource.ofCellSize(CELL_WIDTH, CELL_HEIGHT));
    private final MapWindow window = enable();

    private MapWindow enable() {
        layer.enableMapWindow();
        return layer.getMapWindow();
    }

    /** Runs one frame in the documented order with the camera at the given cell. */
    private void runFrame(int cameraColumn, int cameraRow, float timeSeconds) {
        stack.beginCellEffects(0f);
        stack.setCamera(cameraColumn * CELL_WIDTH, cameraRow * CELL_HEIGHT);
        window.applySlides(timeSeconds);
    }

    private void assertGridOffset(int gridColumn, int gridRow, float expectedX, float expectedY) {
        CellHandle handle = layer.obtainFrameCellHandle(gridColumn, gridRow);
        assertEquals(expectedX, handle.getOffsetX(), DELTA, "x at " + gridColumn + "," + gridRow);
        assertEquals(expectedY, handle.getOffsetY(), DELTA, "y at " + gridColumn + "," + gridRow);
    }

    private void assertNoOffsetAnywhere() {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                assertGridOffset(column, row, 0f, 0f);
            }
        }
    }

    @Test
    void midSlideOffsetIsInPixelsOnBothAxes() {
        window.startSlide(2, 2, 3, 4, 0f, 1f, Interpolation.linear);

        runFrame(0, 0, 0.5f);

        assertGridOffset(3, 4, -0.5f * CELL_WIDTH, -1f * CELL_HEIGHT);
        assertGridOffset(2, 2, 0f, 0f);
    }

    @Test
    void offsetLandsOnTheGridCellAfterTheCameraScrolled() {
        window.startSlide(4, 1, 5, 1, 0f, 1f, Interpolation.linear);

        runFrame(2, 0, 0.5f);

        assertGridOffset(3, 1, -0.5f * CELL_WIDTH, 0f);
        assertGridOffset(5, 1, 0f, 0f);
    }

    @Test
    void finishedSlideIsRemovedAndWritesNothing() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);

        runFrame(0, 0, 1f);
        assertNoOffsetAnywhere();

        runFrame(0, 0, 0.5f);
        assertNoOffsetAnywhere();
    }

    @Test
    void destinationOutsideTheWindowWritesNothingUntilScrolledIn() {
        assertDoesNotThrow(() -> window.startSlide(19, 1, 20, 1, 0f, 1f, Interpolation.linear));

        assertDoesNotThrow(() -> runFrame(0, 0, 0.5f));
        assertNoOffsetAnywhere();

        runFrame(11, 0, 0.5f);
        assertGridOffset(9, 1, -0.5f * CELL_WIDTH, 0f);
    }

    @Test
    void outsideSlideKeepsRunningByTheClock() {
        window.startSlide(19, 1, 20, 1, 0f, 1f, Interpolation.linear);

        runFrame(0, 0, 0.9f);
        runFrame(11, 0, 1f);

        assertNoOffsetAnywhere();
    }

    @Test
    void chainingContinuesFromTheDrawnPosition() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);
        window.startSlide(1, 0, 2, 0, 0.5f, 1f, Interpolation.linear);

        runFrame(0, 0, 0.5f);

        // Drawn at -0.5 cells from cell 1, which is -1.5 cells from cell 2.
        assertGridOffset(2, 0, -1.5f * CELL_WIDTH, 0f);
        assertGridOffset(1, 0, 0f, 0f);
    }

    @Test
    void replacingASlideOnTheSameTargetDoesNotAddOffsets() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);
        window.startSlide(5, 0, 1, 0, 0f, 1f, Interpolation.linear);

        runFrame(0, 0, 0.5f);

        assertGridOffset(1, 0, 2f * CELL_WIDTH, 0f);
    }

    @Test
    void zeroDurationAddsNothing() {
        window.startSlide(0, 0, 1, 0, 0f, 0f, Interpolation.linear);

        runFrame(0, 0, 0f);

        assertNoOffsetAnywhere();
    }

    @Test
    void zeroDurationStillRemovesAChainedSlide() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);
        window.startSlide(1, 0, 2, 0, 0.5f, 0f, Interpolation.linear);

        runFrame(0, 0, 0.5f);

        assertNoOffsetAnywhere();
    }

    @Test
    void zeroDurationStillRemovesAReplacedSlide() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);
        window.startSlide(5, 5, 1, 0, 0f, 0f, Interpolation.linear);

        runFrame(0, 0, 0.5f);

        assertNoOffsetAnywhere();
    }

    @Test
    void clearSlidesDropsEverySlide() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);
        window.startSlide(3, 3, 4, 3, 0f, 1f, Interpolation.linear);

        window.clearSlides();
        runFrame(0, 0, 0.5f);

        assertNoOffsetAnywhere();
    }

    @Test
    void invalidArgumentsThrowAndLeaveExistingSlidesUntouched() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);

        assertThrows(NullPointerException.class,
                () -> window.startSlide(1, 0, 2, 0, 0.5f, 1f, null));
        assertThrows(IllegalArgumentException.class,
                () -> window.startSlide(1, 0, 2, 0, 0.5f, -1f, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> window.startSlide(1, 0, 2, 0, 0.5f, Float.NaN, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> window.startSlide(1, 0, 2, 0, Float.NaN, 1f, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> window.startSlide(1, 0, 2, 0, Float.POSITIVE_INFINITY, 1f,
                        Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> window.startSlide(1, 0, 0, 0, 0.5f, -1f, Interpolation.linear));

        runFrame(0, 0, 0.5f);

        assertGridOffset(1, 0, -0.5f * CELL_WIDTH, 0f);
        assertGridOffset(2, 0, 0f, 0f);
    }

    @Test
    void everyNewMethodThrowsAfterDetach() {
        layer.disableMapWindow();

        assertThrows(IllegalStateException.class,
                () -> window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear));
        assertThrows(IllegalStateException.class, () -> window.applySlides(0f));
        assertThrows(IllegalStateException.class, window::clearSlides);
        assertThrows(IllegalStateException.class, () -> window.getSlideOffsetX(1, 0, 0f));
        assertThrows(IllegalStateException.class, () -> window.getSlideOffsetY(1, 0, 0f));
    }

    @Test
    void offsetQueryMatchesTheAppliedOffset() {
        window.startSlide(2, 2, 3, 4, 0f, 1f, Interpolation.pow2Out);
        float queriedX = window.getSlideOffsetX(3, 4, 0.3f);
        float queriedY = window.getSlideOffsetY(3, 4, 0.3f);

        runFrame(0, 0, 0.3f);

        assertGridOffset(3, 4, queriedX, queriedY);
        assertEquals(-CELL_WIDTH * (1f - Interpolation.pow2Out.apply(0.3f)), queriedX, DELTA);
    }

    @Test
    void offsetQueryIsZeroWithoutASlideAndWorksOutsideTheWindow() {
        window.startSlide(19, 1, 20, 1, 0f, 1f, Interpolation.linear);

        assertEquals(0f, window.getSlideOffsetX(5, 5, 0.5f));
        assertEquals(0f, window.getSlideOffsetY(5, 5, 0.5f));
        assertEquals(-0.5f * CELL_WIDTH, window.getSlideOffsetX(20, 1, 0.5f), DELTA);
        assertEquals(0f, window.getSlideOffsetY(20, 1, 0.5f), DELTA);
    }

    @Test
    void nonFiniteTimeThrowsAndLeavesSlidesUntouched() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);

        for (float badTime : new float[] {Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> window.applySlides(badTime));
            assertThrows(IllegalArgumentException.class,
                    () -> window.getSlideOffsetX(1, 0, badTime));
            assertThrows(IllegalArgumentException.class,
                    () -> window.getSlideOffsetY(1, 0, badTime));
        }
        assertThrows(IllegalArgumentException.class,
                () -> window.startSlide(1, 0, 2, 0, 0.5f, Float.POSITIVE_INFINITY,
                        Interpolation.linear));
        runFrame(0, 0, 0.5f);

        assertGridOffset(1, 0, -0.5f * CELL_WIDTH, 0f);
    }

    @Test
    void offsetQueryDoesNotRemoveFinishedSlides() {
        window.startSlide(0, 0, 1, 0, 0f, 1f, Interpolation.linear);

        assertEquals(0f, window.getSlideOffsetX(1, 0, 2f));
        runFrame(0, 0, 0.5f);

        assertGridOffset(1, 0, -0.5f * CELL_WIDTH, 0f);
    }

    @Test
    void slideOffsetCombinesWithAnOffsetAlreadySetThisFrame() {
        window.startSlide(2, 0, 1, 0, 0f, 1f, Interpolation.linear);
        stack.beginCellEffects(0f);
        window.obtainFrameCellHandle(1, 0).setOffset(3f, 2f);

        window.applySlides(0.5f);

        assertGridOffset(1, 0, 3f + 0.5f * CELL_WIDTH, 2f);
    }
}
