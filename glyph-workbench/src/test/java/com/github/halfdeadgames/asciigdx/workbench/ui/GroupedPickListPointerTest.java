package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickListPointer.Click;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupedPickListPointerTest {

    private static final int LIST_TOP = 10;
    private static final int HEADING_ROW = 0;
    private static final int FIRST_CHOICE = 1;
    private static final int SECOND_CHOICE = 2;

    private final GroupedPickList<String> list = new GroupedPickList<>();
    private final GroupedPickListPointer pointer = new GroupedPickListPointer();

    @BeforeEach
    void fillList() {
        List<SelectList.Row> rows = new ArrayList<>();
        List<String> entries = new ArrayList<>();
        rows.add(new SelectList.Row("HEAD", false, false, 0));
        entries.add(null);
        for (int i = 0; i < 7; i++) {
            rows.add(new SelectList.Row("ITEM" + i, true, false, 0));
            entries.add("item" + i);
        }
        list.setRows(rows, entries);
        list.move(1);
    }

    private static ListHit onRow(int row) {
        return new ListHit(true, false, LIST_TOP, row);
    }

    private Click handle(ListHit hit, int pointerX, int pointerY, boolean clicked) {
        return pointer.handle(list, hit, pointerX, pointerY, clicked, LIST_TOP);
    }

    @Test
    void firstFrameOnlyRecordsThePointer() {
        handle(onRow(SECOND_CHOICE), 5, 5, false);
        assertEquals(FIRST_CHOICE, list.selectedIndex());
    }

    @Test
    void hoverFollowsAfterThePointerMoves() {
        handle(onRow(FIRST_CHOICE), 5, 5, false);
        handle(onRow(SECOND_CHOICE), 6, 5, false);
        assertEquals(SECOND_CHOICE, list.selectedIndex());
    }

    @Test
    void restingPointerDoesNotOverrideArrowKeys() {
        handle(onRow(FIRST_CHOICE), 5, 5, false);
        list.move(1);
        handle(onRow(FIRST_CHOICE), 5, 5, false);
        assertEquals(SECOND_CHOICE, list.selectedIndex());
    }

    @Test
    void resetMakesTheNextFrameRecordOnly() {
        handle(onRow(FIRST_CHOICE), 5, 5, false);
        pointer.reset();
        handle(onRow(SECOND_CHOICE), 9, 9, false);
        assertEquals(FIRST_CHOICE, list.selectedIndex());
    }

    @Test
    void clickOutsideReportsOutside() {
        Click click = handle(new ListHit(false, false, 0, -1), 0, 0, true);
        assertEquals(Click.OUTSIDE, click);
    }

    @Test
    void noClickReportsNone() {
        assertEquals(Click.NONE, handle(onRow(SECOND_CHOICE), 5, 5, false));
    }

    @Test
    void barClickScrollsTheWindowNotTheCursor() {
        list.setCapacity(3);
        int cursor = list.selectedIndex();
        int offsetBefore = list.window().offset();
        int lastBarCell = LIST_TOP + list.shownRows() * SelectList.PITCH - 2;
        Click click = handle(new ListHit(true, true, lastBarCell, -1), 5, 5, true);
        assertEquals(Click.NONE, click);
        assertEquals(offsetBefore + 1, list.window().offset());
        assertEquals(cursor, list.selectedIndex());
    }

    @Test
    void clickOnChoosableRowPicksIt() {
        Click click = handle(onRow(SECOND_CHOICE), 5, 5, true);
        assertEquals(Click.PICK, click);
        assertEquals(SECOND_CHOICE, list.selectedIndex());
    }

    @Test
    void clickOnHeadingPicksNothing() {
        Click click = handle(onRow(HEADING_ROW), 5, 5, true);
        assertEquals(Click.NONE, click);
        assertEquals(FIRST_CHOICE, list.selectedIndex());
    }

    @Test
    void clickOnNoRowPicksNothing() {
        assertEquals(Click.NONE, handle(onRow(-1), 5, 5, true));
    }
}
