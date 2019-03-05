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
package org.corant.suites.jpa.shared;

import static org.corant.shared.util.Assertions.shouldBeNull;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import org.corant.shared.normal.Names.JndiNames;
import org.corant.shared.util.Resources;
import org.corant.shared.util.StringUtils;
import org.corant.suites.jpa.shared.metadata.PersistencePropertiesParser;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceXmlParser;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jpa-shared
 *
 * TODO Support unnamed persistence unit
 *
 * @author bingo 上午10:32:07
 *
 */
public class JpaConfig {

  public static final String DFLT_PU_XML_LOCATION = "META-INF/persistence.xml";
  public static final String DFLT_ORM_XML_LOCATION = "META-INF/*JpaOrm.xml";
  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/EntityManagerFactories";
  public static final String EMPTY_PU_NAME = StringUtils.EMPTY;

  public static final String JC_PREFIX = "jpa.";

  public static final String JCX_TAG = "persistence-unit";
  public static final String JCX_NME = "name";
  public static final String JCX_TRANS_TYP = "transaction-type";
  public static final String JCX_NON_JTA_DS = "non-jta-data-source";
  public static final String JCX_JTA_DS = "jta-data-source";
  public static final String JCX_PROVIDER = "provider";
  public static final String JCX_CLS = "class";
  public static final String JCX_MAP_FILE = "mapping-file";
  public static final String JCX_JAR_FILE = "jar-file";
  public static final String JCX_EX_UL_CLS = "exclude-unlisted-classes";
  public static final String JCX_VAL_MOD = "validation-mode";
  public static final String JCX_SHARE_CACHE_MOD = "shared-cache-mode";
  public static final String JCX_PROS = "properties";
  public static final String JCX_PRO = "property";
  public static final String JCX_PRO_NME = "name";
  public static final String JCX_PRO_VAL = "value";

  public static final String JC_PU_NME = "." + JCX_TAG + "." + JCX_NME;// persistence-unit.name
  public static final String JC_TRANS_TYP = "." + JCX_TRANS_TYP;
  public static final String JC_NON_JTA_DS = "." + JCX_NON_JTA_DS;
  public static final String JC_JTA_DS = "." + JCX_JTA_DS;
  public static final String JC_PROVIDER = "." + JCX_PROVIDER;
  public static final String JC_CLS = "." + JCX_CLS;
  public static final String JC_CLS_PKG = JC_CLS + "-packages";
  public static final String JC_MAP_FILE = "." + JCX_MAP_FILE;
  public static final String JC_MAP_FILE_PATH = "." + JCX_MAP_FILE + ".paths";
  public static final String JC_JAR_FILE = "." + JCX_JAR_FILE;
  public static final String JC_EX_UL_CLS = "." + JCX_EX_UL_CLS;
  public static final String JC_VAL_MOD = "." + JCX_VAL_MOD;
  public static final String JC_SHARE_CACHE_MOD = "." + JCX_SHARE_CACHE_MOD;
  public static final String JC_PRO = "." + "property";

  protected static final Logger logger = Logger.getLogger(JpaConfig.class.getName());

  public static Set<String> defaultPropertyNames() {
    String dfltPrefix = JC_PREFIX.substring(0, JC_PREFIX.length() - 1);
    Set<String> names = new LinkedHashSet<>();
    names.add(dfltPrefix + JC_CLS);
    names.add(dfltPrefix + JC_CLS_PKG);
    names.add(dfltPrefix + JC_EX_UL_CLS);
    names.add(dfltPrefix + JC_JAR_FILE);
    names.add(dfltPrefix + JC_JTA_DS);
    names.add(dfltPrefix + JC_MAP_FILE);
    names.add(dfltPrefix + JC_MAP_FILE_PATH);
    names.add(dfltPrefix + JC_NON_JTA_DS);
    names.add(dfltPrefix + JC_PRO);
    names.add(dfltPrefix + JC_PROVIDER);
    names.add(dfltPrefix + JC_SHARE_CACHE_MOD);
    names.add(dfltPrefix + JC_TRANS_TYP);
    names.add(dfltPrefix + JC_VAL_MOD);
    return names;
  }

  public static Map<String, PersistenceUnitInfoMetaData> from(Config config) {
    Map<String, PersistenceUnitInfoMetaData> metaDatas = new LinkedHashMap<>();
    generateFromXml().forEach(metaDatas::put);
    generateFromConfig(config).forEach(
        (n, u) -> shouldBeNull(metaDatas.put(n, u), "The persistence unit name %s is dup!", n));
    return metaDatas;
  }

  public static Optional<? extends PersistenceProvider> resolvePersistenceProvider() {
    return PersistenceProviderResolverHolder.getPersistenceProviderResolver()
        .getPersistenceProviders().stream().findFirst();
  }

  private static Map<String, PersistenceUnitInfoMetaData> generateFromConfig(Config config) {
    return PersistencePropertiesParser.parse(config);
  }

  private static Map<String, PersistenceUnitInfoMetaData> generateFromXml() {
    Map<String, PersistenceUnitInfoMetaData> map = new LinkedHashMap<>();
    try {
      Resources.fromClassPath(DFLT_PU_XML_LOCATION).map(r -> r.getUrl())
          .map(PersistenceXmlParser::parse).forEach(m -> {
            map.putAll(m);
          });
    } catch (IOException e) {
      logger.warning(() -> String.format("Parse persistence unit meta data from %s error %s",
          DFLT_PU_XML_LOCATION, e.getMessage()));
    }
    return map;
  }

}
