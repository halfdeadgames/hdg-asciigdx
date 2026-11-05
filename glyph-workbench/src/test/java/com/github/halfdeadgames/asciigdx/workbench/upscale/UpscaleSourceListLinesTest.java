package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources.Font;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpscaleSourceListLinesTest {

    private final UpscaleSourceList sourceList = new UpscaleSourceList();

    private static Font square(String folder, int size) {
        return new Font(folder, size, size);
    }

    @Test
    void closedListHasNoLines() {
        assertTrue(sourceList.lines().isEmpty());
    }

    @Test
    void choicesGetTheIntoLineABlankAndRoomForTheList() {
        sourceList.open(square("a", 16), List.of(square("a", 8)));

        List<String> lines = sourceList.lines();

        assertEquals("Into A 16X16", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals(UpscaleSourceList.LINES_ABOVE_LIST + sourceList.list().shownRows() * SelectList.PITCH - 1,
            lines.size());
        assertEquals(" ".repeat(sourceList.list().listWidth()), lines.get(2));
    }

    @Test
    void longFolderNamesAreCutWithDots() {
        sourceList.open(square("x".repeat(40), 16), List.of(square("x".repeat(40), 8)));

        String line = sourceList.lines().get(0);

        assertEquals(UpscaleSourceList.LINE_CELLS, line.length());
        assertTrue(line.endsWith(".."));
    }

    @Test
    void withoutChoicesTheMessageReplacesTheListRoom() {
        sourceList.open(square("a", 8), List.of());

        assertEquals(List.of("Into A 8X8", "", "No smaller font divides", "8X8 evenly"), sourceList.lines());
    }

    @Test
    void smallAreasKeepAtLeastTwoRowsAndLargeOnesShowEverything() {
        sourceList.open(square("a", 64), List.of(square("a", 8), square("a", 16), square("a", 32)));

        sourceList.fitTo(0);
        assertEquals(2, sourceList.list().shownRows());
        assertTrue(sourceList.list().scrolls());

        sourceList.fitTo(1000);
        assertEquals(4, sourceList.list().shownRows());
    }
}
