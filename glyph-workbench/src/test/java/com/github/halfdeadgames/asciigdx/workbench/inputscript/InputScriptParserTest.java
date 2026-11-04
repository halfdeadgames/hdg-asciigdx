package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputScriptParserTest {

  private static ScriptStep parseOne(String line) {
    List<ScriptStep> steps = InputScriptParser.parse(line);
    assertEquals(1, steps.size());
    return steps.get(0);
  }

  private static InputScriptException failure(String script) {
    return assertThrows(InputScriptException.class, () -> InputScriptParser.parse(script));
  }

  @Test
  void emptyScriptGivesNoSteps() {
    assertTrue(InputScriptParser.parse("").isEmpty());
  }

  @Test
  void resultIsUnmodifiable() {
    List<ScriptStep> steps = InputScriptParser.parse("quit");
    assertThrows(UnsupportedOperationException.class, () -> steps.add(new ScriptStep.Quit()));
  }

  @Test
  void blankLinesAndCommentsAreIgnored() {
    String script = "\n   \n# a comment\n   # indented comment\n\t\nquit\n";
    assertEquals(List.of(new ScriptStep.Quit()), InputScriptParser.parse(script));
  }

  @Test
  void windowsLineEndingsAreAccepted() {
    List<ScriptStep> steps = InputScriptParser.parse("press A\r\nquit\r\n");
    assertEquals(2, steps.size());
  }

  @Test
  void stepsKeepScriptOrder() {
    List<ScriptStep> steps = InputScriptParser.parse("wait 1s\nwheel 2\nquit");
    assertEquals(
        List.of(new ScriptStep.WaitSeconds(1f), new ScriptStep.Wheel(2), new ScriptStep.Quit()), steps);
  }

  @Test
  void waitSeconds() {
    assertEquals(new ScriptStep.WaitSeconds(3f), parseOne("wait 3s"));
    assertEquals(new ScriptStep.WaitSeconds(1.5f), parseOne("wait 1.5s"));
  }

  @Test
  void waitMillisecondsConvertToSeconds() {
    assertEquals(new ScriptStep.WaitSeconds(0.5f), parseOne("wait 500ms"));
  }

  @Test
  void waitFrames() {
    assertEquals(new ScriptStep.WaitFrames(10), parseOne("wait 10f"));
  }

  @Test
  void waitUnitsAreCaseInsensitive() {
    assertEquals(new ScriptStep.WaitSeconds(0.25f), parseOne("WAIT 250MS"));
    assertEquals(new ScriptStep.WaitFrames(4), parseOne("Wait 4F"));
  }

  @Test
  void waitRejectsBadDurations() {
    for (String duration : List.of("0s", "0ms", "0f", "-1s", "-5f", "abc", "3", "s", "f", "1.5f", "NaNs", "Infinitys", "1e3s", "99999999999f")) {
      assertEquals(1, failure("wait " + duration).lineNumber(), duration);
    }
    assertEquals(1, failure("wait 1" + "0".repeat(400) + "s").lineNumber());
  }

  @Test
  void waitRejectsWrongArgumentCount() {
    failure("wait");
    failure("wait 1s 2s");
  }

  @Test
  void keyCommandsUseNativeNames() {
    int enter = Input.Keys.ENTER;
    String enterName = Input.Keys.toString(enter);
    assertEquals(new ScriptStep.Press(enter), parseOne("press " + enterName));
    assertEquals(new ScriptStep.Hold(Input.Keys.A), parseOne("hold A"));
    assertEquals(new ScriptStep.Release(Input.Keys.A), parseOne("release a"));
  }

  @Test
  void keyNamesAreCaseInsensitiveAndUnderscoreSpaces() {
    String leftControl = Input.Keys.toString(Input.Keys.CONTROL_LEFT);
    String spelled = leftControl.replace(' ', '_');
    assertEquals(new ScriptStep.Press(Input.Keys.CONTROL_LEFT), parseOne("press " + spelled.toLowerCase()));
    assertEquals(new ScriptStep.Press(Input.Keys.CONTROL_LEFT), parseOne("press " + spelled.toUpperCase()));
  }

  @Test
  void digitKeysResolve() {
    assertEquals(new ScriptStep.Press(Input.Keys.NUM_5), parseOne("press 5"));
  }

  @Test
  void leftHandAliasesResolve() {
    assertEquals(new ScriptStep.Press(Input.Keys.ESCAPE), parseOne("press esc"));
    assertEquals(new ScriptStep.Press(Input.Keys.CONTROL_LEFT), parseOne("press CTRL"));
    assertEquals(new ScriptStep.Press(Input.Keys.SHIFT_LEFT), parseOne("press Shift"));
    assertEquals(new ScriptStep.Press(Input.Keys.ALT_LEFT), parseOne("press alt"));
  }

  @Test
  void keyCommandsRejectUnknownKeyAndBadCounts() {
    failure("press NOT_A_KEY");
    failure("press");
    failure("hold A B");
    failure("release");
  }

  @Test
  void typeKeepsRestOfLineVerbatim() {
    assertEquals(new ScriptStep.TypeText("Hello  World # not a comment"), parseOne("type Hello  World # not a comment"));
  }

  @Test
  void typeStripsOnlyOneSpaceAndLeadingIndent() {
    assertEquals(new ScriptStep.TypeText(" two"), parseOne("   TYPE  two"));
  }

  @Test
  void typeKeepsTrailingSpaces() {
    assertEquals(new ScriptStep.TypeText("a "), parseOne("type a "));
  }

  @Test
  void typeRejectsEmptyText() {
    failure("type");
    failure("type ");
  }

  @Test
  void moveMouse() {
    assertEquals(new ScriptStep.MoveMouse(10, 20), parseOne("move 10 20"));
    assertEquals(new ScriptStep.MoveMouse(0, 0), parseOne("MOVE   0   0"));
  }

  @Test
  void moveRejectsBadInput() {
    failure("move 10");
    failure("move 1 2 3");
    failure("move -1 5");
    failure("move 5 -1");
    failure("move a b");
    failure("move 1.5 2");
  }

  @Test
  void clickDefaultsToLeft() {
    assertEquals(new ScriptStep.Click(5, 6, Input.Buttons.LEFT), parseOne("click 5 6"));
  }

  @Test
  void clickWithButtons() {
    assertEquals(new ScriptStep.Click(5, 6, Input.Buttons.LEFT), parseOne("click 5 6 left"));
    assertEquals(new ScriptStep.Click(5, 6, Input.Buttons.RIGHT), parseOne("click 5 6 RIGHT"));
    assertEquals(new ScriptStep.Click(5, 6, Input.Buttons.MIDDLE), parseOne("Click 5 6 Middle"));
  }

  @Test
  void clickRejectsBadInput() {
    failure("click 5");
    failure("click 5 6 left right");
    failure("click 5 6 back");
    failure("click -5 6");
    failure("click x 6");
  }

  @Test
  void mouseDownAndUpDefaultToLeft() {
    assertEquals(new ScriptStep.MouseDown(Input.Buttons.LEFT), parseOne("mousedown"));
    assertEquals(new ScriptStep.MouseUp(Input.Buttons.LEFT), parseOne("mouseup"));
  }

  @Test
  void mouseDownAndUpTakeButton() {
    assertEquals(new ScriptStep.MouseDown(Input.Buttons.RIGHT), parseOne("MouseDown right"));
    assertEquals(new ScriptStep.MouseUp(Input.Buttons.MIDDLE), parseOne("mouseup middle"));
  }

  @Test
  void mouseButtonCommandsRejectBadInput() {
    failure("mousedown side");
    failure("mouseup left right");
  }

  @Test
  void wheelAcceptsNegativeTicks() {
    assertEquals(new ScriptStep.Wheel(3), parseOne("wheel 3"));
    assertEquals(new ScriptStep.Wheel(-2), parseOne("wheel -2"));
  }

  @Test
  void wheelRejectsBadInput() {
    failure("wheel");
    failure("wheel two");
    failure("wheel 1 2");
  }

  @Test
  void captureAcceptsSafeNamesWithCasePreserved() {
    assertEquals(new ScriptStep.Capture("Main-menu_2"), parseOne("capture Main-menu_2"));
  }

  @Test
  void captureRejectsUnsafeNamesAndBadCounts() {
    failure("capture");
    failure("capture a b");
    failure("capture ../evil");
    failure("capture dir/file");
    failure("capture name.png");
  }

  @Test
  void quit() {
    assertEquals(new ScriptStep.Quit(), parseOne("quit"));
    assertEquals(new ScriptStep.Quit(), parseOne("QUIT"));
  }

  @Test
  void quitRejectsArguments() {
    failure("quit now");
  }

  @Test
  void unknownCommandIsRejected() {
    InputScriptException error = failure("dance 3");
    assertTrue(error.getMessage().contains("dance"));
  }

  @Test
  void errorNamesLineNumberAndOffendingLine() {
    String script = "# header\n\nwait 1s\n  press NOT_A_KEY  \nquit";
    InputScriptException error = failure(script);
    assertEquals(4, error.lineNumber());
    assertTrue(error.getMessage().contains("Line 4"));
    assertTrue(error.getMessage().contains("  press NOT_A_KEY  "));
  }

  @Test
  void firstErrorWins() {
    InputScriptException error = failure("quit\nbogus\nalsobogus");
    assertEquals(2, error.lineNumber());
  }
}
