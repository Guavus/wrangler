/*
 * Copyright © 2017 Cask Data, Inc.
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

package co.cask.wrangler.steps.transformation;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.cdap.api.annotation.Plugin;
import co.cask.wrangler.api.AbstractDirective;
import co.cask.wrangler.api.DirectiveExecutionException;
import co.cask.wrangler.api.RecipeContext;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.api.annotations.Usage;
import org.simmetrics.StringDistance;
import org.simmetrics.metrics.StringDistances;

import java.util.List;

/**
 * Directive for implementing the directive for measuring the difference between two sequence of characters.
 */
@Plugin(type = "directives")
@Name("text-distance")
@Usage("text-distance <method> <column1> <column2> <destination>")
@Description("Calculates a text distance measure between two columns containing string.")
public class TextDistanceMeasure extends AbstractDirective {
  private final String column1;
  private final String column2;
  private final String destination;
  private final StringDistance distance;

  public TextDistanceMeasure(int lineno, String directive, String method, String column1, String column2,
                             String destination) {
    super(lineno, directive);
    this.column1 = column1;
    this.column2 = column2;
    this.destination = destination;

    // defaults to : cosineSimilarity
    switch(method.toLowerCase()) {
      case "euclidean":
        distance = StringDistances.euclideanDistance();
        break;

      case "block-distance":
        distance = StringDistances.blockDistance();
        break;

      case "identity":
        distance = StringDistances.identity();
        break;

      case "block":
        distance = StringDistances.blockDistance();
        break;

      case "dice":
        distance = StringDistances.dice();
        break;

      case "longest-common-subsequence":
        distance = StringDistances.longestCommonSubsequence();
        break;

      case "longest-common-substring":
        distance = StringDistances.longestCommonSubstring();
        break;

      case "overlap-cofficient":
        distance = StringDistances.overlapCoefficient();
        break;

      case "jaccard":
        distance = StringDistances.jaccard();
        break;

      case "damerau-levenshtein":
        distance = StringDistances.damerauLevenshtein();
        break;

      case "generalized-jaccard":
        distance = StringDistances.generalizedJaccard();
        break;

      case "jaro":
        distance = StringDistances.jaro();
        break;

      case "simon-white":
        distance = StringDistances.simonWhite();
        break;

      case "levenshtein":
        distance = StringDistances.levenshtein();
        break;

      case "cosine":
      default:
        distance = StringDistances.cosineSimilarity();
        break;
    }
  }

  /**
   * Executes a wrangle step on single {@link Row} and return an array of wrangled {@link Row}.
   *
   * @param rows List of input {@link Row} to be wrangled by this step.
   * @param context {@link RecipeContext} passed to each step.
   * @return Wrangled List of {@link Row}.
   */
  @Override
  public List<Row> execute(List<Row> rows, RecipeContext context) throws DirectiveExecutionException {
    for (Row row : rows) {
      int idx1 = row.find(column1);
      int idx2 = row.find(column2);

      if (idx1 == -1 || idx2 == -1) {
        row.add(destination, 0.0f);
        continue;
      }

      Object object1 = row.getValue(idx1);
      Object object2 = row.getValue(idx2);

      if (object1 == null || object2 == null) {
        row.add(destination, 0.0f);
        continue;
      }

      if (!(object1 instanceof String) || !(object2 instanceof String)) {
        row.add(destination, 0.0f);
        continue;
      }

      if (((String) object1).isEmpty() || ((String) object2).isEmpty()) {
        row.add(destination, 0.0f);
        continue;
      }

      row.add(destination, distance.distance((String) object1, (String) object2));
    }

    return rows;
  }
}
