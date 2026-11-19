package com.github.halfdeadgames.asciigdx.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The pattern tokens and exception words found in a TeX hyphenation source text. */
final class TexPatternSource {

    private static final String PATTERNS_COMMAND = "\\patterns";
    private static final String HYPHENATION_COMMAND = "\\hyphenation";

    private final List<String> patterns;
    private final List<String> exceptionWords;

    private TexPatternSource(List<String> patterns, List<String> exceptionWords) {
        this.patterns = Collections.unmodifiableList(patterns);
        this.exceptionWords = Collections.unmodifiableList(exceptionWords);
    }

    /**
     * Splits a TeX source into its pattern tokens and exception words.
     *
     * @throws IllegalArgumentException if the patterns block is missing or a block is unclosed
     */
    static TexPatternSource parse(CharSequence texSource) {
        var withoutComments = stripComments(texSource);
        var patternBlock = findBlock(withoutComments, PATTERNS_COMMAND);
        if (patternBlock == null) {
            throw new IllegalArgumentException("TeX source has no \\patterns{...} block");
        }
        var exceptionBlock = findBlock(withoutComments, HYPHENATION_COMMAND);
        var exceptions = exceptionBlock == null ? Collections.<String>emptyList() : splitTokens(exceptionBlock);
        return new TexPatternSource(splitTokens(patternBlock), exceptions);
    }

    List<String> getPatterns() {
        return patterns;
    }

    List<String> getExceptionWords() {
        return exceptionWords;
    }

    private static String stripComments(CharSequence source) {
        var result = new StringBuilder(source.length());
        var inComment = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '\n' || current == '\r') {
                inComment = false;
            } else if (current == '%') {
                inComment = true;
            }
            if (!inComment) {
                result.append(current);
            }
        }
        return result.toString();
    }

    /** Returns the text between the braces after the command, or null if the command is absent. */
    private static String findBlock(String source, String command) {
        int commandIndex = source.indexOf(command);
        if (commandIndex < 0) {
            return null;
        }
        int openIndex = commandIndex + command.length();
        while (openIndex < source.length() && Character.isWhitespace(source.charAt(openIndex))) {
            openIndex++;
        }
        if (openIndex >= source.length() || source.charAt(openIndex) != '{') {
            throw new IllegalArgumentException(command + " is not followed by '{'");
        }
        int closeIndex = source.indexOf('}', openIndex);
        if (closeIndex < 0) {
            throw new IllegalArgumentException("Unclosed brace after " + command);
        }
        return source.substring(openIndex + 1, closeIndex);
    }

    private static List<String> splitTokens(String block) {
        var tokens = new ArrayList<String>();
        for (var token : block.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
