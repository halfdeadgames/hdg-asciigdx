package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Compares the bundled patterns with the Moby Hyphenation List. Moby marks dictionary syllables,
 * not typesetting breaks, so the test measures how often the patterns break where Moby does not
 * ("bad breaks") and ratchets that rate. Never touches the network.
 */
class MobyHyphenationTest {

    /** Maximum bad break rate on the committed sample; NaN means measure and report only. */
    private static final double MAX_SAMPLE_BAD_BREAK_RATE = Double.NaN;

    /** Maximum bad break rate on the full cached list; NaN means measure and report only. */
    private static final double MAX_FULL_BAD_BREAK_RATE = Double.NaN;

    private static final String SAMPLE_RESOURCE = "moby-hyphenation-sample.txt";
    private static final String FULL_FILE_PROPERTY = "mobyHyphenationFile";
    private static final char MAC_ROMAN_BULLET = '•';
    private static final String REPORT_DIRECTORY = "build/reports/moby-hyphenation";

    @Test
    void sampleBadBreakRateStaysWithinRatchet() throws IOException {
        var words = readSample();
        assumeTrue(words != null, "Moby sample not present");

        checkDataSet("sample", words, MAX_SAMPLE_BAD_BREAK_RATE);
    }

    @Test
    void fullListBadBreakRateStaysWithinRatchet() throws IOException {
        var path = System.getProperty(FULL_FILE_PROPERTY);
        assumeTrue(path != null && new File(path).isFile(), "Full Moby list not cached");

        checkDataSet("full", readFullList(new File(path)), MAX_FULL_BAD_BREAK_RATE);
    }

    private static void checkDataSet(String name, List<String> mobyWords, double maxRate)
            throws IOException {
        var hyphenator = Hyphenator.englishUs();
        var offsets = new int[64];
        var offendingWords = new ArrayList<String>();
        long totalBreaks = 0;
        long badBreaks = 0;
        for (var mobyWord : mobyWords) {
            var plainWord = mobyWord.replace("-", "");
            int count = hyphenator.findBreaks(plainWord, 0, plainWord.length(), offsets);
            var found = new StringBuilder(plainWord);
            int badInWord = 0;
            for (int i = Math.min(count, offsets.length) - 1; i >= 0; i--) {
                if (!isMobyBreak(mobyWord, offsets[i])) {
                    badInWord++;
                }
                found.insert(offsets[i], '-');
            }
            totalBreaks += count;
            badBreaks += badInWord;
            if (badInWord > 0) {
                offendingWords.add(found + " (Moby: " + mobyWord + ")");
            }
        }
        double rate = totalBreaks == 0 ? 0.0 : (double) badBreaks / totalBreaks;
        writeReport(name, mobyWords.size(), totalBreaks, badBreaks, rate, offendingWords);
        System.out.printf("Moby %s: bad break rate %.4f (%d of %d breaks)%n",
                name, rate, badBreaks, totalBreaks);
        if (!Double.isNaN(maxRate)) {
            assertTrue(rate <= maxRate, "Bad break rate " + rate + " exceeds " + maxRate);
        }
    }

    /** Returns whether Moby splits the plain word before the letter at the given index. */
    private static boolean isMobyBreak(String mobyWord, int plainIndex) {
        int letters = 0;
        for (int i = 0; i < mobyWord.length(); i++) {
            if (mobyWord.charAt(i) == '-') {
                if (letters == plainIndex) {
                    return true;
                }
            } else {
                letters++;
            }
        }
        return false;
    }

    private static void writeReport(String name, int wordCount, long totalBreaks, long badBreaks,
            double rate, List<String> offendingWords) throws IOException {
        var directory = new File(REPORT_DIRECTORY);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create " + directory.getAbsolutePath());
        }
        try (var writer = new PrintWriter(new File(directory, name + ".txt"), "UTF-8")) {
            writer.println("Words: " + wordCount);
            writer.println("Breaks returned: " + totalBreaks);
            writer.println("Bad breaks: " + badBreaks);
            writer.println("Bad break rate: " + rate);
            writer.println("Words with bad breaks: " + offendingWords.size());
            writer.println();
            offendingWords.forEach(writer::println);
        }
    }

    private static List<String> readSample() throws IOException {
        try (InputStream stream = MobyHyphenationTest.class.getResourceAsStream(SAMPLE_RESOURCE)) {
            if (stream == null) {
                return null;
            }
            var words = new ArrayList<String>();
            var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && line.charAt(0) != '#') {
                    words.add(line.trim());
                }
            }
            return words;
        }
    }

    /** Reads the Mac Roman list with the same filtering as the sample build task. */
    private static List<String> readFullList(File file) throws IOException {
        var words = new ArrayList<String>();
        try (var reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), Charset.forName("x-MacRoman")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var word = toHyphenatedWord(line);
                if (word != null) {
                    words.add(word);
                }
            }
        }
        return words;
    }

    private static String toHyphenatedWord(String mobyLine) {
        var plain = mobyLine.replace(String.valueOf(MAC_ROMAN_BULLET), "");
        if (!plain.matches("[A-Za-z]+")) {
            return null;
        }
        return mobyLine.replace(MAC_ROMAN_BULLET, '-');
    }
}
