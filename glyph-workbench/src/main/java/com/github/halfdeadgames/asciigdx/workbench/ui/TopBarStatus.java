package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;

/** What the top bar shows at its right edge: a plain tag, then a coloured status. Either text may be empty. */
public record TopBarStatus(String tag, String text, Color color) {

    public static TopBarStatus of(String text, Color color) {
        return new TopBarStatus("", text, color);
    }
}
