package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RadioButtonTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final ToggleStyle STYLE = ToggleStyle.of(
            ButtonStyle.of(LabelStyle.of(WHITE, 0x111111FF)), "( )", "(*)");

    private final UiRoot ui = UiRoot.of(UiSurface.ofSize(40, 10));
    private final List<Widget> changes = new ArrayList<>();
    private final ButtonGroup group = new ButtonGroup();

    private RadioButton radioAt(int row, String text) {
        RadioButton radio = new RadioButton(text, STYLE);
        radio.setPosition(0, row);
        radio.addChangeListener(changes::add);
        ui.getRoot().addChild(radio);
        return radio;
    }

    private void click(int row) {
        ui.pointerDown(1, row, 0, 0);
        ui.pointerUp(1, row, 0, 0);
    }

    @Test
    void loneRadioTurnsOnByClickAndStaysOn() {
        RadioButton radio = radioAt(0, "A");

        click(0);
        click(0);

        assertTrue(radio.isChecked());
        assertEquals(List.of(radio), changes);
        assertNull(radio.getGroup());
    }

    @Test
    void loneRadioTurnsOnByEnterAndSpace() {
        RadioButton radio = radioAt(0, "A");
        ui.setKeyboardFocus(radio);

        assertTrue(ui.keyDown(Input.Keys.ENTER));
        assertTrue(radio.isChecked());
        radio.setChecked(false);
        assertTrue(ui.keyDown(Input.Keys.SPACE));
        assertTrue(radio.isChecked());
        assertEquals(2, changes.size());
    }

    @Test
    void userSelectionUnchecksSiblingAndNotifiesLoserThenWinner() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        group.add(first, second);
        first.setChecked(true);

        click(1);

        assertFalse(first.isChecked());
        assertTrue(second.isChecked());
        assertSame(second, group.getChecked());
        assertEquals(List.of(first, second), changes);
    }

    @Test
    void clickOnCheckedMemberKeepsItCheckedWithoutNotification() {
        RadioButton first = radioAt(0, "A");
        group.add(first, radioAt(1, "B"));
        first.setChecked(true);

        click(0);

        assertTrue(first.isChecked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void firstUserSelectionNotifiesOnlyTheWinner() {
        RadioButton first = radioAt(0, "A");
        group.add(first, radioAt(1, "B"));

        click(0);

        assertEquals(List.of(first), changes);
    }

    @Test
    void setCheckedFromCodeMovesSelectionWithoutNotification() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        group.add(first, second);

        first.setChecked(true);
        second.setChecked(true);

        assertFalse(first.isChecked());
        assertSame(second, group.getChecked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void codeCanUncheckTheCheckedMember() {
        RadioButton first = radioAt(0, "A");
        group.add(first);
        first.setChecked(true);

        first.setChecked(false);

        assertNull(group.getChecked());
    }

    @Test
    void disabledMemberCannotBeSelectedByUser() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        group.add(first, second);
        second.setDisabled(true);

        click(1);
        ui.setKeyboardFocus(second);
        ui.keyDown(Input.Keys.SPACE);

        assertNull(group.getChecked());
        assertTrue(changes.isEmpty());
        assertFalse(second.isFocusable());
    }

    @Test
    void addingChecksAndSetsTheGroup() {
        RadioButton first = radioAt(0, "A");
        group.add(first);

        assertSame(group, first.getGroup());
        assertThrows(IllegalArgumentException.class, () -> new ButtonGroup().add(first));
        assertThrows(IllegalArgumentException.class, () -> group.add(first));
        assertThrows(NullPointerException.class, () -> group.add((RadioButton) null));
    }

    @Test
    void addingAtomicallyRejectsDuplicatesInOneCall() {
        RadioButton first = radioAt(0, "A");

        assertThrows(IllegalArgumentException.class, () -> group.add(first, first));
        assertNull(first.getGroup());
    }

    @Test
    void addingSeveralCheckedMembersKeepsTheFirstChecked() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        first.setChecked(true);
        second.setChecked(true);

        group.add(first, second);

        assertSame(first, group.getChecked());
        assertFalse(second.isChecked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void removedMemberLeavesTheGroupAndKeepsItsState() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        group.add(first, second);
        first.setChecked(true);

        group.remove(first);
        group.remove(first);
        second.setChecked(true);

        assertNull(first.getGroup());
        assertTrue(first.isChecked());
        assertSame(second, group.getChecked());
        ButtonGroup other = new ButtonGroup();
        other.add(first);
        assertSame(other, first.getGroup());
    }

    @Test
    void winnerIsNotNotifiedWhenALoserListenerMovedItAway() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        group.add(first, second);
        group.setChecked(first);
        first.addChangeListener(radio -> group.remove(second));

        click(1);

        assertEquals(List.of(first), changes);
    }

    @Test
    void groupSetCheckedAndClearCheckedAreSilent() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        List<ButtonGroup> groupChanges = new ArrayList<>();
        group.add(first, second);
        group.addChangeListener(groupChanges::add);

        group.setChecked(second);
        assertSame(second, group.getChecked());
        group.clearChecked();

        assertNull(group.getChecked());
        assertTrue(changes.isEmpty());
        assertTrue(groupChanges.isEmpty());
        assertThrows(NullPointerException.class, () -> group.setChecked(null));
        assertThrows(IllegalArgumentException.class,
                () -> group.setChecked(new RadioButton("C", STYLE)));
    }

    @Test
    void groupListenerFiresOncePerUserSelectionAfterRadioListeners() {
        RadioButton first = radioAt(0, "A");
        RadioButton second = radioAt(1, "B");
        List<String> order = new ArrayList<>();
        group.add(first, second);
        group.setChecked(first);
        first.addChangeListener(radio -> order.add("A"));
        second.addChangeListener(radio -> order.add("B"));
        group.addChangeListener(changed -> order.add("group " + changed.getChecked().getText()));

        click(1);
        click(1);

        assertEquals(List.of("A", "B", "group B"), order);
    }

    @Test
    void removedGroupListenerIsNotCalled() {
        radioAt(0, "A");
        group.add(radioAt(1, "B"));
        List<ButtonGroup> groupChanges = new ArrayList<>();
        GroupChangeListener listener = groupChanges::add;
        group.addChangeListener(listener);
        group.removeChangeListener(listener);

        click(1);

        assertTrue(groupChanges.isEmpty());
    }
}
