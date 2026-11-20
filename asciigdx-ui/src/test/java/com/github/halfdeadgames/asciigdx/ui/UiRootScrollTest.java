package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiRootScrollTest {

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(20, 10));
    private final List<String> log = new ArrayList<>();

    private static final class Pad extends WidgetGroup {
        Pad(int x, int y, int width, int height) {
            setPosition(x, y);
            setSize(width, height);
        }
    }

    private Pad hoveredPad(boolean consumes) {
        Pad pad = new Pad(2, 2, 6, 6);
        pad.addListener(event -> {
            if (event.getType() == UiEventType.SCROLLED) {
                log.add("pad " + event.getScrollStepsX() + "," + event.getScrollStepsY());
            }
            return consumes;
        });
        ui.getRoot().addChild(pad);
        ui.pointerMoved(3, 3);
        return pad;
    }

    @Test
    void wholeStepsAreDeliveredToTheHoveredWidget() {
        hoveredPad(true);

        assertTrue(ui.scrolled(2f, -1f));

        assertEquals(List.of("pad 2,-1"), log);
    }

    @Test
    void fractionsAccumulateUntilAWholeStep() {
        hoveredPad(true);

        assertFalse(ui.scrolled(0f, 0.4f));
        assertFalse(ui.scrolled(0f, 0.4f));
        assertTrue(ui.scrolled(0f, 0.4f));

        assertEquals(List.of("pad 0,1"), log);
        assertFalse(ui.scrolled(0f, 0.7f));
        assertTrue(ui.scrolled(0f, 0.2f));
        assertEquals(List.of("pad 0,1", "pad 0,1"), log);
    }

    @Test
    void axesAccumulateSeparately() {
        hoveredPad(true);

        assertFalse(ui.scrolled(0.6f, 0.6f));
        assertTrue(ui.scrolled(0.6f, 0f));

        assertEquals(List.of("pad 1,0"), log);
    }

    @Test
    void oppositeDirectionDropsTheKeptRemainder() {
        hoveredPad(true);
        ui.scrolled(0f, 0.9f);

        assertTrue(ui.scrolled(0f, -2.5f));

        assertEquals(List.of("pad 0,-2"), log);
    }

    @Test
    void unhandledScrollBubblesToParents() {
        Pad outer = new Pad(0, 0, 20, 10);
        Pad inner = new Pad(2, 2, 6, 6);
        outer.addChild(inner);
        ui.getRoot().addChild(outer);
        inner.addListener(event -> {
            log.add("inner " + event.getType());
            return false;
        });
        outer.addListener(event -> {
            log.add("outer " + event.getType() + " " + (event.getTarget() == inner));
            return true;
        });
        ui.pointerMoved(3, 3);
        log.clear();

        assertTrue(ui.scrolled(0f, 1f));

        assertEquals(List.of("inner SCROLLED", "outer SCROLLED true"), log);
    }

    @Test
    void nothingHoveredMeansNothingIsHandled() {
        Pad pad = new Pad(2, 2, 6, 6);
        ui.getRoot().addChild(pad);
        pad.addListener(event -> true);

        assertFalse(ui.scrolled(0f, 1f));

        ui.pointerMoved(3, 3);
        assertTrue(ui.scrolled(0f, 1f));
        ui.pointerMoved(15, 8);
        assertFalse(ui.scrolled(0f, 1f));
    }

    @Test
    void nonFiniteAmountsAreIgnored() {
        hoveredPad(true);

        assertFalse(ui.scrolled(Float.NaN, 1f));
        assertFalse(ui.scrolled(0f, Float.POSITIVE_INFINITY));

        assertTrue(log.isEmpty());
    }

    @Test
    void scrollEventsCarryNoStepsForOtherTypes() {
        Pad pad = hoveredPad(false);
        pad.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_DOWN) {
                log.add("down " + event.getScrollStepsX() + "," + event.getScrollStepsY());
            }
            return false;
        });

        ui.scrolled(0f, 2f);
        ui.pointerDown(3, 3, 0, 0);

        assertEquals(List.of("pad 0,2", "down 0,0"), log);
    }

    @Test
    void scrollingInsideADeliveryThrows() {
        Pad pad = hoveredPad(false);
        pad.addListener(event -> {
            assertThrows(IllegalStateException.class, () -> ui.scrolled(0f, 1f));
            return true;
        });

        ui.scrolled(0f, 1f);
    }
}
