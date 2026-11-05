package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.List;

/** What the derive source picker shows: the previewed glyph, the prompt line, and why a source is refused. */
final class PaintDerivePick {

    private final FontEditor editor;
    private final DeriveFlow flow;
    private final StringBuilder text = new StringBuilder();
    private PixelGlyph shown;
    private PixelGlyph blank;
    private boolean valid;
    private String promptText = "";
    private String problemText = "";

    PaintDerivePick(FontEditor editor, DeriveFlow flow) {
        this.editor = editor;
        this.flow = flow;
    }

    /** The glyph to draw in place of the target, or null when nothing is previewed. */
    PixelGlyph shown() {
        return shown;
    }

    boolean isValid() {
        return valid;
    }

    String promptText() {
        return promptText;
    }

    /** Empty when the pick has no problem. */
    String problemText() {
        return problemText;
    }

    void begin(int[] codePoints, int cursor) {
        blank = new PixelGlyph(codePoints[cursor], false, PixelGrid.blank(editor.bodyWidth(), editor.cellHeight()));
        resetToNoSource(codePoints, cursor);
    }

    /** Hides the preview; the other state stays until the next {@link #begin} or {@link #refresh}. */
    void clear() {
        shown = null;
    }

    /** Runs only when the source changes. With the source on the target there is nothing to preview yet. */
    void refresh(int[] codePoints, int cursor) {
        if (flow.operation() == GlyphOp.Kind.OVERLAY) {
            refreshOverlay(codePoints, cursor);
            return;
        }
        if (flow.sourceSlot() == cursor) {
            resetToNoSource(codePoints, cursor);
            return;
        }
        FontEditor.DerivePreview preview = editor.previewDerived(codePoints[cursor], currentOp(codePoints));
        valid = preview.glyph() != null;
        promptText = prompt(codePoints, cursor, valid);
        if (valid) {
            shown = preview.glyph();
            problemText = "";
        } else {
            shown = blank;
            problemText = preview.problem();
        }
    }

    GlyphOp currentOp(int[] codePoints) {
        if (flow.operation() == GlyphOp.Kind.OVERLAY) {
            return DeriveFlow.overlayOp(flow.parts(), codePoints);
        }
        return new GlyphOp(flow.operation(), List.of(new GlyphOp.Source(codePoints[flow.sourceSlot()], 0)));
    }

    String summary(int[] codePoints, int target) {
        text.setLength(0);
        GlyphLabel.appendCodeAndName(text, target);
        text.append(" (").append(DeriveFlow.label(flow.operation())).append(')');
        if (flow.operation() == GlyphOp.Kind.OVERLAY) {
            return text.append(" FROM ").append(flow.parts().size()).append(" PARTS").toString();
        }
        text.append(" FROM ");
        GlyphLabel.appendCodeAndName(text, codePoints[flow.sourceSlot()]);
        return text.toString();
    }

    OverlayCheck.Result overlayCheck(int[] codePoints) {
        PixelGlyph pendingSource = editor.glyph(codePoints[flow.sourceSlot()]);
        List<DeriveFlow.Part> committed = flow.parts();
        PixelGlyph firstPart = committed.isEmpty() ? null : editor.glyph(codePoints[committed.get(0).slot()]);
        return OverlayCheck.check(flow.sourceIsTarget(), pendingSource, firstPart, flow.pendingShift());
    }

    private void resetToNoSource(int[] codePoints, int cursor) {
        shown = null;
        valid = false;
        promptText = prompt(codePoints, cursor, false);
        problemText = "";
    }

    // Previews the committed parts plus the pending one; if the pending one is refused, the valid parts still show.
    private void refreshOverlay(int[] codePoints, int cursor) {
        int target = codePoints[cursor];
        List<DeriveFlow.Part> all = flow.partsWithPending();
        shown = null;
        valid = false;
        problemText = "";
        if (!all.isEmpty()) {
            FontEditor.DerivePreview preview = editor.previewDerived(target, DeriveFlow.overlayOp(all, codePoints));
            problemText = preview.problem();
            if (preview.glyph() != null) {
                valid = true;
                shown = preview.glyph();
            } else if (!flow.parts().isEmpty()) {
                shown = editor.previewDerived(target, DeriveFlow.overlayOp(flow.parts(), codePoints)).glyph();
            }
            OverlayCheck.Result check = overlayCheck(codePoints);
            if (check.hasWarning() && check.problem() != OverlayCheck.Problem.SAME_AS_TARGET) {
                problemText = check.message();
                valid = false;
            }
            if (shown == null) {
                shown = blank;
            }
        }
        promptText = prompt(codePoints, cursor, !flow.sourceIsTarget());
    }

    private String prompt(int[] codePoints, int cursor, boolean withSource) {
        boolean overlay = flow.operation() == GlyphOp.Kind.OVERLAY;
        text.setLength(0);
        text.append(DeriveFlow.label(flow.operation())).append(' ');
        GlyphLabel.appendCodeAndName(text, codePoints[cursor]);
        text.append(':');
        if (withSource) {
            text.append(overlay ? " PART FROM " : " FROM ");
            GlyphLabel.appendCodeAndName(text, codePoints[flow.sourceSlot()]);
        } else {
            text.append(overlay ? " PICK A PART" : " PICK SOURCE");
        }
        if (overlay) {
            int shift = flow.pendingShift();
            text.append("  SHIFT ").append(shift > 0 ? "+" : "").append(shift)
                .append("  PARTS ").append(flow.parts().size());
        }
        return text.toString();
    }
}
