package com.github.halfdeadgames.asciigdx.workbench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Builds sample words in which one letter is replaced by the target letter. */
public final class SampleWords {

    /** A word to draw and the index of the target letter inside it. */
    public record Sample(String text, int targetIndex) {
    }

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 9;

    private static final String[] WORDS = {
        "lorem", "ipsum", "dolor", "amet", "consectetur", "adipiscing", "elit", "eiusmod", "tempor",
        "incididunt", "labore", "dolore", "magna", "aliqua", "enim", "minim", "veniam", "quis",
        "nostrud", "exercitation", "ullamco", "laboris", "nisi", "aliquip", "commodo", "consequat",
        "duis", "aute", "irure", "voluptate", "velit", "esse", "cillum", "fugiat", "nulla",
        "pariatur", "sint", "occaecat", "cupidatat", "proident", "sunt", "culpa", "officia",
        "deserunt", "mollit", "anim", "laborum"
    };

    private SampleWords() {
    }

    /**
     * Returns up to {@code count} samples for a letter, or none for other characters. The result
     * only depends on the code point. The target replaces one letter, never the first when the
     * word has more than one. An uppercase target gives capitals words alternating with
     * lowercase words that hold the single capital.
     */
    public static Sample[] pick(int codePoint, int count) {
        if (!Character.isLetter(codePoint)) {
            return new Sample[0];
        }
        Random random = new Random(codePoint);
        List<String> pool = new ArrayList<>();
        for (String word : WORDS) {
            if (word.length() >= MIN_LENGTH && word.length() <= MAX_LENGTH) {
                pool.add(word);
            }
        }
        Collections.shuffle(pool, random);
        boolean upper = Character.isUpperCase(codePoint);
        Sample[] result = new Sample[Math.min(count, pool.size())];
        for (int i = 0; i < result.length; i++) {
            String base = upper && i % 2 == 0 ? pool.get(i).toUpperCase(Locale.ROOT) : pool.get(i);
            int index = 1 + random.nextInt(base.length() - 1);
            StringBuilder text = new StringBuilder(base);
            text.replace(index, index + 1, new String(Character.toChars(codePoint)));
            result[i] = new Sample(text.toString(), index);
        }
        return result;
    }
}
