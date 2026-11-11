package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.BitSet;

/**
 * The settings of a rule's check. Only the setting its check uses is filled in; the others stay empty or 0.
 *
 * @param rows the row numbers a noInkRows check watches
 * @param columns the column numbers a noInkColumns check watches
 * @param ringSize the width of the blank border for a ring check
 */
record CheckSettings(BitSet rows, BitSet columns, int ringSize) {

    public static final CheckSettings NONE = new CheckSettings(new BitSet(), new BitSet(), 0);

    public CheckSettings {
        rows = (BitSet) rows.clone();
        columns = (BitSet) columns.clone();
    }

    @Override
    public BitSet rows() {
        return (BitSet) rows.clone();
    }

    @Override
    public BitSet columns() {
        return (BitSet) columns.clone();
    }
}
