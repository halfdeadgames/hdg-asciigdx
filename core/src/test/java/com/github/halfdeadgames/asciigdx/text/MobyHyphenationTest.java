package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Compares the bundled patterns with the committed full Moby Hyphenation List. Moby marks
 * dictionary syllables, not typesetting breaks, so the test measures how often the patterns break
 * where Moby does not ("bad breaks") and ratchets that rate. Never touches the network.
 */
class MobyHyphenationTest {

    /**
     * Maximum bad break rate on the full committed list. Measured 0.05333 (13,359 of 250,496
     * breaks) on 2026-11-22; lower it when the patterns improve.
     */
    private static final double MAX_BAD_BREAK_RATE = 0.0534;

    private static final String MOBY_RESOURCE = "mhyph.txt";
    private static final String EXPECTED_SHA_256 =
            "5cf3ce87be48ae0c2f4f0a574998d6e75086591b7f0ca6367f71746eb6e25aa3";
    private static final char MAC_ROMAN_BULLET = '•';
    private static final String REPORT_DIRECTORY = "build/reports/moby-hyphenation";

    @Test
    void committedListMatchesExpectedChecksum() throws IOException, NoSuchAlgorithmException {
        var bytes = readMobyBytes();

        var actualSha256 = toLowerHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertEquals(EXPECTED_SHA_256, actualSha256,
                "The Moby list must be committed byte for byte; check line endings and encoding");
    }

    @Test
    void fullListBadBreakRateStaysWithinRatchet() throws IOException {
        checkDataSet("full", readFullList(openMobyStream()), MAX_BAD_BREAK_RATE);
    }

    private static InputStream openMobyStream() throws IOException {
        var stream = MobyHyphenationTest.class.getResourceAsStream(MOBY_RESOURCE);
        if (stream == null) {
            throw new IOException("Moby list resource " + MOBY_RESOURCE + " is missing");
        }
        return stream;
    }

    private static byte[] readMobyBytes() throws IOException {
        try (InputStream stream = openMobyStream()) {
            return stream.readAllBytes();
        }
    }

    private static String toLowerHex(byte[] bytes) {
        var hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(Character.forDigit((value >> 4) & 0xF, 16));
            hex.append(Character.forDigit(value & 0xF, 16));
        }
        return hex.toString();
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

    /** Reads the Mac Roman list, keeping only entries made of plain letters. */
    private static List<String> readFullList(InputStream stream) throws IOException {
        var words = new ArrayList<String>();
        try (var reader = new BufferedReader(new InputStreamReader(
                stream, Charset.forName("x-MacRoman")))) {
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
