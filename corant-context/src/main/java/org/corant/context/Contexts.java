/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.context;

import static org.corant.context.Beans.tryResolve;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import org.corant.config.Configs;
import org.jboss.weld.context.BoundContext;
import org.jboss.weld.context.WeldAlterableContext;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.MutableBoundRequest;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-context
 *
 * @author bingo 下午2:16:28
 *
 */
public class Contexts {

  public static final String WELD_INTERCEPTOR_BINDINGS_KEY = "org.jboss.weld.interceptor.bindings";

  static final boolean propagateStrictly =
      Configs.getValue("corant.context.propagate.strictly", Boolean.class, Boolean.FALSE);

  /**
   * Returns whether there is an active context for all given scope types.
   *
   * @param scopeTypes
   * @return areContextsActive
   */
  public static boolean areContextsActive(
      @SuppressWarnings("unchecked") Class<? extends Annotation>... scopeTypes) {
    final WeldManager wm;
    return isNotEmpty(scopeTypes) && (wm = tryResolve(WeldManager.class)) != null
        && Arrays.stream(scopeTypes).allMatch(wm::isContextActive);
  }

  /**
   * Captures a snapshot of the set of contextual instances for the currently active
   * WeldAlterableContexts for the Request, Session, and Conversation scope.
   *
   * @return capture
   */
  public static ContextSnapshot capture() {
    return capture(tryResolve(WeldManager.class));
  }

  /**
   * Captures a snapshot of the set of contextual instances for the currently active
   * WeldAlterableContexts for the Request, Session, and Conversation scope.
   *
   * @param weldManager
   * @return capture
   */
  public static ContextSnapshot capture(WeldManager weldManager) {
    return weldManager == null ? ContextSnapshot.EMPTY_INST : new ContextSnapshot(weldManager);
  }

  public static ContextInstaller createInstaller(boolean propagate) {
    return createInstaller(propagate, tryResolve(WeldManager.class));
  }

  public static ContextInstaller createInstaller(boolean propagate, WeldManager manager) {
    return new ContextInstaller(propagate, manager);
  }

  /**
   * Returns whether there is an active context for a given scope type.
   *
   * @param scopeType
   * @return isContextActive
   */
  public static boolean isContextActive(Class<? extends Annotation> scopeType) {
    final WeldManager wm;
    return scopeType != null && (wm = tryResolve(WeldManager.class)) != null
        && wm.isContextActive(scopeType);
  }

  /**
   * corant-context
   * <p>
   * CDI context installer for Weld. In general, create {@link ContextRestorer} instance in
   * propagator thread, invoke {@link #install()} method in other thread(task thread) and hold the
   * return {@link ContextRestorer}, invoke {@link ContextRestorer#restore()} after task completed.
   * <p>
   * Only Request, Session, Conversation contexts can be propagated. Application, Singleton,
   * Dependent contexts are work out of the box, no propagation needed.
   *
   * <p>
   * <b>Note</b>: This is only suitable for Weld and cannot be used for other CDI implementations.
   * {@link javax.annotation.PreDestroy} / {@link javax.enterprise.inject.Disposes} on your beans
   * could cause inconsistent state based on how you perform the propagation. Since the same bean is
   * now used in several threads, all of them can, in invalidating and deactivating contexts,
   * trigger these methods but the bean will still exist in yet another thread. In current
   * implemention, we avoid calling context.invalidate() and only performs context.deactivate() -
   * this avoids invoking {@link javax.annotation.PreDestroy} /
   * {@link javax.enterprise.inject.Disposes} methods but could possibly lead to never invoking them
   * if no thread does it. Note that this problem only concerns request, session and conversation
   * beans.
   *
   * @see <a href=
   *      "https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#contexts">Weld
   *      Context Management</a>
   *
   * @see <a href=
   *      "https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_available_contexts_in_weld">Available
   *      Contexts in Weld</a>
   *
   * @see <a href=
   *      "https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_pitfalls_and_drawbacks">Pitfalls
   *      and drawbacks</a>
   *
   * @author bingo 下午2:23:45
   *
   */
  public static class ContextInstaller {

    static final Logger logger = Logger.getLogger(ContextInstaller.class.getName());

    final boolean propagate;
    final WeldManager manager;
    final ContextSnapshot contextToApply;

    public ContextInstaller(boolean propagate, WeldManager manager) {
      this.propagate = propagate;
      this.manager = manager;
      contextToApply = propagate ? capture(manager) : ContextSnapshot.EMPTY_INST;
      logger.finer(
          () -> String.format("Create CDI context installer with propagate=%s, context to apply %s",
              propagate, contextToApply));
    }

    /**
     *
     * @return the contextToApply
     */
    public ContextSnapshot getContextToApply() {
      return contextToApply;
    }

    /**
     * Install the context and return the restore handle, which is used when the task ends.
     */
    public ContextRestorer install() {
      final ContextSnapshot existingContexts = capture(manager);
      logger.finer(() -> String.format("Capture current thread CDI context %s", existingContexts));
      BoundRequestContext requestCtx =
          resolveBoundContext(existingContexts.getRequestContext(), BoundRequestContext.class);
      BoundSessionContext sessionCtx =
          resolveBoundContext(existingContexts.getSessionContext(), BoundSessionContext.class);
      BoundConversationContext conversationCtx = resolveBoundContext(
          existingContexts.getConversationContext(), BoundConversationContext.class);
      logger.finer(() -> String
          .format("Propagate captured context %s to current thread if necessary", contextToApply));
      Map<String, Object> requestMap = new HashMap<>();
      Map<String, Object> sessionMap = new HashMap<>();
      if (existingContexts.getRequestContext() != null) {
        existingContexts.getRequestContext().clearAndSet(contextToApply.getRequestInstances());
      } else {
        requestCtx.associate(requestMap);
        requestCtx.activate();
        requestCtx.clearAndSet(contextToApply.getRequestInstances());
      }

      if (existingContexts.getSessionContext() != null) {
        existingContexts.getSessionContext().clearAndSet(contextToApply.getSessionInstances());
      } else {
        sessionCtx.associate(sessionMap);
        sessionCtx.activate();
        sessionCtx.clearAndSet(contextToApply.getSessionInstances());
      }

      if (existingContexts.getConversationContext() != null) {
        existingContexts.getConversationContext()
            .clearAndSet(contextToApply.getConversationInstances());
      } else {
        conversationCtx.associate(new MutableBoundRequest(requestMap, sessionMap));
        conversationCtx.activate();
        conversationCtx.clearAndSet(contextToApply.getConversationInstances());
      }

      return () -> {
        ContextSnapshot afterTaskContexts =
            propagate && propagateStrictly ? capture(manager) : ContextSnapshot.EMPTY_INST;

        if (existingContexts.getRequestContext() != null) {
          existingContexts.getRequestContext().clearAndSet(existingContexts.getRequestInstances());
        } else {
          requestCtx.deactivate();
        }

        if (existingContexts.getSessionContext() != null) {
          existingContexts.getSessionContext().clearAndSet(existingContexts.getSessionInstances());
        } else {
          sessionCtx.deactivate();
        }

        if (existingContexts.getConversationContext() != null) {
          existingContexts.getConversationContext()
              .clearAndSet(existingContexts.getConversationInstances());
        } else {
          conversationCtx.deactivate();
        }
        logger.finer(() -> String.format(
            "Restore thread CDI context %s to current thread if necessary", existingContexts));
        if (propagate && propagateStrictly
            && contextToApply.getBeanCount() != afterTaskContexts.getBeanCount()) {
          Set<ContextualInstance<?>> diffRegisteredBeans =
              new HashSet<>(afterTaskContexts.getRequestInstances());
          diffRegisteredBeans.addAll(afterTaskContexts.getSessionInstances());
          diffRegisteredBeans.addAll(afterTaskContexts.getConversationInstances());
          diffRegisteredBeans.removeAll(contextToApply.getRequestInstances());
          diffRegisteredBeans.removeAll(contextToApply.getSessionInstances());
          diffRegisteredBeans.removeAll(contextToApply.getConversationInstances());
          throw new IllegalStateException(String.format(
              "The following CDI beans must be reachable before the context was captured: %s",
              diffRegisteredBeans));
        }
      };
    }

    <T extends BoundContext<?>> T resolveBoundContext(WeldAlterableContext ctx, Class<T> ctxCls) {
      return ctx == null && manager != null
          ? manager.instance().select(ctxCls, BoundLiteral.INSTANCE).get()
          : null;
    }
  }

  @FunctionalInterface
  public interface ContextRestorer {
    void restore();
  }

  /**
   * corant-context
   *
   * @author bingo 下午2:27:08
   *
   */
  public static class ContextSnapshot {

    public static final ContextSnapshot EMPTY_INST = new ContextSnapshot();

    final WeldAlterableContext requestContext;
    final WeldAlterableContext sessionContext;
    final WeldAlterableContext conversationContext;
    final Collection<ContextualInstance<?>> requestInstances;
    final Collection<ContextualInstance<?>> sessionInstances;
    final Collection<ContextualInstance<?>> conversationInstances;

    public ContextSnapshot(WeldManager manager) {
      WeldAlterableContext reqCtx = null;
      WeldAlterableContext sesCtx = null;
      WeldAlterableContext conCtx = null;
      Collection<ContextualInstance<?>> reqInsts = null;
      Collection<ContextualInstance<?>> sesInsts = null;
      Collection<ContextualInstance<?>> conInsts = null;
      for (WeldAlterableContext ctx : manager.getActiveWeldAlterableContexts()) {
        Class<?> scope = ctx.getScope();
        if (scope == RequestScoped.class) {
          reqInsts = ctx.getAllContextualInstances();
          reqCtx = ctx;
        } else if (scope == SessionScoped.class) {
          sesInsts = ctx.getAllContextualInstances();
          sesCtx = ctx;
        } else if (scope == ConversationScoped.class) {
          conInsts = ctx.getAllContextualInstances();
          conCtx = ctx;
        }
      }
      requestContext = reqCtx;
      sessionContext = sesCtx;
      conversationContext = conCtx;
      requestInstances = defaultObject(reqInsts, Collections::emptySet);
      sessionInstances = defaultObject(sesInsts, Collections::emptySet);
      conversationInstances = defaultObject(conInsts, Collections::emptySet);
    }

    private ContextSnapshot() {
      requestInstances = sessionInstances = conversationInstances = Collections.emptySet();
      requestContext = sessionContext = conversationContext = null;
    }

    public int getBeanCount() {
      return requestInstances.size() + sessionInstances.size() + conversationInstances.size();
    }

    public WeldAlterableContext getConversationContext() {
      return conversationContext;
    }

    public Collection<ContextualInstance<?>> getConversationInstances() {
      return conversationInstances;
    }

    public WeldAlterableContext getRequestContext() {
      return requestContext;
    }

    public Collection<ContextualInstance<?>> getRequestInstances() {
      return requestInstances;
    }

    public WeldAlterableContext getSessionContext() {
      return sessionContext;
    }

    public Collection<ContextualInstance<?>> getSessionInstances() {
      return sessionInstances;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(super.toString()).append(" beanCount=" + getBeanCount())
          .append('\n').append("RequestScoped instances = ").append(requestInstances).append('\n')
          .append("SessionScoped instances = ").append(requestInstances).append('\n')
          .append("ConversationScoped instances = ").append(requestInstances);
      return sb.toString();
    }
  }
}
