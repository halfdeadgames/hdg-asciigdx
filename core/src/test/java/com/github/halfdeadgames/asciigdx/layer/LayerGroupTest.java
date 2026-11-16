package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerGroupTest {

    private static final float TOLERANCE = 1e-5f;
    private static final float[][] SAMPLE_COLORS = {
        {0f, 0f, 0f}, {1f, 1f, 1f}, {1f, 0f, 0f}, {0.2f, 0.6f, 0.9f}, {0.75f, 0.1f, 0.4f}
    };

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 8;
        }

        @Override
        public TextureRegion getGlyphRegion(int codePoint) {
            return null;
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return false;
        }

        @Override
        public void dispose() {
        }
    }

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final FakeSource font = new FakeSource();
    private final ResolvedEffects resolved = new ResolvedEffects();

    private Layer addLayer() {
        return stack.add(new TerminalGrid(2, 2), font);
    }

    private static ColorEffects saturation(float value) {
        ColorEffects effects = new ColorEffects();
        effects.setSaturation(value);
        return effects;
    }

    private static ColorEffects tintAndMix() {
        ColorEffects effects = new ColorEffects();
        effects.setTint(0.5f, 0.8f, 1.2f, 0.6f);
        effects.setMix(0.2f, 0.9f, 0.1f, 0.35f);
        return effects;
    }

    private static ColorEffects invert(float value) {
        ColorEffects effects = new ColorEffects();
        effects.setInvert(value);
        return effects;
    }

    private static void copyEffects(ColorEffects from, LayerGroup to) {
        to.getColorEffects().copyFrom(from);
    }

    private static void assertMatrixEquals(ColorMatrix expected, ColorMatrix actual) {
        float[] expectedColor = new float[3];
        float[] actualColor = new float[3];
        for (float[] color : SAMPLE_COLORS) {
            expected.apply(color[0], color[1], color[2], expectedColor);
            actual.apply(color[0], color[1], color[2], actualColor);
            assertArrayEquals(expectedColor, actualColor, TOLERANCE);
        }
    }

    private static ColorMatrix matrixOf(ColorEffects effects) {
        ColorMatrix matrix = new ColorMatrix();
        effects.writeColorMatrix(matrix);
        return matrix;
    }

    @Test
    void layerWithoutGroupsResolvesToItsOwnEffects() {
        ColorEffects own = tintAndMix();

        resolved.resolve(own, new Array<>());

        assertArrayEquals(matrixOf(own).getValues(), resolved.getMatrix().getValues(), 0f);
        assertEquals(0.6f, resolved.getAlpha(), 0f);
    }

    @Test
    void oneGroupRunsAfterTheLayersOwnEffects() {
        ColorEffects own = saturation(0.5f);
        ColorEffects groupEffects = tintAndMix();
        LayerGroup group = stack.addGroup();
        copyEffects(groupEffects, group);

        resolved.resolve(own, groupsOf(group));

        assertAppliesInSequence(resolved.getMatrix(), own, groupEffects);
    }

    @Test
    void twoGroupsRunInJoinOrder() {
        ColorEffects own = saturation(0.5f);
        ColorEffects firstEffects = invert(0.8f);
        ColorEffects secondEffects = tintAndMix();
        LayerGroup first = stack.addGroup();
        LayerGroup second = stack.addGroup();
        copyEffects(firstEffects, first);
        copyEffects(secondEffects, second);

        resolved.resolve(own, groupsOf(first, second));

        assertAppliesInSequence(resolved.getMatrix(), own, firstEffects, secondEffects);
    }

    @Test
    void groupOrderMattersBecauseInvertAndTintDoNotCommute() {
        ColorEffects own = new ColorEffects();
        LayerGroup invertGroup = stack.addGroup();
        LayerGroup tintGroup = stack.addGroup();
        copyEffects(invert(1f), invertGroup);
        copyEffects(tintAndMix(), tintGroup);
        resolved.resolve(own, groupsOf(invertGroup, tintGroup));
        float[] forward = new float[3];
        resolved.getMatrix().apply(0.2f, 0.6f, 0.9f, forward);

        resolved.resolve(own, groupsOf(tintGroup, invertGroup));
        float[] backward = new float[3];
        resolved.getMatrix().apply(0.2f, 0.6f, 0.9f, backward);

        assertFalse(Math.abs(forward[0] - backward[0]) < TOLERANCE);
    }

    @Test
    void alphasMultiply() {
        ColorEffects own = new ColorEffects();
        own.setAlpha(0.5f);
        LayerGroup first = stack.addGroup();
        LayerGroup second = stack.addGroup();
        first.getColorEffects().setAlpha(0.5f);
        second.getColorEffects().setAlpha(0.4f);

        resolved.resolve(own, groupsOf(first, second));

        assertEquals(0.1f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void resolvingAgainDoesNotKeepEarlierGroups() {
        LayerGroup group = stack.addGroup();
        group.getColorEffects().setAlpha(0.5f);
        resolved.resolve(new ColorEffects(), groupsOf(group));

        resolved.resolve(new ColorEffects(), new Array<>());

        assertEquals(1f, resolved.getAlpha(), 0f);
        assertMatrixEquals(new ColorMatrix(), resolved.getMatrix());
    }

    @Test
    void addingTwiceDoesNothing() {
        Layer layer = addLayer();
        LayerGroup group = stack.addGroup();

        group.add(layer);
        group.add(layer);

        assertEquals(1, group.size());
        assertEquals(1, layer.getGroups().size);
        assertTrue(group.contains(layer));
    }

    @Test
    void layerCanJoinSeveralGroupsInOrder() {
        Layer layer = addLayer();
        LayerGroup first = stack.addGroup();
        LayerGroup second = stack.addGroup();

        second.add(layer);
        first.add(layer);

        assertSame(second, layer.getGroups().get(0));
        assertSame(first, layer.getGroups().get(1));
    }

    @Test
    void removingTakesTheLayerOutOfBothSides() {
        Layer layer = addLayer();
        LayerGroup first = stack.addGroup();
        LayerGroup second = stack.addGroup();
        first.add(layer);
        second.add(layer);

        first.remove(layer);
        first.remove(layer);

        assertFalse(first.contains(layer));
        assertEquals(1, layer.getGroups().size);
        assertSame(second, layer.getGroups().get(0));
    }

    @Test
    void removingTheLayerFromTheStackLeavesItsGroups() {
        Layer layer = addLayer();
        LayerGroup group = stack.addGroup();
        group.add(layer);

        stack.remove(layer);

        assertFalse(group.contains(layer));
        assertEquals(0, group.size());
        assertEquals(0, layer.getGroups().size);
    }

    @Test
    void removingTheGroupEmptiesItAndFreesTheLayers() {
        Layer layer = addLayer();
        LayerGroup group = stack.addGroup();
        group.add(layer);

        stack.removeGroup(group);

        assertEquals(0, layer.getGroups().size);
        assertEquals(0, group.size());
        assertThrows(IllegalStateException.class, () -> group.add(layer));
        assertThrows(IllegalStateException.class, () -> stack.removeGroup(group));
    }

    @Test
    void colorEffectsIsTheSameInstanceEveryCall() {
        LayerGroup group = stack.addGroup();

        assertSame(group.getColorEffects(), group.getColorEffects());
        assertNotSame(group.getColorEffects(), stack.addGroup().getColorEffects());
    }

    @Test
    void colorEffectsCannotChangeAfterGroupRemovalButStayReadable() {
        LayerGroup group = stack.addGroup();
        ColorEffects effects = group.getColorEffects();
        effects.setAlpha(0.5f);

        stack.removeGroup(group);

        assertSame(effects, group.getColorEffects());
        assertThrows(IllegalStateException.class, () -> effects.setAlpha(0.25f));
        assertThrows(IllegalStateException.class, () -> effects.setTint(1f, 1f, 1f, 1f));
        assertThrows(IllegalStateException.class, () -> effects.setMix(1f, 1f, 1f, 0f));
        assertThrows(IllegalStateException.class, () -> effects.setSaturation(1f));
        assertThrows(IllegalStateException.class, () -> effects.setInvert(0f));
        assertEquals(0.5f, effects.getTintAlpha());
    }

    @Test
    void colorEffectsCannotChangeAfterStackDispose() {
        ColorEffects effects = stack.addGroup().getColorEffects();

        stack.dispose();

        assertThrows(IllegalStateException.class, () -> effects.setInvert(1f));
    }

    @Test
    void layerOfAnotherStackIsRejected() {
        LayerStack other = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
        Layer foreign = other.add(new TerminalGrid(2, 2), font);
        LayerGroup group = stack.addGroup();

        assertThrows(IllegalArgumentException.class, () -> group.add(foreign));
        assertThrows(IllegalArgumentException.class, () -> group.add(null));
        assertFalse(group.contains(foreign));
    }

    @Test
    void removedLayerCannotJoin() {
        Layer layer = addLayer();
        LayerGroup group = stack.addGroup();
        stack.remove(layer);

        assertThrows(IllegalStateException.class, () -> group.add(layer));
    }

    @Test
    void groupEffectsStartNeutralAndStoreValues() {
        ColorEffects effects = stack.addGroup().getColorEffects();
        assertEquals(1f, effects.getTintAlpha());
        assertEquals(1f, effects.getSaturation());
        assertEquals(0f, effects.getInvert());
        assertEquals(0f, effects.getMixAmount());

        effects.setTint(0.1f, 0.2f, 0.3f, 0.4f);
        effects.setMix(0.5f, 0.6f, 0.7f, 0.8f);
        effects.setSaturation(2f);
        effects.setInvert(0.25f);

        assertEquals(0.3f, effects.getTintBlue());
        assertEquals(0.4f, effects.getTintAlpha());
        assertEquals(0.7f, effects.getMixBlue());
        assertEquals(0.8f, effects.getMixAmount());
        assertEquals(2f, effects.getSaturation());
        assertEquals(0.25f, effects.getInvert());
    }

    @Test
    void groupEffectsDoNotChangeTheLayersOwnValues() {
        Layer layer = addLayer();
        LayerGroup group = stack.addGroup();
        group.add(layer);

        group.getColorEffects().setAlpha(0.3f);
        group.getColorEffects().setInvert(1f);

        assertEquals(1f, layer.getColorEffects().getTintAlpha());
        assertEquals(0f, layer.getColorEffects().getInvert());
    }

    @Test
    void parentEffectsApplyAfterTheChild() {
        ColorEffects own = saturation(0.5f);
        ColorEffects childEffects = invert(0.8f);
        ColorEffects parentEffects = tintAndMix();
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        copyEffects(childEffects, child);
        copyEffects(parentEffects, parent);
        child.setParent(parent);

        resolved.resolve(own, groupsOf(child));

        assertAppliesInSequence(resolved.getMatrix(), own, childEffects, parentEffects);
        assertSame(parent, child.getParent());
    }

    @Test
    void grandparentChainAppliesInOrder() {
        ColorEffects childEffects = invert(0.8f);
        ColorEffects parentEffects = tintAndMix();
        ColorEffects grandparentEffects = saturation(0.3f);
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        LayerGroup grandparent = stack.addGroup();
        copyEffects(childEffects, child);
        copyEffects(parentEffects, parent);
        copyEffects(grandparentEffects, grandparent);
        child.setParent(parent);
        parent.setParent(grandparent);

        resolved.resolve(new ColorEffects(), groupsOf(child));

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), childEffects, parentEffects,
                grandparentEffects);
    }

    @Test
    void sharedParentOfTwoGroupsAppliesOnce() {
        ColorEffects sharedEffects = invert(1f);
        LayerGroup first = stack.addGroup();
        LayerGroup second = stack.addGroup();
        LayerGroup shared = stack.addGroup();
        copyEffects(sharedEffects, shared);
        shared.getColorEffects().setAlpha(0.5f);
        first.setParent(shared);
        second.setParent(shared);

        resolved.resolve(new ColorEffects(), groupsOf(first, second));

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), sharedEffects);
        assertEquals(0.5f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void groupJoinedDirectlyAndAsAncestorAppliesOnce() {
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        parent.getColorEffects().setAlpha(0.5f);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(child, parent));
        assertEquals(0.5f, resolved.getAlpha(), TOLERANCE);

        resolved.resolve(new ColorEffects(), groupsOf(parent, child));
        assertEquals(0.5f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void parentJoinedFirstStillRunsAfterItsChild() {
        ColorEffects childEffects = invert(0.8f);
        ColorEffects parentEffects = tintAndMix();
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        copyEffects(childEffects, child);
        copyEffects(parentEffects, parent);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(parent, child));

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), childEffects, parentEffects);
    }

    @Test
    void childJoinedFirstRunsBeforeItsParent() {
        ColorEffects childEffects = invert(0.8f);
        ColorEffects parentEffects = tintAndMix();
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        copyEffects(childEffects, child);
        copyEffects(parentEffects, parent);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(child, parent));

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), childEffects, parentEffects);
    }

    @Test
    void chainJoinedOutOfOrderRunsDescendantsFirst() {
        ColorEffects grandchildEffects = invert(0.8f);
        ColorEffects childEffects = tintAndMix();
        ColorEffects parentEffects = saturation(0.3f);
        LayerGroup parent = stack.addGroup();
        LayerGroup child = stack.addGroup();
        LayerGroup grandchild = stack.addGroup();
        copyEffects(parentEffects, parent);
        copyEffects(childEffects, child);
        copyEffects(grandchildEffects, grandchild);
        child.setParent(parent);
        grandchild.setParent(child);

        resolved.resolve(new ColorEffects(), groupsOf(parent, grandchild, child));

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), grandchildEffects, childEffects,
                parentEffects);
    }

    @Test
    void diamondRunsSiblingsInJoinOrderThenSharedParentOnce() {
        ColorEffects firstEffects = invert(0.8f);
        ColorEffects secondEffects = tintAndMix();
        ColorEffects sharedEffects = saturation(0.3f);
        LayerGroup shared = stack.addGroup();
        LayerGroup first = stack.addGroup();
        LayerGroup second = stack.addGroup();
        copyEffects(sharedEffects, shared);
        shared.getColorEffects().setAlpha(0.5f);
        copyEffects(firstEffects, first);
        copyEffects(secondEffects, second);
        first.setParent(shared);
        second.setParent(shared);

        resolved.resolve(new ColorEffects(), groupsOf(first, second));

        assertEquals(0.5f * 0.6f, resolved.getAlpha(), TOLERANCE);
        resolved.resolve(new ColorEffects(), groupsOf(second, first));
        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), secondEffects, firstEffects,
                sharedEffects);
    }

    @Test
    void unrelatedGroupsKeepJoinOrderNextToATree() {
        ColorEffects loneEffects = saturation(0.3f);
        ColorEffects childEffects = invert(0.8f);
        ColorEffects parentEffects = tintAndMix();
        LayerGroup lone = stack.addGroup();
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        copyEffects(loneEffects, lone);
        copyEffects(childEffects, child);
        copyEffects(parentEffects, parent);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(lone, child));

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), loneEffects, childEffects,
                parentEffects);
    }

    @Test
    void alphaProductIsTheSameForAnyJoinOrder() {
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        child.getColorEffects().setAlpha(0.5f);
        parent.getColorEffects().setAlpha(0.4f);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(parent, child));

        assertEquals(0.2f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void repeatedResolvesGiveTheSameMatrix() {
        ColorEffects childEffects = invert(0.8f);
        ColorEffects parentEffects = tintAndMix();
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        copyEffects(childEffects, child);
        copyEffects(parentEffects, parent);
        child.setParent(parent);
        Array<LayerGroup> groups = groupsOf(parent, child);

        resolved.resolve(new ColorEffects(), groups);
        resolved.resolve(new ColorEffects(), groups);
        resolved.resolve(new ColorEffects(), groups);

        assertAppliesInSequence(resolved.getMatrix(), new ColorEffects(), childEffects, parentEffects);
    }

    @Test
    void resolvingAgainAppliesTheSameGroupsAgain() {
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        parent.getColorEffects().setAlpha(0.5f);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(child));
        resolved.resolve(new ColorEffects(), groupsOf(child));

        assertEquals(0.5f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void alphaMultipliesThroughTheChain() {
        ColorEffects own = new ColorEffects();
        own.setAlpha(0.5f);
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        LayerGroup grandparent = stack.addGroup();
        child.getColorEffects().setAlpha(0.5f);
        parent.getColorEffects().setAlpha(0.4f);
        grandparent.getColorEffects().setAlpha(0.5f);
        child.setParent(parent);
        parent.setParent(grandparent);

        resolved.resolve(own, groupsOf(child));

        assertEquals(0.05f, resolved.getAlpha(), TOLERANCE);
    }

    @Test
    void ancestorAlphaZeroZeroesTheLayer() {
        LayerGroup child = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        parent.getColorEffects().setAlpha(0f);
        child.setParent(parent);

        resolved.resolve(new ColorEffects(), groupsOf(child));

        assertEquals(0f, resolved.getAlpha(), 0f);
    }

    @Test
    void cycleIsRejectedAndLeavesStateUnchanged() {
        LayerGroup top = stack.addGroup();
        LayerGroup child = stack.addGroup();
        LayerGroup grandchild = stack.addGroup();
        child.setParent(top);
        grandchild.setParent(child);

        assertThrows(IllegalArgumentException.class, () -> top.setParent(top));
        assertThrows(IllegalArgumentException.class, () -> top.setParent(child));
        assertThrows(IllegalArgumentException.class, () -> top.setParent(grandchild));
        assertThrows(IllegalArgumentException.class, () -> child.setParent(grandchild));

        assertNull(top.getParent());
        assertSame(top, child.getParent());
        assertSame(child, grandchild.getParent());
    }

    @Test
    void parentFromAnotherStackIsRejected() {
        LayerStack other = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
        LayerGroup foreign = other.addGroup();
        LayerGroup group = stack.addGroup();

        assertThrows(IllegalArgumentException.class, () -> group.setParent(foreign));
        assertNull(group.getParent());
    }

    @Test
    void removedGroupCannotBeAParent() {
        LayerGroup removed = stack.addGroup();
        LayerGroup group = stack.addGroup();
        stack.removeGroup(removed);

        assertThrows(IllegalStateException.class, () -> group.setParent(removed));
        assertNull(group.getParent());
    }

    @Test
    void removingTheParentLeavesChildrenParentless() {
        LayerGroup grandparent = stack.addGroup();
        LayerGroup parent = stack.addGroup();
        LayerGroup child = stack.addGroup();
        parent.setParent(grandparent);
        child.setParent(parent);

        stack.removeGroup(parent);

        assertNull(child.getParent());
        assertNull(parent.getParent());
        assertNull(grandparent.getParent());
    }

    @Test
    void settingNullClearsTheParent() {
        LayerGroup parent = stack.addGroup();
        LayerGroup child = stack.addGroup();
        child.setParent(parent);

        child.setParent(null);

        assertNull(child.getParent());
    }

    private static Array<LayerGroup> groupsOf(LayerGroup... groups) {
        Array<LayerGroup> array = new Array<>();
        for (LayerGroup group : groups) {
            array.add(group);
        }
        return array;
    }

    /** Runs each sample color through every step one after another, and compares with the resolved matrix. */
    private static void assertAppliesInSequence(ColorMatrix actual, ColorEffects... steps) {
        float[] expected = new float[3];
        float[] resolvedColor = new float[3];
        for (float[] color : SAMPLE_COLORS) {
            float[] current = color.clone();
            for (ColorEffects step : steps) {
                matrixOf(step).apply(current[0], current[1], current[2], expected);
                current = expected.clone();
            }
            actual.apply(color[0], color[1], color[2], resolvedColor);
            assertArrayEquals(current, resolvedColor, TOLERANCE);
        }
    }
}
