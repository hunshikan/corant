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
package org.corant.suites.jpa.shared.inject;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.ObjectUtils.isEquals;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.transaction.TransactionScoped;
import org.corant.Corant;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.inject.JpaProvider.JpaProviderLiteral;
import org.corant.suites.jpa.shared.metadata.PersistenceContextMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:34:41
 *
 */
public class EntityManagerBean implements Bean<EntityManager>, PassivationCapable {

  static final Logger logger = Logger.getLogger(EntityManagerBean.class.getName());
  static final Set<Type> types = Collections.unmodifiableSet(asSet(EntityManager.class));
  final Set<Annotation> extenQualifiers = new HashSet<>();
  final Set<Annotation> transQualifiers = new HashSet<>();
  final BeanManager beanManager;
  final PersistenceUnitInfoMetaData persistenceUnitInfoMetaData;
  final PersistenceContextMetaData persistenceContextMetaData;

  /**
   * @param beanManager
   * @param persistenceContextMetaData
   * @param qualifiers
   */
  public EntityManagerBean(BeanManager beanManager,
      PersistenceContextMetaData persistenceContextMetaData, Annotation... qualifiers) {
    super();
    this.beanManager = beanManager;
    this.persistenceContextMetaData = shouldNotNull(persistenceContextMetaData);
    persistenceUnitInfoMetaData = shouldNotNull(persistenceContextMetaData.getUnit());
    shouldBeTrue(isEquals(persistenceUnitInfoMetaData.getPersistenceUnitName(),
        persistenceContextMetaData.getUnitName()));
    transQualifiers.addAll(asSet(qualifiers));
    transQualifiers.addAll(asSet(TransactionPersistenceContextType.INST, Any.Literal.INSTANCE));
    extenQualifiers.addAll(asSet(qualifiers));
    extenQualifiers.addAll(asSet(ExtendedPersistenceContextType.INST, Any.Literal.INSTANCE));
  }

  @Override
  public EntityManager create(CreationalContext<EntityManager> creationalContext) {
    final JpaProviderLiteral proNme =
        JpaProviderLiteral.of(persistenceUnitInfoMetaData.getPersistenceProviderClassName());
    Instance<AbstractJpaProvider> provider =
        Corant.instance().select(AbstractJpaProvider.class, proNme);
    shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", proNme.value());
    final EntityManager em = provider.get().getEntityManager(persistenceContextMetaData);
    logger.fine(
        () -> String.format("Created an entity manager that persistence unit named %s, scope is %s",
            persistenceUnitInfoMetaData.getPersistenceUnitName(), getScope().getSimpleName()));
    return em;
  }

  @Override
  public void destroy(EntityManager instance, CreationalContext<EntityManager> creationalContext) {
    if (instance != null && instance.isOpen()) {
      instance.close();
      logger.fine(
          () -> String.format("Destroyed entity manager that persistence unit named %s scope is %s",
              persistenceUnitInfoMetaData.getPersistenceUnitName(), getScope().getSimpleName()));
    }
  }

  @Override
  public Class<?> getBeanClass() {
    return EntityManagerBean.class;
  }

  @Override
  public String getId() {
    return EntityManagerBean.class.getName() + "."
        + persistenceUnitInfoMetaData.getPersistenceUnitName();
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public String getName() {
    return "EntityManagerBean." + persistenceContextMetaData.getUnitName();
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return persistenceContextMetaData.getType() == PersistenceContextType.EXTENDED ? extenQualifiers
        : transQualifiers;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return persistenceContextMetaData.getType() == PersistenceContextType.EXTENDED ? Dependent.class
        : TransactionScoped.class;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Collections.emptySet();
  }

  @Override
  public Set<Type> getTypes() {
    return types;
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return false;
  }

}
