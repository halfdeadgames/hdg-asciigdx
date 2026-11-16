/**
 * Cell storage for terminal-style displays: a
 * {@link com.github.halfdeadgames.asciigdx.grid.TerminalGrid} of cells, a read-only
 * {@link com.github.halfdeadgames.asciigdx.grid.TerminalView} of it, and
 * {@link com.github.halfdeadgames.asciigdx.grid.DirtyTracker} for finding which cells changed.
 *
 * <p>Each cell holds a code point (a Unicode value that picks the glyph the font draws), a
 * foreground color, and a background color. The package stores display data only.
 *
 * <p>Coordinates: positions are column and row indices in cells, not pixels. The origin (0, 0) is
 * the top-left cell, {@code x} grows to the right, and {@code y} grows downward.
 *
 * <p>Colors: packed RGBA8888 ints ({@code 0xRRGGBBAA}). A 6-digit hex literal such as
 * {@code 0xFF4040} has no alpha byte and draws the wrong color; wrap it with
 * {@link com.github.halfdeadgames.asciigdx.color.CellColor#rgb(int)}.
 *
 * <p>Bounds rule: writes outside the grid are silently ignored, which allows clipped drawing. Reads
 * outside the grid, including
 * {@link com.github.halfdeadgames.asciigdx.grid.DirtyTracker#isDirty(int, int)}, throw
 * {@link java.lang.IndexOutOfBoundsException}. Use
 * {@link com.github.halfdeadgames.asciigdx.grid.TerminalView#contains(int, int)} to check first.
 *
 * <p>Dirty tracking: a cell becomes dirty when its content actually changes; writing identical
 * content does not mark it. Each consumer creates its own tracker, which starts with all cells
 * dirty, visits the dirty cells, marks them clean, and releases the tracker when done. Resizing the
 * grid with a call that changes anything, or scrolling it, makes every cell dirty for every
 * registered tracker.
 *
 * <p>Thread rule: nothing in this package is thread-safe. Use it from the render thread.
 *
 * <p>Example:
 * <pre>{@code
 * TerminalGrid grid = new TerminalGrid(80, 25);
 * grid.set(0, 0, 'A', CellColor.rgb(0xFFFFFF), TerminalGrid.TRANSPARENT_RGBA);
 * grid.setCodePoint(1, 0, 'B');
 * int codePoint = grid.getCodePoint(0, 0);
 *
 * DirtyTracker tracker = grid.createDirtyTracker();
 * tracker.forEachDirtyCell((x, y) -> {
 *     // redraw the cell at column x, row y
 * });
 * tracker.markAllClean();
 *
 * tracker.release();
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.grid;
