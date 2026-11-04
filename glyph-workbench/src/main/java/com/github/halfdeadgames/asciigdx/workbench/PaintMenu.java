package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar.Item;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar.Menu;
import java.util.List;

/** The paint screen's menus and which of their actions can be used right now. */
final class PaintMenu {

    enum Action {
        SAVE, NEW_SIZE, NEW_FONT, BACK, UNDO, REDO,
        NEW_GLYPH, DERIVE, UNLOCK_DERIVED, GO_TO_SOURCE, CANCEL_DERIVE,
        NEXT_PREVIEW, GUIDES, BIGGER_UI, SMALLER_UI, AUTO_UI,
        RULES, CONTROLS
    }

    /** What the screen is busy with, which decides what the menus offer. */
    enum Context {
        IDLE,
        DERIVING,
        /** The unsaved-changes prompt, or the guide list (including its rename field). */
        MODAL,
        RULES_OPEN,
        /** The Controls panel: it takes every key and click, so all menus are locked. */
        CONTROLS_OPEN
    }

    private PaintMenu() {
    }

    /** @param autoScale whether the UI size is automatic, which marks the auto size item */
    static MenuBar<Action> create(boolean autoScale) {
        return new MenuBar<>(List.of(
            new Menu<>("FILE", 'f', List.of(
                new Item<>("SAVE", "CTRL+S", Action.SAVE),
                new Item<>("NEW SIZE", "", Action.NEW_SIZE),
                new Item<>("NEW FONT", "", Action.NEW_FONT),
                new Item<>("BACK", "ESC", Action.BACK))),
            new Menu<>("EDIT", 'e', List.of(
                new Item<>("UNDO", "CTRL+Z", Action.UNDO),
                new Item<>("REDO", "CTRL+Y", Action.REDO))),
            new Menu<>("GLYPH", 'g', List.of(
                new Item<>("NEW GLYPH", "N", Action.NEW_GLYPH),
                new Item<>("DERIVE", "D", Action.DERIVE),
                new Item<>("UNLOCK DERIVED", "U", Action.UNLOCK_DERIVED),
                new Item<>("GO TO SOURCE", "G", Action.GO_TO_SOURCE),
                new Item<>("CANCEL DERIVE", "ESC", Action.CANCEL_DERIVE))),
            new Menu<>("VIEW", 'v', List.of(
                new Item<>("NEXT PREVIEW", "TAB", Action.NEXT_PREVIEW),
                new Item<>("GUIDES", "F2", Action.GUIDES),
                new Item<>("BIGGER UI", "CTRL+=", Action.BIGGER_UI),
                new Item<>("SMALLER UI", "CTRL+-", Action.SMALLER_UI),
                new Item<>(autoScale ? "*AUTO UI SIZE" : "AUTO UI SIZE", "CTRL+0", Action.AUTO_UI))),
            new Menu<>("HELP", 'h', List.of(
                new Item<>("RULES", "F1", Action.RULES),
                new Item<>("CONTROLS", "", Action.CONTROLS)))));
    }

    static boolean isAvailable(Action action, Context context) {
        boolean help = action == Action.RULES || action == Action.CONTROLS;
        return switch (context) {
            case RULES_OPEN, CONTROLS_OPEN -> false;
            case MODAL -> help;
            case DERIVING -> action == Action.CANCEL_DERIVE || help;
            case IDLE -> action != Action.CANCEL_DERIVE;
        };
    }
}
