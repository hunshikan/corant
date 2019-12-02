package org.corant.suites.query.mongodb;
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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.CollectionUtils.getSize;
import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.ConversionUtils.toEnum;
import static org.corant.shared.util.MapUtils.getMapEnum;
import static org.corant.shared.util.MapUtils.getOpt;
import static org.corant.shared.util.MapUtils.getOptMapObject;
import static org.corant.shared.util.ObjectUtils.max;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.shared.util.ConversionUtils;
import org.corant.suites.query.mongodb.MgNamedQuerier.MgOperator;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.corant.suites.query.shared.NamedQuerierResolver;
import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;
import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CountOptions;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:20:43
 *
 */
public abstract class AbstractMgNamedQueryService extends AbstractNamedQueryService {

  public static final String PRO_KEY_MAX_TIMEMS = "mg.maxTimeMs";
  public static final String PRO_KEY_MAX_AWAIT_TIMEMS = "mg.maxAwaitTimeMs";
  public static final String PRO_KEY_NO_CURSOR_TIMEOUT = "mg.noCursorTimeout";
  public static final String PRO_KEY_OPLOG_REPLAY = "mg.oplogReplay";
  public static final String PRO_KEY_PARTIAL = "mg.partial";
  public static final String PRO_KEY_CURSOR_TYPE = "mg.cursorType";
  public static final String PRO_KEY_BATCH_SIZE = "mg.batchSize";
  public static final String PRO_KEY_RETURN_KEY = "mg.returnKey";
  public static final String PRO_KEY_COMMENT = "mg.comment";
  public static final String PRO_KEY_SHOW_RECORDID = "mg.showRecordId";

  public static final String PRO_KEY_CO = "mg.count-options";
  public static final String PRO_KEY_CO_LIMIT = PRO_KEY_CO + ".limit";
  public static final String PRO_KEY_CO_SKIP = PRO_KEY_CO + ".skip";
  public static final String PRO_KEY_CO_MAX_TIMEMS = PRO_KEY_CO + ".maxTimeMS";

  public static final String PRO_KEY_CO_COLA = PRO_KEY_CO + ".collation";
  public static final String PRO_KEY_CO_COLA_LOCALE = PRO_KEY_CO_COLA + ".locale";
  public static final String PRO_KEY_CO_COLA_CASE_LEVEL = PRO_KEY_CO_COLA + ".caseLevel";
  public static final String PRO_KEY_CO_COLA_CASE_FIRST = PRO_KEY_CO_COLA + ".caseFirst";
  public static final String PRO_KEY_CO_COLA_STRENGTH = PRO_KEY_CO_COLA + ".strength";
  public static final String PRO_KEY_CO_COLA_NUMORD = PRO_KEY_CO_COLA + ".numericOrdering";
  public static final String PRO_KEY_CO_COLA_ALTERNATE = PRO_KEY_CO_COLA + ".alternate";
  public static final String PRO_KEY_CO_COLA_MAXVAR = PRO_KEY_CO_COLA + ".maxVariable";
  public static final String PRO_KEY_CO_COLA_NORMA = PRO_KEY_CO_COLA + ".normalization";
  public static final String PRO_KEY_CO_COLA_BACKWORDS = PRO_KEY_CO_COLA + ".backwards";

  @Override
  public void fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
    int maxSize = fetchQuery.isMultiRecords() ? fetchQuery.getMaxSize() : 1;
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    MgNamedQuerier querier = getResolver().resolve(refQueryName, fetchParam);
    log(refQueryName, querier.getQueryParameter(), querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).limit(maxSize);
    List<Map<String, Object>> fetchedList =
        streamOf(fi).map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    if (result instanceof List) {
      parentQuerier.resolveFetchedResult((List<?>) result, fetchedList, fetchQuery);
    } else {
      parentQuerier.resolveFetchedResult(result, fetchedList, fetchQuery);
    }
    fetch(fetchedList, querier);
    querier.resolveResultHints(fetchedList);
  }

  @Override
  public <T> ForwardList<T> forward(String queryName, Object parameter) {
    MgNamedQuerier querier = getResolver().resolve(queryName, parameter);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    ForwardList<T> result = ForwardList.inst();
    FindIterable<Document> fi = query(querier).skip(offset).limit(limit + 1);
    List<Map<String, Object>> list =
        streamOf(fi).map(Decimal128Utils::convert).collect(Collectors.toList());
    int size = getSize(list);
    if (size > 0) {
      this.fetch(list, querier);
      if (size > limit) {
        list.remove(size - 1);
        result.withHasNext(true);
      }
    }
    return result.withResults(querier.resolveResult(list));
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    MgNamedQuerier querier = getResolver().resolve(queryName, parameter);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).limit(1);
    Map<String, Object> result = Decimal128Utils.convert(fi.iterator().tryNext());
    this.fetch(result, querier);
    return querier.resolveResult(result);
  }

  @Override
  public <T> PagedList<T> page(String queryName, Object parameter) {
    MgNamedQuerier querier = getResolver().resolve(queryName, parameter);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    PagedList<T> result = PagedList.of(offset, limit);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).skip(offset).limit(limit);
    List<Map<String, Object>> list =
        streamOf(fi).map(Decimal128Utils::convert).collect(Collectors.toList());
    int size = getSize(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        result.withTotal(Long.valueOf(queryCount(querier)).intValue());
      }
      this.fetch(list, querier);
    }
    return result.withResults(querier.resolveResult(list));
  }

  @Override
  public <T> List<T> select(String queryName, Object parameter) {
    MgNamedQuerier querier = getResolver().resolve(queryName, parameter);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    int maxSelectSize = resolveMaxSelectSize(querier);
    FindIterable<Document> fi = query(querier).limit(maxSelectSize + 1);
    List<Map<String, Object>> list =
        streamOf(fi).map(Decimal128Utils::convert).collect(Collectors.toList());
    int size = getSize(list);
    if (size > 0) {
      if (size > maxSelectSize) {
        throw new QueryRuntimeException(
            "[%s] Result record number overflow, the allowable range is %s.", queryName,
            maxSelectSize);
      }
      this.fetch(list, querier);
    }
    return querier.resolveResult(list);
  }

  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    MgNamedQuerier querier = getResolver().resolve(queryName, parameter);
    log("stream->" + queryName, querier.getQueryParameter(), querier.getOriginalScript());
    return streamOf(query(querier)).map(result -> {
      this.fetch(Decimal128Utils.convert(result), querier);
      return querier.resolveResult(result);
    });
  }

  protected abstract MongoDatabase getDataBase();

  protected abstract NamedQuerierResolver<String, Object, MgNamedQuerier> getResolver();

  protected FindIterable<Document> query(MgNamedQuerier querier) {
    FindIterable<Document> fi =
        getDataBase().getCollection(resolveCollectionName(querier.getName())).find();
    EnumMap<MgOperator, Bson> script = querier.getScript(null);
    for (MgOperator op : MgOperator.values()) {
      switch (op) {
        case FILTER:
          getOpt(script, op).ifPresent(fi::filter);
          break;
        case PROJECTION:
          getOpt(script, op).ifPresent(fi::projection);
          break;
        case MIN:
          getOpt(script, op).ifPresent(fi::min);
          break;
        case MAX:
          getOpt(script, op).ifPresent(fi::max);
          break;
        case HINT:
          getOpt(script, op).ifPresent(fi::hint);
          break;
        case SORT:
          getOpt(script, op).ifPresent(fi::sort);
          break;
        default:
          break;
      }
    }
    Map<String, String> pros = querier.getQuery().getProperties();
    // handle properties
    getOptMapObject(pros, PRO_KEY_BATCH_SIZE, ConversionUtils::toInteger).ifPresent(fi::batchSize);
    getOptMapObject(pros, PRO_KEY_COMMENT, ConversionUtils::toString).ifPresent(fi::comment);
    CursorType ct = getMapEnum(pros, PRO_KEY_CURSOR_TYPE, CursorType.class);
    if (ct != null) {
      fi.cursorType(ct);
    }
    getOptMapObject(pros, PRO_KEY_MAX_AWAIT_TIMEMS, ConversionUtils::toLong)
        .ifPresent(t -> fi.maxAwaitTime(t, TimeUnit.MILLISECONDS));
    getOptMapObject(pros, PRO_KEY_MAX_TIMEMS, ConversionUtils::toLong)
        .ifPresent(t -> fi.maxTime(t, TimeUnit.MILLISECONDS));
    getOptMapObject(pros, PRO_KEY_NO_CURSOR_TIMEOUT, ConversionUtils::toBoolean)
        .ifPresent(fi::noCursorTimeout);
    getOptMapObject(pros, PRO_KEY_OPLOG_REPLAY, ConversionUtils::toBoolean)
        .ifPresent(fi::oplogReplay);
    getOptMapObject(pros, PRO_KEY_PARTIAL, ConversionUtils::toBoolean).ifPresent(fi::partial);
    getOptMapObject(pros, PRO_KEY_RETURN_KEY, ConversionUtils::toBoolean).ifPresent(fi::returnKey);
    getOptMapObject(pros, PRO_KEY_SHOW_RECORDID, ConversionUtils::toBoolean)
        .ifPresent(fi::showRecordId);
    resovleCollation(querier).ifPresent(fi::collation);
    return fi;
  }

  protected long queryCount(MgNamedQuerier querier) {
    CountOptions co = new CountOptions();
    if (querier.getScript(null).get(MgOperator.HINT) != null) {
      co.hint(querier.getScript(null).get(MgOperator.HINT));
    }
    Map<String, String> pros = querier.getQuery().getProperties();
    getOptMapObject(pros, PRO_KEY_CO_LIMIT, ConversionUtils::toInteger).ifPresent(co::limit);
    getOptMapObject(pros, PRO_KEY_CO_MAX_TIMEMS, ConversionUtils::toLong)
        .ifPresent(t -> co.maxTime(t, TimeUnit.MILLISECONDS));
    getOptMapObject(pros, PRO_KEY_CO_SKIP, ConversionUtils::toInteger).ifPresent(co::skip);
    resovleCollation(querier).ifPresent(co::collation);
    if (co.getLimit() <= 0) {
      co.limit(max(resolveCountOptionsLimit(), 1));
    }
    Bson bson = querier.getScript(null).getOrDefault(MgOperator.FILTER, new BasicDBObject());
    return getDataBase().getCollection(resolveCollectionName(querier.getName()))
        .countDocuments(bson, co);
  }

  protected String resolveCollectionName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }

  protected int resolveCountOptionsLimit() {
    return 1024;
  }

  protected Optional<Collation> resovleCollation(MgNamedQuerier querier) {
    Map<String, String> pros = querier.getQuery().getProperties();
    if (pros.keySet().stream().anyMatch(t -> t.startsWith(PRO_KEY_CO_COLA))) {
      Collation.Builder b = Collation.builder();
      getOptMapObject(pros, PRO_KEY_CO_COLA_ALTERNATE, t -> toEnum(t, CollationAlternate.class))
          .ifPresent(b::collationAlternate);
      getOptMapObject(pros, PRO_KEY_CO_COLA_BACKWORDS, t -> toBoolean(t)).ifPresent(b::backwards);
      getOptMapObject(pros, PRO_KEY_CO_COLA_CASE_FIRST, t -> toEnum(t, CollationCaseFirst.class))
          .ifPresent(b::collationCaseFirst);
      getOptMapObject(pros, PRO_KEY_CO_COLA_CASE_LEVEL, t -> toBoolean(t)).ifPresent(b::caseLevel);
      getOptMapObject(pros, PRO_KEY_CO_COLA_LOCALE, t -> ConversionUtils.toString(t))
          .ifPresent(b::locale);
      getOptMapObject(pros, PRO_KEY_CO_COLA_MAXVAR, t -> toEnum(t, CollationMaxVariable.class))
          .ifPresent(b::collationMaxVariable);
      getOptMapObject(pros, PRO_KEY_CO_COLA_NORMA, t -> toBoolean(t)).ifPresent(b::normalization);
      getOptMapObject(pros, PRO_KEY_CO_COLA_NUMORD, t -> toBoolean(t))
          .ifPresent(b::numericOrdering);
      getOptMapObject(pros, PRO_KEY_CO_COLA_STRENGTH, t -> toEnum(t, CollationStrength.class))
          .ifPresent(b::collationStrength);
      return Optional.of(b.build());
    }

    return Optional.empty();
  }
}
