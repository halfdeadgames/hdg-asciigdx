package com.github.halfdeadgames.asciigdx.grid;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class GridSetBenchmark {

    private static final int FOREGROUND = 0xFFFFFFFF;
    private static final int BACKGROUND = 0x000000FF;

    @Param({BenchmarkGridSize.SMALL, BenchmarkGridSize.STRESS_SCENE,
            BenchmarkGridSize.FOUR_K_AT_8_PIXELS})
    public String gridSize;

    private int columns;
    private int rows;
    private TerminalGrid grid;
    private DirtyTracker tracker;
    private int codePoint;

    @Setup
    public void createGrid() {
        columns = BenchmarkGridSize.columnsOf(gridSize);
        rows = BenchmarkGridSize.rowsOf(gridSize);
        grid = new TerminalGrid(columns, rows);
        tracker = grid.createDirtyTracker();
    }

    /** Alternates the code point so every set changes the cell; an unchanged set returns early. */
    @Benchmark
    public void setEveryCell() {
        codePoint = codePoint == 'a' ? 'b' : 'a';
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                grid.set(column, row, codePoint, FOREGROUND, BACKGROUND);
            }
        }
        tracker.markAllClean();
    }
}
