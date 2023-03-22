/*
 *    Copyright 2023 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The Class FlatMapUtility.
 */
public final class FlatMapUtility {

  /**
   * Instantiates an empty {@link FlatMapUtility}.
   */
  private FlatMapUtility() {
    throw new AssertionError("Cannot instantiate.");
  }

  /**
   * Flatten.
   *
   * @param map the map
   * @return the map
   */
  public static Map<String, Object> flatten(Map<String, Object> map) {
    return map.entrySet().stream().flatMap(FlatMapUtility::flatten).collect(
        LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()),
        LinkedHashMap::putAll);
  }

  /**
   * Flatten.
   *
   * @param entry the entry
   * @return the stream
   */
  private static Stream<Map.Entry<String, Object>> flatten(
    Map.Entry<String, Object> entry) {

    if (entry == null) {
      return Stream.empty();
    }

    if (entry.getValue() instanceof Map<?, ?>) {
      return ((Map<?, ?>) entry.getValue()).entrySet().stream()
          .flatMap(e -> flatten(new AbstractMap.SimpleEntry<>(
              entry.getKey() + "/" + e.getKey(), e.getValue())));
    }

    if (entry.getValue() instanceof List<?>) {
      List<?> list = (List<?>) entry.getValue();
      return IntStream.range(0, list.size())
          .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(
              entry.getKey() + "/" + i, list.get(i)))
          .flatMap(FlatMapUtility::flatten);
    }

    return Stream.of(entry);
  }
}
