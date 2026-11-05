package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewTabs;

/**
 * Dev-only startup hooks driven by system properties: {@code workbench.select} picks a glyph and
 * {@code workbench.capture} saves a screenshot after a few frames, then exits, and
 * {@code workbench.derive} opens the derive menu or a source pick, {@code workbench.tab} picks the preview tab,
 * {@code workbench.guides} opens the guide list with that row selected, and {@code workbench.menu} opens
 * a menu bar menu, lights a title, or (with {@code controls}) opens the Controls panel, and
 * {@code workbench.newglyph} opens the New glyph dialog with that text typed into it, and
 * {@code workbench.newsize} opens the New size dialog (optionally {@code down:N} rows into the list).
 * {@code workbench.upscalesource} opens the Upscale from dialog (optionally {@code down:N} rows into the list).
 * {@code workbench.newfont} opens the New font name dialog with that text typed into it.
 * {@code workbench.newfontsize} opens the New font size page for a name (optionally {@code :down:N} sizes in).
 * Without {@code workbench.mode=paint}, both New font hooks open on the New font screen of the start flow.
 */
public final class DevCapture {

    static final String SELECT_PROPERTY = "workbench.select";
    static final String DERIVE_PROPERTY = "workbench.derive";
    static final String TAB_PROPERTY = "workbench.tab";
    static final String GUIDES_PROPERTY = "workbench.guides";
    static final String MENU_PROPERTY = "workbench.menu";
    static final String NEW_GLYPH_PROPERTY = "workbench.newglyph";
    static final String NEW_SIZE_PROPERTY = "workbench.newsize";
    static final String UPSCALE_SOURCE_PROPERTY = "workbench.upscalesource";
    static final String NEW_FONT_PROPERTY = "workbench.newfont";
    static final String NEW_FONT_SIZE_PROPERTY = "workbench.newfontsize";
    static final String CAPTURE_PROPERTY = "workbench.capture";
    private static final int CAPTURE_FRAME = 60;

    private final String capturePath = System.getProperty(CAPTURE_PROPERTY);
    private int frames;

    /** Returns the first non-blank slot holding the requested code point, or -1. */
    public static int slotToSelect(int[] codePoints, boolean[] blank) {
        int wanted = parseCodePoint(System.getProperty(SELECT_PROPERTY));
        if (wanted < 0) {
            return -1;
        }
        for (int slot = 0; slot < codePoints.length; slot++) {
            if (codePoints[slot] == wanted && !blank[slot]) {
                return slot;
            }
        }
        return -1;
    }

    /** The preview tab named by {@code workbench.tab}, or null when unset or unknown. */
    public static PreviewTabs.Tab requestedTab() {
        return parseTab(System.getProperty(TAB_PROPERTY));
    }

    static PreviewTabs.Tab parseTab(String name) {
        if (name == null) {
            return null;
        }
        for (PreviewTabs.Tab tab : PreviewTabs.Tab.values()) {
            if (tab.name().equalsIgnoreCase(name.trim())) {
                return tab;
            }
        }
        return null;
    }

    /** The row to select in the opened guide list named by {@code workbench.guides}, or -1 to leave the list closed. */
    public static int guidesRequest() {
        return parseRowIndex(System.getProperty(GUIDES_PROPERTY));
    }

    static int parseRowIndex(String text) {
        if (text == null) {
            return -1;
        }
        try {
            return Math.max(-1, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The raw {@code workbench.menu} value, or null when unset. */
    public static String menuRequest() {
        return System.getProperty(MENU_PROPERTY);
    }

    /** The text to type into the opened New glyph dialog named by {@code workbench.newglyph}, or null to leave it closed. */
    public static String newGlyphRequest() {
        return System.getProperty(NEW_GLYPH_PROPERTY);
    }

    /** The text to type into the opened New font dialog named by {@code workbench.newfont}, or null to leave it closed. */
    public static String newFontRequest() {
        return parseNewFontRequest(System.getProperty(NEW_FONT_PROPERTY));
    }

    /** Any value opens the dialog and is typed as given, spaces included; a blank value opens it empty. */
    static String parseNewFontRequest(String request) {
        return request;
    }

    /** A parsed {@code workbench.newfontsize} request: the font name and how many sizes to move down. */
    public record NewFontSizeRequest(String name, int moves) { }

    static final String DEFAULT_FONT_NAME = "my-font";

    /** The New font size page to open, or null to leave it closed. */
    public static NewFontSizeRequest newFontSizeRequest() {
        return parseNewFontSizeRequest(System.getProperty(NEW_FONT_SIZE_PROPERTY));
    }

    /** Parses {@code <name>}, {@code <name>:down:N}, {@code down:N}, or a blank value; null when unset. */
    static NewFontSizeRequest parseNewFontSizeRequest(String request) {
        if (request == null) {
            return null;
        }
        String[] fields = request.trim().split(":", -1);
        String name = fields[0].trim();
        int moves = 0;
        if (fields.length == 2 && "down".equalsIgnoreCase(name)) {
            name = "";
            moves = Math.max(0, parseRowIndex(fields[1]));
        } else if (fields.length == 3 && "down".equalsIgnoreCase(fields[1].trim())) {
            moves = Math.max(0, parseRowIndex(fields[2]));
        }
        return new NewFontSizeRequest(name.isEmpty() ? DEFAULT_FONT_NAME : name, moves);
    }

    /** Rows to move down in the New size dialog opened by {@code workbench.newsize}, or -1 to leave it closed. */
    public static int newSizeRequest() {
        return parseNewSizeRequest(System.getProperty(NEW_SIZE_PROPERTY));
    }

    /** Any value opens the dialog; {@code down:N} also moves the cursor N rows. */
    static int parseNewSizeRequest(String request) {
        if (request == null) {
            return -1;
        }
        String[] fields = request.trim().split(":", -1);
        if (fields.length == 2 && "down".equalsIgnoreCase(fields[0].trim())) {
            return Math.max(0, parseRowIndex(fields[1]));
        }
        return 0;
    }

    /** Rows to move down in the Upscale from dialog opened by {@code workbench.upscalesource}, or -1 to leave it closed. */
    public static int upscaleSourceRequest() {
        return parseUpscaleSourceRequest(System.getProperty(UPSCALE_SOURCE_PROPERTY));
    }

    /** Only a blank value, {@code open}, or {@code down:N} opens the dialog; {@code down:N} also moves the cursor N rows. */
    static int parseUpscaleSourceRequest(String request) {
        if (request == null) {
            return -1;
        }
        String trimmed = request.trim();
        if (trimmed.isEmpty() || "open".equalsIgnoreCase(trimmed)) {
            return 0;
        }
        String[] fields = trimmed.split(":", -1);
        if (fields.length == 2 && "down".equalsIgnoreCase(fields[0].trim())) {
            return Math.max(0, parseRowIndex(fields[1]));
        }
        return -1;
    }

    /** True for the request {@code controls}, which opens the Controls panel instead of a menu. */
    public static boolean opensControls(String request) {
        return request != null && "controls".equalsIgnoreCase(request.trim());
    }

    /** A parsed menu request; {@code item} is -1 for the first available item. */
    public record MenuRequest(String title, boolean hoverOnly, int item, boolean inspect) { }

    /**
     * Parses {@code edit}, {@code edit:1}, {@code hover:view}, {@code inspect:file} (hover a title), or
     * {@code inspect:glyph:0} (open that menu and hover that item); null for anything else.
     */
    public static MenuRequest parseMenuRequest(String request) {
        if (request == null) {
            return null;
        }
        String[] fields = request.trim().split(":", -1);
        if ("inspect".equalsIgnoreCase(fields[0].trim())) {
            return parseInspectRequest(fields);
        }
        if (fields.length == 2 && "hover".equalsIgnoreCase(fields[0].trim()) && !fields[1].isBlank()) {
            return new MenuRequest(fields[1].trim(), true, -1, false);
        }
        return parseOpenMenuRequest(fields);
    }

    private static MenuRequest parseInspectRequest(String[] fields) {
        if (fields.length < 2 || fields.length > 3 || fields[1].isBlank()) {
            return null;
        }
        int item = -1;
        if (fields.length == 3) {
            item = parseMenuItemIndex(fields[2]);
            if (item < 0) {
                return null;
            }
        }
        return new MenuRequest(fields[1].trim(), false, item, true);
    }

    private static MenuRequest parseOpenMenuRequest(String[] fields) {
        if (fields[0].isBlank() || fields.length > 2) {
            return null;
        }
        int item = -1;
        if (fields.length == 2) {
            item = parseMenuItemIndex(fields[1]);
            if (item < 0) {
                return null;
            }
        }
        return new MenuRequest(fields[0].trim(), false, item, false);
    }

    /** The item index parsed from the field, or -1 if the field is not a whole number of 0 or more. */
    private static int parseMenuItemIndex(String field) {
        try {
            return Math.max(-1, Integer.parseInt(field.trim()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The raw {@code workbench.derive} value, or null when unset. */
    public static String deriveRequest() {
        return System.getProperty(DERIVE_PROPERTY);
    }

    /** A parsed {@code overlay:<code point>[:<shift>]} request. */
    public record OverlayRequest(int codePoint, int shift) { }

    /** Parses a derive request such as {@code overlay:U+0021} or {@code overlay:U+0021:5}; null for anything else. */
    public static OverlayRequest parseOverlayRequest(String request) {
        if (request == null) {
            return null;
        }
        String[] fields = request.trim().split(":", -1);
        if (fields.length < 2 || fields.length > 3 || !"overlay".equalsIgnoreCase(fields[0].trim())) {
            return null;
        }
        int codePoint = parseCodePoint(fields[1]);
        if (codePoint < 0) {
            return null;
        }
        int shift = 0;
        if (fields.length == 3) {
            try {
                shift = Integer.parseInt(fields[2].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new OverlayRequest(codePoint, shift);
    }

    public static int parseCodePoint(String text) {
        if (text == null) {
            return -1;
        }
        String trimmed = text.trim();
        if (trimmed.regionMatches(true, 0, "U+", 0, 2)) {
            try {
                return Integer.parseInt(trimmed.substring(2), 16);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return trimmed.codePointCount(0, trimmed.length()) == 1 ? trimmed.codePointAt(0) : -1;
    }

    /** Call at the end of each render, after the frame has been drawn. */
    public void afterRender() {
        if (capturePath == null || capturePath.isEmpty()) {
            return;
        }
        frames++;
        if (frames < CAPTURE_FRAME) {
            return;
        }
        try {
            ScreenshotFile.writeBackBuffer(capturePath);
            Gdx.app.log("Workbench", "Captured " + capturePath);
        } catch (RuntimeException e) {
            Gdx.app.error("Workbench", "Capture failed: " + e.getMessage());
        }
        Gdx.app.exit();
    }
}
