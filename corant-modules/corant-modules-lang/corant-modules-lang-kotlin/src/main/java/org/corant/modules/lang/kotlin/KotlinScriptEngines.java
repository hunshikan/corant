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
package org.corant.modules.lang.kotlin;

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Strings.isBlank;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Systems;

/**
 * corant-modules-lang-kotlin
 *
 * @author bingo 下午12:58:15
 *
 */
public class KotlinScriptEngines {

  static {
    Systems.setProperty("idea.use.native.fs.for.win", "false");
  }

  public static final ScriptEngineManager ENGINE_MANAGER =
      new ScriptEngineManager(defaultClassLoader());

  public static final ThreadLocal<ScriptEngine> ENGINES =
      ThreadLocal.withInitial(() -> ENGINE_MANAGER.getEngineByExtension("kts"));

  public static Consumer<Object[]> createConsumer(String funcScript, String... paraNames) {
    if (isBlank(funcScript)) {
      return null;
    }
    return pns -> {
      Bindings bindings = new SimpleBindings();
      try {
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        ENGINES.get().eval(funcScript, bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      } finally {
        bindings.clear();
      }
    };
  }

  public static ScriptEngine createEngine() {
    return new ScriptEngineManager(defaultClassLoader()).getEngineByExtension("kts");
  }

  public static Function<Object[], Object> createFunction(String funcScript, String... paraNames) {
    if (isBlank(funcScript)) {
      return null;
    }
    return pns -> {
      Bindings bindings = new SimpleBindings();
      try {
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        return ENGINES.get().eval(funcScript, bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      } finally {
        bindings.clear();
      }
    };
  }

}
