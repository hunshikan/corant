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
package org.corant.suites.ddd.unitwork;

import static org.corant.shared.util.Objects.areEqual;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.suites.bundle.exception.GeneralRuntimeException;
import org.corant.suites.cdi.CDIs;
import org.corant.suites.ddd.annotation.qualifier.AggregateType.AggregateTypeLiteral;
import org.corant.suites.ddd.event.AggregatePersistEvent;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.MessageUtils;
import org.corant.suites.ddd.model.AbstractAggregate.DefaultAggregateIdentifier;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;
import org.corant.suites.ddd.model.Entity.EntityManagerProvider;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 *
 * corant-suites-ddd
 *
 * <p>
 * Abstract unit of work implementation, using JPA as a persistent implementation; this
 * implementation records the life cycle changes of entities and provide related registration and
 * deregistration operations, also provide the {@link EntityManager}.
 * </p>
 * <p>
 * Note that in the current implementation, a unit of work can contain multiple EntityManagers, each
 * of which has a specific {@link PersistenceContext}.
 * </p>
 *
 * @author bingo 下午7:13:58
 *
 */
public abstract class AbstractJPAUnitOfWork implements UnitOfWork, EntityManagerProvider {

  static final boolean USE_MANUAL_FLUSH_MODEL = ConfigProvider.getConfig()
      .getOptionalValue("ddd.unitofwork.use-manual-flush", Boolean.class).orElse(Boolean.FALSE);

  protected final Logger logger = Logger.getLogger(this.getClass().toString());

  protected final UnitOfWorksManager manager;
  protected final Map<PersistenceContext, EntityManager> entityManagers = new HashMap<>();
  protected final Map<AggregateIdentifier, Lifecycle> registeredAggregates = new LinkedHashMap<>();
  protected final Map<AggregateIdentifier, Lifecycle> evolutiveAggregates = new LinkedHashMap<>();
  protected final Map<Object, Object> registeredVariables = new LinkedHashMap<>();
  protected final LinkedList<Message> registeredMessages = new LinkedList<>();

  protected volatile boolean activated = false;

  protected AbstractJPAUnitOfWork(UnitOfWorksManager manager) {
    this.manager = manager;
    activated = true;
  }

  @Override
  public void complete(boolean success) {
    activated = false;
  }

  @Override
  public void deregister(Object obj) {
    if (isActivated()) {
      if (obj instanceof Aggregate) {
        Aggregate aggregate = (Aggregate) obj;
        if (aggregate.getId() != null) {
          AggregateIdentifier ai = new DefaultAggregateIdentifier(aggregate);
          registeredAggregates.remove(ai);
          evolutiveAggregates.remove(ai);
          registeredMessages.removeIf(e -> areEqual(e.getMetadata().getSource(), ai));
        }
      } else if (obj instanceof Message) {
        registeredMessages.remove(obj);
      } else if (obj instanceof Map.Entry<?, ?>) {
        Map.Entry<?, ?> p = (Map.Entry<?, ?>) obj;
        registeredVariables.remove(p.getKey());
      }
    } else {
      throw new GeneralRuntimeException(PkgMsgCds.ERR_UOW_NOT_ACT);
    }
  }

  /**
   * Aggregates in the scope of the current unit of work.
   *
   * @return the registered aggregates
   */
  public Map<AggregateIdentifier, Lifecycle> getAggregates() {
    return Collections.unmodifiableMap(registeredAggregates);
  }

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    return entityManagers.computeIfAbsent(pc, manager.getPersistenceService()::getEntityManager);
  }

  /**
   * Message queue collected by the current unit of work.
   *
   * @return the registered messsages
   */
  public List<Message> getMessages() {
    return Collections.unmodifiableList(registeredMessages);
  }

  @Override
  public Registration getRegistrations() {
    return new Registration(this);
  }

  /**
   * Variables in the scope of the current unit of work.
   *
   * @return the registered variables
   */
  public Map<Object, Object> getVariables() {
    return Collections.unmodifiableMap(registeredVariables);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <ul>
   * <b>Parameter descriptions</b>
   * <li>If the parameter is aggregation, the aggregation id and life cycle state, as well as the
   * message queue in the aggregation, are registered.</li>
   * <li>If the parameter is a message, the message is merged into a message queue when the message
   * type is mergeable, and non-mergeable message is added directly to the message queue.</li>
   * <li>If the parameter is a named object (represented by Pair), the object is added to a Map as a
   * key-value pair</li>
   * </ul>
   *
   * @see AggregateIdentifier
   * @see Pair
   * @see Message
   */
  @Override
  public void register(Object obj) {
    if (isActivated()) {
      if (obj instanceof Aggregate) {
        Aggregate aggregate = (Aggregate) obj;
        if (aggregate.getId() != null) {
          AggregateIdentifier ai = new DefaultAggregateIdentifier(aggregate);
          Lifecycle al = aggregate.getLifecycle();
          registeredAggregates.put(ai, al);
          if (al.signFlushed()) {
            evolutiveAggregates.put(ai, al);
          }
          for (Message message : aggregate.extractMessages(true)) {
            MessageUtils.mergeToQueue(registeredMessages, message);
          }
        }
      } else if (obj instanceof Message) {
        MessageUtils.mergeToQueue(registeredMessages, (Message) obj);
      } else if (obj instanceof Map.Entry<?, ?>) {
        Map.Entry<?, ?> p = (Map.Entry<?, ?>) obj;
        registeredVariables.put(p.getKey(), p.getValue());
      }
    } else {
      throw new GeneralRuntimeException(PkgMsgCds.ERR_UOW_NOT_ACT);
    }
  }

  protected void clear() {
    entityManagers.values().forEach(em -> {
      if (em.isOpen()) {
        em.close();
      }
    });
    evolutiveAggregates.clear();
    registeredAggregates.clear();
    registeredMessages.clear();
    registeredVariables.clear();
  }

  protected boolean extractMessages(LinkedList<Message> messages) {
    if (!registeredMessages.isEmpty()) {
      registeredMessages.stream().sorted(Message::compare).forEach(messages::offer);
      registeredMessages.clear();
      return true;
    }
    return false;
  }

  protected UnitOfWorksManager getManager() {
    return manager;
  }

  protected void handlePostCompleted(final boolean success) {
    final Registration registration = new Registration(this);
    manager.getListeners().forEach(listener -> {
      try {
        listener.onCompleted(registration, success);
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex, () -> "Handle UOW post-completed occurred error!");
      }
    });
    if (success) {
      evolutiveAggregates.forEach((k, v) -> {
        if (v.signFlushed()) {
          try {
            CDIs.fireAsyncEvent(new AggregatePersistEvent(k, v),
                AggregateTypeLiteral.of(k.getTypeCls()));
          } catch (Exception ex) {
            logger.log(Level.WARNING, ex, () -> "Fire persist event occurred error!");
          }
        }
      });
    }
  }

  protected void handlePreComplete() {
    manager.getHandlers().forEach(handler -> {
      try {
        handler.onPreComplete(this);
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex, () -> "Handle UOW pre-complete occurred error!");
      }
    });
  }

  protected boolean isActivated() {
    return activated;
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 上午9:50:36
   *
   */
  public static class Registration {

    final Map<AggregateIdentifier, Lifecycle> aggregates;
    final Map<Object, Object> variables;

    Registration(AbstractJPAUnitOfWork uow) {
      aggregates = Collections.unmodifiableMap(new LinkedHashMap<>(uow.registeredAggregates));
      variables = Collections.unmodifiableMap(new LinkedHashMap<>(uow.registeredVariables));
    }

    public Map<AggregateIdentifier, Lifecycle> getAggregates() {
      return aggregates;
    }

    public Map<Object, Object> getVariables() {
      return variables;
    }
  }
}
