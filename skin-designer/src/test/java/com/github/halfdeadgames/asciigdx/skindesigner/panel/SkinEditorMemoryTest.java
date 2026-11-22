package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkinEditorMemoryTest {

    @Test
    void remembersTheScrollRow() {
        var memory = new SkinEditorMemory();

        memory.rememberScrollRow(7);

        assertEquals(7, memory.getScrollRow());
    }

    @Test
    void aPendingScrollResetBeatsTheNextSavedRowOnce() {
        var memory = new SkinEditorMemory();
        memory.rememberScrollRow(7);

        memory.requestScrollReset();
        memory.rememberScrollRow(9);
        assertEquals(0, memory.getScrollRow());

        memory.rememberScrollRow(4);
        assertEquals(4, memory.getScrollRow());
    }

    @Test
    void focusedFieldIsHandedBackOnlyOnce() {
        var memory = new SkinEditorMemory();

        memory.rememberFocusedField("labels.text.foreground");

        assertEquals("labels.text.foreground", memory.takeFocusedField());
        assertNull(memory.takeFocusedField());
    }

    @Test
    void picksTheRememberedStyleOrTheFirst() {
        var memory = new SkinEditorMemory();
        var first = new StyleChoice("labels", "text");
        var second = new StyleChoice("labels", "title");

        assertEquals(first, memory.pickFrom(List.of(first, second)));
        memory.rememberSelection(second);
        assertEquals(second, memory.pickFrom(List.of(first, second)));
        assertEquals(first, memory.pickFrom(List.of(first)));
    }
}
