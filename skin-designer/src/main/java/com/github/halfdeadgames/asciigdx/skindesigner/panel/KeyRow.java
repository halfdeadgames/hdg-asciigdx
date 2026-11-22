package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition;
import java.util.List;

/**
 * One key to edit: where it is, what the schema says about it, and its current value.
 *
 * @param value the value now in the tree; never {@code null}, because an absent key has no editor
 * @param partners the keys the schema says must be given or left out together with this one
 */
record KeyRow(KeyLocation location, SkinKeyDefinition definition, JsonValue value,
        List<SkinKeyDefinition> partners) {
}
