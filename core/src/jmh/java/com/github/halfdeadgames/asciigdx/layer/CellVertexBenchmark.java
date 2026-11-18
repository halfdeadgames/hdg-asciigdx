package com.github.halfdeadgames.asciigdx.layer;

import com.github.halfdeadgames.asciigdx.grid.BenchmarkGridSize;
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

/** Times a full rebuild: every cell's quad rewritten, as after a resize or a font switch. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class CellVertexBenchmark {

    private static final int FOREGROUND = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x000000FF;

    @Param({BenchmarkGridSize.SMALL, BenchmarkGridSize.STRESS_SCENE,
            BenchmarkGridSize.FOUR_K_AT_8_PIXELS})
    public String gridSize;

    private int columns;
    private int rows;
    private FlatCellWriter writer;

    @Setup
    public void createGridAndCache() {
        columns = BenchmarkGridSize.columnsOf(gridSize);
        rows = BenchmarkGridSize.rowsOf(gridSize);
        TerminalGrid grid = new TerminalGrid(columns, rows);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                grid.set(column, row, 'a', FOREGROUND, BACKGROUND);
            }
        }
        int cellSize = SingleRegionGlyphSource.CELL_SIZE;
        CellVertexCache cache = new CellVertexCache(columns, rows, cellSize, cellSize);
        writer = new FlatCellWriter(grid, cache, new SingleRegionGlyphSource());
    }

    @Benchmark
    public void rebuildAllCells() {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                writer.write(column, row, false, null);
            }
        }
    }
}
