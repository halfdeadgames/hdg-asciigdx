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
    void preselectedColumnReplacesTheFirstCandidateUntilChosen() {
        ViewedColumn view = new ViewedColumn();
        view.setDefault(3);
        assertEquals(3, view.resolve(-1, 4));
        assertEquals(1, view.resolve(1, 4));
        view.choose(2);
        assertEquals(2, view.resolve(-1, 4));
        view.reset();
        assertEquals(1, view.resolve(-1, 4));
    }

    @Test
    void defaultColumnPrefersSavedThenHandThenNearest() {
        String[] picks = {"nearest", "scale2x", "hand"};
        assertEquals(2, ViewedColumn.defaultColumn(picks, "scale2x", "hand", "nearest"));
        assertEquals(3, ViewedColumn.defaultColumn(picks, null, "hand", "nearest"));
        assertEquals(3, ViewedColumn.defaultColumn(picks, "none", "hand", "nearest"));
        assertEquals(1, ViewedColumn.defaultColumn(new String[] {"nearest", "mmpx"}, null, "hand", "nearest"));
        assertEquals(-1, ViewedColumn.defaultColumn(new String[] {"mmpx"}, null, "hand", "nearest"));
    }

    @Test
    void indexesAreClampedToTheColumnCount() {
        ViewedColumn view = new ViewedColumn();
        view.choose(5);
        assertEquals(2, view.resolve(-1, 3));
        assertEquals(2, view.resolve(9, 3));
    }
}
