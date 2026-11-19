/**
 * Static drawing helpers that write text, rectangles, boxes (see
 * {@link com.github.halfdeadgames.asciigdx.draw.BoxStyle}), eighth-block bars, and cell shapes
 * into a
 * {@link com.github.halfdeadgames.asciigdx.grid.TerminalGrid}. The entry point is
 * {@link com.github.halfdeadgames.asciigdx.draw.GridDrawing}.
 *
 * <p>Positions are column and row indices in cells, not pixels. The origin (0, 0) is the top-left
 * cell, columns grow to the right, and rows grow downward (y-down).
 *
 * <p>Colors are packed RGBA8888 ints ({@code 0xRRGGBBAA}). A 6-digit hex literal has no alpha byte
 * and draws the wrong color, so wrap it with
 * {@link com.github.halfdeadgames.asciigdx.color.CellColor#rgb(int)}.
 *
 * <p>Writes outside the grid are skipped, so text, rectangles, boxes, bars, and shapes may start or end
 * off-grid. Size arguments must not be negative, and object arguments must not be {@code null}.
 *
 * <p>Nothing in this package is thread-safe. Call it from the render thread.
 *
 * <p>Example:
 * <pre>{@code
 * TerminalGrid grid = new TerminalGrid(40, 12);
 * GridDrawing.drawBox(grid, 1, 1, 20, 5, 0xFFFFFFFF, 0x000000FF);
 * GridDrawing.drawText(grid, 3, 2, "Hello", 0xFFFF00FF, 0x000000FF);
 * GridDrawing.drawBox(grid, 24, 1, 10, 5, BoxStyle.DOUBLE, 0xFFFFFFFF, 0x000000FF);
 * GridDrawing.drawHorizontalBar(grid, 3, 4, 10, 0.55f, 0x00FF00FF, 0x000000FF);
 *
 * RectangleShape frame = RectangleShape.forSquareCells(24, 1, 8, 5, ShapeFill.OUTLINE);
 * GridDrawing.fillShape(grid, frame, '#', 0x00FF00FF, 0x000000FF);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.draw;
