package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Dev-only startup hooks driven by system properties: {@code workbench.select} picks a glyph and
 * {@code workbench.capture} saves a screenshot after a few frames, then exits, and
 * {@code workbench.derive} opens the derive menu or a source pick, and {@code workbench.tab} picks the preview tab.
 */
final class DevCapture {

    static final String SELECT_PROPERTY = "workbench.select";
    static final String DERIVE_PROPERTY = "workbench.derive";
    static final String TAB_PROPERTY = "workbench.tab";
    static final String CAPTURE_PROPERTY = "workbench.capture";
    private static final int CAPTURE_FRAME = 60;

    private final String capturePath = System.getProperty(CAPTURE_PROPERTY);
    private int frames;

    /** Returns the first non-blank slot holding the requested code point, or -1. */
    static int slotToSelect(int[] codePoints, boolean[] blank) {
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
    static PreviewTabs.Tab requestedTab() {
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

    /** The raw {@code workbench.derive} value, or null when unset. */
    static String deriveRequest() {
        return System.getProperty(DERIVE_PROPERTY);
    }

    /** A parsed {@code overlay:<code point>[:<shift>]} request. */
    record OverlayRequest(int codePoint, int shift) { }

    /** Parses a derive request such as {@code overlay:U+0021} or {@code overlay:U+0021:5}; null for anything else. */
    static OverlayRequest parseOverlayRequest(String request) {
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

    static int parseCodePoint(String text) {
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
    void afterRender() {
        if (capturePath == null || capturePath.isEmpty()) {
            return;
        }
        frames++;
        if (frames < CAPTURE_FRAME) {
            return;
        }
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        try {
            pixmap.getPixels().put(pixels).rewind();
            PixmapIO.writePNG(Gdx.files.absolute(capturePath), pixmap);
            Gdx.app.log("Workbench", "Captured " + capturePath);
        } catch (RuntimeException e) {
            Gdx.app.error("Workbench", "Capture failed: " + e.getMessage());
        } finally {
            pixmap.dispose();
        }
        Gdx.app.exit();
    }
}
