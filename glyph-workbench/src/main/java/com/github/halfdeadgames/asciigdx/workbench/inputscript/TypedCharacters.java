package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Input;

final class TypedCharacters {

  static final char NONE = 0;
  private static final char FORWARD_DELETE = 127;

  private TypedCharacters() {}

  static char forKey(int keyCode, boolean shiftDown) {
    if (keyCode >= Input.Keys.A && keyCode <= Input.Keys.Z) {
      char lower = (char) ('a' + (keyCode - Input.Keys.A));
      return shiftDown ? Character.toUpperCase(lower) : lower;
    }
    if (keyCode >= Input.Keys.NUM_0 && keyCode <= Input.Keys.NUM_9) {
      return (char) ('0' + (keyCode - Input.Keys.NUM_0));
    }
    return switch (keyCode) {
      case Input.Keys.SPACE -> ' ';
      case Input.Keys.ENTER -> '\n';
      case Input.Keys.BACKSPACE -> '\b';
      case Input.Keys.TAB -> '\t';
      case Input.Keys.FORWARD_DEL -> FORWARD_DELETE;
      default -> NONE;
    };
  }
}
