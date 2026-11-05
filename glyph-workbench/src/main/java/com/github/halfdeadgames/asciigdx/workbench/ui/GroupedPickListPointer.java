package com.github.halfdeadgames.asciigdx.workbench.ui;

/**
 * Turns the pointer into list actions for a {@link GroupedPickList} in a dialog. The highlight follows the
 * pointer only after it moves, so arrow keys are not undone by a resting pointer.
 */
public final class GroupedPickListPointer {

    /** What a frame's pointer did that the dialog must act on. */
    public enum Click { NONE, OUTSIDE, PICK }

    private boolean lastPointerKnown;
    private int lastPointerX;
    private int lastPointerY;

    /** Forgets the last pointer, so the next frame only records it. */
    public void reset() {
        lastPointerKnown = false;
    }

    /**
     * Follows the pointer with the highlight, then applies a click.
     *
     * @param hit what lies under the pointer
     * @param listTop the cell row of the first list line
     * @return {@link Click#OUTSIDE} for a click off the dialog, {@link Click#PICK} for a click on a choosable row
     *     (now highlighted), otherwise {@link Click#NONE}; a click on the scroll bar scrolls the window
     */
    public Click handle(GroupedPickList<?> list, ListHit hit, int pointerX, int pointerY, boolean clicked,
                        int listTop) {
        followPointer(list, hit, pointerX, pointerY);
        if (!clicked) {
            return Click.NONE;
        }
        if (!hit.inside()) {
            return Click.OUTSIDE;
        }
        if (hit.onBar()) {
            clickBar(list, hit, listTop);
            return Click.NONE;
        }
        list.hover(hit.row());
        return list.isChoosable(hit.row()) ? Click.PICK : Click.NONE;
    }

    private void followPointer(GroupedPickList<?> list, ListHit hit, int pointerX, int pointerY) {
        if (!lastPointerKnown) {
            lastPointerKnown = true;
            lastPointerX = pointerX;
            lastPointerY = pointerY;
            return;
        }
        if (pointerX == lastPointerX && pointerY == lastPointerY) {
            return;
        }
        lastPointerX = pointerX;
        lastPointerY = pointerY;
        list.hover(hit.row());
    }

    private static void clickBar(GroupedPickList<?> list, ListHit hit, int listTop) {
        int barCells = list.shownRows() * SelectList.PITCH - 1;
        ScrollBarHit.click(list.window(), ScrollBarHit.part(list.window(), hit.cellRow() - listTop, barCells));
    }
}
