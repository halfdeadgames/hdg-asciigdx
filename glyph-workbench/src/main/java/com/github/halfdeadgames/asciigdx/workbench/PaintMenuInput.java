package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuKeys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuPointer;
import java.util.function.Predicate;

/** Reads the mouse and keyboard for the paint menu bar and reports the action the user picked. */
final class PaintMenuInput {

    /** What the menu input needs from the paint screen. */
    interface Host {
        /** The current bar. A menu action can swap it mid-frame, so read it again after running one. */
        MenuBar<PaintMenu.Action> menuBar();

        void updatePointerCell();

        int pointerX();

        int pointerY();

        int cell();

        int worldWidth();

        boolean altDown();

        boolean leftClicked();

        /** True while a pixel stroke or a column bar drag holds the mouse. */
        boolean mouseHeldByPainting();

        boolean renamingGuide();

        /** Ends any pixel stroke, then runs the chosen action. */
        void runMenuAction(PaintMenu.Action action);
    }

    private final Host host;
    private final MenuPointer<PaintMenu.Action> menuPointer;
    private final MenuKeys<PaintMenu.Action> menuKeys;
    private final Predicate<PaintMenu.Action> menuAvailability;
    private final KeyRepeat repeatLeft;
    private final KeyRepeat repeatRight;
    private final KeyRepeat repeatUp;
    private final KeyRepeat repeatDown;
    // A menu state requested by the dev hook stays as set until the pointer moves, a click lands, or Esc is pressed.
    private boolean menuPinned;
    private boolean menuPinCellKnown;
    private int menuPinX;
    private int menuPinY;
    // Alt as the input events saw it, because typed characters arrive before this frame's polling can say so.
    private boolean altSeenDown;

    PaintMenuInput(Host host, MenuPointer<PaintMenu.Action> menuPointer, MenuKeys<PaintMenu.Action> menuKeys,
                   Predicate<PaintMenu.Action> menuAvailability, KeyRepeat repeatLeft, KeyRepeat repeatRight,
                   KeyRepeat repeatUp, KeyRepeat repeatDown) {
        this.host = host;
        this.menuPointer = menuPointer;
        this.menuKeys = menuKeys;
        this.menuAvailability = menuAvailability;
        this.repeatLeft = repeatLeft;
        this.repeatRight = repeatRight;
        this.repeatUp = repeatUp;
        this.repeatDown = repeatDown;
    }

    void pin() {
        menuPinned = true;
    }

    void noteAltPressed() {
        altSeenDown = true;
    }

    void noteAltReleased() {
        altSeenDown = false;
    }

    boolean altSeenDown() {
        return altSeenDown;
    }

    // Runs before everything else. While the menu owns the frame, nothing below it may read input.
    boolean handle(float delta) {
        host.updatePointerCell();
        forgetAltWhenReleased();
        MenuBar<PaintMenu.Action> menuBar = host.menuBar();
        int pointerX = host.pointerX();
        int pointerY = host.pointerY();
        if (menuPinned) {
            rememberPinCell(pointerX, pointerY);
            if (pinHolds(pointerX, pointerY)) {
                return menuBar.isOpen();
            }
            menuPinned = false;
        }
        boolean wasOpen = menuBar.isOpen();
        boolean escape = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        PaintMenu.Action chosen = pointMenu(menuBar, pointerX, pointerY, escape);
        boolean keysOwnFrame = false;
        if (chosen == null && (!escape || host.altDown())) {
            chosen = routeMenuKeys(menuBar, delta);
            keysOwnFrame = menuKeys.consumed();
        }
        runChosenAction(chosen, wasOpen);
        return menuPointer.captured() || keysOwnFrame;
    }

    private void forgetAltWhenReleased() {
        if (!host.altDown()) {
            altSeenDown = false;
        }
    }

    private void rememberPinCell(int pointerX, int pointerY) {
        if (menuPinCellKnown) {
            return;
        }
        menuPinCellKnown = true;
        menuPinX = pointerX;
        menuPinY = pointerY;
    }

    private boolean pinHolds(int pointerX, int pointerY) {
        return pointerX == menuPinX && pointerY == menuPinY && !host.leftClicked()
            && !Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    private PaintMenu.Action pointMenu(MenuBar<PaintMenu.Action> menuBar, int pointerX, int pointerY,
                                       boolean escape) {
        int cell = host.cell();
        return menuPointer.update(menuBar, menuAvailability,
            Math.floorDiv(pointerX, cell), Math.floorDiv(pointerY, cell), host.worldWidth() / cell,
            escape, host.leftClicked(),
            Gdx.input.isButtonPressed(Input.Buttons.LEFT), host.mouseHeldByPainting());
    }

    private void runChosenAction(PaintMenu.Action chosen, boolean wasOpen) {
        if (chosen != null) {
            host.runMenuAction(chosen);
        }
        // An action can swap the bar, so the open state is read from the host again.
        if (host.menuBar().isOpen() != wasOpen) {
            holdArrowRepeats();
        }
    }

    private void holdArrowRepeats() {
        repeatLeft.hold(Gdx.input.isKeyPressed(Input.Keys.LEFT));
        repeatRight.hold(Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        repeatUp.hold(Gdx.input.isKeyPressed(Input.Keys.UP));
        repeatDown.hold(Gdx.input.isKeyPressed(Input.Keys.DOWN));
    }

    // Arrow repeats are read only while a menu is open, so a closed menu leaves them to the cursor.
    private PaintMenu.Action routeMenuKeys(MenuBar<PaintMenu.Action> menuBar, float delta) {
        boolean alt = host.altDown();
        boolean steering = menuBar.isOpen() && !alt;
        int lefts = steering ? repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta) : 0;
        int rights = steering ? repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta) : 0;
        int ups = steering ? repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta) : 0;
        int downs = steering ? repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta) : 0;
        boolean enter = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
        return menuKeys.route(menuBar, menuAvailability, !host.renamingGuide(), alt,
            alt ? pressedAccessKey(menuBar) : MenuKeys.NO_KEY, Gdx.input.isKeyJustPressed(Input.Keys.F10), enter,
            lefts, rights, ups, downs);
    }

    private static char pressedAccessKey(MenuBar<PaintMenu.Action> menuBar) {
        for (int i = 0; i < menuBar.menuCount(); i++) {
            char key = Character.toLowerCase(menuBar.menu(i).accessKey());
            if (key >= 'a' && key <= 'z' && Gdx.input.isKeyJustPressed(Input.Keys.A + (key - 'a'))) {
                return key;
            }
        }
        return MenuKeys.NO_KEY;
    }
}
