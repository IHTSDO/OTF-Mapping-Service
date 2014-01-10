package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter extends XmlAdapter<MapAdapter.AdaptedMap, Map<String, String>> {
	
	public static class AdaptedMap {
		public List<Entry> entries = new ArrayList<Entry>(); 
		
		public void sort() {
			Collections.sort(entries, new Comparator<Entry>() {
				@Override
				public int compare(Entry e1, Entry e2) {
					return e1.key.compareTo(e2.key);
				}
			});
		}
	}
	
	public static class Entry {
		public String key;
		public String value;		
	}
	
	@Override
	public Map<String, String> unmarshal(AdaptedMap adaptedMap) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
        for(Entry entry : adaptedMap.entries) {
            map.put(entry.key, entry.value);
        }
        return map;
	}
	
	@Override
	public AdaptedMap marshal(Map<String, String> map) throws Exception {
		AdaptedMap adaptedMap = new AdaptedMap();
        for(Map.Entry<String, String> mapEntry : map.entrySet()) {
            Entry entry = new Entry();
            entry.key = mapEntry.getKey();
            entry.value = mapEntry.getValue();
            adaptedMap.entries.add(entry);
        }
        adaptedMap.sort();
        
        return adaptedMap;
	}
	
	
	

}
