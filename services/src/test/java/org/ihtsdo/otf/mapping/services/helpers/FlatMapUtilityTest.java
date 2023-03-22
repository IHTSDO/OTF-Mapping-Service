package org.ihtsdo.otf.mapping.services.helpers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class FlatMapUtilityTest {

  @Test
  public void testFlattenSimple() {
    // Test with a simple map with only primitive values
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("string", "value");
    map.put("integer", 1);
    map.put("decimal", 10.0);
    final Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("/string", "value");
    expected.put("/integer", 1);
    expected.put("/decimal", 10.0);
    assertEquals(expected, FlatMapUtility.flatten(map));
  }

  @Test
  public void testFlattenNested() {
    // Test with a nested map with primitive and nested maps
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("string", "value");
    final Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("integer", 1);
    nested.put("decimal", 10.0);
    nested.put("string-list", Map.of("one", "two", "three", "four"));
    map.put("nested", nested);
    final Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("/string", "value");
    expected.put("/nested/integer", 1);
    expected.put("/nested/decimal", 10.0);
    expected.put("/nested/string-list/one", "two");
    expected.put("/nested/string-list/three", "four");
    assertEquals(expected, FlatMapUtility.flatten(map));
  }

  @Test
  public void testFlattenList() {
    // Test with a list of primitive values
    Map<String, Object> map = new LinkedHashMap<>();
    List<String> list = new ArrayList<>();
    list.add("one");
    list.add("two");
    list.add("three");
    map.put("list", list);
    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("/list/0", "one");
    expected.put("/list/1", "two");
    expected.put("/list/2", "three");
    assertEquals(expected, FlatMapUtility.flatten(map));
  }

  @Test
  public void testFlattenListNested() {
    // Test with a list of nested maps
    final Map<String, Object> map = new LinkedHashMap<>();
    final List<Map<String, Object>> list = new ArrayList<>();
    final Map<String, Object> map1 = new LinkedHashMap<>();
    map1.put("string", "value");
    map1.put("integer", 1);
    final Map<String, Object> map2 = new LinkedHashMap<>();
    map2.put("string", "value2");
    map2.put("integer", 2);
    list.add(map1);
    list.add(map2);
    map.put("list", list);
    final Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("/list/0/string", "value");
    expected.put("/list/0/integer", 1);
    expected.put("/list/1/string", "value2");
    expected.put("/list/1/integer", 2);
    assertEquals(expected, FlatMapUtility.flatten(map));
  }

}
