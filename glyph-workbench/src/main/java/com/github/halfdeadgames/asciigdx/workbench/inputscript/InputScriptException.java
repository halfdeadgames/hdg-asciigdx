package com.github.halfdeadgames.asciigdx.workbench.inputscript;

public class InputScriptException extends RuntimeException {

  private final int lineNumber;

  public InputScriptException(int lineNumber, String line, String problem) {
    super("Line " + lineNumber + ": " + problem + " in \"" + line + "\"");
    this.lineNumber = lineNumber;
  }

  public int lineNumber() {
    return lineNumber;
  }
}
