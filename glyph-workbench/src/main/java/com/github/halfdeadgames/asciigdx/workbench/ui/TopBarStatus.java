package com.github.halfdeadgames.asciigdx.workbench.ui;

/**
 * What the top bar shows at its right edge: a plain tag, then a status in a packed RGBA8888 color.
 * Either text may be empty.
 */
public record TopBarStatus(String tag, String text, int color) {

    public static TopBarStatus of(String text, int color) {
        return new TopBarStatus("", text, color);
    }
}
