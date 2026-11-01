package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewedColumnTest {

    @Test
    void defaultsToTheFirstCandidate() {
        assertEquals(1, new ViewedColumn().resolve(-1, 4));
    }

    @Test
    void usesTheOriginalWhenThereAreNoCandidates() {
        assertEquals(0, new ViewedColumn().resolve(-1, 1));
        assertEquals(0, new ViewedColumn().resolve(-1, 0));
    }

    @Test
    void hoverWinsOverTheChosenColumn() {
        ViewedColumn view = new ViewedColumn();
        view.choose(3);
        assertEquals(2, view.resolve(2, 4));
        assertEquals(0, view.resolve(0, 4));
    }

    @Test
    void chosenColumnPersistsWithoutHover() {
        ViewedColumn view = new ViewedColumn();
        view.choose(2);
        assertEquals(2, view.resolve(-1, 4));
        assertEquals(2, view.resolve(-1, 4));
    }

    @Test
    void originalCanBeChosen() {
        ViewedColumn view = new ViewedColumn();
        view.choose(0);
        assertEquals(0, view.resolve(-1, 4));
    }

    @Test
    void resetReturnsToTheFirstCandidate() {
        ViewedColumn view = new ViewedColumn();
        view.choose(3);
        view.reset();
        assertEquals(1, view.resolve(-1, 4));
    }

    @Test
    void indexesAreClampedToTheColumnCount() {
        ViewedColumn view = new ViewedColumn();
        view.choose(5);
        assertEquals(2, view.resolve(-1, 3));
        assertEquals(2, view.resolve(9, 3));
    }
}
