package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.SampleWords.Sample;
import org.junit.jupiter.api.Test;

class SampleWordsTest {

    @Test
    void targetSitsAtTheReportedIndexAndNeverFirst() {
        for (char letter = 'a'; letter <= 'z'; letter++) {
            assertTargetPlaced(letter);
            assertTargetPlaced(Character.toUpperCase(letter));
        }
        assertTargetPlaced('ç');
        assertTargetPlaced('Ç');
    }

    private static void assertTargetPlaced(char letter) {
        Sample[] samples = SampleWords.pick(letter, 4);
        assertEquals(4, samples.length);
        for (Sample sample : samples) {
            assertEquals(letter, sample.text().charAt(sample.targetIndex()), sample.text());
            assertNotEquals(0, sample.targetIndex(), sample.text());
        }
    }

    @Test
    void positionsVaryAcrossTheSet() {
        for (char letter = 'a'; letter <= 'z'; letter++) {
            Sample[] samples = SampleWords.pick(letter, 4);
            boolean varies = false;
            for (Sample sample : samples) {
                varies |= sample.targetIndex() != samples[0].targetIndex();
            }
            if (varies) {
                return;
            }
        }
        throw new AssertionError("every letter put the target at the same index in all words");
    }

    @Test
    void sameLetterAlwaysGivesTheSameWords() {
        assertArrayEquals(SampleWords.pick('k', 4), SampleWords.pick('k', 4));
    }

    @Test
    void lowercaseTargetGivesLowercaseWords() {
        for (Sample sample : SampleWords.pick('k', 4)) {
            assertEquals(sample.text().toLowerCase(), sample.text());
        }
    }

    @Test
    void uppercaseTargetGivesHalfCapitalsAndHalfSingleCapital() {
        Sample[] samples = SampleWords.pick('K', 4);
        for (int i = 0; i < samples.length; i++) {
            String text = samples[i].text();
            if (i % 2 == 0) {
                assertEquals(text.toUpperCase(), text);
            } else {
                int capitals = 0;
                for (char c : text.toCharArray()) {
                    capitals += Character.isUpperCase(c) ? 1 : 0;
                }
                assertEquals(1, capitals, text);
            }
        }
    }

    @Test
    void nonLettersGetNoWords() {
        assertEquals(0, SampleWords.pick('1', 4).length);
        assertEquals(0, SampleWords.pick('#', 4).length);
    }

    @Test
    void returnsAtMostRequestedCount() {
        assertEquals(2, SampleWords.pick('e', 2).length);
    }
}
