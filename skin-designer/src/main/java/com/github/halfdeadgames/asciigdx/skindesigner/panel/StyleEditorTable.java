package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.SkinObjectSchema;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the table of editors for one style: a name row and a controls row for every key of the
 * style's schema, with the keys of a group indented below the group's name.
 */
final class StyleEditorTable {
    private static final int INDENT_CELLS = 2;
    private static final String DEFAULT_TEXT = "default";
    private static final String MISSING_TEXT = "missing";
    private static final String RESET_TEXT = "reset";
    private static final String ADD_TEXT = "add";
    private static final String USE_NAME_TEXT = "use style name";

    private final EditorContext context;
    private final KeyEditors editors;

    StyleEditorTable(EditorContext context) {
        this.context = context;
        this.editors = new KeyEditors(context);
    }

    /** Returns the table for a style that exists in the context's tree. */
    Table create(StyleChoice choice) {
        Table table = new Table();
        JsonValue style = context.tree().get(choice.section()).get(choice.styleName());
        SkinObjectSchema schema = SkinSchema.getEntrySchema(choice.section());
        addKeys(table, schema, style, keyName -> KeyLocation.ofStyleKey(choice.section(),
                choice.styleName(), keyName), 0);
        table.pack();
        return table;
    }

    private void addKeys(Table table, SkinObjectSchema schema, JsonValue owner,
            LocationFactory locations, int depth) {
        for (SkinKeyDefinition definition : schema.keys()) {
            KeyLocation location = locations.locate(definition.name());
            JsonValue value = owner.get(definition.name());
            addRow(table, depth, createNameLabel(definition));
            List<SkinKeyDefinition> partners = partnersOf(schema, definition.name());
            if (value == null) {
                addRow(table, depth + 1, createAbsentControls(definition, location, partners));
                continue;
            }
            addPresentKey(table, new KeyRow(location, definition, value, partners), depth);
        }
    }

    // Keys that the schema says go together are added and removed in one edit.
    private static List<SkinKeyDefinition> partnersOf(SkinObjectSchema schema, String keyName) {
        List<SkinKeyDefinition> partners = new ArrayList<>();
        for (SkinObjectSchema.KeyPair pair : schema.togetherPairs()) {
            String partnerName = pair.first().equals(keyName) ? pair.second()
                    : pair.second().equals(keyName) ? pair.first() : null;
            if (partnerName == null) {
                continue;
            }
            for (SkinKeyDefinition candidate : schema.keys()) {
                if (candidate.name().equals(partnerName)) {
                    partners.add(candidate);
                }
            }
        }
        return partners;
    }

    private Widget createAbsentControls(SkinKeyDefinition definition, KeyLocation location,
            List<SkinKeyDefinition> partners) {
        if (definition.required()) {
            return context.widgets().createLabel(MISSING_TEXT);
        }
        Widget addButton = context.widgets().createButton(ADD_TEXT, () -> context
                .applyClickEdit(location, SkinEdits.add(location, definition.type(), partners)));
        return ControlRow.of(context.widgets().createLabel(DEFAULT_TEXT), addButton);
    }

    private void addPresentKey(Table table, KeyRow row, int depth) {
        KeyEditorKind kind = KeyEditorKind.forType(row.definition().type());
        if (kind == KeyEditorKind.GROUP) {
            addGroup(table, row, depth);
            return;
        }
        if (kind == KeyEditorKind.STYLE_REFERENCE && row.value().isObject()) {
            addInlineObject(table, row, depth);
            return;
        }
        Widget controls = editors.forKind(kind).createControls(row);
        addRow(table, depth + 1, withResetButton(controls, row));
    }

    private void addGroup(Table table, KeyRow row, int depth) {
        if (!row.definition().required()) {
            addRow(table, depth + 1, withResetButton(null, row));
        }
        SkinKeyType.Group group = (SkinKeyType.Group) row.definition().type();
        addKeys(table, group.schema(), row.value(), row.location()::nested, depth + 1);
    }

    // An inline style has the keys of the referenced kind, so it is laid out like a group.
    private void addInlineObject(Table table, KeyRow row, int depth) {
        SkinKeyType.StyleReference type = (SkinKeyType.StyleReference) row.definition().type();
        String section = StyleReferenceEditor.sectionOf(type.kind());
        Widget controls = withResetButton(createUseNamedStyleButton(row, section), row);
        if (controls != null) {
            addRow(table, depth + 1, controls);
        }
        addKeys(table, SkinSchema.getEntrySchema(section), row.value(), row.location()::nested,
                depth + 1);
    }

    // Replaces the inline object with the name of the first style of its kind.
    private Widget createUseNamedStyleButton(KeyRow row, String section) {
        List<String> names = SeedValues.namesIn(context.tree(), section);
        if (names.isEmpty()) {
            return null;
        }
        return context.widgets().createButton(USE_NAME_TEXT, () -> context.applyClickEdit(
                row.location(), SkinEdits.setText(row.location(), names.get(0))));
    }

    // An optional key that is present can be removed, so its default applies again.
    private Widget withResetButton(Widget controls, KeyRow row) {
        if (row.definition().required()) {
            return controls;
        }
        Widget resetButton = context.widgets().createButton(RESET_TEXT,
                () -> context.applyClickEdit(row.location(),
                        SkinEdits.reset(row.location(), partnerNames(row))));
        return controls == null ? ControlRow.of(resetButton) : ControlRow.of(controls, resetButton);
    }

    private static List<String> partnerNames(KeyRow row) {
        List<String> names = new ArrayList<>();
        for (SkinKeyDefinition partner : row.partners()) {
            names.add(partner.name());
        }
        return names;
    }

    private Widget createNameLabel(SkinKeyDefinition definition) {
        return context.widgets().createLabel(definition.name());
    }

    private static void addRow(Table table, int depth, Widget widget) {
        table.add(widget).padLeft(depth * INDENT_CELLS);
        table.row();
    }

    /** Finds where a key of the object being laid out sits in the tree. */
    @FunctionalInterface
    private interface LocationFactory {
        KeyLocation locate(String keyName);
    }
}
