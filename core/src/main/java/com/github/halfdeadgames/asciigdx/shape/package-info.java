/**
 * Allocation-free descriptions of sets of grid cells (rectangles, circles, and lines) that report
 * each cell with its distance from the shape's center. Pure Java with no grid, layer, or rendering
 * dependency.
 *
 * <p>Coordinates are cells: columns grow to the right and rows grow downward (y-down). Every shape
 * implements {@link com.github.halfdeadgames.asciigdx.shape.CellShape}, which visits its cells
 * row by row from top to bottom and left to right. Shapes are mutable and not thread-safe.
 * Consumers such as {@link com.github.halfdeadgames.asciigdx.draw.GridDrawing} and
 * {@link com.github.halfdeadgames.asciigdx.zone.EffectZone} take a shape and clip it to their own
 * bounds.
 *
 * <p>Example, drawing the outline of a rectangle onto a grid and then reading the cells of a
 * circle:
 * <pre>{@code
 * RectangleShape frame = RectangleShape.forSquareCells(1, 1, 5, 5, ShapeFill.OUTLINE);
 * GridDrawing.fillShape(grid, frame, '#', foreground, background);
 *
 * CircleShape disc = new CircleShape(10.5f, 10.5f, 4f, ShapeFill.FILLED, 2f);
 * disc.forEachCell((column, row, distance, distanceFraction) ->
 *         grid.set(column, row, '*', foreground, background));
 * }</pre>
 *
 * <p>Lines run between any two cells with
 * {@link com.github.halfdeadgames.asciigdx.shape.LineShape}, for example
 * {@code LineShape.forSquareCells(0, 0, 9, 3, LineStyle.DIAGONAL_STEPS)}.
 */
package com.github.halfdeadgames.asciigdx.shape;
