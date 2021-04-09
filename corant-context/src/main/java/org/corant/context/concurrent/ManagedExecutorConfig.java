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
package org.corant.context.concurrent;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.defaultString;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.concurrent.ContextServiceConfig.ContextInfo;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.Config;
import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy;

/**
 * corant-context
 *
 * @author bingo 下午7:56:44
 *
 */
@ConfigKeyRoot(value = "corant.concurrent.executor", ignoreNoAnnotatedItem = false, keyIndex = 3)
public class ManagedExecutorConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = -1732163277606881747L;

  public static final String DFLT_NAME = Names.CORANT.toUpperCase(Locale.ROOT);
  public static final ManagedExecutorConfig DFLT_INST = new ManagedExecutorConfig(DFLT_NAME);

  protected boolean longRunningTasks = false;
  protected long hungTaskThreshold = 60000L;// millis
  protected int corePoolSize = Systems.getCPUs() << 1;
  protected int maxPoolSize = Systems.getCPUs() << 2;
  protected Duration keepAliveTime = Duration.ofSeconds(5L);
  protected Duration threadLifeTime = Duration.ofSeconds(30L);
  protected Duration awaitTermination = Duration.ofSeconds(5L);
  protected RejectPolicy rejectPolicy = RejectPolicy.ABORT;
  protected int threadPriority = Thread.NORM_PRIORITY;
  protected String threadName;
  protected int queueCapacity = Integer.MAX_VALUE;
  protected ContextInfo[] contextInfos = ContextInfo.values();
  protected boolean enableJndi = false;

  public ManagedExecutorConfig() {}

  private ManagedExecutorConfig(String name) {
    setName(name);
  }

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
    ManagedExecutorConfig other = (ManagedExecutorConfig) obj;
    if (name == null) {
      return other.name == null;
    } else {
      return name.equals(other.name);
    }
  }

  /**
   *
   * @return the awaitTermination
   */
  public Duration getAwaitTermination() {
    return awaitTermination;
  }

  public ContextInfo[] getContextInfos() {
    return Arrays.copyOf(contextInfos, contextInfos.length);
  }

  public int getCorePoolSize() {
    return max(corePoolSize, Systems.getCPUs());
  }

  public long getHungTaskThreshold() {
    return hungTaskThreshold <= 0 ? 60000L : hungTaskThreshold;
  }

  public Duration getKeepAliveTime() {
    return defaultObject(keepAliveTime, () -> Duration.ofSeconds(5));
  }

  public int getMaxPoolSize() {
    return maxPoolSize <= getCorePoolSize() ? getCorePoolSize() : maxPoolSize;
  }

  @Override
  public String getName() {
    return name;
  }

  public int getQueueCapacity() {
    return queueCapacity < 0 ? Integer.MAX_VALUE : queueCapacity;
  }

  public RejectPolicy getRejectPolicy() {
    return defaultObject(rejectPolicy, () -> RejectPolicy.ABORT);
  }

  public Duration getThreadLifeTime() {
    return threadLifeTime;
  }

  public String getThreadName() {
    return defaultString(threadName, defaultString(name, DFLT_NAME));
  }

  public int getThreadPriority() {
    return threadPriority <= 0 ? Thread.NORM_PRIORITY : threadPriority;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    return result;
  }

  /**
   *
   * @return the enableJndi
   */
  public boolean isEnableJndi() {
    return enableJndi;
  }

  public boolean isLongRunningTasks() {
    return longRunningTasks;
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }

  @Override
  public String toString() {
    return "ManagedExecutorConfig [longRunningTasks=" + longRunningTasks + ", hungTaskThreshold="
        + hungTaskThreshold + ", corePoolSize=" + corePoolSize + ", maxPoolSize=" + maxPoolSize
        + ", keepAliveTime=" + keepAliveTime + ", threadLifeTime=" + threadLifeTime
        + ", awaitTermination=" + awaitTermination + ", rejectPolicy=" + rejectPolicy
        + ", threadPriority=" + threadPriority + ", threadName=" + threadName + ", queueCapacity="
        + queueCapacity + ", contextInfos=" + Arrays.toString(contextInfos) + ", enableJndi="
        + enableJndi + "]";
  }

  protected void setAwaitTermination(Duration awaitTermination) {
    this.awaitTermination = awaitTermination;
  }

  protected void setContextInfos(ContextInfo[] contextInfos) {
    this.contextInfos = contextInfos;
  }

  protected void setCorePoolSize(int corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  protected void setEnableJndi(boolean enableJndi) {
    this.enableJndi = enableJndi;
  }

  protected void setHungTaskThreshold(long hungTaskThreshold) {
    this.hungTaskThreshold = hungTaskThreshold;
  }

  protected void setKeepAliveTime(Duration keepAliveTime) {
    this.keepAliveTime = keepAliveTime;
  }

  protected void setLongRunningTasks(boolean longRunningTasks) {
    this.longRunningTasks = longRunningTasks;
  }

  protected void setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  protected void setQueueCapacity(int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

  protected void setRejectPolicy(RejectPolicy rejectPolicy) {
    this.rejectPolicy = rejectPolicy;
  }

  protected void setThreadLifeTime(Duration threadLifeTime) {
    this.threadLifeTime = threadLifeTime;
  }

  protected void setThreadName(String threadName) {
    this.threadName = threadName;
  }

  protected void setThreadPriority(int threadPriority) {
    this.threadPriority = threadPriority;
  }

}
