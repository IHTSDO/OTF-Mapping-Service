package org.ihtsdo.otf.mapping.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.ihtsdo.otf.mapping.helpers.MapAdapter;
import org.ihtsdo.otf.mapping.model.MapXmlTest;

@XmlRootElement(name = "mapTest")
public class MapXmlTestJpa implements MapXmlTest {

	Map<String, String> map = new HashMap<String, String>();
	
	public MapXmlTestJpa() {
		
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");
	}

	@XmlJavaTypeAdapter(MapAdapter.class)
	@XmlElement(name = "map")
	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}
}
