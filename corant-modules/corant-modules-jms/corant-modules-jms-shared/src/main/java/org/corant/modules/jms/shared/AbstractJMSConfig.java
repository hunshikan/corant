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
package org.corant.modules.jms.shared;

import static org.corant.shared.util.Objects.max;
import java.time.Duration;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Qualifiers.NamedObject;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午10:30:53
 *
 */
@ConfigKeyRoot(value = "corant.jms", keyIndex = 2)
public abstract class AbstractJMSConfig implements NamedObject, DeclarativeConfig {

  private static final long serialVersionUID = 2263743463205278263L;

  public static final AbstractJMSConfig DFLT_INSTANCE = new AbstractJMSConfig() {

    private static final long serialVersionUID = 5340760550873711017L;
  };

  // the connection factory id means a artemis server or cluster
  @ConfigKeyItem
  protected String connectionFactoryId = Qualifiers.EMPTY_NAME;

  @ConfigKeyItem
  protected String username;

  @ConfigKeyItem
  protected String password;

  @ConfigKeyItem
  protected String clientId;

  @ConfigKeyItem
  protected Boolean enable = true;

  @ConfigKeyItem
  protected Boolean xa = true;

  @ConfigKeyItem(defaultValue = "PT30S")
  protected Duration receiveTaskInitialDelay;

  @ConfigKeyItem(defaultValue = "PT1S")
  protected Duration receiveTaskDelay;

  @ConfigKeyItem(defaultValue = "PT4S")
  protected Duration receiverExecutorAwaitTermination;

  @ConfigKeyItem(defaultValue = "2")
  protected Integer receiveTaskThreads = max(2, Runtime.getRuntime().availableProcessors());

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractJMSConfig other = (AbstractJMSConfig) obj;
    if (connectionFactoryId == null) {
      if (other.connectionFactoryId != null) {
        return false;
      }
    } else if (!connectionFactoryId.equals(other.connectionFactoryId)) {
      return false;
    }
    return true;
  }

  public String getClientId() {
    return clientId;
  }

  /**
   *
   * @return the connectionFactoryId
   */
  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  @Override
  public String getName() {
    return connectionFactoryId;
  }

  public String getPassword() {
    return password;
  }

  /**
   *
   * @return the receiverExecutorAwaitTermination
   */
  public Duration getReceiverExecutorAwaitTermination() {
    return receiverExecutorAwaitTermination;
  }

  /**
   *
   * @return the receiveTaskDelay
   */
  public Duration getReceiveTaskDelay() {
    return receiveTaskDelay;
  }

  /**
   *
   * @return the receiveTaskInitialDelay
   */
  public Duration getReceiveTaskInitialDelay() {
    return receiveTaskInitialDelay;
  }

  /**
   *
   * @return the receiveTaskThreads
   */
  public Integer getReceiveTaskThreads() {
    return receiveTaskThreads;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (connectionFactoryId == null ? 0 : connectionFactoryId.hashCode());
    return result;
  }

  /**
   *
   * @return the enable
   */
  public Boolean isEnable() {
    return enable;
  }

  /**
   *
   * @return the xa
   */
  public Boolean isXa() {
    return xa;
  }

}
