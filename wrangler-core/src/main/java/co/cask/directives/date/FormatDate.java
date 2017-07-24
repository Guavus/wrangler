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
import co.cask.wrangler.api.parser.Text;
import co.cask.wrangler.api.parser.TokenType;
import co.cask.wrangler.api.parser.UsageDefinition;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A directive for managing date formats.
 */
@Plugin(type = Directive.Type)
@Name("format-date")
@Categories(categories = {"date", "format"})
@Description("Formats a column using a date-time format. Use 'parse-as-date` beforehand.")
public class FormatDate implements Directive {
  public static final String NAME = "format-date";
  private String format;
  private String column;
  private DateFormat destinationFmt;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("column", TokenType.COLUMN_NAME);
    builder.define("format", TokenType.TEXT);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.column = ((ColumnName) args.value("column")).value();
    this.format = ((Text) args.value("format")).value();
    this.destinationFmt = new SimpleDateFormat(this.format);
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    List<Row> results = new ArrayList<>();
    for (Row row : rows) {
      Row dt = new Row(row);
      int idx = dt.find(column);
      if (idx != -1) {
        Object object = row.getValue(idx);
        if (object != null && object instanceof Date) {
          dt.setValue(idx, destinationFmt.format((Date) object));
        } else {
          throw new DirectiveExecutionException(
            String.format("%s : Invalid type '%s' of column '%s'. Apply 'parse-as-date' directive first.", toString(),
                          object != null ? object.getClass().getName() : "null", column)
          );
        }
      } else {
        throw new DirectiveExecutionException(toString() + " : '" +
                                  column + "' column is not defined in the row. Please check the wrangling step."
        );
      }
      results.add(dt);
    }
    return results;
  }
}