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

import static org.corant.shared.util.Conversions.toObject;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * corant-context
 *
 * @author bingo 下午12:00:56
 *
 */
public interface Principal extends java.security.Principal, Serializable {

  default Map<String, ? extends Serializable> getProperties() {
    return Collections.emptyMap();
  }

  default <T> T getProperty(String propertyName, Class<T> propertyType) {
    return null;
  }

  class DefaultPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 282297555381317944L;

    protected String name;
    protected Map<String, ? extends Serializable> properties = Collections.emptyMap();

    /**
     * @param name
     * @param properties
     */
    public DefaultPrincipal(String name, Map<String, ? extends Serializable> properties) {
      this.name = name;
      if (properties != null) {
        this.properties = Collections.unmodifiableMap(properties);
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Map<String, ? extends Serializable> getProperties() {
      return properties;
    }

    @Override
    public <T> T getProperty(String name, Class<T> type) {
      Object property;
      if (properties != null && (property = properties.get(name)) != null) {
        return toObject(property, type);
      }
      return null;
    }

    @Override
    public String toString() {
      return "DefaultPrincipal [name=" + name + ", properties=" + properties + "]";
    }
  }

}
