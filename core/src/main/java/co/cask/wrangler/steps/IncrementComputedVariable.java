package co.cask.wrangler.steps;

import co.cask.wrangler.api.AbstractStep;
import co.cask.wrangler.api.ErrorRecordException;
import co.cask.wrangler.api.PipelineContext;
import co.cask.wrangler.api.Record;
import co.cask.wrangler.api.StepException;
import co.cask.wrangler.api.Usage;
import co.cask.wrangler.steps.transformation.JexlHelper;
import co.cask.wrangler.steps.transformation.functions.Types;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;

import java.util.List;

/**
 * Class description here.
 */
@Usage(
  directive = "increment-variable",
  usage = "increment-variable <variable> <value> <expression>",
  description = "Increments the computed variable when expression is true by the value specified"
)
public class IncrementComputedVariable extends AbstractStep {
  private final String variable;
  private final long incrementBy;
  private final String expression;
  private final JexlEngine engine;
  private final JexlScript script;

  public IncrementComputedVariable(int lineno, String detail, String variable, String value, String expression) {
    super(lineno, detail);
    this.variable = variable;
    this.expression = expression;
    engine = JexlHelper.getEngine();
    script = engine.createScript(this.expression);
    if (Types.isNumber(value)) {
      incrementBy = Long.parseLong(value);
    } else {
      incrementBy = 1;
    }
  }

  /**
   * Executes a wrangle step on single {@link Record} and return an array of wrangled {@link Record}.
   *
   * @param records List of input {@link Record} to be wrangled by this step.
   * @param context {@link PipelineContext} passed to each step.
   * @return Wrangled List of {@link Record}.
   */
  @Override
  public List<Record> execute(List<Record> records, PipelineContext context)
    throws StepException, ErrorRecordException {
    for (Record record : records) {
      // Move the fields from the record into the context.
      JexlContext ctx = new MapContext();
      ctx.set("this", record);
      for (int i = 0; i < record.length(); ++i) {
        ctx.set(record.getColumn(i), record.getValue(i));
      }

      // Transient variables are added.
      if (context != null) {
        for (String variable : context.getTransientStore().getVariables()) {
          ctx.set(variable, context.getTransientStore().get(variable));
        }
      }

      // Execution of the script / expression based on the record data
      // mapped into context.
      try {
        boolean result = (Boolean) script.execute(ctx);
        if (result) {
          context.getTransientStore().increment(variable, incrementBy);
        }
      } catch (JexlException e) {
        // Generally JexlException wraps the original exception, so it's good idea
        // to check if there is a inner exception, if there is wrap it in 'StepException'
        // else just print the error message.
        if (e.getCause() != null) {
          throw new StepException(toString() + " : " + e.getMessage(), e.getCause());
        } else {
          throw new StepException(toString() + " : " + e.getMessage());
        }
      } catch (NumberFormatException e) {
        throw new StepException(toString() + " : " + " type mismatch. Change type of constant " +
                                  "or convert to right data type using conversion functions available. Reason : " + e.getMessage());
      } catch (Exception e) {
        // We want to propogate this exception up!
        if (e instanceof ErrorRecordException) {
          throw e;
        }
        if (e.getCause() != null) {
          throw new StepException(toString() + " : " + e.getMessage(), e.getCause());
        } else {
          throw new StepException(toString() + " : " + e.getMessage());
        }
      }
    }
    return records;
  }
}
