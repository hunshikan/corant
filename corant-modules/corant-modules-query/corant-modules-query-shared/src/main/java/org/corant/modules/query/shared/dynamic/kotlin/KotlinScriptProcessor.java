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
package org.corant.modules.query.shared.dynamic.kotlin;

import javax.inject.Singleton;
import javax.script.Compilable;
import org.corant.modules.lang.kotlin.KotlinScriptEngines;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.ScriptProcessor.CompilableScriptProcessor;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午2:11:37
 *
 */
@Singleton
public class KotlinScriptProcessor extends CompilableScriptProcessor {

  @Override
  public boolean supports(Script script) {
    return script != null && script.getType() == ScriptType.KT;
  }

  @Override
  protected Compilable getCompilable(ScriptType type) {
    return (Compilable) KotlinScriptEngines.createEngine();
  }

}
