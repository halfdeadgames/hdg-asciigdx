package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.rules.NotesMarkdown.Kind;
import com.github.halfdeadgames.asciigdx.workbench.rules.NotesMarkdown.NoteLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotesMarkdownTest {

    @Test
    void emptyInputGivesNoLines() {
        assertTrue(NotesMarkdown.parse("").isEmpty());
        assertTrue(NotesMarkdown.parse(" \n\n  ").isEmpty());
    }

    @Test
    void headingsLoseTheirHashes() {
        assertEquals(
                List.of(new NoteLine(Kind.HEADING, "Title"), new NoteLine(Kind.HEADING, "Sub")),
                NotesMarkdown.parse("# Title\n### Sub"));
    }

    @Test
    void hashWithoutSpaceIsText() {
        assertEquals(List.of(new NoteLine(Kind.TEXT, "#hashtag")), NotesMarkdown.parse("#hashtag"));
    }

    @Test
    void bulletsAcceptBothMarkersAndIndent() {
        assertEquals(List.of(new NoteLine(Kind.BULLET, "one"), new NoteLine(Kind.BULLET, "two"),
                        new NoteLine(Kind.BULLET, "three")),
                NotesMarkdown.parse("- one\n* two\n   - three"));
    }

    @Test
    void textKeepsInlineMarkdownAndLosesTrailingSpace() {
        assertEquals(List.of(new NoteLine(Kind.TEXT, "**bold** [link](x) `code`")),
                NotesMarkdown.parse("**bold** [link](x) `code`   "));
    }

    @Test
    void blankRunsCollapseAndEdgesAreDropped() {
        var lines = NotesMarkdown.parse("\n\nfirst\n\n  \n\nsecond\n\n");
        assertEquals(List.of(new NoteLine(Kind.TEXT, "first"), new NoteLine(Kind.BLANK, ""),
                new NoteLine(Kind.TEXT, "second")), lines);
    }

    @Test
    void windowsLineEndingsAreHandled() {
        assertEquals(List.of(new NoteLine(Kind.HEADING, "A"), new NoteLine(Kind.BLANK, ""),
                new NoteLine(Kind.BULLET, "b")), NotesMarkdown.parse("# A\r\n\r\n- b\r\n"));
    }
}
