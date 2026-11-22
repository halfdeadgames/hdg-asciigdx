package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.ui.SkinFileException;
import java.util.function.Consumer;

/**
 * Sends the panel's edits to the document, one undo step per committed edit. A rejected edit
 * leaves the document as it was (the document restores itself), tells the status sink why, and
 * lets the editor put its widget back.
 */
final class EditGateway {
    private static final String CHANGED_PREFIX = "Changed ";
    private static final String REJECTED_PREFIX = "Not changed: ";

    private final SkinDocument document;
    private final Consumer<String> statusSink;

    EditGateway(SkinDocument document, Consumer<String> statusSink) {
        this.document = document;
        this.statusSink = statusSink;
    }

    /**
     * Applies an edit to the document.
     *
     * @param description what the edit changes, shown in the status text
     * @param edit the change, run on the document's current tree
     * @param onRejected called when the document refused the edit
     */
    void apply(String description, Consumer<JsonValue> edit, Runnable onRejected) {
        try {
            document.apply(edit);
        } catch (SkinFileException | IllegalArgumentException | IllegalStateException rejection) {
            statusSink.accept(REJECTED_PREFIX + rejection.getMessage());
            onRejected.run();
            return;
        }
        statusSink.accept(CHANGED_PREFIX + description);
    }
}
