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
package org.corant.asosat.ddd.domain.shared;

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.asosat.ddd.domain.model.AbstractValueObject;

@Embeddable
@MappedSuperclass
public class WeightInfo extends AbstractValueObject implements Comparable<WeightInfo> {

  private static final long serialVersionUID = 6145301734319644562L;

  static final WeightInfo WI0 = new WeightInfo(BigDecimal.ZERO, MeasureUnit.KG);

  @Column
  private BigDecimal weight;

  @Column
  private MeasureUnit unit;

  /**
   * @param weight
   * @param unit
   */
  public WeightInfo(BigDecimal weight, MeasureUnit unit) {
    super();
    this.weight = weight;
    this.unit = unit;
  }

  protected WeightInfo() {
    super();
  }

  public static WeightInfo of(BigDecimal weight, MeasureUnit unit) {
    return new WeightInfo(weight, unit);
  }

  @Override
  public int compareTo(WeightInfo o) {
    return defaultObject(weight, BigDecimal.ZERO)
        .compareTo(defaultObject(o.weight, BigDecimal.ZERO));
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
    WeightInfo other = (WeightInfo) obj;
    if (unit != other.unit) {
      return false;
    }
    if (weight == null) {
      if (other.weight != null) {
        return false;
      }
    } else if (!weight.equals(other.weight)) {
      return false;
    }
    return true;
  }

  public MeasureUnit getUnit() {
    return unit;
  }

  public BigDecimal getWeight() {
    return weight;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (unit == null ? 0 : unit.hashCode());
    result = prime * result + (weight == null ? 0 : weight.hashCode());
    return result;
  }

  protected void setUnit(MeasureUnit unit) {
    this.unit = unit;
  }

  protected void setWeight(BigDecimal weight) {
    this.weight = weight;
  }

}
