package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Builds the controls that edit the value of one key. */
@FunctionalInterface
interface KeyEditor {

    /** Returns the controls for the key, already sized, with the key's current value shown. */
    Widget createControls(KeyRow row);
}
