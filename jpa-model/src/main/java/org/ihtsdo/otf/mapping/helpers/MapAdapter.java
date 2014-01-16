package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter extends XmlAdapter<MapAdapter.Entries, Map<Object, Object>> {
        
		
        public static class Entries {
                public List<Entry> entry = new ArrayList<Entry>();
        }
        
        public static class Entry {
                public Object key;
                public Object value;                
        }
        
        @Override
        public Map<Object, Object> unmarshal(Entries entries) throws Exception {
                Map<Object, Object> map = new HashMap<Object, Object>();
	        for(Entry entry : entries.entry) {
	            map.put(entry.key, entry.value);
	        }
	        return map;
        }
        
        @Override
        public Entries marshal(Map<Object, Object> map) throws Exception {
                Entries entries = new Entries();
                
		        for(Map.Entry<Object, Object> mapEntry : map.entrySet()) {
		            Entry entry = new Entry();
		            entry.key = mapEntry.getKey();
		            
		            // if entry is itself a map
		            if (mapEntry.getValue() instanceof Map){
		            	
		            	System.out.println("map detected");
		            	
		            	Entries nestedEntries = new Entries();
		            	            	
		            	for (Map.Entry<Object, Object> nestedMapEntry : ((Map<Object, Object>) mapEntry.getValue()).entrySet()) {
		            		
		            		System.out.println(nestedMapEntry.toString());
		            		
		            		Entry nestedEntry = new Entry();
		            		nestedEntry.key = nestedMapEntry.getKey();
		            		nestedEntry.value = nestedMapEntry.getValue();
		            
		            		nestedEntries.entry.add(nestedEntry);
		            	}  	
		            	
		            	System.out.println("Adding to nested adapted map");
		            	entry.value = nestedEntries;
		       	
		            } else  {
		            	entry.value = mapEntry.getValue();
		            } 
		            
		            
		            entries.entry.add(entry);
		        }
        
        return entries;
        }
}