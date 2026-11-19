package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class TableTest {

    private static final class Box extends Widget {
        private final int preferredWidth;
        private final int preferredHeight;
        private final int rowSnap;

        Box(int preferredWidth, int preferredHeight) {
            this(preferredWidth, preferredHeight, 1);
        }

        Box(int preferredWidth, int preferredHeight, int rowSnap) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
            this.rowSnap = rowSnap;
        }

        @Override
        public int getPreferredWidth() {
            return preferredWidth;
        }

        @Override
        public int getPreferredHeight() {
            return preferredHeight;
        }

        @Override
        public int getRowSnap() {
            return rowSnap;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }
    }

    private static final class CountingTable extends Table {
        int layoutCalls;

        @Override
        protected void layout() {
            layoutCalls++;
            super.layout();
        }
    }

    private static void assertBounds(Widget widget, int x, int y, int width, int height) {
        assertEquals(x, widget.getX(), "x");
        assertEquals(y, widget.getY(), "y");
        assertEquals(width, widget.getWidth(), "width");
        assertEquals(height, widget.getHeight(), "height");
    }

    @Test
    void countsRowsAndColumnsWithColspans() {
        Table table = new Table();
        table.add(new Box(1, 1));
        table.add(new Box(1, 1));
        table.row();
        table.add(new Box(1, 1)).colspan(3);

        assertEquals(2, table.getRowCount());
        assertEquals(3, table.getColumnCount());
    }

    @Test
    void rowOnEmptyRowDoesNothing() {
        Table table = new Table();
        table.row();
        table.row();
        table.add(new Box(1, 1));
        table.row();
        table.row();
        table.add(new Box(1, 1));

        assertEquals(2, table.getRowCount());
    }

    @Test
    void emptyTableHasNoRowsAndSnapOne() {
        Table table = new Table();

        assertEquals(0, table.getRowCount());
        assertEquals(0, table.getColumnCount());
        assertEquals(1, table.getRowSnap());
    }

    @Test
    void preferredSizeSumsColumnsRowsAndPadding() {
        Table table = new Table();
        table.setPadding(1);
        table.add(new Box(3, 1));
        table.add(new Box(5, 2));

        assertEquals(10, table.getPreferredWidth());
        assertEquals(4, table.getPreferredHeight());
    }

    @Test
    void columnWidthIsMaxOfItsCells() {
        Table table = new Table();
        table.add(new Box(3, 1));
        Box second = new Box(2, 1);
        table.add(second);
        table.row();
        table.add(new Box(6, 1));
        table.validate();

        assertEquals(8, table.getPreferredWidth());
        assertEquals(6, second.getX());
    }

    @Test
    void colspanShortfallIsSpreadWithRemainderToLeftmost() {
        Table table = new Table();
        table.add(new Box(2, 1));
        Box second = new Box(2, 1);
        table.add(second);
        table.row();
        table.add(new Box(7, 1)).colspan(2);
        table.validate();

        assertEquals(7, table.getPreferredWidth());
        assertEquals(4, second.getX());
    }

    @Test
    void colspanThatFitsChangesNothing() {
        Table table = new Table();
        table.add(new Box(4, 1));
        table.add(new Box(4, 1));
        table.row();
        table.add(new Box(5, 1)).colspan(2);

        assertEquals(8, table.getPreferredWidth());
    }

    @Test
    void expandXSharesExtraEqually() {
        Table table = new Table();
        table.add(new Box(5, 1)).expandX();
        Box second = new Box(5, 1);
        table.add(second).expandX();
        table.setSize(20, 1);
        table.validate();

        assertEquals(10, second.getX());
    }

    @Test
    void expandXRemainderGoesToLeftmostExpandingColumns() {
        Table table = new Table();
        table.add(new Box(5, 1)).expandX();
        Box second = new Box(5, 1);
        table.add(second).expandX();
        table.setSize(21, 1);
        table.validate();

        assertEquals(11, second.getX());
    }

    @Test
    void onlyExpandingColumnsGrow() {
        Table table = new Table();
        Box first = new Box(5, 1);
        table.add(first);
        Box second = new Box(5, 1);
        table.add(second).expandX();
        Box third = new Box(2, 1);
        table.add(third);
        table.setSize(20, 1);
        table.validate();

        assertEquals(5, second.getX());
        assertEquals(18, third.getX());
    }

    @Test
    void extraWidthStaysAtRightWithoutExpand() {
        Table table = new Table();
        Box second = new Box(3, 1);
        table.add(new Box(3, 1));
        table.add(second);
        table.setSize(30, 1);
        table.validate();

        assertEquals(3, second.getX());
    }

    @Test
    void expandYSharesExtraAndRemainder() {
        Table table = new Table();
        table.add(new Box(1, 1)).expandY();
        table.row();
        Box second = new Box(1, 1);
        table.add(second).expandY();
        table.setSize(1, 9);
        table.validate();

        assertEquals(5, second.getY());
    }

    @Test
    void smallerTableDoesNotShrinkContent() {
        Table table = new Table();
        table.add(new Box(6, 2));
        Box second = new Box(6, 2);
        table.add(second);
        table.setSize(3, 1);
        table.validate();

        assertEquals(6, second.getX());
        assertEquals(6, second.getWidth());
    }

    @Test
    void fillStretchesWidgetToAreaAndNoFillKeepsPreferredSize() {
        Table table = new Table();
        Box filled = new Box(3, 1);
        Box plain = new Box(3, 1);
        table.add(new Box(10, 4));
        table.row();
        table.add(filled).fillX();
        table.row();
        table.add(plain);
        table.validate();

        assertBounds(filled, 0, 4, 10, 1);
        assertBounds(plain, 0, 5, 3, 1);
    }

    @Test
    void fillYStretchesToRowHeight() {
        Table table = new Table();
        Box filled = new Box(2, 1);
        table.add(filled).fillY();
        table.add(new Box(2, 5));
        table.validate();

        assertBounds(filled, 0, 0, 2, 5);
    }

    @Test
    void fillStretchesBothAxesAndExpandSharesBoth() {
        Table table = new Table();
        Box box = new Box(2, 1);
        table.add(box).expand().fill();
        table.setSize(8, 6);
        table.validate();

        assertBounds(box, 0, 0, 8, 6);
    }

    @Test
    void allNineAlignmentsPlaceWidgetInCellArea() {
        int[] expectedX = {0, 3, 7};
        int[] expectedY = {0, 2, 4};
        HorizontalAlignment[] horizontals = HorizontalAlignment.values();
        VerticalAlignment[] verticals = VerticalAlignment.values();
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                Table table = new Table();
                Box box = new Box(3, 1);
                table.add(box).expand().align(horizontals[column], verticals[row]);
                table.setSize(10, 5);
                table.validate();

                assertBounds(box, expectedX[column], expectedY[row], 3, 1);
            }
        }
    }

    @Test
    void separateAlignmentSettersMatchCombinedSetter() {
        Table table = new Table();
        Box box = new Box(3, 1);
        table.add(box).expand().alignX(HorizontalAlignment.RIGHT)
                .alignY(VerticalAlignment.BOTTOM);
        table.setSize(10, 5);
        table.validate();

        assertBounds(box, 7, 4, 3, 1);
    }

    @Test
    void cellPaddingShiftsWidgetAndGrowsCell() {
        Table table = new Table();
        Box box = new Box(3, 1);
        table.add(box).pad(1, 2, 3, 4);
        table.validate();

        assertEquals(9, table.getPreferredWidth());
        assertEquals(5, table.getPreferredHeight());
        assertBounds(box, 2, 1, 3, 1);
    }

    @Test
    void singleSidePaddingSettersChangeOnlyTheirSide() {
        Table table = new Table();
        TableCell cell = table.add(new Box(3, 1)).pad(1).padTop(2).padLeft(3).padBottom(4)
                .padRight(5);

        assertEquals(2, cell.getPaddingTop());
        assertEquals(3, cell.getPaddingLeft());
        assertEquals(4, cell.getPaddingBottom());
        assertEquals(5, cell.getPaddingRight());
    }

    @Test
    void fillYWithOddTopPaddingStaysInsideTallRow() {
        Table table = new Table();
        Box tall = new Box(3, 2, 2);
        table.add(tall).padTop(1).fillY();
        table.validate();

        assertBounds(tall, 0, 2, 3, 2);
    }

    @Test
    void fillYInHigherRowEndsAtRowBottom() {
        Table table = new Table();
        Box tall = new Box(3, 2, 2);
        table.add(tall).padTop(1).padBottom(1).fillY();
        table.add(new Box(1, 7));
        table.validate();

        assertBounds(tall, 0, 2, 3, 4);
    }

    @Test
    void bottomAlignedTallWidgetStaysOnEvenRowInsideRow() {
        Table table = new Table();
        Box tall = new Box(3, 2, 2);
        table.add(tall).alignY(VerticalAlignment.BOTTOM);
        table.add(new Box(1, 7));
        table.validate();

        assertEquals(4, tall.getY());
        assertTrue(tall.getY() + tall.getHeight() <= 7);
    }

    @Test
    void centerAlignedTallWidgetRoundsOffsetDownToEvenRow() {
        Table table = new Table();
        Box tall = new Box(3, 2, 2);
        table.add(tall).alignY(VerticalAlignment.CENTER);
        table.add(new Box(1, 7));
        table.validate();

        assertEquals(2, tall.getY());
    }

    @Test
    void removedCellSettersThrowAndGettersKeepWorking() {
        Table table = new Table();
        Box box = new Box(1, 1);
        TableCell cell = table.add(box).pad(2).expandX();
        table.removeChild(box);

        assertThrows(IllegalStateException.class, () -> cell.pad(1));
        assertThrows(IllegalStateException.class, () -> cell.expandX(false));
        assertThrows(IllegalStateException.class, () -> cell.fill());
        assertThrows(IllegalStateException.class, () -> cell.colspan(2));
        assertThrows(IllegalStateException.class,
                () -> cell.alignX(HorizontalAlignment.RIGHT));
        assertSame(box, cell.getWidget());
        assertEquals(2, cell.getPaddingTop());
        assertTrue(cell.isExpandX());
    }

    @Test
    void booleanFormsSetAndClearFlags() {
        TableCell cell = new Table().add(new Box(1, 1));

        cell.expandX(true).expandY(true).fillX(true).fillY(true);
        assertTrue(cell.isExpandX() && cell.isExpandY() && cell.isFillX() && cell.isFillY());

        cell.expandX(false).expandY(false).fillX(false).fillY(false);
        assertFalse(cell.isExpandX() || cell.isExpandY() || cell.isFillX() || cell.isFillY());

        cell.expand().fill();
        assertTrue(cell.isExpandX() && cell.isExpandY() && cell.isFillX() && cell.isFillY());
    }

    @Test
    void getCellFindsTheCellOfAChild() {
        Table table = new Table();
        Box first = new Box(1, 1);
        Box second = new Box(1, 1);
        TableCell firstCell = table.add(first);
        table.add(second);

        assertSame(firstCell, table.getCell(first));
        assertSame(second, table.getCell(second).getWidget());
    }

    @Test
    void getCellRejectsNullAndStrangers() {
        Table table = new Table();

        assertThrows(NullPointerException.class, () -> table.getCell(null));
        assertThrows(IllegalArgumentException.class, () -> table.getCell(new Box(1, 1)));
    }

    @Test
    void fillRespectsCellPadding() {
        Table table = new Table();
        Box box = new Box(2, 1);
        table.add(box).pad(1).fill().expand();
        table.setSize(10, 6);
        table.validate();

        assertBounds(box, 1, 1, 8, 4);
    }

    @Test
    void tablePaddingOffsetsContent() {
        Table table = new Table();
        table.setPadding(1, 2, 3, 4);
        Box box = new Box(3, 1);
        table.add(box);
        table.validate();

        assertEquals(9, table.getPreferredWidth());
        assertEquals(5, table.getPreferredHeight());
        assertBounds(box, 2, 1, 3, 1);
        assertEquals(1, table.getPaddingTop());
        assertEquals(2, table.getPaddingLeft());
        assertEquals(3, table.getPaddingBottom());
        assertEquals(4, table.getPaddingRight());
    }

    @Test
    void expandSharesSpaceInsideTablePadding() {
        Table table = new Table();
        table.setPadding(2);
        Box box = new Box(2, 1);
        table.add(box).expand().fill();
        table.setSize(10, 9);
        table.validate();

        assertBounds(box, 2, 2, 6, 5);
    }

    @Test
    void tallWidgetAfterOneRowStartsOnEvenRowAndGapCounts() {
        Table table = new Table();
        table.add(new Box(4, 1));
        table.row();
        Box tall = new Box(3, 2, 2);
        table.add(tall);
        table.validate();

        assertEquals(2, tall.getY());
        assertEquals(4, table.getPreferredHeight());
        assertEquals(2, table.getRowSnap());
    }

    @Test
    void tallWidgetRespectsOddTablePadding() {
        Table table = new Table();
        table.setPadding(1, 0, 0, 0);
        Box tall = new Box(3, 2, 2);
        table.add(tall);
        table.validate();

        assertEquals(2, tall.getY());
        assertEquals(4, table.getPreferredHeight());
    }

    @Test
    void tallWidgetWithOddTopPaddingRoundsUp() {
        Table table = new Table();
        Box tall = new Box(3, 2, 2);
        table.add(tall).padTop(1);
        table.validate();

        assertEquals(2, tall.getY());
        assertEquals(4, table.getPreferredHeight());
    }

    @Test
    void rowWithSquareAndTallWidgetsKeepsSquareOnRowTop() {
        Table table = new Table();
        table.add(new Box(1, 1));
        table.row();
        Box square = new Box(2, 1);
        Box tall = new Box(2, 2, 2);
        table.add(square);
        table.add(tall);
        table.validate();

        assertEquals(2, square.getY());
        assertEquals(2, tall.getY());
    }

    @Test
    void squareOnlyTableKeepsSnapOneAndNoGaps() {
        Table table = new Table();
        table.add(new Box(1, 1));
        table.row();
        table.add(new Box(1, 1));

        assertEquals(1, table.getRowSnap());
        assertEquals(2, table.getPreferredHeight());
    }

    @Test
    void removeChildRemovesCellAndShiftsLaterCellsLeft() {
        Table table = new Table();
        Box first = new Box(3, 1);
        Box second = new Box(4, 1);
        table.add(first);
        table.add(second);
        table.removeChild(first);
        table.validate();

        assertEquals(1, table.getColumnCount());
        assertEquals(0, second.getX());
        assertEquals(null, first.getParent());
    }

    @Test
    void removingLastCellOfRowRemovesTheRow() {
        Table table = new Table();
        Box first = new Box(1, 1);
        Box second = new Box(1, 1);
        Box third = new Box(1, 1);
        table.add(first);
        table.row();
        table.add(second);
        table.row();
        table.add(third);
        table.removeChild(second);
        table.validate();

        assertEquals(2, table.getRowCount());
        assertEquals(1, third.getY());
    }

    @Test
    void removeChildRejectsStrangers() {
        Table table = new Table();

        assertThrows(IllegalArgumentException.class, () -> table.removeChild(new Box(1, 1)));
        assertThrows(NullPointerException.class, () -> table.removeChild(null));
    }

    @Test
    void addChildGoesThroughAdd() {
        Table table = new Table();
        Box first = new Box(2, 1);
        Box second = new Box(2, 1);
        table.addChild(first);
        table.addChild(second);
        table.validate();

        assertEquals(2, table.getChildCount());
        assertEquals(1, table.getRowCount());
        assertEquals(2, second.getX());
    }

    @Test
    void addReturnsCellOfTheWidget() {
        Table table = new Table();
        Box box = new Box(1, 1);
        TableCell cell = table.add(box);

        assertSame(box, cell.getWidget());
        assertSame(table, box.getParent());
    }

    @Test
    void addRejectsBadWidgets() {
        Table table = new Table();
        Box box = new Box(1, 1);
        table.add(box);

        assertThrows(NullPointerException.class, () -> table.add(null));
        assertThrows(IllegalArgumentException.class, () -> table.add(box));
        assertThrows(IllegalArgumentException.class, () -> table.add(table));
    }

    @Test
    void cellDefaultsAreDocumentedOnes() {
        TableCell cell = new Table().add(new Box(1, 1));

        assertEquals(0, cell.getPaddingTop() + cell.getPaddingLeft() + cell.getPaddingBottom()
                + cell.getPaddingRight());
        assertFalse(cell.isExpandX() || cell.isExpandY() || cell.isFillX() || cell.isFillY());
        assertEquals(HorizontalAlignment.LEFT, cell.getHorizontalAlignment());
        assertEquals(VerticalAlignment.TOP, cell.getVerticalAlignment());
        assertEquals(1, cell.getColspan());
    }

    @Test
    void cellSettersMarkTableForLayout() {
        Consumer<TableCell>[] setters = settersUnderTest();
        for (Consumer<TableCell> setter : setters) {
            Table table = new Table();
            TableCell cell = table.add(new Box(1, 1));
            table.validate();
            assertFalse(table.isLayoutNeeded());

            setter.accept(cell);

            assertTrue(table.isLayoutNeeded());
        }
    }

    @SuppressWarnings("unchecked")
    private static Consumer<TableCell>[] settersUnderTest() {
        return new Consumer[] {
            (Consumer<TableCell>) cell -> cell.pad(1),
            (Consumer<TableCell>) cell -> cell.pad(1, 2, 3, 4),
            (Consumer<TableCell>) cell -> cell.padTop(1),
            (Consumer<TableCell>) cell -> cell.padLeft(1),
            (Consumer<TableCell>) cell -> cell.padBottom(1),
            (Consumer<TableCell>) cell -> cell.padRight(1),
            (Consumer<TableCell>) cell -> cell.expandX(),
            (Consumer<TableCell>) cell -> cell.expandX(false),
            (Consumer<TableCell>) cell -> cell.expandY(false),
            (Consumer<TableCell>) cell -> cell.fillX(false),
            (Consumer<TableCell>) cell -> cell.fillY(false),
            (Consumer<TableCell>) cell -> cell.expandY(),
            (Consumer<TableCell>) cell -> cell.expand(),
            (Consumer<TableCell>) cell -> cell.fillX(),
            (Consumer<TableCell>) cell -> cell.fillY(),
            (Consumer<TableCell>) cell -> cell.fill(),
            (Consumer<TableCell>) cell -> cell.alignX(HorizontalAlignment.RIGHT),
            (Consumer<TableCell>) cell -> cell.alignY(VerticalAlignment.BOTTOM),
            (Consumer<TableCell>) cell -> cell.align(HorizontalAlignment.CENTER,
                    VerticalAlignment.CENTER),
            (Consumer<TableCell>) cell -> cell.colspan(2)
        };
    }

    @Test
    void tablePaddingMarksTableForLayout() {
        Table table = new Table();
        table.validate();

        table.setPadding(1);

        assertTrue(table.isLayoutNeeded());
    }

    @Test
    void settersReturnTheSameCell() {
        TableCell cell = new Table().add(new Box(1, 1));

        assertSame(cell, cell.pad(1).expand().fill().colspan(2)
                .align(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM));
    }

    @Test
    void invalidArgumentsThrow() {
        Table table = new Table();
        TableCell cell = table.add(new Box(1, 1));

        assertThrows(IllegalArgumentException.class, () -> cell.pad(-1));
        assertThrows(IllegalArgumentException.class, () -> cell.pad(0, 0, 0, -2));
        assertThrows(IllegalArgumentException.class, () -> cell.padTop(-1));
        assertThrows(IllegalArgumentException.class, () -> cell.padLeft(-1));
        assertThrows(IllegalArgumentException.class, () -> cell.padBottom(-1));
        assertThrows(IllegalArgumentException.class, () -> cell.padRight(-1));
        assertThrows(IllegalArgumentException.class, () -> cell.colspan(0));
        assertThrows(NullPointerException.class, () -> cell.alignX(null));
        assertThrows(NullPointerException.class, () -> cell.alignY(null));
        assertThrows(NullPointerException.class,
                () -> cell.align(null, VerticalAlignment.TOP));
        assertThrows(NullPointerException.class,
                () -> cell.align(HorizontalAlignment.LEFT, null));
        assertThrows(IllegalArgumentException.class, () -> table.setPadding(-1));
        assertThrows(IllegalArgumentException.class, () -> table.setPadding(0, 0, -3, 0));
    }

    @Test
    void invalidPaddingMessageNamesTheValue() {
        Table table = new Table();

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> table.setPadding(-7));

        assertTrue(thrown.getMessage().contains("-7"));
    }

    @Test
    void validateTwiceLaysOutOnlyOnce() {
        CountingTable table = new CountingTable();
        table.add(new Box(2, 1)).expand();
        table.setSize(10, 4);

        table.validate();
        table.validate();

        assertEquals(1, table.layoutCalls);
    }
}
