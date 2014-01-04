package org.ihtsdo.otf.mapping.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;

/**
 * Container for map entrys.
 */
@XmlRootElement(name = "IdNameMap")
public class IdNameMap {


	private Map<Long, String> idNameMap = new HashMap<Long, String>();

	/**
	 * Instantiates a new id name map.
	 */
	public IdNameMap() {
		// do nothing
	}

	/**
	 * Gets the map entrys.
	 * 
	 * @return the map entrys
	 */
	//@XmlElement(type=MapEntryJpa.class, name="MapEntry")
	public Map<Long, String> getIdNameMap() {
		return idNameMap;
	}

	/**
	 * Sets the IdNameMap.
	 * 
	 * @param jpaMetadataResults
	 *            the new id name map
	 */
	public void setIdNameMap(Map<Long, String> jpaMetadataResults) {
		this.idNameMap = jpaMetadataResults;
	}
	

	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the map
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return idNameMap.size();
	}

}
