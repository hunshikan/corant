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
package org.corant.suites.query.sql;

import static org.corant.shared.util.CollectionUtils.getSize;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapInteger;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.suites.query.shared.QueryUtils.convert;
import static org.corant.suites.query.sql.SqlHelper.getLimit;
import static org.corant.suites.query.sql.SqlHelper.getOffset;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.query.shared.NamedQuery;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryUtils;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;
import org.corant.suites.query.sql.SqlNamedQueryResolver.Querier;
import org.corant.suites.query.sql.dialect.Dialect;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:33:21
 *
 */
@ApplicationScoped
public abstract class AbstractSqlNamedQuery implements NamedQuery {

  protected SqlQueryExecutor executor;

  @Inject
  Logger logger;

  @Inject
  SqlNamedQueryResolver<String, Map<String, Object>, String, Object[], FetchQuery, QueryHint> resolver;

  @Inject
  @Any
  Instance<ResultHintHandler> resultHintHandlers;

  public Object adaptiveSelect(String q, Map<String, Object> param) {
    if (param != null && param.containsKey(QueryUtils.OFFSET_PARAM_NME)) {
      if (param.containsKey(QueryUtils.LIMIT_PARAM_NME)) {
        return this.page(q, param);
      } else {
        return this.forward(q, param);
      }
    } else {
      return this.select(q, param);
    }
  }

  @Override
  public <T> ForwardList<T> forward(String q, Map<String, Object> param) {
    Querier<String, Object[], FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1);
    try {
      log(q, queryParam, sql, "Limit: " + limitSql);
      ForwardList<T> result = ForwardList.inst();
      List<Map<String, Object>> list = getExecutor().select(limitSql, queryParam);
      int size = getSize(list);
      if (size > 0) {
        this.fetch(list, fetchQueries, param);
        if (size > limit) {
          list.remove(size - 1);
          result.withHasNext(true);
        }
        handleResultHints(resultClass, hints, param, list);
      }
      return result.withResults(convert(list, resultClass));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the forward query [%s].", q);
    }
  }

  @Override
  public <T> T get(String q, Map<String, Object> param) {
    Querier<String, Object[], FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    try {
      log(q, queryParam, sql);
      Map<String, Object> result = getExecutor().get(sql, queryParam);
      this.fetch(result, fetchQueries, param);
      handleResultHints(resultClass, hints, param, result);
      return convert(result, resultClass);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the get query [%s].",
          q);
    }
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    Querier<String, Object[], FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    String limitSql = getDialect().getLimitSql(sql, offset, limit);
    try {
      log(q, queryParam, sql, "Limit: " + limitSql);
      List<Map<String, Object>> list = getExecutor().select(limitSql, queryParam);
      PagedList<T> result = PagedList.of(offset, limit);
      int size = getSize(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = getDialect().getCountSql(sql);
          log("total-> " + q, queryParam, totalSql);
          result.withTotal(
              getMapInteger(getExecutor().get(totalSql, queryParam), Dialect.COUNT_FIELD_NAME));
        }
        this.fetch(list, fetchQueries, param);
        handleResultHints(resultClass, hints, param, list);
      }
      return result.withResults(convert(list, resultClass));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the page query [%s].",
          q);
    }
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    Querier<String, Object[], FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    try {
      log(q, queryParam, sql);
      List<Map<String, Object>> result = getExecutor().select(sql, queryParam);
      int size = getSize(result);
      if (size > 0) {
        this.fetch(result, fetchQueries, param);
        handleResultHints(rcls, hints, param, result);
      }
      return convert(result, rcls);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the select query [%s].",
          q);
    }
  }

  @Override
  public <T> Stream<T> stream(String q, Map<String, Object> param) {
    throw new NotSupportedException();
  }

  protected <T> void fetch(List<T> list, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (!isEmpty(list) && !isEmpty(fetchQueries)) {
      list.forEach(e -> fetchQueries.stream().forEach(f -> this.fetch(e, f, new HashMap<>(param))));
    }
  }

  protected <T> void fetch(T obj, FetchQuery fetchQuery, Map<String, Object> param) {
    if (null == obj || fetchQuery == null) {
      return;
    }
    Map<String, Object> fetchParam = QueryUtils.resolveFetchParam(obj, fetchQuery, param);
    if (!QueryUtils.decideFetch(obj, fetchQuery, param)) {
      return;
    }
    int maxSize = fetchQuery.getMaxSize();
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    Querier<String, Object[], FetchQuery, QueryHint> querier =
        resolver.resolve(refQueryName, fetchParam);
    String sql = querier.getScript();
    // Class<?> resultClass = defaultObject(fetchQuery.getResultClass(), querier.getResultClass());
    Class<?> resultClass = Map.class;
    Object[] params = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    if (maxSize > 0) {
      sql = getDialect().getLimitSql(sql, QueryUtils.OFFSET_PARAM_VAL, maxSize);
    }
    try {
      log("fetch-> " + refQueryName, params, sql);
      List<Map<String, Object>> fetchedList = getExecutor().select(sql, params);
      Object fetchedResult = null;
      if (multiRecords) {
        fetchedResult = fetchedList;
      } else if (!isEmpty(fetchedList)) {
        fetchedResult = fetchedList.get(0);
        fetchedList = fetchedList.subList(0, 1);
      }
      QueryUtils.resolveFetchResult(obj, fetchedResult, injectProName);
      this.fetch(fetchedList, fetchQueries, param);
      handleResultHints(resultClass, hints, param, fetchedList);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  protected <T> void fetch(T obj, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (obj != null && !isEmpty(fetchQueries)) {
      fetchQueries.stream().forEach(f -> this.fetch(obj, f, new HashMap<>(param)));
    }
  }

  protected abstract SqlQueryConfiguration getConfiguration();

  protected Dialect getDialect() {
    return getConfiguration().getDialect();
  }

  protected SqlQueryExecutor getExecutor() {
    return executor;
  }

  protected SqlNamedQueryResolver<String, Map<String, Object>, String, Object[], FetchQuery, QueryHint> getResolver() {
    return resolver;
  }

  protected void handleResultHints(Class<?> resultClass, List<QueryHint> hints, Object param,
      Object result) {
    if (result != null && !resultHintHandlers.isUnsatisfied()) {
      hints.forEach(qh -> {
        AtomicBoolean exclusive = new AtomicBoolean(false);
        resultHintHandlers.stream().filter(h -> h.canHandle(resultClass, qh))
            .sorted(ResultHintHandler::compare).forEachOrdered(h -> {
              if (!exclusive.get()) {
                try {
                  h.handle(qh, param, result);
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                } finally {
                  if (h.exclusive()) {
                    exclusive.set(true);
                  }
                }
              }
            });
      });
    }
  }

  protected void log(String name, Object[] param, String... sql) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query sql]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", sql)));
  }

  protected void setExecutor(SqlQueryExecutor executor) {
    this.executor = executor;
  }
}
