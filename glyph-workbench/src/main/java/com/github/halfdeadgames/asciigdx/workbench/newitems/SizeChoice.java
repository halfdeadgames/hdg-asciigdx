package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;

/** What a choosable row of the "new size" list stands for. */
sealed interface SizeChoice permits SizeChoice.Size, SizeChoice.Other {

    /** A size that does not exist yet. */
    record Size(CellSize size) implements SizeChoice {
    }

    /** The row that opens the typed page. */
    record Other() implements SizeChoice {
    }
}
