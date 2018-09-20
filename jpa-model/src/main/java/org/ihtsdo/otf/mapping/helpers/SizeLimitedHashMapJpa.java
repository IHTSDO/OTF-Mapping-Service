package org.ihtsdo.otf.mapping.helpers;

import java.util.LinkedHashMap;
import java.util.Map;

public class SizeLimitedHashMapJpa<K,V> extends LinkedHashMap<K,V> {
  
  private int MAX_ENTRIES = 5;

  public SizeLimitedHashMapJpa(int maxSize) {
    super();
    MAX_ENTRIES = maxSize;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry eldest) {
      return size() > MAX_ENTRIES;
  }

}
