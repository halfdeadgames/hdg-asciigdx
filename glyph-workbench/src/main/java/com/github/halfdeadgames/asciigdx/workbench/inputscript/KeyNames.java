package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Input;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

final class KeyNames {

  private static final Map<String, Integer> CODES_BY_NAME = buildLookup();

  private KeyNames() {}

  static OptionalInt codeOf(String name) {
    Integer code = CODES_BY_NAME.get(normalize(name));
    return code == null ? OptionalInt.empty() : OptionalInt.of(code);
  }

  static String normalize(String name) {
    return name.toUpperCase(Locale.ROOT).replace(' ', '_');
  }

  private static Map<String, Integer> buildLookup() {
    Map<String, Integer> codes = new HashMap<>();
    for (int code = 0; code <= Input.Keys.MAX_KEYCODE; code++) {
      String name = Input.Keys.toString(code);
      if (name != null) codes.putIfAbsent(normalize(name), code);
    }
    codes.putIfAbsent("ESC", Input.Keys.ESCAPE);
    codes.putIfAbsent("CTRL", Input.Keys.CONTROL_LEFT);
    codes.putIfAbsent("SHIFT", Input.Keys.SHIFT_LEFT);
    codes.putIfAbsent("ALT", Input.Keys.ALT_LEFT);
    return Collections.unmodifiableMap(codes);
  }
}
