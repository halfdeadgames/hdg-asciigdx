package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import java.util.function.Consumer;

/**
 * What the scene gives the panel.
 *
 * @param uiRoot the root the panel's widgets live in
 * @param document the skin document the panel edits
 * @param statusSink receives a short text for the status line after each edit or refusal
 * @param styleChosen called after the user picked another style, so the scene can rebuild the panel
 */
public record PanelHost(UiRoot uiRoot, SkinDocument document, Consumer<String> statusSink,
        Runnable styleChosen) {
}
