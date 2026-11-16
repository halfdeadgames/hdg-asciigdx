package com.github.halfdeadgames.asciigdx.workbench.ui;

/** The colors of the workbench UI kit, as packed RGBA8888 ints. */
public final class UiPalette {

    public static final int BG = 0x101116FF;
    public static final int PANEL = 0x161824FF;
    public static final int FRAME = 0x4A5A8CFF;
    public static final int FOCUS = 0x8FB4FFFF;
    public static final int TITLE = 0xC8A0FFFF;
    public static final int ACCENT = 0xFFD23CFF;
    public static final int TEXT = 0xE6E8EEFF;
    public static final int DIM = 0x6E748CFF;
    /**
     * Text for rules that do not check the selected glyph, and the gray size example in the New
     * font dialog.
     */
    public static final int INACTIVE = 0xA4AABCFF;
    public static final int WARN = 0xFF6B6BFF;
    public static final int OK = 0x6BDC8AFF;
    public static final int KEYBG = 0x3C466EFF;
    public static final int SHADOW = 0x282C40FF;
    public static final int MENU = 0x2E3A66FF;
    public static final int MENU_DISABLED = 0x0C0E1AFF;
    /**
     * Text of an unavailable menu entry under the pointer; its row background is
     * {@link #MENU_DISABLED}.
     */
    public static final int MENU_DISABLED_HOVER_TEXT = 0xC4CADCFF;
    public static final int ORANGE = 0xFF9A30FF;
    /** Glyphs made from other glyphs. */
    public static final int DERIVED = 0x30E0D0FF;

    /** A pending overlay part that fits the cell, drawn faded. */
    public static final int GHOST = 0x30E0D073;
    /** A pending overlay part pixel that falls outside the cell, drawn faded. */
    public static final int GHOST_OUTSIDE = 0xFF6B6B99;

    /** Opaque white, for lit glyph pixels. */
    public static final int WHITE = 0xFFFFFFFF;

    private UiPalette() {
    }
}
