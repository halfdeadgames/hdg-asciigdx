package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;

/** The colours of the workbench UI kit. */
public final class UiPalette {

    public static final Color BG = new Color(0x101116FF);
    public static final Color PANEL = new Color(0x161824FF);
    public static final Color FRAME = new Color(0x4A5A8CFF);
    public static final Color FOCUS = new Color(0x8FB4FFFF);
    public static final Color TITLE = new Color(0xC8A0FFFF);
    public static final Color ACCENT = new Color(0xFFD23CFF);
    public static final Color TEXT = new Color(0xE6E8EEFF);
    public static final Color DIM = new Color(0x6E748CFF);
    public static final Color WARN = new Color(0xFF6B6BFF);
    public static final Color OK = new Color(0x6BDC8AFF);
    public static final Color KEYBG = new Color(0x3C466EFF);
    public static final Color SHADOW = new Color(0x282C40FF);
    public static final Color ORANGE = new Color(0xFF9A30FF);
    /** Glyphs made from other glyphs. */
    public static final Color DERIVED = new Color(0x30E0D0FF);

    /** A pending overlay part that fits the body, drawn faded. */
    public static final Color GHOST = new Color(0x30E0D073);
    /** A pending overlay part pixel that falls outside the body, drawn faded. */
    public static final Color GHOST_OUTSIDE = new Color(0xFF6B6B99);

    private UiPalette() {
    }
}
