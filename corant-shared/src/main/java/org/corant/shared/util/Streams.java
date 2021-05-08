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
package org.corant.shared.util;

import static org.corant.shared.normal.Defaults.MAX_BUFFERED_BYTES;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.emptyConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * corant-shared
 *
 * @author bingo 2017年4月7日
 */
public class Streams {

  public static final int DFLT_BATCH_SIZE = 64;

  private Streams() {}

  /**
   * Receive stream object converts it to a list stream, use stream grouping.
   *
   * @param <T> the element type
   * @param batchSize the batch size
   * @param source the source
   * @return the list stream
   */
  public static <T> Stream<List<T>> batchCollectStream(int batchSize, Stream<T> source) {
    final int useBatchSize = batchSize < 0 ? DFLT_BATCH_SIZE : batchSize;
    final AtomicInteger counter = new AtomicInteger();
    return shouldNotNull(source)
        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / useBatchSize)).values()
        .stream();
  }

  /**
   * Receive iterable object converts it to a list stream, use for batch processing.
   *
   * NOTE : parallel not support
   *
   * @param <T> the element type
   * @param batchSize the batch size
   * @param source the source
   * @return the list stream
   */
  public static <T> Stream<List<T>> batchStream(int batchSize, Iterable<? extends T> source) {
    return batchStream(batchSize, shouldNotNull(source).iterator());
  }

  /**
   * Receive iterator object converts it to a list stream, use for batch processing.
   *
   * NOTE : parallel not support
   *
   * @param <T> the element type
   * @param batchSize the batch size
   * @param it the source
   * @return the list stream
   */
  public static <T> Stream<List<T>> batchStream(int batchSize, Iterator<? extends T> it) {

    return streamOf(new Iterator<List<T>>() {

      final int useBatchSize = batchSize < 0 ? DFLT_BATCH_SIZE : batchSize;
      final Iterator<? extends T> useIt = shouldNotNull(it);
      final List<T> buffer = new ArrayList<>(useBatchSize);
      boolean end = false;

      @Override
      public boolean hasNext() {
        if (end) {
          return false;
        }
        if (isNotEmpty(buffer)) {
          return true;
        }
        int i = 0;
        while (useIt.hasNext()) {
          buffer.add(useIt.next());
          if (++i == useBatchSize) {
            break;
          }
        }
        end = i != useBatchSize;
        return i > 0;
      }

      @Override
      public List<T> next() {
        if (isNotEmpty(buffer) || hasNext()) {
          List<T> list = new ArrayList<>(buffer);
          buffer.clear();
          return list;
        } else {
          throw new NoSuchElementException();
        }
      }
    });
  }

  /**
   * Receive stream object converts it to a list stream, use for batch processing.
   *
   * NOTE : parallel not support
   *
   * @param <T> the element type
   * @param batchSize the batch size
   * @param source the source
   * @return the list stream
   */
  public static <T> Stream<List<T>> batchStream(int batchSize, Stream<? extends T> source) {
    return batchStream(batchSize, shouldNotNull(source).iterator());
  }

  /**
   * Copy the given input stream to the given output stream without closing the streams.
   *
   * @param input the input stream
   * @param output the output stream
   * @return the bytes length
   * @throws IOException If I/O errors occur
   */
  public static long copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[4096];
    long count;
    int n;
    for (count = 0L; -1 != (n = input.read(buffer)); count += n) {
      output.write(buffer, 0, n);
    }
    return count;
  }

  /**
   * Convert an Iterable object to parallel Stream object
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<T> parallelStreamOf(final Iterable<? extends T> iterable) {
    if (iterable != null) {
      return StreamSupport.stream((Spliterator<T>) iterable.spliterator(), true);
    }
    return Stream.empty();
  }

  /**
   * Convert Iterator object to parallel Stream object
   */
  public static <T> Stream<T> parallelStreamOf(final Iterator<? extends T> iterator) {
    if (iterator != null) {
      return StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), true);
    }
    return Stream.empty();
  }

  /**
   * Read the input stream to byte array without closing the input stream.
   *
   * @param is the input stream for reading
   * @return the bytes
   * @throws IOException If I/O errors occur
   */
  public static byte[] readAllBytes(InputStream is) throws IOException {
    byte[] buf = new byte[4096];
    int capacity = buf.length;
    int nread = 0;
    int n;
    for (;;) {
      while ((n = is.read(buf, nread, capacity - nread)) > 0) {
        nread += n;
      }
      // returned -1, done
      if (n < 0) {
        break;
      }
      if (capacity <= MAX_BUFFERED_BYTES - capacity) {
        capacity = capacity << 1;
      } else {
        if (capacity == MAX_BUFFERED_BYTES) {
          throw new OutOfMemoryError("Required array size too large");
        }
        capacity = MAX_BUFFERED_BYTES;
      }
      buf = Arrays.copyOf(buf, capacity);
    }
    return capacity == nread ? buf : Arrays.copyOf(buf, nread);
  }

  /**
   * Convert Enumeration object to Stream object
   *
   * @param <T> the element type
   * @param enumeration the source
   * @return the enumeration object stream
   */
  public static <T> Stream<T> streamOf(final Enumeration<? extends T> enumeration) {
    if (enumeration != null) {
      return streamOf(new Iterator<T>() {
        @Override
        public boolean hasNext() {
          return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
          if (enumeration.hasMoreElements()) {
            return enumeration.nextElement();
          }
          throw new NoSuchElementException();
        }
      });
    }
    return Stream.empty();
  }

  /**
   * Convert Iterable object to Stream object
   *
   * @param <T> the element type
   * @param iterable the source
   * @return the object stream
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> Stream<T> streamOf(final Iterable<? extends T> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection) iterable).stream();
    } else if (iterable != null) {
      return StreamSupport.stream((Spliterator<T>) iterable.spliterator(), false);
    }
    return Stream.empty();
  }

  /**
   * Convert Iterator object to Stream object
   *
   * @param <T> the element type
   * @param iterator the source
   * @return the object stream
   */
  public static <T> Stream<T> streamOf(final Iterator<? extends T> iterator) {
    if (iterator != null) {
      return StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
    return Stream.empty();
  }

  /**
   * Convert the entries of the given map to Stream or {@link Stream#empty()} if given map is null.
   *
   * @param <K> the type of entry key
   * @param <V> the type of entry value
   * @param map the source
   * @return then entries stream
   */
  public static <K, V> Stream<Map.Entry<K, V>> streamOf(Map<K, V> map) {
    if (map != null) {
      return map.entrySet().stream();
    }
    return Stream.empty();
  }

  /**
   * Convert object array to Stream
   *
   * @param <T> the element type
   * @param objects the source
   * @return the object stream
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<T> streamOf(T... objects) {
    return Arrays.stream(objects);
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:39:59
   *
   */
  public abstract static class AbstractBatchHandlerSpliterator<T> extends AbstractSpliterator<T> {

    private final int batchSize;
    private final Consumer<Long> handler;

    protected AbstractBatchHandlerSpliterator(long est, int additionalCharacteristics,
        int forEachBathSize, Consumer<Long> handler) {
      super(est, additionalCharacteristics);
      this.batchSize = forEachBathSize;
      this.handler = handler == null ? emptyConsumer() : handler;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
      long j = 0;
      do {
        if (j % this.batchSize == 0 && j > 0) {
          this.handler.accept(j);
        }
        j++;
      } while (tryAdvance(action));
      this.handler.accept(j);
    }

    @Override
    public Comparator<? super T> getComparator() {
      if (hasCharacteristics(SORTED)) {
        return null;
      }
      throw new IllegalStateException();
    }

  }

}
