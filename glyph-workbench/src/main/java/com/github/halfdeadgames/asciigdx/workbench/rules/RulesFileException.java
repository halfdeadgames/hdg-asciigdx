package com.github.halfdeadgames.asciigdx.workbench.rules;

/** A rules file that cannot be read; the message is written for the designer. */
final class RulesFileException extends RuntimeException {

    public RulesFileException(String message) {
        super(message);
    }
}
