package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DevCaptureTest {

    @Test
    void overlayRequestHasACodePointAndAnOptionalShift() {
        assertEquals(new DevCapture.OverlayRequest(0x21, 0), DevCapture.parseOverlayRequest("overlay:U+0021"));
        assertEquals(new DevCapture.OverlayRequest(0x21, 5), DevCapture.parseOverlayRequest("overlay:U+0021:5"));
        assertEquals(new DevCapture.OverlayRequest(0x41, -2), DevCapture.parseOverlayRequest(" OVERLAY:A:-2 "));
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
