package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.function.Predicate;

/**
 * Turns one frame of keyboard state into {@link MenuBar} calls and says whether the menu owns that
 * frame's keys. Callers pass key-repeat counts rather than raw key states. Nothing here reads input devices.
 */
public final class MenuKeys<A> {

    /** Passed as the access key when no letter was pressed. */
    public static final char NO_KEY = 0;

    private boolean consumed;

    /**
     * Whether the last {@link #route} took the frame's keys: the screen must ignore all keys then. Also
     * true while Alt is held, so no plain letter shortcut fires under an Alt chord.
     */
    public boolean consumed() {
        return consumed;
    }

    /**
     * @param enabled      false while text is being typed: every key is then left to the screen
     * @param altDown      either Alt key is held
     * @param accessKey    letter pressed this frame (any case), or {@link #NO_KEY}; used only with Alt
     * @param f10Pressed   F10 went down this frame
     * @param enterPressed Enter or numpad Enter went down this frame
     * @param lefts        repeat-aware move counts for each arrow key this frame
     * @return the action chosen with Enter, or null
     */
    public A route(MenuBar<A> bar, Predicate<A> availability, boolean enabled, boolean altDown, char accessKey,
                   boolean f10Pressed, boolean enterPressed, int lefts, int rights, int ups, int downs) {
        consumed = false;
        if (!enabled) {
            return null;
        }
        bar.refresh(availability);
        boolean wasOpen = bar.isOpen();
        if (altDown) {
            consumed = true;
            if (accessKey != NO_KEY) {
                toggleByAccessKey(bar, availability, accessKey);
            }
            return null;
        }
        if (f10Pressed) {
            consumed = true;
            if (wasOpen) {
                bar.close();
            } else {
                bar.openFirst(availability);
            }
            return null;
        }
        if (!wasOpen) {
            return null;
        }
        consumed = true;
        for (int i = 0; i < lefts; i++) {
            bar.moveMenu(-1, availability);
        }
        for (int i = 0; i < rights; i++) {
            bar.moveMenu(1, availability);
        }
        for (int i = 0; i < ups; i++) {
            bar.moveItem(-1, availability);
        }
        for (int i = 0; i < downs; i++) {
            bar.moveItem(1, availability);
        }
        return enterPressed ? bar.chooseHighlighted(availability) : null;
    }

    private static <A> void toggleByAccessKey(MenuBar<A> bar, Predicate<A> availability, char accessKey) {
        if (bar.isOpen() && Character.toLowerCase(bar.menu(bar.openMenu()).accessKey())
                == Character.toLowerCase(accessKey)) {
            bar.close();
            return;
        }
        bar.openByAccessKey(accessKey, availability);
    }
}
