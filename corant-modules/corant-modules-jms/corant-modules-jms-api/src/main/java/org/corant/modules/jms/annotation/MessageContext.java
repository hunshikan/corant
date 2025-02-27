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
package org.corant.modules.jms.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.jms.JMSContext;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午3:43:58
 *
 */
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
public @interface MessageContext {
  /**
   * The connection factory id, used to represent a JMS service or cluster, usually set up through a
   * configuration file.
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   */
  String connectionFactoryId() default EMPTY;

  /**
   * In the Jakarta EE web or EJB container, when there is no active JTA transaction in progress:
   * <p>
   * The argument acknowledgeMode must be set to either of JMSContext.AUTO_ACKNOWLEDGE or
   * JMSContext.DUPS_OK_ACKNOWLEDGE. The session will be non-transacted and messages received by
   * this session will be acknowledged automatically according to the value of acknowledgeMode. For
   * a definition of the meaning of these acknowledgement modes see the links below. The values
   * JMSContext.SESSION_TRANSACTED and JMSContext.CLIENT_ACKNOWLEDGE may not be used.
   * <p>
   * Note: The final value type is <b>boolean</b> type; in order to support configurability, the
   * string is used as the value type of annotation property, and the value will eventually be
   * converted to boolean type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to boolean value.
   *
   * @see JMSContext#AUTO_ACKNOWLEDGE
   * @see JMSContext#CLIENT_ACKNOWLEDGE
   * @see JMSContext#SESSION_TRANSACTED
   * @see JMSContext#DUPS_OK_ACKNOWLEDGE
   *
   * @return sessionMode
   */
  String dupsOkAck() default "false";
}
