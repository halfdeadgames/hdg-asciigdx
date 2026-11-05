package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class InputScriptParser {

  private static final Pattern DECIMAL = Pattern.compile("\\d+(\\.\\d*)?|\\.\\d+");
  private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d+");
  private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_-]+");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final String DURATION_USAGE = "expected a positive duration like 3s, 500ms, or 10f";

  private InputScriptParser() {}

  public static List<ScriptStep> parse(String scriptText) {
    Objects.requireNonNull(scriptText, "scriptText");
    List<ScriptStep> steps = new ArrayList<>();
    int lineNumber = 0;
    for (String line : (Iterable<String>) scriptText.lines()::iterator) {
      lineNumber++;
      if (isIgnorable(line)) continue;
      steps.add(parseLine(lineNumber, line));
    }
    return Collections.unmodifiableList(steps);
  }

  private static boolean isIgnorable(String line) {
    String trimmed = line.strip();
    return trimmed.isEmpty() || trimmed.startsWith("#");
  }

  private static ScriptStep parseLine(int lineNumber, String line) {
    try {
      return parseCommand(line.stripLeading());
    } catch (IllegalArgumentException problem) {
      throw new InputScriptException(lineNumber, line, problem.getMessage());
    }
  }

  private static ScriptStep parseCommand(String command) {
    int wordEnd = indexOfWhitespace(command);
    String word = wordEnd < 0 ? command : command.substring(0, wordEnd);
    String rest = wordEnd < 0 ? "" : command.substring(wordEnd + 1);
    String[] arguments = splitArguments(rest);
    return switch (word.toLowerCase(Locale.ROOT)) {
      case "wait" -> parseWait(arguments);
      case "press" -> new ScriptStep.Press(parseKey(singleArgument(arguments, "press KEY")));
      case "hold" -> new ScriptStep.Hold(parseKey(singleArgument(arguments, "hold KEY")));
      case "release" -> new ScriptStep.Release(parseKey(singleArgument(arguments, "release KEY")));
      case "type" -> parseType(rest);
      default -> parseMouseOrSessionCommand(word, arguments);
    };
  }

  private static ScriptStep parseMouseOrSessionCommand(String word, String[] arguments) {
    return switch (word.toLowerCase(Locale.ROOT)) {
      case "move" -> parseMove(arguments);
      case "click" -> parseClick(arguments);
      case "mousedown" -> new ScriptStep.MouseDown(parseOptionalButton(arguments, "mousedown [button]"));
      case "mouseup" -> new ScriptStep.MouseUp(parseOptionalButton(arguments, "mouseup [button]"));
      case "wheel" -> new ScriptStep.Wheel(parseInteger(singleArgument(arguments, "wheel N")));
      case "capture" -> parseCapture(arguments);
      case "quit" -> parseQuit(arguments);
      default -> throw new IllegalArgumentException("unknown command '" + word + "'");
    };
  }

  private static int indexOfWhitespace(String text) {
    for (int index = 0; index < text.length(); index++) {
      if (Character.isWhitespace(text.charAt(index))) return index;
    }
    return -1;
  }

  private static String[] splitArguments(String rest) {
    String trimmed = rest.strip();
    return trimmed.isEmpty() ? new String[0] : WHITESPACE.split(trimmed);
  }

  private static ScriptStep parseWait(String[] arguments) {
    String duration = singleArgument(arguments, "wait DURATION");
    String lower = duration.toLowerCase(Locale.ROOT);
    if (lower.endsWith("ms")) return new ScriptStep.WaitSeconds(positiveDecimal(lower, 2) / 1000f);
    if (lower.endsWith("s")) return new ScriptStep.WaitSeconds(positiveDecimal(lower, 1));
    if (lower.endsWith("f")) return new ScriptStep.WaitFrames(positiveFrames(lower));
    throw new IllegalArgumentException(DURATION_USAGE);
  }

  private static float positiveDecimal(String duration, int suffixLength) {
    String number = duration.substring(0, duration.length() - suffixLength);
    if (!DECIMAL.matcher(number).matches()) throw new IllegalArgumentException(DURATION_USAGE);
    float value = Float.parseFloat(number);
    if (!Float.isFinite(value) || value <= 0f) throw new IllegalArgumentException(DURATION_USAGE);
    return value;
  }

  private static int positiveFrames(String duration) {
    String number = duration.substring(0, duration.length() - 1);
    if (!WHOLE_NUMBER.matcher(number).matches()) throw new IllegalArgumentException(DURATION_USAGE);
    int frames = parseInteger(number);
    if (frames <= 0) throw new IllegalArgumentException(DURATION_USAGE);
    return frames;
  }

  private static ScriptStep parseType(String rest) {
    if (rest.isEmpty()) throw new IllegalArgumentException("usage: type TEXT");
    return new ScriptStep.TypeText(rest);
  }

  private static ScriptStep parseMove(String[] arguments) {
    requireArgumentCount(arguments, 2, 2, "move X Y");
    return new ScriptStep.MoveMouse(parseCoordinate(arguments[0]), parseCoordinate(arguments[1]));
  }

  private static ScriptStep parseClick(String[] arguments) {
    requireArgumentCount(arguments, 2, 3, "click X Y [left|right|middle]");
    int button = arguments.length == 3 ? parseButton(arguments[2]) : Input.Buttons.LEFT;
    return new ScriptStep.Click(parseCoordinate(arguments[0]), parseCoordinate(arguments[1]), button);
  }

  private static ScriptStep parseCapture(String[] arguments) {
    String name = singleArgument(arguments, "capture NAME");
    if (!SAFE_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("capture name may only use letters, digits, hyphens, and underscores");
    }
    return new ScriptStep.Capture(name);
  }

  private static ScriptStep parseQuit(String[] arguments) {
    requireArgumentCount(arguments, 0, 0, "quit");
    return new ScriptStep.Quit();
  }

  private static int parseOptionalButton(String[] arguments, String usage) {
    requireArgumentCount(arguments, 0, 1, usage);
    return arguments.length == 0 ? Input.Buttons.LEFT : parseButton(arguments[0]);
  }

  private static int parseButton(String name) {
    return switch (name.toLowerCase(Locale.ROOT)) {
      case "left" -> Input.Buttons.LEFT;
      case "right" -> Input.Buttons.RIGHT;
      case "middle" -> Input.Buttons.MIDDLE;
      default -> throw new IllegalArgumentException("unknown button '" + name + "', expected left, right, or middle");
    };
  }

  private static int parseKey(String name) {
    OptionalInt code = KeyNames.codeOf(name);
    if (code.isEmpty()) throw new IllegalArgumentException("unknown key '" + name + "'");
    return code.getAsInt();
  }

  private static int parseCoordinate(String token) {
    int value = parseInteger(token);
    if (value < 0) throw new IllegalArgumentException("coordinate must not be negative: " + token);
    return value;
  }

  private static int parseInteger(String token) {
    try {
      return Integer.parseInt(token);
    } catch (NumberFormatException notANumber) {
      throw new IllegalArgumentException("not a whole number: '" + token + "'");
    }
  }

  private static String singleArgument(String[] arguments, String usage) {
    requireArgumentCount(arguments, 1, 1, usage);
    return arguments[0];
  }

  private static void requireArgumentCount(String[] arguments, int minimum, int maximum, String usage) {
    if (arguments.length < minimum || arguments.length > maximum) {
      throw new IllegalArgumentException("wrong number of arguments, usage: " + usage);
    }
  }
}
