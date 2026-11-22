package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Edits a glyph: a field for the {@code U+XXXX} code point, and a one-cell preview of it. */
final class GlyphEditor implements KeyEditor {
    private static final String PREVIEW_LOOK = "text";
    private static final int CODE_POINT_FIELD_CELLS = 5;

    private final EditorContext context;

    GlyphEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        String currentGlyph = row.value().asString();
        LabelStyle look = context.widgets().getSkin().getLabelStyle(PREVIEW_LOOK);
        GlyphPreview preview = new GlyphPreview(firstCodePoint(currentGlyph),
                look.getForegroundRgba(), look.getBackgroundRgba());
        TextField field = context.widgets().createCommittedField(row.location().describe(),
                CODE_POINT_FIELD_CELLS,
                GlyphText.toTypedText(currentGlyph), TextFieldFilter.any(),
                context.textCommit(row.location(), SkinEdits::setGlyph));
        field.addChangeListener(changed -> showTypedGlyph(preview, changed.getText()));
        return ControlRow.of(field, preview);
    }

    // Half-typed text is not an error here; the preview keeps its last good glyph.
    private static void showTypedGlyph(GlyphPreview preview, String typedText) {
        try {
            preview.setCodePoint(firstCodePoint(GlyphText.toSkinText(typedText)));
        } catch (IllegalArgumentException incompleteText) {
            return;
        }
    }

    private static int firstCodePoint(String glyph) {
        return glyph.isEmpty() ? ' ' : glyph.codePointAt(0);
    }
}
