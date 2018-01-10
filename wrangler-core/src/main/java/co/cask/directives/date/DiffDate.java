/*
 *  Copyright © 2017 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package co.cask.directives.date;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.cdap.api.annotation.Plugin;
import co.cask.wrangler.api.Arguments;
import co.cask.wrangler.api.Directive;
import co.cask.wrangler.api.DirectiveExecutionException;
import co.cask.wrangler.api.DirectiveParseException;
import co.cask.wrangler.api.ExecutorContext;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.api.annotations.Categories;
import co.cask.wrangler.api.parser.ColumnName;
import co.cask.wrangler.api.parser.Identifier;
import co.cask.wrangler.api.parser.TokenType;
import co.cask.wrangler.api.parser.UsageDefinition;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Date;
import java.util.List;

/**
 * A directive for taking difference in Dates.
 */
@Plugin(type = Directive.Type)
@Name("diff-date")
@Categories(categories = {"date"})
@Description("Calculates the difference in milliseconds between two Date objects." +
  "Positive if <column2> earlier. Must use 'parse-as-date' or 'parse-as-simple-date' first.")
public class DiffDate implements Directive {
  public static final String NAME = "diff-date";
  private String column1;
  private String column2;
  private String destCol;
  private String unit;
  private final Date date = new Date();

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("column1", TokenType.COLUMN_NAME);
    builder.define("column2", TokenType.COLUMN_NAME);
    builder.define("destination", TokenType.COLUMN_NAME);
    builder.define("unit", TokenType.IDENTIFIER);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.column1 = ((ColumnName) args.value("column1")).value();
    this.column2 = ((ColumnName) args.value("column2")).value();
    this.destCol = ((ColumnName) args.value("destination")).value();

    if (args.contains("unit")) {
      String u = ((Identifier) args.value("unit")).value();
      u = u.toLowerCase();
      switch (u) {
        case "days":
        case "day":
          this.unit = "days";
          break;

        case "hours":
        case "hour":
          this.unit = "hours";
          break;

        case "minutes":
        case "minute":
          this.unit = "minutes";
          break;

        case "seconds":
        case "second":
          this.unit = "seconds";
          break;

        case "milliseconds":
        case "millisecond":
          this.unit = "milliseconds";
          break;

        default:
          throw new DirectiveParseException(
            String.format(
              "Incorect unit specified. Possible values are days, hours, minutes or seconds."
            )
          );
      }
    } else {
      this.unit = "milliseconds";
    }
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    for (Row row : rows) {
      DateTime date1 = getDate(row, column1);
      DateTime date2 = getDate(row, column2);
      if (date1 != null && date2 != null) {
        long units = Math.abs(Seconds.secondsBetween(date1, date2).getSeconds());
        switch(unit) {
          case "days":
            units = units / ( 24 * 60 * 60 );
            break;

          case "hours":
            units = units / ( 60 * 60);
            break;

          case "minutes":
            units = units / 60;
            break;

          case "seconds":
            units = units * 1;
            break;

          case "milliseconds":
            units = units * 1000;
            break;
        }
        row.addOrSet(destCol, units);
      } else {
        row.addOrSet(destCol, null);
      }
    }
    return rows;
  }

  private DateTime getDate(Row row, String colName) throws DirectiveExecutionException {
    // If one of the column contains now, then we return
    // the current date.
    if (colName.equalsIgnoreCase("now")) {
      return new DateTime(date);
    }

    // Else attempt to find the column.
    int idx = row.find(colName);
    if (idx == -1) {
      throw new DirectiveExecutionException(toString() + " : '" +
                                colName + "' column is not defined in the row.");
    }
    Object o = row.getValue(idx);
    if (o == null || !(o instanceof Date)) {
      return null;
    }
    return new DateTime(o);
  }
}
