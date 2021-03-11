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
package org.corant.context.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

/**
 * corant-context
 *
 * @author bingo 下午4:30:10
 *
 */
public class DefaultPrincipal implements Principal, Serializable {

  private static final long serialVersionUID = 282297555381317944L;

  protected String name;
  protected Map<String, String> properties;

  /**
   * @param name
   * @param properties
   */
  public DefaultPrincipal(String name, Map<String, String> properties) {
    this.name = name;
    this.properties = properties;
  }

  @Override
  public String getName() {
    return name;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return "DefaultPrincipal [name=" + name + ", properties=" + properties + "]";
  }

}
