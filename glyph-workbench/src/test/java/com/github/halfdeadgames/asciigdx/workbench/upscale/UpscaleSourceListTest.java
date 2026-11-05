package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources.Font;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpscaleSourceListTest {

    private final UpscaleSourceList sourceList = new UpscaleSourceList();

    private static Font square(String folder, int size) {
        return new Font(folder, size, size);
    }

    private SelectList.Row row(int index) {
        return sourceList.list().rows().get(index);
    }

    @Test
    void ownFamilyComesFirstEvenWhenAnotherFolderSortsEarlier() {
        sourceList.open(square("zeta", 32), List.of(square("alpha", 8), square("zeta", 8)));
        assertEquals("ZETA", row(0).text());
        assertEquals("ALPHA", row(2).text());
    }

    @Test
    void otherFoldersKeepFirstAppearanceOrder() {
        sourceList.open(square("a", 32), List.of(square("c", 8), square("b", 8), square("c", 16)));
        assertEquals("C", row(0).text());
        assertEquals("B", row(3).text());
    }

    @Test
    void headingsAreNotSelectableOrDimmed() {
        sourceList.open(square("a", 16), List.of(square("a", 8)));
        assertEquals(new SelectList.Row("A", false, false, 0), row(0));
        assertFalse(sourceList.list().isChoosable(0));
    }

    @Test
    void cursorStartsOnTheLargestUsableOfTheOwnFamily() {
        sourceList.open(square("a", 32), List.of(square("b", 16), square("a", 8), square("a", 16)));
        assertEquals(square("a", 16), sourceList.selected().font());
        assertEquals(2, sourceList.list().selectedIndex());
    }

    @Test
    void unusableRowsAreDimmedIndentedAndUnchoosable() {
        sourceList.open(square("a", 24), List.of(square("a", 8), square("a", 16)));
        assertEquals(new SelectList.Row("  16X16  NOT EVEN", false, true, 0), row(2));
        assertFalse(sourceList.list().isChoosable(2));
    }

    @Test
    void moveSkipsGreyedRowsAndHeadings() {
        sourceList.open(square("a", 24), List.of(square("a", 8), square("a", 16), square("b", 12)));
        assertEquals(square("a", 8), sourceList.selected().font());
        sourceList.move(1);
        assertEquals(square("b", 12), sourceList.selected().font());
        sourceList.move(-1);
        assertEquals(square("a", 8), sourceList.selected().font());
    }

    @Test
    void folderWithOnlyTheTargetIsSkipped() {
        sourceList.open(square("a", 16), List.of(square("a", 16), square("b", 8)));
        assertEquals(2, sourceList.list().rows().size());
        assertEquals("B", row(0).text());
    }

    @Test
    void noUsableOptionMeansNoChoicesAndAMessage() {
        sourceList.open(new Font("a", 16, 24), List.of(square("a", 8)));
        assertFalse(sourceList.hasChoices());
        assertNull(sourceList.selected());
        assertEquals(List.of("No smaller font divides", "16X24 evenly"), sourceList.noChoiceLines());
    }

    @Test
    void usableRowLabelShowsSizeAndFactor() {
        sourceList.open(new Font("a", 16, 32), List.of(new Font("a", 8, 16)));
        assertEquals(new SelectList.Row("8X16  2X", true, false, 0), row(1));
    }

    @Test
    void openAndCloseToggleTheState() {
        assertFalse(sourceList.isOpen());
        sourceList.open(square("a", 16), List.of(square("a", 8)));
        assertTrue(sourceList.isOpen());
        sourceList.close();
        assertFalse(sourceList.isOpen());
        assertTrue(sourceList.list().rows().isEmpty());
    }
}
