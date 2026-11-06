package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class UpscalePickTest {

    private static final UpscaleSources.Font FONT = new UpscaleSources.Font("classic", 8, 8);
    private static final int COLUMNS = 16;

    private static String rows(String row) {
        return String.join("\n", Collections.nCopies(8, row));
    }

    private static final String FONT_TEXT = "cell 8x8\n\n"
        + "U+0041 A\n" + rows("#######") + "\n\n"
        + "U+0042 B\n" + rows(".......") + "\n\n"
        + "U+0043 C\n" + rows("#######") + "\n";

    private static UpscaleSourceSheet sheet() {
        return UpscaleSourceSheet.of(FontDataParser.parse(FONT_TEXT), FONT);
    }

    private static int slotOf(UpscaleSourceSheet sheet, int codePoint) {
        for (int slot = 0; slot < sheet.editor().slotCount(); slot++) {
            if (sheet.codePointAt(slot) == codePoint) {
                return slot;
            }
        }
        return -1;
    }

    @Test
    void startsOnTheGivenSlotAndReportsItsCodePoint() {
        UpscaleSourceSheet sheet = sheet();
        UpscalePick pick = new UpscalePick(sheet, slotOf(sheet, 0x41), COLUMNS);
        assertEquals(slotOf(sheet, 0x41), pick.cursor());
        assertEquals(0x41, pick.codePoint());
    }

    @Test
    void movingSkipsSlotsThatCannotBePicked() {
        UpscaleSourceSheet sheet = sheet();
        UpscalePick pick = new UpscalePick(sheet, slotOf(sheet, 0x41), COLUMNS);
        pick.move(1, 0);
        assertEquals(0x43, pick.codePoint());
    }

    @Test
    void selectingAnUnpickableSlotChangesNothing() {
        UpscaleSourceSheet sheet = sheet();
        int start = slotOf(sheet, 0x41);
        UpscalePick pick = new UpscalePick(sheet, start, COLUMNS);
        pick.select(slotOf(sheet, 0x42));
        pick.select(-1);
        assertEquals(start, pick.cursor());
    }

    @Test
    void selectingAPickableSlotMovesTheCursor() {
        UpscaleSourceSheet sheet = sheet();
        UpscalePick pick = new UpscalePick(sheet, slotOf(sheet, 0x41), COLUMNS);
        pick.select(slotOf(sheet, 0x43));
        assertEquals(0x43, pick.codePoint());
    }

    @Test
    void onlyAClickOnTheSelectedSlotConfirms() {
        UpscaleSourceSheet sheet = sheet();
        UpscalePick pick = new UpscalePick(sheet, slotOf(sheet, 0x41), COLUMNS);
        assertFalse(pick.isConfirmingClick(slotOf(sheet, 0x43)));
        assertTrue(pick.isConfirmingClick(slotOf(sheet, 0x41)));
        pick.select(slotOf(sheet, 0x43));
        assertTrue(pick.isConfirmingClick(slotOf(sheet, 0x43)));
        assertFalse(pick.isConfirmingClick(slotOf(sheet, 0x41)));
    }
}
