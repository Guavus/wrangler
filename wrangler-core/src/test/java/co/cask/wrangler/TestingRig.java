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

package co.cask.wrangler;

import co.cask.wrangler.api.DirectiveLoadException;
import co.cask.wrangler.api.DirectiveNotFoundException;
import co.cask.wrangler.api.DirectiveParseException;
import co.cask.wrangler.api.RecipeException;
import co.cask.wrangler.api.RecipeParser;
import co.cask.wrangler.api.RecipePipeline;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.executor.RecipePipelineExecutor;
import co.cask.wrangler.parser.GrammarBasedParser;
import co.cask.wrangler.parser.MigrateToV2;
import co.cask.wrangler.registry.CompositeDirectiveRegistry;
import co.cask.wrangler.registry.SystemDirectiveRegistry;

import java.util.List;

/**
 * Utilities for testing.
 */
public final class TestingRig {

  private TestingRig() {
    // Avoid creation of this object.
  }

  /**
   * Executes the directives on the record specified.
   *
   * @param recipe to be executed.
   * @param rows to be executed on directives.
   * @return transformed directives.
   */
  public static List<Row> execute(String[] recipe, List<Row> rows)
    throws RecipeException, DirectiveParseException, DirectiveLoadException, DirectiveNotFoundException {
    CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
      new SystemDirectiveRegistry()
    );

    String migrate = new MigrateToV2(recipe).migrate();
    RecipeParser parser = new GrammarBasedParser(migrate, registry);
    parser.initialize(null);
    RecipePipeline pipeline = new RecipePipelineExecutor();
    pipeline.configure(parser, null);
    return pipeline.execute(rows);
  }

  public static RecipePipeline execute(String[] recipe)
    throws RecipeException, DirectiveParseException, DirectiveLoadException, DirectiveNotFoundException {
    CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
      new SystemDirectiveRegistry()
    );

    String migrate = new MigrateToV2(recipe).migrate();
    RecipeParser parser = new GrammarBasedParser(migrate, registry);
    parser.initialize(null);
    RecipePipeline pipeline = new RecipePipelineExecutor();
    pipeline.configure(parser, null);
    return pipeline;
  }

  public static RecipeParser parse(String[] recipe)
    throws RecipeException, DirectiveParseException, DirectiveLoadException, DirectiveNotFoundException {
    CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
      new SystemDirectiveRegistry()
    );

    String migrate = new MigrateToV2(recipe).migrate();
    RecipeParser parser = new GrammarBasedParser(migrate, registry);
    parser.initialize(null);
    return parser;
  }

}
