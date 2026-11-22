package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.SkinObjectSchema;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.SkinStyleKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeyEditorKindTest {

    @Test
    void everyKeyTypeMapsToItsEditorKind() {
        assertEquals(KeyEditorKind.COLOR, KeyEditorKind.forType(new SkinKeyType.Color()));
        assertEquals(KeyEditorKind.GLYPH, KeyEditorKind.forType(new SkinKeyType.Glyph()));
        assertEquals(KeyEditorKind.MARK, KeyEditorKind.forType(new SkinKeyType.Mark()));
        assertEquals(KeyEditorKind.WHOLE_NUMBER,
                KeyEditorKind.forType(new SkinKeyType.WholeNumber(0, "cells")));
        assertEquals(KeyEditorKind.DECIMAL_NUMBER, KeyEditorKind.forType(
                new SkinKeyType.DecimalNumber(0, 1, "a number", "from 0 to 1")));
        assertEquals(KeyEditorKind.FLAG, KeyEditorKind.forType(new SkinKeyType.Flag()));
        assertEquals(KeyEditorKind.CHOICE,
                KeyEditorKind.forType(new SkinKeyType.Choice("one of", List.of("a", "b"))));
        assertEquals(KeyEditorKind.STYLE_REFERENCE, KeyEditorKind.forType(
                SkinKeyType.StyleReference.nameOnly(SkinStyleKind.LABEL)));
        assertEquals(KeyEditorKind.STYLE_REFERENCE, KeyEditorKind.forType(
                SkinKeyType.StyleReference.nameOrObject(SkinStyleKind.BUTTON)));
        assertEquals(KeyEditorKind.GROUP,
                KeyEditorKind.forType(new SkinKeyType.Group(SkinObjectSchema.of("an object"))));
    }

    @Test
    void everyKeyOfTheBuiltInSchemaHasAnEditor() {
        for (String section : SkinSchema.getSectionNames()) {
            if (section.equals(SkinSchema.COLORS)) {
                continue;
            }
            assertNoUnsupportedKey(SkinSchema.getEntrySchema(section));
        }
    }

    private static void assertNoUnsupportedKey(SkinObjectSchema schema) {
        for (SkinKeyDefinition key : schema.keys()) {
            assertNotEquals(KeyEditorKind.UNSUPPORTED, KeyEditorKind.forType(key.type()),
                    key.name());
            if (key.type() instanceof SkinKeyType.Group group) {
                assertNoUnsupportedKey(group.schema());
            }
        }
    }
}
