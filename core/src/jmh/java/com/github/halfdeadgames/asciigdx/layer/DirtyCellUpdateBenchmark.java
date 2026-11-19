package com.github.halfdeadgames.asciigdx.layer;

import com.github.halfdeadgames.asciigdx.grid.BenchmarkGridSize;
import com.github.halfdeadgames.asciigdx.grid.DirtyCellVisitor;
import com.github.halfdeadgames.asciigdx.grid.DirtyTracker;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Times a normal frame's CPU work: changing a share of cells with grid.set, then rewriting only
 * the dirty quads. Includes the set cost; GridSetBenchmark times set alone.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class DirtyCellUpdateBenchmark {

    private static final int FOREGROUND = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x000000FF;
    private static final int ALL_CELLS_PERCENT = 100;

    @Param({BenchmarkGridSize.SMALL, BenchmarkGridSize.STRESS_SCENE,
            BenchmarkGridSize.FOUR_K_AT_8_PIXELS})
    public String gridSize;

    /** Share of cells changed per frame, spread evenly over the grid. */
    @Param({"10", "100"})
    public int dirtyPercent;

    private int cellCount;
    private int columns;
    private TerminalGrid grid;
    private DirtyTracker tracker;
    private DirtyCellVisitor rewriteCell;
    private int codePoint;

    @Setup
    public void createGridAndCache() {
        columns = BenchmarkGridSize.columnsOf(gridSize);
        int rows = BenchmarkGridSize.rowsOf(gridSize);
        cellCount = columns * rows;
        grid = new TerminalGrid(columns, rows);
        tracker = grid.createDirtyTracker();
        int cellSize = SingleRegionGlyphSource.CELL_SIZE;
        CellVertexCache cache = new CellVertexCache(columns, rows, cellSize, cellSize);
        FlatCellWriter writer = new FlatCellWriter(grid, cache, new SingleRegionGlyphSource());
        rewriteCell = (column, row) -> writer.write(column, row, false, null);
        tracker.forEachDirtyCell(rewriteCell);
        tracker.markAllClean();
    }

    /** Alternates the code point so every set changes the cell; an unchanged set returns early. */
    @Benchmark
    public void updateDirtyCells() {
        codePoint = codePoint == 'a' ? 'b' : 'a';
        int stride = 100 / dirtyPercent;
        for (int cell = 0; cell < cellCount; cell += stride) {
            grid.set(cell % columns, cell / columns, codePoint, FOREGROUND, BACKGROUND);
        }
        tracker.forEachDirtyCell(rewriteCell);
        tracker.markAllClean();
    }

    /**
     * Same cells and values as {@link #updateDirtyCells()}, with column and row stepped alongside
     * the flat index instead of divided out of it.
     */
    @Benchmark
    public void updateDirtyCellsWithoutIndexDivision() {
        codePoint = codePoint == 'a' ? 'b' : 'a';
        int stride = ALL_CELLS_PERCENT / dirtyPercent;
        int column = 0;
        int row = 0;
        for (int cell = 0; cell < cellCount; cell += stride) {
            grid.set(column, row, codePoint, FOREGROUND, BACKGROUND);
            column += stride;
            while (column >= columns) {
                column -= columns;
                row++;
            }
        }
        tracker.forEachDirtyCell(rewriteCell);
        tracker.markAllClean();
    }
}
