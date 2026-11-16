package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class TweenSequenceTest {

    private static final float TOLERANCE = 1e-6f;

    private final TweenSequence chain = TweenSequence.startingAt(1f, 10f)
            .to(20f, 2f)
            .to(0f, 4f)
            .build();

    private final TweenSequence envelope = TweenSequence.startingAt(0f, 0f)
            .to(1f, 0.1f)
            .hold(0.5f)
            .to(0f, 0.2f)
            .build();

    @Test
    void valueBeforeStartIsStartValue() {
        assertEquals(10f, chain.getValueAt(-3f), TOLERANCE);
    }

    @Test
    void valueAtPieceBoundariesIsTheChainedValue() {
        assertEquals(10f, chain.getValueAt(1f), TOLERANCE);
        assertEquals(20f, chain.getValueAt(3f), TOLERANCE);
        assertEquals(0f, chain.getValueAt(7f), TOLERANCE);
    }

    @Test
    void valueAtPieceMidpointsIsHalfway() {
        assertEquals(15f, chain.getValueAt(2f), TOLERANCE);
        assertEquals(10f, chain.getValueAt(5f), TOLERANCE);
    }

    @Test
    void holdKeepsTheValue() {
        assertEquals(1f, envelope.getValueAt(0.1f), TOLERANCE);
        assertEquals(1f, envelope.getValueAt(0.4f), TOLERANCE);
        assertEquals(1f, envelope.getValueAt(0.599f), 1e-3f);
    }

    @Test
    void envelopeRisesHoldsAndFalls() {
        assertEquals(0f, envelope.getValueAt(0f), TOLERANCE);
        assertEquals(0.5f, envelope.getValueAt(0.05f), TOLERANCE);
        assertEquals(1f, envelope.getValueAt(0.3f), TOLERANCE);
        assertEquals(0.5f, envelope.getValueAt(0.7f), TOLERANCE);
        assertEquals(0f, envelope.getValueAt(0.8f), TOLERANCE);
    }

    @Test
    void zeroDurationPieceJumps() {
        TweenSequence jump = TweenSequence.startingAt(0f, 0f)
                .hold(1f)
                .to(5f, 0f)
                .hold(1f)
                .build();
        assertEquals(0f, jump.getValueAt(0.999f), TOLERANCE);
        assertEquals(5f, jump.getValueAt(1f), TOLERANCE);
        assertEquals(5f, jump.getValueAt(1.5f), TOLERANCE);
    }

    @Test
    void valueAfterEndIsLastValue() {
        assertEquals(0f, chain.getValueAt(100f), TOLERANCE);
    }

    @Test
    void startEndAndPieceCountDescribeTheSequence() {
        assertEquals(1f, chain.getStartSeconds(), TOLERANCE);
        assertEquals(7f, chain.getEndSeconds(), TOLERANCE);
        assertEquals(2, chain.getPieceCount());
    }

    @Test
    void seekingOutOfOrderGivesTheSameValues() {
        float first = chain.getValueAt(5f);
        chain.getValueAt(0f);
        chain.getValueAt(2f);
        chain.getValueAt(50f);
        assertEquals(first, chain.getValueAt(5f), 0f);
    }

    @Test
    void buildWithNoPiecesThrows() {
        assertThrows(IllegalStateException.class, () -> TweenSequence.startingAt(0f, 0f).build());
    }

    @Test
    void negativeDurationThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TweenSequence.startingAt(0f, 0f).to(1f, -1f));
    }

    @Test
    void nonFiniteStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> TweenSequence.startingAt(Float.NaN, 0f));
    }

    @Test
    void reusingTheBuilderDoesNotChangeTheBuiltSequence() {
        TweenSequence.Builder builder = TweenSequence.startingAt(0f, 0f).to(1f, 1f);
        TweenSequence built = builder.build();
        builder.to(9f, 1f);
        assertEquals(1, built.getPieceCount());
        assertEquals(1f, built.getValueAt(5f), TOLERANCE);
    }

    @Test
    void easingIsAppliedPerPiece() {
        TweenSequence eased = TweenSequence.startingAt(0f, 0f)
                .to(100f, 2f, Interpolation.pow2In)
                .to(0f, 2f)
                .build();
        assertEquals(25f, eased.getValueAt(1f), TOLERANCE);
        assertEquals(50f, eased.getValueAt(3f), TOLERANCE);
    }
}
