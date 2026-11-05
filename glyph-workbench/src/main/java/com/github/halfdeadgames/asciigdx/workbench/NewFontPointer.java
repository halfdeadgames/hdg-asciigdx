package com.github.halfdeadgames.asciigdx.workbench;

/** One frame of mouse state for the new-font dialogs: wheel notches, left click, pixel position, and cell size. */
record NewFontPointer(int wheelTicks, boolean clicked, int pointerX, int pointerY, int cell) {
}
