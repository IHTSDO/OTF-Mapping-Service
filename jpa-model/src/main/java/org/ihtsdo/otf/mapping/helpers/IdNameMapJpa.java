package org.ihtsdo.otf.mapping.helpers;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;


/**
 * Container for map entrys.
 *
 * @author ${author}
 */
@XmlRootElement(name = "idNameMap")
public class IdNameMapJpa implements IdNameMap {


	/** The entries. */
	private Map<Long, String> entries = new HashMap<Long, String>();
	
	/** The name. */
	private String name = "";

	/**
	 * Instantiates a new id name map.
	 */
	public IdNameMapJpa() {
		// do nothing
	}

	/**
	 * Gets the IdNameMap.
	 * 
	 * @return the id name map
	 */
	@Override
	public Map<Long, String> getIdNameMap() {
		return entries;
	}

	/**
	 * Sets the IdNameMap.
	 * 
	 * @param idNameMap
	 *            the new id name map
	 */
	@Override
	public void setIdNameMap(Map<Long, String> idNameMap) {
		this.entries = idNameMap;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#addIdNameMapEntry(java.lang.Long, java.lang.String)
	 */
	@Override
	public void addIdNameMapEntry(Long id, String name) {
		entries.put(id,  name);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;	
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entries == null) ? 0 : entries.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdNameMapJpa other = (IdNameMapJpa) obj;
		if (entries == null) {
			if (other.entries != null)
				return false;
		} else if (!entries.equals(other.entries))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IdNameMapJpa [entries=" + entries + ", name=" + name + "]";
	}

}
