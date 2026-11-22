package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.text.EffectRun;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.zone.EffectZone;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the effect runs of one {@link Label} to the text layer. The zones are rebuilt when the
 * list of runs is replaced (the text, color names, effects, or markup flag changed), when the
 * label's absolute position or width changes, or when the text column of the first or last letter
 * of any run changes. That last check covers the alignment, a hidden ancestor, and the view size
 * or scroll position of a scroll pane. A change to a letter in the middle of a run alone is not
 * noticed. A frame without a change allocates nothing.
 */
final class LabelEffectZones {
    // A text cell is half as wide as it is high.
    private static final float TEXT_CELL_ROW_HEIGHT_PER_COLUMN_WIDTH = 2f;
    private static final int COLUMNS_PER_RUN_SIGNATURE = 2;

    private final Label label;
    private final List<EffectZone> zones = new ArrayList<>();
    private List<EffectRun> builtRuns;
    private int[] builtRunColumns = new int[0];
    private int builtColumn;
    private int builtRow;
    private int builtWidth;

    LabelEffectZones(Label label) {
        this.label = label;
    }

    void apply(Layer textLayer, float seconds) {
        if (hasLabelChanged()) {
            rebuildZones();
        }
        for (int i = 0; i < zones.size(); i++) {
            zones.get(i).apply(textLayer, seconds);
        }
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

    private void rebuildZones() {
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
                1, ShapeFill.FILLED, TEXT_CELL_ROW_HEIGHT_PER_COLUMN_WIDTH);
        zones.add(new EffectZone(shape, run.getEffect()));
    }
}
