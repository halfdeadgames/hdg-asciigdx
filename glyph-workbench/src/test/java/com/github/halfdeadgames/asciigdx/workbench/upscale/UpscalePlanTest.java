package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpscalePlanTest {

    @Test
    void targetIsTheLargestSizePlusEight() {
        UpscalePlan.Choice choice = UpscalePlan.choiceFor("f", List.of(8));
        assertEquals(16, choice.target());
        assertEquals(List.of(8), choice.sources());
        assertEquals(2, choice.factorFrom(8));
    }

    @Test
    void twentyFourComesFromEightOnly() {
        UpscalePlan.Choice choice = UpscalePlan.choiceFor("f", List.of(8, 16));
        assertEquals(24, choice.target());
        assertEquals(List.of(8), choice.sources());
        assertEquals(3, choice.factorFrom(8));
    }

    @Test
    void thirtyTwoComesFromSixteenOrEightAndSixteenIsPrimary() {
        UpscalePlan.Choice choice = UpscalePlan.choiceFor("f", List.of(8, 16, 24));
        assertEquals(32, choice.target());
        assertEquals(List.of(8, 16), choice.sources());
        assertEquals(16, choice.primarySource());
        assertEquals(4, choice.factorFrom(8));
        assertEquals(2, choice.factorFrom(16));
    }

    @Test
    void noEvenSourceIsReported() {
        UpscalePlan.Choice choice = UpscalePlan.choiceFor("f", List.of(16));
        assertEquals(24, choice.target());
        assertTrue(choice.sources().isEmpty());
        assertFalse(choice.hasSource());
    }

    @Test
    void planGroupsSizesPerFolder() {
        List<UpscalePlan.Choice> plan = UpscalePlan.plan(List.of(
            new FontChoice("alpha", 8), new FontChoice("saiksistt", 8), new FontChoice("saiksistt", 16)));
        assertEquals(2, plan.size());
        assertEquals("ALPHA 8 -> 16", plan.get(0).label());
        assertEquals("SAIKSISTT 16 -> 24", plan.get(1).label());
    }
}
