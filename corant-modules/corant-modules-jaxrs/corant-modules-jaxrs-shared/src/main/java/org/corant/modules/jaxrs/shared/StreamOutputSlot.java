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
package org.corant.modules.jaxrs.shared;

import java.io.OutputStream;
import java.util.function.Consumer;
import javax.ws.rs.core.Response;

/**
 * corant-modules-jaxrs-shared
 *
 * @author bingo 上午1:38:31
 *
 */
public class StreamOutputSlot extends AbstractStreamOutputHandler<StreamOutputSlot> {

  public StreamOutputSlot() {}

  public Response connect(Consumer<OutputStream> consumer) {
    return connect(consumer, false);
  }

  public Response connect(Consumer<OutputStream> consumer, boolean loose) {
    return super.handle(consumer::accept, loose);
  }

  @Override
  protected StreamOutputSlot me() {
    return this;
  }

}
