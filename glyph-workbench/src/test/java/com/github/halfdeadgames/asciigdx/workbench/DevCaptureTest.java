package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewTabs;
import org.junit.jupiter.api.Test;

class DevCaptureTest {

    @Test
    void overlayRequestHasACodePointAndAnOptionalShiftAndDown() {
        assertEquals(new DevCapture.OverlayRequest(0x21, 0, 0), DevCapture.parseOverlayRequest("overlay:U+0021"));
        assertEquals(new DevCapture.OverlayRequest(0x21, 5, 0), DevCapture.parseOverlayRequest("overlay:U+0021:5"));
        assertEquals(new DevCapture.OverlayRequest(0x41, -2, 0), DevCapture.parseOverlayRequest(" OVERLAY:A:-2 "));
        assertEquals(new DevCapture.OverlayRequest(0x21, 5, 2), DevCapture.parseOverlayRequest("overlay:U+0021:5:2"));
        assertEquals(new DevCapture.OverlayRequest(0x21, 0, -1), DevCapture.parseOverlayRequest("overlay:U+0021:0:-1"));
    }

    @Test
    void overlayRequestWithABadDownIsRejected() {
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:5:x"));
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:5:"));
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:5:2:1"));
    }

    @Test
    void previewTabIsFoundByNameIgnoringCase() {
        assertEquals(PreviewTabs.Tab.TEXT, DevCapture.parseTab("text"));
        assertEquals(PreviewTabs.Tab.TILE, DevCapture.parseTab(" Tile "));
        assertEquals(PreviewTabs.Tab.SCALES, DevCapture.parseTab("SCALES"));
        assertNull(DevCapture.parseTab("nope"));
        assertNull(DevCapture.parseTab(null));
    }

    @Test
    void guidesRequestIsARowIndexOrMinusOne() {
        assertEquals(0, DevCapture.parseRowIndex("0"));
        assertEquals(7, DevCapture.parseRowIndex(" 7 "));
        assertEquals(-1, DevCapture.parseRowIndex("-4"));
        assertEquals(-1, DevCapture.parseRowIndex("x"));
        assertEquals(-1, DevCapture.parseRowIndex(""));
        assertEquals(-1, DevCapture.parseRowIndex(null));
    }

    @Test
    void upscaleSourceRequestOpensOnBlankOpenOrDown() {
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest(null));
        assertEquals(0, DevCapture.parseUpscaleSourceRequest(""));
        assertEquals(0, DevCapture.parseUpscaleSourceRequest(" OPEN "));
        assertEquals(2, DevCapture.parseUpscaleSourceRequest("down:2"));
        assertEquals(0, DevCapture.parseUpscaleSourceRequest("down:-1"));
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest("1"));
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest("sideways"));
    }

    @Test
    void upscaleSourcePickRequestOpensAndMovesLikeDownThenChooses() {
        assertEquals(0, DevCapture.parseUpscaleSourceRequest("pick"));
        assertEquals(0, DevCapture.parseUpscaleSourceRequest(" PICK "));
        assertEquals(3, DevCapture.parseUpscaleSourceRequest("pick:down:3"));
        assertEquals(0, DevCapture.parseUpscaleSourceRequest("pick:down:-1"));
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest("pick:sideways"));
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest("pickle"));
    }

    @Test
    void onlyPickRequestsChooseTheFont() {
        assertTrue(DevCapture.parseUpscaleSourcePick("pick"));
        assertTrue(DevCapture.parseUpscaleSourcePick(" Pick:down:2 "));
        assertFalse(DevCapture.parseUpscaleSourcePick(null));
        assertFalse(DevCapture.parseUpscaleSourcePick(""));
        assertFalse(DevCapture.parseUpscaleSourcePick("open"));
        assertFalse(DevCapture.parseUpscaleSourcePick("down:2"));
        assertFalse(DevCapture.parseUpscaleSourcePick("pickle"));
    }

    @Test
    void candidateRequestOpensAndChoosesTheFontLikePick() {
        assertEquals(0, DevCapture.parseUpscaleSourceRequest("cand"));
        assertEquals(0, DevCapture.parseUpscaleSourceRequest(" CAND:3 "));
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest("cand:x"));
        assertEquals(-1, DevCapture.parseUpscaleSourceRequest("candle"));
        assertTrue(DevCapture.parseUpscaleSourcePick("cand"));
        assertTrue(DevCapture.parseUpscaleSourcePick("cand:2"));
        assertFalse(DevCapture.parseUpscaleSourcePick("cand:x"));
    }

    @Test
    void candidateRequestNamesTheOneBasedCandidate() {
        assertEquals(1, DevCapture.parseUpscaleCandidate("cand"));
        assertEquals(1, DevCapture.parseUpscaleCandidate("cand:1"));
        assertEquals(3, DevCapture.parseUpscaleCandidate(" Cand:3 "));
        assertEquals(1, DevCapture.parseUpscaleCandidate("cand:0"));
        assertEquals(-1, DevCapture.parseUpscaleCandidate("cand:x"));
        assertEquals(-1, DevCapture.parseUpscaleCandidate("pick"));
        assertEquals(-1, DevCapture.parseUpscaleCandidate(null));
    }

    @Test
    void newSizeRequestOpensTheDialogAndMayMoveDown() {
        assertEquals(-1, DevCapture.parseNewSizeRequest(null));
        assertEquals(0, DevCapture.parseNewSizeRequest("1"));
        assertEquals(0, DevCapture.parseNewSizeRequest(""));
        assertEquals(2, DevCapture.parseNewSizeRequest("down:2"));
        assertEquals(3, DevCapture.parseNewSizeRequest(" DOWN: 3 "));
        assertEquals(0, DevCapture.parseNewSizeRequest("down:x"));
        assertEquals(0, DevCapture.parseNewSizeRequest("down:-1"));
    }

    @Test
    void newSizeOtherRequestIsTheTypedTextOrNullForTheList() {
        assertNull(DevCapture.parseNewSizeOther(null));
        assertNull(DevCapture.parseNewSizeOther("list"));
        assertNull(DevCapture.parseNewSizeOther("down:2"));
        assertEquals("", DevCapture.parseNewSizeOther("other"));
        assertEquals("", DevCapture.parseNewSizeOther("other:"));
        assertEquals("24x32", DevCapture.parseNewSizeOther(" Other: 24x32 "));
        assertEquals(0, DevCapture.parseNewSizeRequest("other:24x32"));
        assertEquals(0, DevCapture.parseNewSizeRequest("list"));
    }

    @Test
    void newFontRequestIsTheTypedTextOrNullWhenUnset() {
        assertNull(DevCapture.parseNewFontRequest(null));
        assertEquals("", DevCapture.parseNewFontRequest(""));
        assertEquals("My Font", DevCapture.parseNewFontRequest("My Font"));
    }

    @Test
    void newFontSizeRequestHasANameAndOptionalTypedText() {
        assertNull(DevCapture.parseNewFontSizeRequest(null));
        assertEquals(new DevCapture.NewFontSizeRequest("my-font", ""), DevCapture.parseNewFontSizeRequest(""));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", ""), DevCapture.parseNewFontSizeRequest("tiny"));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", "64x128"),
            DevCapture.parseNewFontSizeRequest("tiny:64x128"));
        assertEquals(new DevCapture.NewFontSizeRequest("my-font", "7x13"), DevCapture.parseNewFontSizeRequest(":7x13"));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", ""), DevCapture.parseNewFontSizeRequest("tiny:"));
    }

    @Test
    void otherRequestsAreNotOverlayRequests() {
        assertNull(DevCapture.parseOverlayRequest(null));
        assertNull(DevCapture.parseOverlayRequest("menu"));
        assertNull(DevCapture.parseOverlayRequest("U+0021"));
        assertNull(DevCapture.parseOverlayRequest("overlay:"));
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:x"));
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:1:2:3"));
    }
}
