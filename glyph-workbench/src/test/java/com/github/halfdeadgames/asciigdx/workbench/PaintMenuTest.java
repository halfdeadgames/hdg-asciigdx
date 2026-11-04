package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.PaintMenu.Action;
import com.github.halfdeadgames.asciigdx.workbench.PaintMenu.Context;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaintMenuTest {

    private static void assertAvailableOnly(Context context, Set<Action> expected) {
        for (Action action : Action.values()) {
            assertEquals(expected.contains(action), PaintMenu.isAvailable(action, context),
                context + " " + action.name());
        }
    }

    private static Set<String> openableTitles(Context context) {
        MenuBar<Action> bar = PaintMenu.create(true);
        Set<String> titles = new java.util.LinkedHashSet<>();
        for (int i = 0; i < bar.menuCount(); i++) {
            if (!bar.isLocked(i, action -> PaintMenu.isAvailable(action, context))) {
                titles.add(bar.menu(i).title());
            }
        }
        return titles;
    }

    @Test
    void whenIdleEverythingButCancelDeriveIsAvailable() {
        for (Action action : Action.values()) {
            boolean expected = action != Action.CANCEL_DERIVE;
            assertEquals(expected, PaintMenu.isAvailable(action, Context.IDLE), action.name());
        }
    }

    @Test
    void whileDerivingOnlyCancelAndTheHelpItemsAreAvailable() {
        assertAvailableOnly(Context.DERIVING, Set.of(Action.CANCEL_DERIVE, Action.RULES, Action.CONTROLS));
    }

    @Test
    void inAModalScreenOnlyTheHelpItemsAreAvailable() {
        assertAvailableOnly(Context.MODAL, Set.of(Action.RULES, Action.CONTROLS));
    }

    @Test
    void withTheRulesOverlayOpenNothingIsAvailable() {
        assertAvailableOnly(Context.RULES_OPEN, Set.of());
    }

    @Test
    void withTheControlsPanelOpenNothingIsAvailable() {
        assertAvailableOnly(Context.CONTROLS_OPEN, Set.of());
    }

    @Test
    void controlsIsAvailableWhereverRulesIs() {
        for (Context context : Context.values()) {
            assertEquals(PaintMenu.isAvailable(Action.RULES, context), PaintMenu.isAvailable(Action.CONTROLS, context),
                context.name());
        }
    }

    @Test
    void menusLockWithTheirContext() {
        assertEquals(Set.of("FILE", "EDIT", "GLYPH", "VIEW", "HELP"), openableTitles(Context.IDLE));
        assertEquals(Set.of("GLYPH", "HELP"), openableTitles(Context.DERIVING));
        assertEquals(Set.of("HELP"), openableTitles(Context.MODAL));
        assertEquals(Set.of(), openableTitles(Context.RULES_OPEN));
        assertEquals(Set.of(), openableTitles(Context.CONTROLS_OPEN));
    }

    @Test
    void everyActionHasOneMenuItem() {
        MenuBar<Action> bar = PaintMenu.create(false);
        int items = 0;
        for (int i = 0; i < bar.menuCount(); i++) {
            items += bar.menu(i).items().size();
        }
        assertEquals(Action.values().length, items);
        assertTrue(bar.openByAccessKey('H', action -> true));
    }

    @Test
    void onlyTheAutoSizeLabelChangesWithTheScaleState() {
        MenuBar<Action> auto = PaintMenu.create(true);
        MenuBar<Action> fixed = PaintMenu.create(false);
        for (int i = 0; i < auto.menuCount(); i++) {
            for (int j = 0; j < auto.menu(i).items().size(); j++) {
                MenuBar.Item<Action> marked = auto.menu(i).items().get(j);
                MenuBar.Item<Action> plain = fixed.menu(i).items().get(j);
                assertEquals(marked.action(), plain.action());
                if (marked.action() == Action.AUTO_UI) {
                    assertEquals("*AUTO UI SIZE", marked.label());
                    assertEquals("AUTO UI SIZE", plain.label());
                } else {
                    assertEquals(plain.label(), marked.label());
                }
            }
        }
    }
}
