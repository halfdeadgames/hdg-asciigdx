package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.workbench.CursorNavigator;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/** The source font's sheet the user picks a glyph from during upscale. Slots without ink cannot be picked. */
public final class UpscaleSourceSheet {

    private static final int NO_SLOT = -1;

    private final FontEditor editor;
    private final UpscaleSources.Font font;
    private final int[] codePoints;
    private final boolean[] blocked;

    private UpscaleSourceSheet(FontEditor editor, UpscaleSources.Font font) {
        this.editor = editor;
        this.font = font;
        codePoints = readCodePoints(editor);
        blocked = findBlockedSlots(editor, codePoints);
    }

    /**
     * Reads {@code <fontsRoot>/<folderName>/<cellHeight>.txt} as UTF-8.
     *
     * @throws IOException if the file cannot be read
     * @throws com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException if the file is not valid font data
     */
    public static UpscaleSourceSheet load(Path fontsRoot, UpscaleSources.Font font) throws IOException {
        Path file = fontsRoot.resolve(font.folderName()).resolve(font.cellHeight() + ".txt");
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return of(FontDataParser.parse(text), font);
    }

    public static UpscaleSourceSheet of(FontData data, UpscaleSources.Font font) {
        return new UpscaleSourceSheet(new FontEditor(data), font);
    }

    private static int[] readCodePoints(FontEditor editor) {
        int[] slots = new int[editor.slotCount()];
        for (int slot = 0; slot < slots.length; slot++) {
            slots[slot] = editor.codePointAtSlot(slot);
        }
        return slots;
    }

    private static boolean[] findBlockedSlots(FontEditor editor, int[] codePoints) {
        Set<Integer> withInk = editor.codePointsWithInk();
        boolean[] result = new boolean[codePoints.length];
        for (int slot = 0; slot < codePoints.length; slot++) {
            result[slot] = codePoints[slot] == FontEditor.NO_GLYPH || !withInk.contains(codePoints[slot]);
        }
        return result;
    }

    /** The editor the sheet view draws from; it is never edited. */
    public FontEditor editor() {
        return editor;
    }

    public UpscaleSources.Font font() {
        return font;
    }

    /** One flag per slot, true when it cannot be picked. Callers must not change the array. */
    public boolean[] blocked() {
        return blocked;
    }

    public int codePointAt(int slot) {
        return codePoints[slot];
    }

    public boolean isPickable(int slot) {
        return slot >= 0 && slot < blocked.length && !blocked[slot];
    }

    /** The glyph in the slot with derived glyphs resolved, or null when the slot is not pickable. */
    public PixelGlyph glyphAt(int slot) {
        if (!isPickable(slot)) {
            return null;
        }
        return editor.glyph(codePoints[slot]);
    }

    /** The pickable slot holding the code point, else the first pickable slot, else -1. */
    public int startSlot(int codePoint) {
        int slot = slotOf(codePoint);
        if (isPickable(slot)) {
            return slot;
        }
        return CursorNavigator.firstNonBlank(blocked);
    }

    private int slotOf(int codePoint) {
        if (codePoint == FontEditor.NO_GLYPH) {
            return NO_SLOT;
        }
        for (int slot = 0; slot < codePoints.length; slot++) {
            if (codePoints[slot] == codePoint) {
                return slot;
            }
        }
        return NO_SLOT;
    }

    /** The sheet frame title, such as {@code FROM 8X8}. */
    public String title() {
        return "FROM " + shortTitle();
    }

    /** The cell size alone, such as {@code 8X8}, for a frame too narrow for {@link #title()}. */
    public String shortTitle() {
        return editor.cellWidth() + "X" + editor.cellHeight();
    }
}
