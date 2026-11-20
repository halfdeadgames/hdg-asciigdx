package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiRootKeyRepeatTest {
    private static final float INITIAL = UiRoot.KEY_REPEAT_INITIAL_DELAY_SECONDS;
    private static final float INTERVAL = UiRoot.KEY_REPEAT_INTERVAL_SECONDS;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final ButtonStyle BUTTON_STYLE =
            ButtonStyle.of(LabelStyle.of(UiFont.SQUARE, WHITE, BLACK));
    private static final TextFieldStyle FIELD_STYLE =
            TextFieldStyle.of(LabelStyle.of(UiFont.SQUARE, WHITE, BLACK));

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(40, 10));
    private final List<Integer> keyDowns = new ArrayList<>();

    private void recordUnhandledKeyDowns() {
        ui.addUnhandledKeyListener(event -> {
            if (event.getType() == UiEventType.KEY_DOWN) {
                keyDowns.add(event.getKeycode());
            }
            return true;
        });
    }

    private TextButton buttonAt(int x) {
        TextButton button = new TextButton("Go", BUTTON_STYLE);
        button.setPosition(x, 0);
        ui.getRoot().addChild(button);
        return button;
    }

    @Test
    void arrowRepeatsAfterTheInitialDelayThenEveryInterval() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.LEFT);
        ui.update(INITIAL / 2);
        assertEquals(1, keyDowns.size());
        ui.update(INITIAL / 2);
        assertEquals(2, keyDowns.size());
        ui.update(INTERVAL / 2);
        assertEquals(2, keyDowns.size());
        ui.update(INTERVAL / 2);
        assertEquals(3, keyDowns.size());
        ui.update(INTERVAL);
        assertEquals(List.of(Input.Keys.LEFT, Input.Keys.LEFT, Input.Keys.LEFT, Input.Keys.LEFT),
                keyDowns);
    }

    @Test
    void longFrameCatchesUpWithEveryDueRepeat() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.DOWN);
        ui.update(INITIAL + 4 * INTERVAL + INTERVAL / 2);

        assertEquals(6, keyDowns.size());
    }

    @Test
    void onlyArrowKeysRepeat() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.ENTER);
        ui.update(10f);
        ui.keyDown(Input.Keys.SPACE);
        ui.keyDown(Input.Keys.A);
        ui.update(10f);

        assertEquals(List.of(Input.Keys.ENTER, Input.Keys.SPACE, Input.Keys.A), keyDowns);
    }

    @Test
    void keyUpOfTheHeldKeyStopsTheRepeat() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.RIGHT);
        ui.keyUp(Input.Keys.RIGHT);
        ui.update(10f);

        assertEquals(1, keyDowns.size());
    }

    @Test
    void keyUpOfAnotherKeyDoesNotStopTheRepeat() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.RIGHT);
        ui.keyUp(Input.Keys.A);
        ui.update(INITIAL);

        assertEquals(2, keyDowns.size());
    }

    @Test
    void anyOtherKeyDownStopsTheRepeat() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.LEFT);
        ui.keyDown(Input.Keys.A);
        ui.update(10f);

        assertEquals(List.of(Input.Keys.LEFT, Input.Keys.A), keyDowns);
    }

    @Test
    void anotherArrowTakesOverTheRepeat() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.LEFT);
        ui.keyDown(Input.Keys.UP);
        ui.update(INITIAL);

        assertEquals(List.of(Input.Keys.LEFT, Input.Keys.UP, Input.Keys.UP), keyDowns);
    }

    @Test
    void secondKeyDownBeforeKeyUpKeepsTheTimerOffUntilRelease() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.LEFT);
        ui.keyDown(Input.Keys.LEFT);
        ui.update(10f);
        assertEquals(2, keyDowns.size());

        ui.keyUp(Input.Keys.LEFT);
        ui.keyDown(Input.Keys.LEFT);
        ui.update(INITIAL);
        assertEquals(4, keyDowns.size());
    }

    @Test
    void focusChangeByOtherMeansStopsTheRepeat() {
        recordUnhandledKeyDowns();
        TextButton first = buttonAt(0);
        TextButton second = buttonAt(6);
        ui.setKeyboardFocus(first);

        ui.keyDown(Input.Keys.DOWN);
        ui.setKeyboardFocus(second);
        ui.update(10f);

        assertEquals(1, keyDowns.size());
    }

    @Test
    void focusChangeStopsTheTimerButTheKeyStaysHeldSoAPlatformRepeatIsIgnored() {
        recordUnhandledKeyDowns();
        TextButton first = buttonAt(0);
        TextButton second = buttonAt(6);
        ui.setKeyboardFocus(first);

        ui.keyDown(Input.Keys.DOWN);
        ui.setKeyboardFocus(second);
        ui.update(10f);
        assertEquals(1, keyDowns.size());

        ui.keyDown(Input.Keys.DOWN);
        ui.update(10f);
        assertEquals(2, keyDowns.size());

        ui.keyUp(Input.Keys.DOWN);
        ui.keyDown(Input.Keys.DOWN);
        ui.update(INITIAL);
        assertEquals(4, keyDowns.size());
    }

    @Test
    void releaseHeldKeysStopsTheRepeatAndAllowsANewOne() {
        recordUnhandledKeyDowns();

        ui.keyDown(Input.Keys.LEFT);
        ui.releaseHeldKeys();
        ui.update(10f);
        assertEquals(1, keyDowns.size());

        ui.keyDown(Input.Keys.LEFT);
        ui.update(INITIAL);
        assertEquals(3, keyDowns.size());
    }

    @Test
    void releaseHeldKeysWithNothingHeldDoesNothing() {
        recordUnhandledKeyDowns();

        ui.releaseHeldKeys();
        ui.update(10f);

        assertTrue(keyDowns.isEmpty());
    }

    @Test
    void repeatingTheKeyKeepsMovingFocusAlongTheRow() {
        TextButton first = buttonAt(0);
        TextButton second = buttonAt(6);
        TextButton third = buttonAt(12);
        TextButton fourth = buttonAt(18);
        ui.setKeyboardFocus(first);

        ui.keyDown(Input.Keys.RIGHT);
        assertSame(second, ui.getKeyboardFocus());
        ui.update(INITIAL);
        assertSame(third, ui.getKeyboardFocus());
        ui.update(INTERVAL);
        assertSame(fourth, ui.getKeyboardFocus());
    }

    @Test
    void repeatMovesTheCaretOfAFocusedTextField() {
        TextField field = new TextField(FIELD_STYLE, 20);
        ui.getRoot().addChild(field);
        field.setText("abcdefgh");
        ui.setKeyboardFocus(field);

        ui.keyDown(Input.Keys.LEFT);
        assertEquals(7, field.getCaretIndex());
        ui.update(INITIAL);
        ui.update(INTERVAL);
        ui.update(INTERVAL);

        assertEquals(4, field.getCaretIndex());
    }

    @Test
    void repeatReachesTheUnhandledListenersWithTheSameKeycode() {
        List<Integer> seen = new ArrayList<>();
        ui.addUnhandledKeyListener(event -> {
            seen.add(event.getKeycode());
            return false;
        });
        ui.keyDown(Input.Keys.UP);
        ui.update(INITIAL);

        assertTrue(seen.size() == 2 && seen.get(0).equals(seen.get(1)));
    }
}
