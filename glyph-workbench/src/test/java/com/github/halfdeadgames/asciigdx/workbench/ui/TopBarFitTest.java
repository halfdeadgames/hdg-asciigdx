package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TopBarFitTest {

    private static final List<String> CRUMBS = List.of("GLYPH WORKBENCH", "SAIKSISTT", "PAINT 16X16");

    @Test
    void crumbsDropFromTheLeftOnceTheyNoLongerFit() {
        assertEquals(CRUMBS, TopBar.fit(CRUMBS, 42));
        assertEquals(CRUMBS.subList(1, 3), TopBar.fit(CRUMBS, 41));
    }

    @Test
    void theLastCrumbAlwaysStays() {
        assertEquals(CRUMBS.subList(2, 3), TopBar.fit(CRUMBS, 1));
        assertEquals(CRUMBS.subList(2, 3), TopBar.fit(CRUMBS, 12));
    }

    @Test
    void exactFitLeavesABlankCellBeforeTheTag() {
        int columns = 80;
        String tag = "UI 4X AUTO";
        int available = columns - 1 - TopBar.rightWidth(tag, "");
        assertEquals(CRUMBS, TopBar.fit(CRUMBS, available));
        assertEquals(available + 1, TopBar.tagX(columns, tag, ""));
    }
}
