package com.github.halfdeadgames.asciigdx.workbench.ui;

/**
 * One frame of keyboard state for {@link MenuKeys#route}.
 *
 * @param enabled      false while text is being typed: every key is then left to the screen
 * @param altDown      either Alt key is held
 * @param accessKey    letter pressed this frame (any case), or {@link MenuKeys#NO_KEY}; used only with Alt
 * @param f10Pressed   F10 went down this frame
 * @param enterPressed Enter or numpad Enter went down this frame
 * @param lefts        repeat-aware move count for the left arrow this frame
 * @param rights       repeat-aware move count for the right arrow this frame
 * @param ups          repeat-aware move count for the up arrow this frame
 * @param downs        repeat-aware move count for the down arrow this frame
 */
public record MenuKeyInput(boolean enabled, boolean altDown, char accessKey, boolean f10Pressed,
                           boolean enterPressed, int lefts, int rights, int ups, int downs) {
}
