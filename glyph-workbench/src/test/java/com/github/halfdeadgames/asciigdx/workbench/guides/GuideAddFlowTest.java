package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuideAddFlowTest {

    private static final String FILE = """
        row CAP 1 all
        column C 7 uppercase,digits
        bar MID 6 1 lowercase
        """;

    private final GuideListModel model = new GuideListModel(PaintGuides.parse(FILE), 8, 8);
    private final GuideAddFlow flow = new GuideAddFlow(model);

    private void type(String text) {
        for (char character : text.toCharArray()) {
            model.typeChar(character);
        }
    }

    @Test
    void startOpensThePicker() {
        flow.start();
        assertEquals(GuideAddFlow.State.PICKING, flow.state());
    }

    @Test
    void eachEnabledKindAddsItsGuideType() {
        assertAdds(GuideAddFlow.Kind.SOLID_ACROSS, PaintGuides.ListedKind.ROW);
        assertAdds(GuideAddFlow.Kind.SOLID_DOWN, PaintGuides.ListedKind.COLUMN);
        assertAdds(GuideAddFlow.Kind.DOTTED_ACROSS, PaintGuides.ListedKind.BAR);
        assertAdds(GuideAddFlow.Kind.DOTTED_DOWN, PaintGuides.ListedKind.COLUMN_BAR);
    }

    private void assertAdds(GuideAddFlow.Kind kind, PaintGuides.ListedKind expected) {
        GuideListModel fresh = new GuideListModel(PaintGuides.parse(FILE), 8, 8);
        GuideAddFlow freshFlow = new GuideAddFlow(fresh);
        freshFlow.start();
        freshFlow.pick(kind);
        assertEquals(GuideAddFlow.State.NAMING, freshFlow.state());
        assertEquals(4, fresh.size());
        assertEquals(expected, fresh.selectedGuide().kind());
    }

    @Test
    void dottedDownAddsAColumnBarNamedLikeTheOthers() {
        flow.start();
        flow.pick(GuideAddFlow.Kind.DOTTED_DOWN);
        type("band");
        flow.accept();
        assertEquals(GuideAddFlow.State.CLOSED, flow.state());
        assertEquals(4, model.size());
        assertEquals(PaintGuides.ListedKind.COLUMN_BAR, model.selectedGuide().kind());
        assertEquals("BAND", model.selectedGuide().label());
    }

    @Test
    void escapeWhileNamingAColumnBarRemovesIt() {
        flow.start();
        flow.pick(GuideAddFlow.Kind.DOTTED_DOWN);
        assertEquals(4, model.size());
        flow.cancel();
        assertEquals(GuideAddFlow.State.CLOSED, flow.state());
        assertEquals(3, model.size());
        assertFalse(model.isChanged());
    }

    @Test
    void escapeInThePickerAddsNothing() {
        flow.start();
        flow.cancel();
        assertEquals(GuideAddFlow.State.CLOSED, flow.state());
        assertEquals(3, model.size());
        assertFalse(model.isChanged());
    }

    @Test
    void escapeWhileNamingDropsTheGuideAndRestoresTheSelection() {
        model.select(1);
        flow.start();
        flow.pick(GuideAddFlow.Kind.SOLID_ACROSS);
        type("NEW");
        flow.cancel();
        assertEquals(GuideAddFlow.State.CLOSED, flow.state());
        assertFalse(model.isRenaming());
        assertEquals(3, model.size());
        assertEquals(1, model.selectedIndex());
        assertFalse(model.isChanged());
    }

    @Test
    void enterWithAnEmptyNameIsRefused() {
        flow.start();
        flow.pick(GuideAddFlow.Kind.SOLID_DOWN);
        assertFalse(flow.canAccept());
        flow.accept();
        assertEquals(GuideAddFlow.State.NAMING, flow.state());
        assertTrue(model.isRenaming());
    }

    @Test
    void acceptedGuideIsSelectedAndNamed() {
        flow.start();
        flow.pick(GuideAddFlow.Kind.DOTTED_ACROSS);
        type("dots");
        flow.accept();
        assertEquals(GuideAddFlow.State.CLOSED, flow.state());
        assertFalse(model.isRenaming());
        assertEquals(4, model.size());
        assertEquals(3, model.selectedIndex());
        assertEquals("DOTS", model.selectedGuide().label());
        assertEquals(PaintGuides.ListedKind.BAR, model.selectedGuide().kind());
        assertTrue(model.isChanged());
    }

    @Test
    void pickingOutsideThePickerDoesNothing() {
        flow.pick(GuideAddFlow.Kind.SOLID_ACROSS);
        assertEquals(3, model.size());
        assertEquals(GuideAddFlow.State.CLOSED, flow.state());
    }
}
