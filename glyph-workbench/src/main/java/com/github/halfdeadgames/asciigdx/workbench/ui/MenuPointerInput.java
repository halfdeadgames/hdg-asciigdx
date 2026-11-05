package com.github.halfdeadgames.asciigdx.workbench.ui;

/**
 * One frame of pointer state for {@link MenuPointer#update}.
 *
 * @param column        pointer cell column
 * @param row           pointer cell row, counted from the top of the screen
 * @param totalColumns  screen width in cells
 * @param escapePressed Escape went down this frame
 * @param pressStarted  the left button went down this frame
 * @param buttonDown    the left button is down now; false together with a held press means a release
 * @param pointerBusy   another interaction (a paint stroke, a scrollbar drag) owns the pointer: the bar
 *                      then ignores presses and lights nothing
 */
public record MenuPointerInput(int column, int row, int totalColumns, boolean escapePressed,
                               boolean pressStarted, boolean buttonDown, boolean pointerBusy) {
}
