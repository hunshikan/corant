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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.compare;
import java.beans.Transient;
import java.io.Serializable;
import java.util.Map;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 上午10:29:13
 *
 */
public interface Tuple {

  /**
   * corant-shared
   *
   * @author bingo 上午10:37:41
   *
   */
  public static class Pair<L, R> implements Map.Entry<L, R>, Serializable {

    private static final long serialVersionUID = -474294448204498274L;

    @SuppressWarnings("rawtypes")
    static final Pair emptyInstance = new Pair();

    private final L left;
    private final R right;

    protected Pair() {
      this(null, null);
    }

    protected Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }

    @SuppressWarnings("unchecked")
    public static <L, R> Pair<L, R> empty() {
      return emptyInstance;
    }

    public static <L, R> Pair<L, R> of(final L left, final R right) {
      return new Pair<>(left, right);
    }

    public static <L, R> Pair<L, R> of(final Map.Entry<L, R> entry) {
      return new Pair<>(entry.getKey(), entry.getValue());
    }

    public String asString(final String format) {
      return String.format(format, getLeft(), getRight());
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Map.Entry<?, ?>) {
        final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
        return Objects.areEqual(getKey(), other.getKey())
            && Objects.areEqual(getValue(), other.getValue());
      }
      return false;
    }

    @Override
    @Transient
    public L getKey() {
      return getLeft();
    }

    public L getLeft() {
      return left;
    }

    public R getRight() {
      return right;
    }

    @Override
    @Transient
    public R getValue() {
      return getRight();
    }

    @Override
    public int hashCode() {
      return (getKey() == null ? 0 : getKey().hashCode())
          ^ (getValue() == null ? 0 : getValue().hashCode());
    }

    public boolean isEmpty() {
      return left == null && right == null;
    }

    @Override
    public R setValue(R value) {
      throw new NotSupportedException();
    }

    @Override
    public String toString() {
      return asString("[%s,%s]");
    }

    public Pair<L, R> withKey(final L key) {
      return new Pair<>(key, getValue());
    }

    public Pair<L, R> withLeft(final L left) {
      return new Pair<>(left, getRight());
    }

    public Pair<L, R> withRight(final R right) {
      return new Pair<>(getLeft(), right);
    }

    public Pair<L, R> withValue(final R value) {
      return new Pair<>(getKey(), value);
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 20:26:59
   *
   */
  public static class Range<T extends Comparable<T>> {

    private final T start;
    private final T end;

    protected Range(T start, T end) {
      super();
      this.start = start;
      this.end = end;
    }

    public static <T extends Comparable<T>> Range<T> of(T start, T end) {
      shouldBeTrue(compare(start, end) <= 0, IllegalArgumentException::new);
      return new Range<>(start, end);
    }

    @SuppressWarnings("rawtypes")
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
      Range other = (Range) obj;
      if (end == null) {
        if (other.end != null) {
          return false;
        }
      } else if (!end.equals(other.end)) {
        return false;
      }
      if (start == null) {
        if (other.start != null) {
          return false;
        }
      } else if (!start.equals(other.start)) {
        return false;
      }
      return true;
    }

    public T getEnd() {
      return end;
    }

    public T getStart() {
      return start;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (end == null ? 0 : end.hashCode());
      result = prime * result + (start == null ? 0 : start.hashCode());
      return result;
    }

    public boolean isConflict(Range<T> other) {
      return equalOrAfter(start, other.getStart()) && equalOrBefore(start, other.getEnd())
          || equalOrBefore(start, other.getStart()) && equalOrAfter(end, other.getEnd())
          || equalOrAfter(end, other.getStart()) && equalOrBefore(end, other.getEnd());
    }

    public boolean isCover(Range<T> other) {
      return equalOrBefore(start, other.getStart()) && equalOrAfter(end, other.getEnd());
    }

    public boolean isCover(T value) {
      return equalOrBefore(start, value) && equalOrAfter(end, value);
    }

    public boolean same(Range<T> other) {
      if (other == null) {
        return false;
      } else if (this.equals(other)) {
        return true;
      } else {
        return compare(start, other.start) == 0 && compare(end, other.end) == 0;
      }
    }

    public Range<T> withEnd(T end) {
      return of(start, end);
    }

    public Range<T> withStart(T start) {
      return of(start, end);
    }

    private boolean equalOrAfter(T d1, T d2) {
      return compare(d1, d2) >= 0;
    }

    private boolean equalOrBefore(T d1, T d2) {
      return compare(d1, d2) <= 0;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:37:46
   *
   */
  public static class Triple<L, M, R> implements Serializable {

    private static final long serialVersionUID = 6441751980847755625L;

    @SuppressWarnings("rawtypes")
    static final Triple emptyInstance = new Triple();

    private final L left;
    private final M middle;
    private final R right;

    protected Triple() {
      this(null, null, null);
    }

    protected Triple(final L left, final M middle, final R right) {
      this.left = left;
      this.middle = middle;
      this.right = right;
    }

    @SuppressWarnings("unchecked")
    public static <L, M, R> Triple<L, M, R> empty() {
      return emptyInstance;
    }

    public static <L, M, R> Triple<L, M, R> of(final L left, final M middle, final R right) {
      return new Triple<>(left, middle, right);
    }

    public String asString(final String format) {
      return String.format(format, getLeft(), getMiddle(), getRight());
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Triple<?, ?, ?>) {
        final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
        return Objects.areEqual(getLeft(), other.getLeft())
            && Objects.areEqual(getMiddle(), other.getMiddle())
            && Objects.areEqual(getRight(), other.getRight());
      }
      return false;
    }

    public L getLeft() {
      return left;
    }

    public M getMiddle() {
      return middle;
    }

    public R getRight() {
      return right;
    }

    @Override
    public int hashCode() {
      return (getLeft() == null ? 0 : getLeft().hashCode())
          ^ (getMiddle() == null ? 0 : getMiddle().hashCode())
          ^ (getRight() == null ? 0 : getRight().hashCode());
    }

    public boolean isEmpty() {
      return left == null && middle == null && right == null;
    }

    @Override
    public String toString() {
      return asString("[%s,%s,%s]");
    }

    public Triple<L, M, R> withLeft(final L left) {
      return new Triple<>(left, getMiddle(), getRight());
    }

    public Triple<L, M, R> withMiddle(final M middle) {
      return new Triple<>(getLeft(), middle, getRight());
    }

    public Triple<L, M, R> withRight(final R right) {
      return new Triple<>(getLeft(), getMiddle(), right);
    }
  }
}
