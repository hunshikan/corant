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
package org.corant.asosat.exp.interfaces;

import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.MapUtils.getMapString;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import org.corant.asosat.exp.application.TestApplicationService3;
import org.corant.asosat.exp.application.TestQueryService;
import org.corant.shared.conversion.Conversions;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.util.ConversionUtils;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午8:18:27
 *
 */
@ApplicationScoped
@WebServlet(urlPatterns = "/test")
public class TestServlet extends HttpServlet {

  private static final long serialVersionUID = 8174294172579816895L;

  @Inject
  TestApplicationService3 as;

  @Inject
  TestQueryService qs;

  // @Inject
  // @PersistenceContext(unitName = "dmmsPu", type =
  // PersistenceContextType.EXTENDED)
  // EntityManager em;

  @Transactional
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    as.testRepo(req.getParameter("ok"));
    StringBuilder sb = new StringBuilder("<table>");
    List<Map<String, Object>> list = qs.select("Test.query", null);
    list.forEach(m -> {
      sb.append("<tr><td>").append(getMapString(m, "name")).append("</td></tr>");
    });
    sb.append("</table>");
    System.out.println(ConversionUtils.toEnum("ENABLED", Lifecycle.class));
    System.out.println(ConversionUtils.toBigDecimal("12312.12"));
    System.out.println(Conversions.convert(Instant.now(), LocalDate.class,
        asMap(ConverterHints.CVT_ZONE_ID_KEY, ZoneId.systemDefault())));
    resp.getWriter().write(sb.toString());
  }

}
