/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.query.shared.dynamic.freemarker;

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.defaultString;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.AbstractDynamicQuerierBuilder;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;
import org.corant.modules.query.spi.ParameterReviser;
import org.corant.shared.ubiquity.Tuple.Triple;
import freemarker.core.Environment;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleNumber;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午10:00:50
 *
 */
@SuppressWarnings("deprecation")
public abstract class FreemarkerDynamicQuerierBuilder<P, S, Q extends DynamicQuerier<P, S>>
    extends AbstractDynamicQuerierBuilder<P, S, Q> {

  protected final Template execution;

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Create a free marker dynamic querier builder
   *
   * @param query the query object
   * @param queryHandler the query handler
   * @param fetchQueryHandler the fetch query handler
   */
  protected FreemarkerDynamicQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler) {
    super(query, queryHandler, fetchQueryHandler);
    try {
      String scriptSource =
          defaultString(query.getMacroScript()).concat(query.getScript().getCode());// FIXME
      execution = new Template(query.getName(), scriptSource, FreemarkerConfigurations.FM_CFG);
    } catch (IOException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the query template [%s].", query.getName());
    }
  }

  @Override
  public Q build(Object param) {
    return build(execute(resolveParameter(param)));
  }

  protected abstract Q build(Triple<QueryParameter, P, String> processed);

  protected Triple<QueryParameter, P, String> execute(QueryParameter param) {
    try (StringWriter sw = new StringWriter()) {
      // Inject query criteria
      Environment e = execution.createProcessingEnvironment(param.getCriteria(), sw);

      // Inject template method model
      DynamicTemplateMethodModelEx<P> tmm = getTemplateMethodModelEx();
      String tmmTyp = tmm.getType();
      checkVarNames(e, tmmTyp);
      e.setVariable(tmmTyp, tmm);

      // Inject query limit & offset

      // if (param.getLimit() != null) {
      // checkVarNames(e, QueryParameter.LIMIT_PARAM_NME);
      // e.setVariable(QueryParameter.LIMIT_PARAM_NME, new SimpleNumber(param.getLimit()));
      // }

      if (param.getLimit() != null
          && !e.getKnownVariableNames().contains(QueryParameter.LIMIT_PARAM_NME)) {
        // For compatibility issues, temporarily use this
        e.setVariable(QueryParameter.LIMIT_PARAM_NME, new SimpleNumber(param.getLimit()));
      }

      // if (param.getOffset() != null) {
      // checkVarNames(e, QueryParameter.OFFSET_PARAM_NME);
      // e.setVariable(QueryParameter.OFFSET_PARAM_NME, new SimpleNumber(param.getOffset()));
      // }

      if (param.getOffset() != null
          && !e.getKnownVariableNames().contains(QueryParameter.OFFSET_PARAM_NME)) {
        // For compatibility issues, temporarily use this
        e.setVariable(QueryParameter.OFFSET_PARAM_NME, new SimpleNumber(param.getOffset()));
      }

      // Inject query context
      ObjectWrapper ow = execution.getObjectWrapper();
      if (isNotEmpty(param.getContext())) {
        for (Entry<String, Object> ctx : param.getContext().entrySet()) {
          checkVarNames(e, ctx.getKey());
          TemplateModel val = ctx.getValue() == null ? new SimpleHash(ow) : ow.wrap(ctx.getValue());
          e.setVariable(ctx.getKey(), val);
        }
      }

      // Inject parameter reviser
      // setEnvironmentVariables(e, ow); deprecated since 1.6.2
      e.process();
      return Triple.of(param, tmm.getParameters(), sw.toString());
    } catch (IOException | TemplateException e) {
      throw new QueryRuntimeException(e,
          "Freemarker dynamic querier builder [%s] execute occurred error!", getQuery().getName());
    }
  }

  /**
   * Returns a template method model use for variables handling. The "extended method" template
   * language data type: Objects that act like functions.
   */
  protected abstract DynamicTemplateMethodModelEx<P> getTemplateMethodModelEx();

  @Deprecated
  protected void setEnvironmentVariables(Environment env, ObjectWrapper ow) {
    select(ParameterReviser.class).stream().filter(r -> r.supports(getQuery()))
        .sorted((x, y) -> Integer.compare(x.getPriority(), y.getPriority()) * -1).forEach(r -> {
          Map<String, Object> vars = r.get();
          if (vars != null) {
            for (Entry<String, Object> entry : vars.entrySet()) {
              String key = entry.getKey();
              Object val = entry.getValue();
              try {
                if (key != null && val != null) {
                  if (!env.getKnownVariableNames().contains(key)) {
                    env.setVariable(key, ow.wrap(val));
                  } else {
                    logger.warning(() -> String.format(
                        "Query [%s] parameter reviser occurred variable name [%s] conflict.",
                        getQuery().getName(), key));
                  }
                }
              } catch (TemplateModelException e) {
                throw new QueryRuntimeException(e,
                    "Freemarker dynamic querier builder handle environment variables [%s] [%s] occurred error!",
                    getQuery().getName(), key);
              }
            }
          }
        });
  }

  void checkVarNames(Environment e, String... varNames) throws TemplateModelException {
    for (String varName : varNames) {
      if (e.getKnownVariableNames().contains(varName)) {
        throw new QueryRuntimeException(
            "Freemarker dynamic querier buildr [%s] error, the key [%s] name conflict.",
            query.getName(), varName);
      }
    }
  }

}
