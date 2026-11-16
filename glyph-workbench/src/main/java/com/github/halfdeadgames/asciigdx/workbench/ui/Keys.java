package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Keyboard checks and key-driven moves that several screens and dialogs share. */
public final class Keys {

    private Keys() {
    }

    public static boolean enterJustPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(
                Input.Keys.NUMPAD_ENTER);
    }

    public static boolean controlDown() {
        return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(
                Input.Keys.CONTROL_RIGHT);
    }

    /** Runs {@code onBackspace} once for every backspace the held key produces this frame. */
    public static void repeatBackspace(KeyRepeat repeat, float delta, Runnable onBackspace) {
        int backspaces = repeat.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            onBackspace.run();
        }
    }

    /** Makes arrows that are down now stay quiet until released. */
    public static void holdArrows(ArrowRepeats arrowRepeats) {
        arrowRepeats.left().hold(Gdx.input.isKeyPressed(Input.Keys.LEFT));
        arrowRepeats.right().hold(Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        arrowRepeats.up().hold(Gdx.input.isKeyPressed(Input.Keys.UP));
        arrowRepeats.down().hold(Gdx.input.isKeyPressed(Input.Keys.DOWN));
    }

    /** Scrolls by page, or to the top or bottom, for Page Up, Page Down, Home, and End. */
    public static void scrollByPageKeys(ScrollState scroll) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            scroll.pageUp();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            scroll.pageDown();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            scroll.toTop();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            scroll.toBottom();
        }
    }
}
