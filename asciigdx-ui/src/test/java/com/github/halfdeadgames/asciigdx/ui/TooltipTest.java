package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TooltipTest {
    private static final int COLUMNS = 40;
    private static final int ROWS = 20;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final LabelStyle HINT = LabelStyle.of(WHITE, BLACK);
    private static final ButtonStyle BUTTON_STYLE = ButtonStyle.of(HINT);

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(COLUMNS, ROWS));

    private TextButton buttonAt(int x, int y) {
        TextButton button = new TextButton("Go", BUTTON_STYLE);
        button.setPosition(x, y);
        ui.getRoot().addChild(button);
        return button;
    }

    private Tooltip hintOn(Widget target, String text) {
        Tooltip tooltip = Tooltip.attachText(ui, target, text, HINT);
        tooltip.setDelaySeconds(1f);
        return tooltip;
    }

    private void showHint(Tooltip tooltip, int pointerColumn, int pointerRow) {
        ui.pointerMoved(pointerColumn, pointerRow);
        ui.update(1f);
        assertTrue(tooltip.isShowing());
    }

    @Test
    void showsExactlyWhenDelayHasPassed() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        ui.pointerMoved(6, 5);

        ui.update(0.5f);
        ui.update(0.25f);
        assertFalse(tooltip.isShowing());
        ui.update(0.25f);

        assertTrue(tooltip.isShowing());
        assertEquals(1, ui.getPopupLayer().getChildCount());
        assertSame(ui.getPopupLayer(), tooltip.getContent().getParent().getParent());
    }

    private static final class BigBox extends Widget {
        @Override
        public int getPreferredWidth() {
            return 50;
        }

        @Override
        public int getPreferredHeight() {
            return ROWS;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }
    }

    @Test
    void hintOverThePointerNeverTakesHover() {
        TextButton button = buttonAt(5, 5);
        button.setSize(10, 1);
        Tooltip tooltip = Tooltip.attach(ui, button, new BigBox());
        tooltip.setDelaySeconds(1f);
        showHint(tooltip, 6, 5);

        ui.pointerMoved(7, 5);
        ui.pointerMoved(8, 5);

        assertTrue(tooltip.isShowing());
        assertSame(button, ui.getHoveredWidget());
    }

    @Test
    void hintShowsAgainWhenTargetReturnsWhilePointerStaysOver() {
        TextButton button = buttonAt(5, 5);
        Tooltip tooltip = hintOn(button, "Hint!!");
        showHint(tooltip, 6, 5);

        ui.getRoot().removeChild(button);
        ui.update(0f);
        assertFalse(tooltip.isShowing());
        ui.getRoot().addChild(button);
        ui.update(0.5f);
        assertFalse(tooltip.isShowing());
        ui.update(0.5f);

        assertTrue(tooltip.isShowing());
    }

    @Test
    void detachWhileDelayIsPendingShowsNothing() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        ui.pointerMoved(6, 5);
        ui.update(0.5f);

        tooltip.detach();
        ui.update(5f);

        assertFalse(tooltip.isShowing());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void pressOnTheVisibleHintHidesItAndPassesThrough() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        showHint(tooltip, 6, 5);

        ui.pointerDown(8, 6, 0, 0);

        assertFalse(tooltip.isShowing());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void defaultDelayIsHalfASecond() {
        Tooltip tooltip = Tooltip.attachText(ui, buttonAt(5, 5), "Hint!!", HINT);
        ui.pointerMoved(6, 5);

        assertEquals(Tooltip.DEFAULT_DELAY_SECONDS, tooltip.getDelaySeconds());
        ui.update(0.5f);

        assertTrue(tooltip.isShowing());
    }

    @Test
    void hidesWhenPointerLeavesTargetAndRestartsDelayOnReturn() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        showHint(tooltip, 6, 5);

        ui.pointerMoved(30, 15);

        assertFalse(tooltip.isShowing());
        assertEquals(0, ui.getPopupLayer().getChildCount());
        ui.pointerMoved(6, 5);
        ui.update(0.5f);
        assertFalse(tooltip.isShowing());
        ui.update(0.5f);
        assertTrue(tooltip.isShowing());
    }

    @Test
    void leavingBeforeDelayCancelsTheHint() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        ui.pointerMoved(6, 5);
        ui.update(0.75f);
        ui.pointerMoved(30, 15);

        ui.update(5f);

        assertFalse(tooltip.isShowing());
    }

    @Test
    void placesHintOneCellRightAndBelowThePointer() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");

        showHint(tooltip, 6, 5);

        assertEquals(7, tooltip.getContent().getX());
        assertEquals(6, tooltip.getContent().getY());
        assertEquals(3, tooltip.getContent().getWidth());
    }

    @Test
    void hintStaysWhereItAppearedWhilePointerMoves() {
        TextButton button = buttonAt(5, 5);
        button.setSize(10, 1);
        Tooltip tooltip = hintOn(button, "Hint!!");
        showHint(tooltip, 6, 5);

        ui.pointerMoved(10, 5);

        assertTrue(tooltip.isShowing());
        assertEquals(7, tooltip.getContent().getX());
    }

    @Test
    void usesPointerPositionOfLastMoveBeforeShowing() {
        TextButton button = buttonAt(5, 5);
        button.setSize(10, 1);
        Tooltip tooltip = hintOn(button, "Hint!!");
        ui.pointerMoved(6, 5);
        ui.pointerMoved(9, 5);

        ui.update(1f);

        assertEquals(10, tooltip.getContent().getX());
    }

    @Test
    void flipsLeftOfPointerNearRightEdge() {
        TextButton button = buttonAt(35, 5);
        button.setSize(5, 1);
        Tooltip tooltip = hintOn(button, "Hint!!");

        showHint(tooltip, 38, 5);

        assertEquals(35, tooltip.getContent().getX());
    }

    @Test
    void flipsAbovePointerNearBottomEdge() {
        Tooltip tooltip = hintOn(buttonAt(5, 19), "Hint!!");

        showHint(tooltip, 6, 19);

        assertEquals(18, tooltip.getContent().getY());
    }

    @Test
    void clampsHintWiderThanSurfaceToLeftEdge() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "x".repeat(100));

        showHint(tooltip, 6, 5);

        assertEquals(0, tooltip.getContent().getX());
    }

    @Test
    void pressWhileShowingHidesUntilPointerLeavesTarget() {
        TextButton button = buttonAt(5, 5);
        Tooltip tooltip = hintOn(button, "Hint!!");
        showHint(tooltip, 6, 5);

        ui.pointerDown(6, 5, 0, 0);
        ui.pointerUp(6, 5, 0, 0);

        assertFalse(tooltip.isShowing());
        ui.update(5f);
        assertFalse(tooltip.isShowing());
        ui.pointerMoved(30, 15);
        ui.pointerMoved(6, 5);
        ui.update(1f);
        assertTrue(tooltip.isShowing());
    }

    @Test
    void pressBeforeShowingRestartsDelay() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        ui.pointerMoved(6, 5);
        ui.update(0.75f);

        ui.pointerDown(6, 5, 0, 0);
        ui.pointerUp(6, 5, 0, 0);
        ui.update(0.75f);
        assertFalse(tooltip.isShowing());
        ui.update(0.25f);

        assertTrue(tooltip.isShowing());
    }

    @Test
    void pressWithAnotherPointerDoesNothing() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        showHint(tooltip, 6, 5);

        ui.pointerDown(30, 15, 1, 0);

        assertTrue(tooltip.isShowing());
    }

    @Test
    void touchLongPressShowsHintAndLiftingHidesIt() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");

        ui.pointerDown(6, 5, 0, 0);
        ui.update(1f);
        assertTrue(tooltip.isShowing());
        ui.pointerUp(6, 5, 0, 0);
        ui.pointerMoved(-1, -1);

        assertFalse(tooltip.isShowing());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void hidesWhenTargetLeavesTheTree() {
        TextButton button = buttonAt(5, 5);
        Tooltip tooltip = hintOn(button, "Hint!!");
        showHint(tooltip, 6, 5);

        ui.getRoot().removeChild(button);
        ui.update(0f);

        assertFalse(tooltip.isShowing());
        assertEquals(0, ui.getPopupLayer().getChildCount());
    }

    @Test
    void neverShowsForTargetOutsideTheTree() {
        TextButton button = buttonAt(5, 5);
        Tooltip tooltip = hintOn(button, "Hint!!");
        ui.pointerMoved(6, 5);
        ui.getRoot().removeChild(button);

        ui.update(5f);

        assertFalse(tooltip.isShowing());
    }

    @Test
    void detachHidesStopsHintingAndIsSafeTwice() {
        TextButton button = buttonAt(5, 5);
        Tooltip tooltip = hintOn(button, "Hint!!");
        showHint(tooltip, 6, 5);

        tooltip.detach();
        tooltip.detach();

        assertFalse(tooltip.isShowing());
        assertEquals(0, ui.getPopupLayer().getChildCount());
        ui.pointerMoved(30, 15);
        ui.pointerMoved(6, 5);
        ui.update(5f);
        assertFalse(tooltip.isShowing());
    }

    @Test
    void rejectsNegativeAndNanDelay() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");

        assertThrows(IllegalArgumentException.class, () -> tooltip.setDelaySeconds(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> tooltip.setDelaySeconds(Float.NaN));
        tooltip.setDelaySeconds(0f);
        assertEquals(0f, tooltip.getDelaySeconds());
    }

    @Test
    void zeroDelayShowsOnFirstUpdate() {
        Tooltip tooltip = hintOn(buttonAt(5, 5), "Hint!!");
        tooltip.setDelaySeconds(0f);
        ui.pointerMoved(6, 5);

        ui.update(0f);

        assertTrue(tooltip.isShowing());
    }

    @Test
    void exposesTargetAndContent() {
        TextButton button = buttonAt(5, 5);
        Label content = new Label("Hint!!", HINT);

        Tooltip tooltip = Tooltip.attach(ui, button, content);

        assertSame(button, tooltip.getTarget());
        assertSame(content, tooltip.getContent());
    }

    @Test
    void rejectsNullArguments() {
        TextButton button = buttonAt(5, 5);
        Label content = new Label("Hint!!", HINT);

        assertThrows(NullPointerException.class, () -> Tooltip.attach(null, button, content));
        assertThrows(NullPointerException.class, () -> Tooltip.attach(ui, null, content));
        assertThrows(NullPointerException.class, () -> Tooltip.attach(ui, button, null));
        assertThrows(NullPointerException.class,
                () -> Tooltip.attachText(ui, button, null, HINT));
        assertThrows(NullPointerException.class,
                () -> Tooltip.attachText(ui, button, "Hint", null));
    }

    @Test
    void neverConsumesEventsSoParentListenersStillSeeMoves() {
        WidgetGroup parent = new WidgetGroup();
        ui.getRoot().addChild(parent);
        TextButton button = new TextButton("Go", BUTTON_STYLE);
        button.setPosition(5, 5);
        parent.addChild(button);
        int[] movesSeenByParent = new int[1];
        parent.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_MOVED) {
                movesSeenByParent[0]++;
            }
            return false;
        });
        hintOn(button, "Hint!!");

        boolean handled = ui.pointerMoved(6, 5);
        ui.pointerMoved(7, 5);

        assertFalse(handled);
        assertEquals(2, movesSeenByParent[0]);
    }
}
