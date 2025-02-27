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
package org.corant.context.required;

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.defaultObject;
import javax.enterprise.inject.spi.AnnotatedType;
import org.corant.shared.service.Required;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-context
 *
 * @author bingo 下午9:24:34
 *
 */
public class RequiredExt extends Required {

  public static final RequiredExt INSTANCE = new RequiredExt();

  public boolean shouldVeto(AnnotatedType<?> type) {
    return shouldVeto(type.getJavaClass());
  }

  @Override
  protected Object getConfigValue(String key, Class<?> valueType, Object dfltNullValue) {
    String value = ConfigProvider.getConfig().getOptionalValue(key, String.class).orElse(null);
    return defaultObject(getConvertValue(value, valueType), dfltNullValue);
  }

  @Override
  protected Object getConvertValue(String value, Class<?> valueType) {
    return toObject(value, valueType);
  }

}
