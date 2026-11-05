package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PointerTargetsTest {

    private final List<String> calls = new ArrayList<>();

    private final class FakeTarget implements PointerTarget {
        private final String name;
        private final boolean claims;
        private final boolean captured;

        FakeTarget(String name, boolean claims, boolean captured) {
            this.name = name;
            this.claims = claims;
            this.captured = captured;
        }

        @Override
        public void notifyPress(int x, int y) {
            calls.add(name + ".notify(" + x + "," + y + ")");
        }

        @Override
        public boolean claimPress(int x, int y) {
            calls.add(name + ".claim");
            return claims;
        }

        @Override
        public boolean isCaptured() {
            return captured;
        }

        @Override
        public void drag(int x, int y) {
            calls.add(name + ".drag(" + x + "," + y + ")");
        }

        @Override
        public void release() {
            calls.add(name + ".release");
        }
    }

    private PointerTargets targets(PointerTarget... all) {
        return new PointerTargets(List.of(all));
    }

    @Test
    void everyTargetIsNotifiedInOrderBeforeAnyClaim() {
        targets(new FakeTarget("a", false, false), new FakeTarget("b", false, false))
            .dispatch(3, 4, true, true);

        assertEquals(List.of("a.notify(3,4)", "b.notify(3,4)", "a.claim", "b.claim"), calls);
    }

    @Test
    void firstClaimStopsLaterClaims() {
        targets(new FakeTarget("a", false, false), new FakeTarget("b", true, false), new FakeTarget("c", true, false))
            .dispatch(1, 2, true, true);

        assertEquals(List.of("a.notify(1,2)", "b.notify(1,2)", "c.notify(1,2)", "a.claim", "b.claim"), calls);
    }

    @Test
    void noClaimIsFine() {
        targets(new FakeTarget("a", false, false)).dispatch(0, 0, true, false);

        assertEquals(List.of("a.notify(0,0)", "a.claim"), calls);
    }

    @Test
    void clickTakesPriorityOverOngoingCapture() {
        targets(new FakeTarget("a", false, true)).dispatch(5, 6, true, true);

        assertEquals(List.of("a.notify(5,6)", "a.claim"), calls);
    }

    @Test
    void firstCapturedTargetGetsDragWhilePressed() {
        targets(new FakeTarget("a", false, false), new FakeTarget("b", false, true), new FakeTarget("c", false, true))
            .dispatch(7, 8, false, true);

        assertEquals(List.of("b.drag(7,8)"), calls);
    }

    @Test
    void firstCapturedTargetGetsReleaseWhenNotPressed() {
        targets(new FakeTarget("a", false, true), new FakeTarget("b", false, true))
            .dispatch(7, 8, false, false);

        assertEquals(List.of("a.release"), calls);
    }

    @Test
    void noCapturedTargetDoesNothing() {
        targets(new FakeTarget("a", true, false)).dispatch(1, 1, false, true);

        assertEquals(List.of(), calls);
    }
}
