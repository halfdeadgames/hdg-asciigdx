package com.github.halfdeadgames.asciigdx.layer;

import com.github.halfdeadgames.asciigdx.grid.BenchmarkGridSize;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Times the CPU cost per cell when every cell is animated through its frame handle each frame:
 * setting the handle values, combining them with the lasting handle, and rewriting the cell's quad.
 * Each invocation sweeps {@value #CELLS_PER_INVOCATION} cells, continuing where the last one
 * stopped and wrapping around the grid, so the score is ns per cell.
 *
 * <p>Run with {@code .\gradlew.bat :core:jmh -PjmhIncludes=AnimatedCell}. For comparisons add
 * {@code -PjmhForks=3 -PjmhIterations=10}.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AnimatedCellBenchmark {

    private static final int FOREGROUND = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x000000FF;
    private static final int CELLS_PER_INVOCATION = 4096;
    private static final int VALUE_TABLE_SIZE = 1024;
    private static final int VALUE_TABLE_MASK = VALUE_TABLE_SIZE - 1;
    private static final float MAX_OFFSET = 4f;
    private static final float MAX_ROTATION_DEGREES = 30f;
    private static final float MIN_SCALE = 0.5f;
    private static final float SCALE_RANGE = 1f;
    private static final float MIN_ALPHA = 0.25f;
    private static final float ALPHA_RANGE = 0.75f;
    private static final float TABLE_STEP_RADIANS = (float) (2 * Math.PI / 64);
    private static final int INITIAL_SHIFT_STEP = 7;

    @Param({BenchmarkGridSize.SMALL, BenchmarkGridSize.STRESS_SCENE,
            BenchmarkGridSize.FOUR_K_AT_8_PIXELS})
    public String gridSize;

    private int columns;
    private int cellCount;
    private CellHandle[] lastingHandles;
    private CellHandle[] frameHandles;
    private CellHandle[] combinedHandles;
    private FlatCellWriter writer;
    private float[] offsets;
    private float[] rotations;
    private float[] scales;
    private float[] alphas;
    private int nextCell;
    private int tableShift;

    @Setup
    public void createHandlesAndWriter() {
        columns = BenchmarkGridSize.columnsOf(gridSize);
        int rows = BenchmarkGridSize.rowsOf(gridSize);
        cellCount = columns * rows;
        TerminalGrid grid = new TerminalGrid(columns, rows);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                grid.set(column, row, 'a', FOREGROUND, BACKGROUND);
            }
        }
        int cellSize = SingleRegionGlyphSource.CELL_SIZE;
        CellVertexCache cache = new CellVertexCache(columns, rows, cellSize, cellSize);
        writer = new FlatCellWriter(grid, cache, new SingleRegionGlyphSource());
        createHandles();
        fillValueTables();
        tableShift = INITIAL_SHIFT_STEP;
    }

    private void createHandles() {
        lastingHandles = new CellHandle[cellCount];
        frameHandles = new CellHandle[cellCount];
        combinedHandles = new CellHandle[cellCount];
        for (int cell = 0; cell < cellCount; cell++) {
            int column = cell % columns;
            int row = cell / columns;
            lastingHandles[cell] = new CellHandle(column, row);
            frameHandles[cell] = new CellHandle(column, row);
            combinedHandles[cell] = new CellHandle(column, row);
        }
    }

    private void fillValueTables() {
        offsets = new float[VALUE_TABLE_SIZE];
        rotations = new float[VALUE_TABLE_SIZE];
        scales = new float[VALUE_TABLE_SIZE];
        alphas = new float[VALUE_TABLE_SIZE];
        for (int i = 0; i < VALUE_TABLE_SIZE; i++) {
            float wave = (float) Math.sin(i * TABLE_STEP_RADIANS);
            float unitWave = 0.5f + 0.5f * wave;
            offsets[i] = MAX_OFFSET * wave;
            rotations[i] = MAX_ROTATION_DEGREES * wave;
            scales[i] = MIN_SCALE + SCALE_RANGE * unitWave;
            alphas[i] = MIN_ALPHA + ALPHA_RANGE * unitWave;
        }
    }

    /** Frame handles already hold their values: only combining and writing is timed. */
    @Benchmark
    @OperationsPerInvocation(CELLS_PER_INVOCATION)
    public void unchangedHandles() {
        for (int step = 0; step < CELLS_PER_INVOCATION; step++) {
            int cell = takeNextCell();
            combineAndWrite(cell);
        }
    }

    /** Every cell's frame handle gets a new offset and cell alpha. */
    @Benchmark
    @OperationsPerInvocation(CELLS_PER_INVOCATION)
    public void animateOffsetAndAlpha() {
        advanceTableShift();
        for (int step = 0; step < CELLS_PER_INVOCATION; step++) {
            int cell = takeNextCell();
            setOffsetAndAlpha(frameHandles[cell], cell);
            combineAndWrite(cell);
        }
    }

    /** Every cell's frame handle gets a new offset, cell alpha, rotation, and scale. */
    @Benchmark
    @OperationsPerInvocation(CELLS_PER_INVOCATION)
    public void animateOffsetAlphaRotationAndScale() {
        advanceTableShift();
        for (int step = 0; step < CELLS_PER_INVOCATION; step++) {
            int cell = takeNextCell();
            CellHandle frame = frameHandles[cell];
            setOffsetAndAlpha(frame, cell);
            frame.setRotation(rotations[(cell + tableShift) & VALUE_TABLE_MASK]);
            float scale = scales[(cell + 2 * tableShift) & VALUE_TABLE_MASK];
            frame.setScale(scale, scale);
            combineAndWrite(cell);
        }
    }

    private void setOffsetAndAlpha(CellHandle frame, int cell) {
        int index = (cell + tableShift) & VALUE_TABLE_MASK;
        frame.setOffset(offsets[index], offsets[(index + INITIAL_SHIFT_STEP) & VALUE_TABLE_MASK]);
        frame.setCellAlpha(alphas[index]);
    }

    private void combineAndWrite(int cell) {
        CellHandle combined = combinedHandles[cell];
        CellHandleCombine.combine(combined, lastingHandles[cell], frameHandles[cell]);
        writer.write(cell % columns, cell / columns, combined.isLifted(), combined);
    }

    private int takeNextCell() {
        int cell = nextCell;
        nextCell = cell + 1 == cellCount ? 0 : cell + 1;
        return cell;
    }

    private void advanceTableShift() {
        tableShift = (tableShift + INITIAL_SHIFT_STEP) & VALUE_TABLE_MASK;
    }
}
