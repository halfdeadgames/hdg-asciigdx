/**
 * Text layout and drawing that handles text longer than its area.
 *
 * <p>{@link com.github.halfdeadgames.asciigdx.text.TextDrawing} fits one line of text into a fixed
 * width, either cut short with an ellipsis or scrolling over time.
 * {@link com.github.halfdeadgames.asciigdx.text.TextScroll} describes the scrolling motion.
 *
 * <p>Conventions:
 * <ul>
 *   <li>Positions and sizes are in cells. Columns grow to the right and rows grow down.</li>
 *   <li>Text length is counted in code points, one cell each.</li>
 *   <li>Time is in seconds, passed in by the game. Nothing here reads a clock or counts
 *       frames.</li>
 *   <li>Drawing methods are called on the render thread and do not allocate.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * // Cut short with an ellipsis.
 * TextDrawing.drawTruncated(grid, 2, 1, 12, title, TextDrawing.DEFAULT_ELLIPSIS, fg, bg);
 *
 * // Scroll back and forth at 6 cells per second, pausing 1 second at each end.
 * TextScroll scroll = TextScroll.backAndForth(6f, 1f);
 * TextDrawing.drawScrolling(grid, 2, 2, 12, title, scroll, secondsSinceStart,
 *         TextDrawing.DEFAULT_ELLIPSIS, fg, bg);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.text;
