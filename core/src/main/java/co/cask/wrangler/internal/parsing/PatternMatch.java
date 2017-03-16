/*
 * Copyright © 2016, 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.wrangler.internal.parsing;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.util.Parser;
import co.cask.wrangler.api.PatternMatchableStep;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents an input matching for a pattern and provided arguments.
 */
public final class PatternMatch {

  private final PatternMatchableStep command;
  private final String input;

  /**
   * @param command the {@link PatternMatchableStep} that was matched to the {@link #input}
   * @param input   the input that was provided
   */
  public PatternMatch(PatternMatchableStep command, String input) {
    this.command = command;
    this.input = input;
  }

  /**
   * @return the {@link PatternMatchableStep} that was matched to the {@link #input}
   */
  public PatternMatchableStep getCommand() {
    return command;
  }

  /**
   * @return the {@link Arguments} parsed from the {@link #input} and {@link #command} pattern
   */
  public Arguments getArguments() {
    return parseArguments(input.trim(), command.getPattern());
  }

  public String getInput() {
    return input;
  }

  /**
   * Parse arguments from the input and command pattern.
   *
   * @param input   the input
   * @param pattern the command pattern
   * @return parsed arguments
   */
  private Arguments parseArguments(String input, String pattern) {
    ImmutableMap.Builder<String, String> args = ImmutableMap.builder();

    List<String> splitInput = Parser.parseInput(input);
    List<String> splitPattern = Parser.parsePattern(pattern);

    while (!splitInput.isEmpty()) {
      if (splitPattern.isEmpty()) {
        throw new IllegalArgumentException("Expected format: " + command.getPattern());
      }
      String patternPart = splitPattern.get(0);
      String inputPart = processInputArgument(splitInput.get(0));
      if (patternPart.startsWith((Character.toString(Parser.OPTIONAL_PART_BEGINNING))) &&
        patternPart.endsWith((Character.toString(Parser.OPTIONAL_PART_ENDING)))) {
        args.putAll(parseOptional(splitInput, getEntry(patternPart)));
      } else {
        if (patternPart.startsWith((Character.toString(Parser.MANDATORY_ARG_BEGINNING))) &&
          patternPart.endsWith((Character.toString(Parser.MANDATORY_ARG_ENDING)))) {
          args.put(getEntry(patternPart), inputPart);
        } else if (!patternPart.equals(inputPart)) {
          throw new IllegalArgumentException("Expected format: " + command.getPattern());
        }
        splitInput.remove(0);
      }
      splitPattern.remove(0);
    }
    return new Arguments(args.build(), input);
  }

  /**
   * Parse arguments from the split input and pattern.
   * Used for parsing optional parameters. Does not cause the effect in case specified parameter absent.
   *
   * @param splitInput the split input
   * @param pattern    the pattern
   * @return the map of arguments
   */
  private Map<String, String> parseOptional(List<String> splitInput, String pattern) {
    ImmutableMap.Builder<String, String> args = ImmutableMap.builder();

    List<String> copyInput = new ArrayList<String>(splitInput);
    List<String> splitPattern = Parser.parsePattern(pattern);

    while (!splitPattern.isEmpty()) {
      if (copyInput.isEmpty()) {
        return Collections.emptyMap();
      }
      String patternPart = splitPattern.get(0);
      String inputPart = processInputArgument(copyInput.get(0));
      if (patternPart.startsWith((Character.toString(Parser.MANDATORY_ARG_BEGINNING))) &&
        patternPart.endsWith((Character.toString(Parser.MANDATORY_ARG_ENDING)))) {
        args.put(getEntry(patternPart), inputPart);
      } else if (patternPart.startsWith((Character.toString(Parser.OPTIONAL_PART_BEGINNING))) &&
        patternPart.endsWith((Character.toString(Parser.OPTIONAL_PART_ENDING)))) {
        args.putAll(parseOptional(copyInput, getEntry(patternPart)));
      } else if (!patternPart.equals(inputPart)) {
        return Collections.emptyMap();
      }
      splitPattern.remove(0);
      copyInput.remove(0);
    }

    splitInput.clear();
    splitInput.addAll(copyInput);
    return args.build();
  }

  /**
   * Strip the surrounding ' or " from the input and process the escaped quotes.
   *
   * @param inputPart input argument to process
   * @return the processed input
   */
  private String processInputArgument(String inputPart) {
    if (inputPart.startsWith("'") && inputPart.endsWith("'")) {
      inputPart = inputPart.substring(1, inputPart.length() - 1);
    } else if (inputPart.startsWith("\"") && inputPart.endsWith("\"")) {
      inputPart = inputPart.substring(1, inputPart.length() - 1);
    }
    inputPart = inputPart.replaceAll("\\'", "'");
    inputPart = inputPart.replaceAll("\\\"", "\"");
    return inputPart;
  }

  /**
   * Retrieves entry from input {@link String}.
   * For example, for input "<some input>" returns "some input".
   *
   * @param input the input
   * @return entry {@link String}
   */
  private String getEntry(String input) {
    return input.substring(1, input.length() - 1);
  }
}
