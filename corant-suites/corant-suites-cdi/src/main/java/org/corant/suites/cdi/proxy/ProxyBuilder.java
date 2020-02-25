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
package org.corant.suites.cdi.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.suites.cdi.proxy.ProxyInvocationHandler.MethodInvoker;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午5:03:55
 *
 */
public class ProxyBuilder {

  @SuppressWarnings("unchecked")
  public static <T> T build(final Class<?> clazz,
      final Function<Method, MethodInvoker> invokerHandler) {
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz},
        new ProxyInvocationHandler(clazz, invokerHandler));
  }

  @SuppressWarnings("unchecked")
  public static <T> T buildContextual(final BeanManager beanManager, final Class<?> clazz,
      final Function<Method, MethodInvoker> invokerHandler) {
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz},
        new ContextualInvocationHandler(beanManager, clazz, invokerHandler));
  }

  public static Set<ProxyMethod> buildMethods(AnnotatedType<?> annotatedType,
      Predicate<AnnotatedMethod<?>> methodPredicate) {
    Set<ProxyMethod> annotatedMethods = new LinkedHashSet<>();
    for (AnnotatedMethod<?> am : annotatedType.getMethods()) {
      if (methodPredicate.test(am)) {
        annotatedMethods.add(new ProxyMethod(am));
      }
    }
    return annotatedMethods;
  }
}
