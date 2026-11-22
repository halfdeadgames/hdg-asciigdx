package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.text.EffectRun;
import com.github.halfdeadgames.asciigdx.zone.EffectZone;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Applies the effect runs of one {@link Label} to the text layer of a {@link UiSurface}, as
 * {@link EffectZone}s, so the letters inside {@code [effect:...]} tags move or change each frame.
 *
 * <p>Create one instance per label and keep it. The zones are rebuilt when:
 *
 * <ul>
 *   <li>the list of runs is replaced (the text, color names, effects, or markup flag changed);
 *   <li>the label's absolute position or width changes;
 *   <li>the text column of the first or last letter of any run changes, which covers the
 *       alignment, a hidden ancestor, and the view size or scroll position of a scroll pane;
 *   <li>the cell height divided by the cell width of the text layer's glyph source changes.
 * </ul>
 *
 * <p>Limit: a change to a letter in the middle of a run alone is not noticed. A frame without a
 * change allocates nothing.
 *
 * <p>Threading and ownership: use it on the render thread only; it is not thread safe. The
 * instance keeps a strong reference to its label until the instance is discarded, so drop the
 * instance when the label is no longer shown. It keeps no reference to a layer, and the layer
 * may differ from call to call.
 *
 * <p>Time: the zones keep no time of their own. Each {@code seconds} value goes straight to the
 * effects, and the built-in effects compute their look from that value alone, so a value that
 * goes backward or restarts at zero moves the effects to the look they have at that time, with
 * no error and no state to reset. A custom effect that keeps its own state may react
 * differently.
 *
 * <p>Example, once per frame on the render thread:
 *
 * <pre>{@code
 * LabelEffectZones waveZones = LabelEffectZones.of(waveLabel);
 *
 * // in render(delta):
 * sceneSeconds += delta;
 * layers.beginCellEffects(delta);
 * waveZones.apply(surface.getTextLayer(), sceneSeconds);
 * }</pre>
 */
public final class LabelEffectZones {
    private static final int COLUMNS_PER_RUN_SIGNATURE = 2;
    private static final float NO_ASPECT = 0f;

    private final Label label;
    private final List<EffectZone> zones = new ArrayList<>();
    private List<EffectRun> builtRuns;
    private int[] builtRunColumns = new int[0];
    private int builtColumn;
    private int builtRow;
    private int builtWidth;
    private float builtRowHeightPerColumnWidth = NO_ASPECT;

    private LabelEffectZones(Label label) {
        this.label = label;
    }

    /**
     * Creates the zones holder of a label. The label is read only; it is not changed.
     *
     * @param label the label whose effect runs are applied, not {@code null}
     * @return a new holder that starts without zones and builds them on the first
     *         {@link #apply} call
     * @throws NullPointerException if {@code label} is {@code null}
     */
    public static LabelEffectZones of(Label label) {
        return new LabelEffectZones(Objects.requireNonNull(label, "label"));
    }

    /**
     * Applies the effect runs of the label to the text layer, rebuilding the zones first if
     * anything listed in the class description changed. Call it once per frame on the render
     * thread, after {@code LayerStack.beginCellEffects} and before the stack is drawn. A frame
     * without a change allocates nothing. Not thread safe: render thread only.
     *
     * <p>The zones are applied to whichever layer is passed, so a different layer may be passed
     * on each call; the effects only reach that layer's frame handles. A layer whose cell
     * height divided by cell width differs from the previous call triggers a rebuild.
     *
     * @param textLayer the layer that draws the text grid of the surface the label is shown on,
     *         for example {@link UiSurface#getTextLayer()}; not {@code null}
     * @param seconds the game's elapsed time in seconds, which drives the effects; normally
     *         steadily increasing. If it goes backward or restarts at zero, nothing breaks: the
     *         zones keep no time state, and the built-in effects show the look for that time
     *         (a custom effect with its own state may behave differently)
     * @throws NullPointerException if {@code textLayer} is {@code null}
     */
    public void apply(Layer textLayer, float seconds) {
        Objects.requireNonNull(textLayer, "textLayer");
        float rowHeightPerColumnWidth = rowHeightPerColumnWidthOf(textLayer);
        if (hasLabelChanged() || builtRowHeightPerColumnWidth != rowHeightPerColumnWidth) {
            rebuildZones(rowHeightPerColumnWidth);
        }
        for (int i = 0; i < zones.size(); i++) {
            zones.get(i).apply(textLayer, seconds);
        }
    }

    private static float rowHeightPerColumnWidthOf(Layer textLayer) {
        GlyphSource glyphSource = textLayer.getGlyphSource();
        return (float) glyphSource.getCellHeight() / glyphSource.getCellWidth();
    }

    private boolean hasLabelChanged() {
        return builtRuns != label.getEffectRuns()
                || builtColumn != label.getAbsoluteX()
                || builtRow != label.getAbsoluteY()
                || builtWidth != label.getWidth()
                || haveRunEndColumnsChanged();
    }

    private boolean haveRunEndColumnsChanged() {
        for (int i = 0; i < builtRuns.size(); i++) {
            EffectRun run = builtRuns.get(i);
            int signatureIndex = COLUMNS_PER_RUN_SIGNATURE * i;
            if (builtRunColumns[signatureIndex]
                    != label.getAbsoluteTextColumnOfLetter(run.getStartLetter())
                    || builtRunColumns[signatureIndex + 1]
                    != label.getAbsoluteTextColumnOfLetter(run.getEndLetterExclusive() - 1)) {
                return true;
            }
        }
        return false;
    }

    private void rebuildZones(float rowHeightPerColumnWidth) {
        builtRowHeightPerColumnWidth = rowHeightPerColumnWidth;
        builtRuns = label.getEffectRuns();
        builtColumn = label.getAbsoluteX();
        builtRow = label.getAbsoluteY();
        builtWidth = label.getWidth();
        builtRunColumns = new int[COLUMNS_PER_RUN_SIGNATURE * builtRuns.size()];
        zones.clear();
        for (int i = 0; i < builtRuns.size(); i++) {
            EffectRun run = builtRuns.get(i);
            builtRunColumns[COLUMNS_PER_RUN_SIGNATURE * i] =
                    label.getAbsoluteTextColumnOfLetter(run.getStartLetter());
            builtRunColumns[COLUMNS_PER_RUN_SIGNATURE * i + 1] =
                    label.getAbsoluteTextColumnOfLetter(run.getEndLetterExclusive() - 1);
            addZonesOfRun(run);
        }
    }

    // A run can be split in pieces when a part of it is cut off or hidden.
    private void addZonesOfRun(EffectRun run) {
        int pieceStart = Label.NOT_SHOWN;
        int pieceLength = 0;
        for (int letter = run.getStartLetter(); letter < run.getEndLetterExclusive(); letter++) {
            int column = label.getAbsoluteTextColumnOfLetter(letter);
            if (pieceLength > 0 && column == pieceStart + pieceLength) {
                pieceLength++;
                continue;
            }
            addZone(run, pieceStart, pieceLength);
            pieceStart = column;
            pieceLength = column == Label.NOT_SHOWN ? 0 : 1;
        }
        addZone(run, pieceStart, pieceLength);
    }

    private void addZone(EffectRun run, int column, int length) {
        if (length == 0) {
            return;
        }
        RectangleShape shape = RectangleShape.forCellAspect(column, label.getAbsoluteY(), length,
                1, ShapeFill.FILLED, builtRowHeightPerColumnWidth);
        zones.add(new EffectZone(shape, run.getEffect()));
    }
}
