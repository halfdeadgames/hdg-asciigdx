package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;

/** One style of the skin tree that the panel can edit: its section and its name. */
record StyleChoice(String section, String styleName) {

    /**
     * Lists the styles of the given sections that exist in the tree, section by section in the
     * order given, each section's styles in file order. A section the tree lacks adds nothing.
     */
    static List<StyleChoice> listIn(JsonValue tree, List<String> sections) {
        List<StyleChoice> choices = new ArrayList<>();
        for (String section : sections) {
            JsonValue styles = tree.get(section);
            if (styles == null) {
                continue;
            }
            for (JsonValue style = styles.child; style != null; style = style.next) {
                choices.add(new StyleChoice(section, style.name));
            }
        }
        return choices;
    }

    String displayName() {
        return section + ": " + styleName;
    }
}
