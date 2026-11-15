/**
 * Fitting a glyph grid to a window. Finds the largest whole-number scale that still shows a
 * minimum number of columns and rows, picks the first font in a preference list that meets that
 * minimum, and scales a libGDX viewport by whole numbers only: either the largest factor that
 * fits or the fixed scale of a fit.
 *
 * <p>Call {@code GridFit.calculate} whenever the window size changes, not every frame:
 *
 * <pre>{@code
 * public void resize(int width, int height) {
 *     GridFit fit = GridFit.calculate(width, height, 8, 8, 80, 45);
 *     if (fit.isEmpty()) {
 *         return;
 *     }
 *     grid = new TerminalGrid(fit.getColumns(), fit.getRows());
 *     fit.applyTo(viewport, true);
 * }
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.fit;
