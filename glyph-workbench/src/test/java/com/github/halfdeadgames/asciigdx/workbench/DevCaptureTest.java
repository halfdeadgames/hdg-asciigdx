package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewTabs;
import org.junit.jupiter.api.Test;

class DevCaptureTest {

    @Test
    void overlayRequestHasACodePointAndAnOptionalShift() {
        assertEquals(new DevCapture.OverlayRequest(0x21, 0), DevCapture.parseOverlayRequest("overlay:U+0021"));
        assertEquals(new DevCapture.OverlayRequest(0x21, 5), DevCapture.parseOverlayRequest("overlay:U+0021:5"));
        assertEquals(new DevCapture.OverlayRequest(0x41, -2), DevCapture.parseOverlayRequest(" OVERLAY:A:-2 "));
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
    void newFontRequestIsTheTypedTextOrNullWhenUnset() {
        assertNull(DevCapture.parseNewFontRequest(null));
        assertEquals("", DevCapture.parseNewFontRequest(""));
        assertEquals("My Font", DevCapture.parseNewFontRequest("My Font"));
    }

    @Test
    void newFontSizeRequestHasANameAndOptionalMoves() {
        assertNull(DevCapture.parseNewFontSizeRequest(null));
        assertEquals(new DevCapture.NewFontSizeRequest("my-font", 0), DevCapture.parseNewFontSizeRequest(""));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", 0), DevCapture.parseNewFontSizeRequest("tiny"));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", 3), DevCapture.parseNewFontSizeRequest("tiny:down:3"));
        assertEquals(new DevCapture.NewFontSizeRequest("my-font", 2), DevCapture.parseNewFontSizeRequest("down:2"));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", 0), DevCapture.parseNewFontSizeRequest("tiny:down:x"));
        assertEquals(new DevCapture.NewFontSizeRequest("tiny", 0), DevCapture.parseNewFontSizeRequest("tiny:down:-1"));
    }

    @Test
    void otherRequestsAreNotOverlayRequests() {
        assertNull(DevCapture.parseOverlayRequest(null));
        assertNull(DevCapture.parseOverlayRequest("menu"));
        assertNull(DevCapture.parseOverlayRequest("U+0021"));
        assertNull(DevCapture.parseOverlayRequest("overlay:"));
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:x"));
        assertNull(DevCapture.parseOverlayRequest("overlay:U+0021:1:2"));
    }
}
